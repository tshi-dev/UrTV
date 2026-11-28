#!/usr/bin/env python3
"""4.4 beta 4 (Xiaomi A9, Skyworth): the return from an app to the home flickered or was blurred. On a tight TV the home lets its photos go behind
another app (onTrimMemory) and leaves a small stand-in; the return showed it, blurred, until the photo was decoded again and faded in over it.
Now the shown photo's own bitmap is kept through the light trims (WallKeep: below TRIM_MEMORY_MODERATE) and the return puts it back as it was
in onStart, before its first frame is drawn (showKept: no stand-in, no fade), unless the photo is due to move on (then the next one, as
before) or the scenes home shows. A trim at MODERATE or worse lets it go, as before. WallKeep (no Android) on cases, then the wiring.
Run: python3 tests/wall_keep_test.py"""
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
    i = main.index(sig); return main[i:main.index("\n    }\n", i)]
start, kept, trim, show = body("protected void onStart()"), body("private boolean showKept()"), body("public void onTrimMemory(int level)"), body("private void showWallpaper(Bitmap bm, String key)")
check("onStart after a trim: the kept photo first (before any frame of the return), else the return's photo as before", "if (wallTrimmed) { wallTrimmed = false; if (!showKept()) returnWallpaper(); }" in start)
check("showKept: only the same photo (not due), not over the scenes, then at once: no fade (alpha 1), the timer keeps the rest of the interval",
      "WallKeep.reuse(" in kept and "WallTurn.due(at, now, WALLPAPER_INTERVAL_MS), scenesOn)" in kept and "wallFront.setImageBitmap(b);" in kept
      and "wallFront.setAlpha(1f);" in kept and "scheduleWallpaper(WallTurn.left(at, now, WALLPAPER_INTERVAL_MS));" in kept and "animate().alpha" not in kept)
check("the trim keeps the bitmap only at a light level (WallKeep.keep), before the stand-in is made", "if (!WallKeep.keep(level)) { wallKept = null; wallKeptKey = null; }" in trim
      and trim.index("WallKeep.keep(level)") < trim.index("thinWallpaper();"))
check("every photo shown is the one kept", "wallKept = bm; wallKeptKey = key;" in show)
check("a kept bitmap that was recycled is never drawn", "b != null && !b.isRecycled()" in kept)

if not shutil.which("javac"):
    print("SKIP wall keep: no javac here")
    sys.exit(1 if failed else 0)
CASES = [
    ("TRIM_MEMORY_UI_HIDDEN (20, behind an app): kept", ["keep", "20"], "1"),
    ("TRIM_MEMORY_BACKGROUND (40): kept", ["keep", "40"], "1"),
    ("TRIM_MEMORY_MODERATE (60): let go, as before", ["keep", "60"], "0"),
    ("TRIM_MEMORY_COMPLETE (80): let go", ["keep", "80"], "0"),
    ("a kept photo, not due, no scenes: put back", ["reuse", "1", "0", "0"], "1"),
    ("due to move on: the next photo instead", ["reuse", "1", "1", "0"], "0"),
    ("the scenes home shows: nothing", ["reuse", "1", "0", "1"], "0"),
    ("nothing kept: decoded as before", ["reuse", "0", "0", "0"], "0"),
]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(H / "WallKeep.java"), str(ROOT / "tests/WallKeepRun.java")], capture_output=True, text=True)
    check("WallKeep compiles alone", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.WallKeepRun", *[x for c in CASES for x in c[1]]], capture_output=True, text=True).stdout.split()
        for (name, _, want), got in zip(CASES, out + [""] * len(CASES)):
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
