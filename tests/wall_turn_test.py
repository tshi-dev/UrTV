#!/usr/bin/env python3
"""4.4 beta 4 (Xiaomi A14): the home's photo moved on at every return from an app. A return (onStart after a trim, onResume) showed the next photo
whenever the photos had been let go (a trim) or the home process was new, and the interval was measured from the process's own clock. Now a
return shows the photo it left (returnWallpaper: the same index, decoded again if it was let go) and the photo moves on only on the timer, or
on a return after the whole interval, measured on the wall clock kept in the prefs. WallTurn (no Android) on cases, then the wiring.
Run: python3 tests/wall_turn_test.py"""
import re
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

main = (H / "MainActivity.java").read_text(encoding="utf-8")
def body(sig):
    i = main.index(sig); j = main.index("\n    }\n", i); return main[i:j]
start, resume, ret, nxt = body("protected void onStart()"), body("protected void onResume()"), body("private void returnWallpaper()"), body("private void nextWallpaper()")
check("onStart after a trim: the return's photo (returnWallpaper), not the next one", "returnWallpaper();" in start and "nextWallpaper();" not in start)
check("onResume: the return's photo, due by WallTurn on the kept wall-clock time (not the process's own clock)",
      'WallTurn.due(prefs().getLong("wallpaperAt", 0), System.currentTimeMillis(), WALLPAPER_INTERVAL_MS)' in resume and "returnWallpaper();" in resume
      and "nextWallpaper();" not in resume and "elapsedRealtime() - wallChangedAt" not in resume)
check("onResume within the interval: the timer gets the rest of it", 'scheduleWallpaper(WallTurn.left(prefs().getLong("wallpaperAt", 0), System.currentTimeMillis(), WALLPAPER_INTERVAL_MS));' in resume)
check("returnWallpaper: due -> the next photo; else the same one (wallStay) and the rest of the interval on the timer",
      "if (WallTurn.due(at, now, WALLPAPER_INTERVAL_MS)) { nextWallpaper(); return; }" in ret and ret.index("wallStay = true;") < ret.index("nextWallpaper();", ret.index("wallStay = true;"))
      and "scheduleWallpaper(WallTurn.left(at, now, WALLPAPER_INTERVAL_MS));" in ret)
check("nextWallpaper: a stay keeps the index (and writes no new time); a turn moves it and starts the interval",
      "if (!stay || wallIndex < 0) {" in nxt and "} else wallIndex %= total * 4096;" in nxt
      and 'if (!stay) prefs().edit().putLong("wallpaperAt", System.currentTimeMillis()).apply();' in nxt
      and nxt.index("final boolean stay = wallStay;") < nxt.index("new Thread("))
check("the timer itself still moves the photo on", "private final Runnable rotateWallpaper = this::nextWallpaper;" in main)

if not shutil.which("javac"):
    print("SKIP wall turn: no javac here")
    sys.exit(1 if failed else 0)
IV = 600_000
T = 1_765_000_000_000
CASES = [
    ("never chosen: due, a whole interval", f"0,{T},{IV}", f"1 {IV}"),
    ("back after 30 s: not due, 9.5 min left", f"{T},{T + 30_000},{IV}", f"0 {IV - 30_000}"),
    ("back after 9 min 59 s: not due", f"{T},{T + IV - 1000},{IV}", "0 1000"),
    ("back after 10 min: due", f"{T},{T + IV},{IV}", "1 0"),
    ("back after an hour: due", f"{T},{T + 3_600_000},{IV}", "1 0"),
    ("the clock went back: due once, a whole interval", f"{T},{T - 5_000},{IV}", f"1 {IV}"),
]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(H / "WallTurn.java"), str(ROOT / "tests/WallTurnRun.java")], capture_output=True, text=True)
    check("WallTurn compiles alone", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.WallTurnRun", *[c[1] for c in CASES]], capture_output=True, text=True).stdout.strip().split("\n")
        for (name, _, want), got in zip(CASES, out + [""] * len(CASES)):
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
