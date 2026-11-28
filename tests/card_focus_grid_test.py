#!/usr/bin/env python3
"""4.4 beta 4, two reports on the home's look:
1. The matches card (the football home's widget cards) looked smaller with the focus: its focus was an inset ring 3 units into every side. The
   owner's focus is the size and a shadow: now a 3 % scale and a deeper drop shadow, no ring (tests/fb_focus.test.mjs in a browser).
2. The «كل التطبيقات» grid was cut at the top: a grid of any height showed the row above the focused one cut once it scrolled. Its height is now
   whole rows of the room it has inside the 90 % safe area's panel (GridFit), no faded edge; the rest of the room stays below it.
Run: python3 tests/card_focus_grid_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

for page in ("src/template.html", "dist/tv.html"):
    s = (ROOT / page).read_text(encoding="utf-8")
    check(f"{page}: the football widget's focus is a scale and a shadow, no inset ring", ".fbw.f .fbc{transform:scale(1.03);box-shadow:inset 0 1px 0 var(--fhl),0 22px 44px rgba(0,0,0,.42)}" in s
          and ".fbw.f .fbc{box-shadow:inset 0 0 0" not in s)
aa = (H / "AllAppsActivity.java").read_text(encoding="utf-8")
check("«كل التطبيقات»: the panel stays inside the 90 % safe area (78 % x 86 %, centred)", "Math.round(dm.widthPixels * 0.78f), Math.round(dm.heightPixels * 0.86f), Gravity.CENTER" in aa)
check("«كل التطبيقات»: once a row is laid out the grid takes whole rows (GridFit), the rest below it, once", "GridFit.height(room, grid.getChildAt(0).getHeight(), dp(10), grid.getPaddingTop(), grid.getPaddingBottom())" in aa
      and "grid.removeOnLayoutChangeListener(this);" in aa and "gp.bottomMargin = room - h;" in aa and "grid.setVerticalSpacing(dp(10));" in aa)
check("«كل التطبيقات»: no faded top row", "grid.setVerticalFadingEdgeEnabled(false);" in aa)
check("tests/run.sh runs the browser half", "node tests/fb_focus.test.mjs" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP grid fit: no javac here")
    sys.exit(1 if failed else 0)
CASES = [
    ("room for 3.6 rows: 3 whole rows", "500,120,20,12,12", str(3 * 120 + 2 * 20 + 24)),
    ("room for exactly 4 rows: all of it", str(4 * 120 + 3 * 20 + 24) + ",120,20,12,12", str(4 * 120 + 3 * 20 + 24)),
    ("room for less than one row: one row, never more than the room", "100,120,20,12,12", "100"),
    ("no row laid out yet: the room as it is", "500,0,20,12,12", "500"),
]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(H / "GridFit.java"), str(ROOT / "tests/GridFitRun.java")], capture_output=True, text=True)
    check("GridFit compiles alone", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.GridFitRun", *[c[1] for c in CASES]], capture_output=True, text=True).stdout.split()
        for (name, _, want), got in zip(CASES, out + [""] * len(CASES)):
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
