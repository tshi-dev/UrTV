#!/usr/bin/env python3
"""Hotfix 4.3: after a cold start the favourites row showed only five apps (two users, 4.2 and 4.3). showApps() found fewer of the chosen favourites than were saved (the system's app list was not whole
yet) and topped the row up to five; it came back only when the home built the row again. Now it is built again 3 s, 10 s, 30 s and 60 s later while favourites are missing, and the saved list is never touched.
    python3 tests/dock_retry_test.py"""
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
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


main = (H / "MainActivity.java").read_text(encoding="utf-8")
show = main.split("private void showApps(")[1].split("dockRow.removeAllViews();")[0]
check("showApps counts the chosen favourites it found and builds the row again soon when some are missing, before the dock is topped up to the minimum",
      "DockRetry.next(dockRetries, wanted.length, dock.size())" in show and "handler.postDelayed(dockRetry, again)" in show and show.index("DockRetry.next") < show.index("AppsEdit.topUp"))
check("the retry is a plain buildApps() (the saved list is never written by it), skipped once the home is gone", "private final Runnable dockRetry = () -> { if (!isDestroyed() && !isFinishing()) buildApps(); };" in main
      and "dockApps = installed.toArray" not in show)
check("the retries are counted per start of the home (a cold start), logged under UrTVHome", "private int dockRetries;" in main and 'favourites: " + dock.size() + " of " + wanted.length + " found' in main)
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "DockRetry.java"), str(ROOT / "tests/DockRetryRun.java")], capture_output=True, text=True)
        check("DockRetry compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.DockRetryRun"], capture_output=True, text=True, timeout=60).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            for n, v in lines:
                check(n, v == "ok")
            check("every case ran", len(lines) == 4, out)
else:
    check("java is installed", False)
sys.exit(1 if failed else 0)
