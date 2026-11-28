#!/usr/bin/env python3
"""R-451 (a report: «the focus jumps and another app opens»). The home builds its favourites and its grid again on a package update, on the favourites'
retry after a start (DockRetry: up to four times in the first 100 s), on a look switch. Every tile gets a new view id at each build, so the restore by
the old id never found it and the focus fell to the first favourite, where the person's next OK opened another app. Now the focused app is kept by its
package (FocusKeep, run on a plain JVM) and given the focus back once its tile exists. Needs javac for the JVM part.

    python3 tests/home_focus_keep_test.py
"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
show = main[main.index("    private void showApps(List<App> all, String[] wanted, int gen) {"):]
show = show[:show.index("\n    }\n")]
check("showApps reads the focused app (by package) before it empties the bar and the grid",
      "FocusKeep.keep(keyedSinceHome, focusedPkg(), focusPkgAfter)" in show
      and show.index("FocusKeep.keep(") < show.index("cardByPkg.clear();") < show.index("dockRow.removeAllViews();"))
check("showApps gives it back by package (focusPkgAfter, applyPendingFocus after the bar is built)",
      "focusPkgAfter = keepPkg;" in show and show.index("focusPkgAfter = keepPkg;") < show.index("applyPendingFocus();"))
check("the first favourite takes the focus only when no app is kept, or the kept one is in the bar (no jump to the bar for a grid app)",
      "else if (!dockCards.isEmpty() && (keepPkg == null || cardByPkg.containsKey(keepPkg))) dockCards.get(0).requestFocus();" in show)
grid = main[main.index("    private void addGridRows("):]
grid = grid[:grid.index("\n    }\n")]
check("a grid app gets the focus back with its row; a kept app that is gone gives the first favourite, never no focus",
      "applyPendingFocus();" in grid and "if (focusPkgAfter != null && getCurrentFocus() == null && !dockCards.isEmpty()) dockCards.get(0).requestFocus();" in grid
      and grid.index("getCurrentFocus() == null") < grid.index("focusPkgAfter = null;   // not found"))
check("Home still puts the focus on the first app (keyedSinceHome false after Home)", "keyedSinceHome = false;   // Home: the first app" in main)

if not shutil.which("javac"):
    print("SKIP the JVM part: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "FocusKeep.java"), str(ROOT / "tests/FocusKeepRun.java")], capture_output=True, text=True)
    check("FocusKeep compiles on a plain JVM", r.returncode == 0, r.stderr)
    if r.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.FocusKeepRun"], capture_output=True, text=True)
        print(r.stdout, end="")
        failed += r.returncode != 0 or r.stdout.count("PASS ") != 4
sys.exit(1 if failed else 0)
