#!/usr/bin/env python3
"""4.4 beta 4: the still picture of the screensaver (SaverSnapshot, drawn at the start until the live page paints) stayed up to about 3 s, a frozen clock
and photo. It now gives way after 1.5 s at most (CoverWait.WAIT_MS), at once when the page has painted, and at once when the minute turns while it
is up (the picture shows the clock of its minute). CoverWait (no Android) on cases, then the wiring in SaverStart and SaverSnapshot.
Run: python3 tests/cover_wait_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

start = (APP / "SaverStart.java").read_text(encoding="utf-8")
snap = (APP / "SaverSnapshot.java").read_text(encoding="utf-8")
check("SaverStart asks CoverWait with the page's state, the wait and the wall clock", "CoverWait.done(page.getAlpha() >= 1f, android.os.SystemClock.elapsedRealtime() - since, shownAt, System.currentTimeMillis())" in start)
check("the picture's longest wait is CoverWait's (one number)", "LIVE_WAIT_MS = (int) CoverWait.WAIT_MS" in snap)
if not shutil.which("javac"):
    print("SKIP cover wait: no javac here")
    sys.exit(1 if failed else 0)
M = 60_000
T = 1_765_000_020_000 // M * M + 5_000   # 5 s into a minute
CASES = [
    ("the page painted: at once", f"1,100,{T},{T + 100}", "1"),
    ("not painted, 1 s: the picture stays", f"0,1000,{T},{T + 1000}", "0"),
    ("not painted, 1.5 s: it gives way (was 3 s)", f"0,1500,{T},{T + 1500}", "1"),
    ("not painted, 2.9 s: long gone", f"0,2900,{T},{T + 2900}", "1"),
    ("the minute turned after 0.4 s: at once (its clock is the old minute's)", f"0,400,{T + M - 5_300},{T + M - 4_900}", "1"),
    ("the same minute, 0.4 s: stays", f"0,400,{T},{T + 400}", "0"),
]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(APP / "CoverWait.java"), str(ROOT / "tests/CoverWaitRun.java")], capture_output=True, text=True)
    check("CoverWait compiles alone", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.CoverWaitRun", *[c[1] for c in CASES]], capture_output=True, text=True).stdout.split()
        for (name, _, want), got in zip(CASES, out + [""] * len(CASES)):
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
