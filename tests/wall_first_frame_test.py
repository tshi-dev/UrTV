#!/usr/bin/env python3
"""4.3.1 the home never shows black on the way back (a user's Mi Box on 4.3: the cards on black for a few seconds, no photo and no apps bar). WallFirstFrame runs on a plain JVM; the wiring is
read from MainActivity: the small copy of the photo in the first frame of a start and a return, the stand-in kept on a trim, the picture of the home taken only with the photo and the bar, the
timing line under UrTVHome.
    python3 tests/wall_first_frame_test.py"""
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
check("the first frame: the small copy is put in the photo view at the start, on onRestart and on onResume (memory, else the file), before the live photo", main.count("firstFrameWallpaper();") >= 3
      and "WallFirstFrame.source(has, smallPhoto != null, f.isFile())" in main)
check("a trim keeps the small copy as the stand-in (480 px, not 160), and the full photo is marked gone", "wallFront.setImageBitmap(smallPhoto)" in main and "fullPhotoShown = false;" in main)
check("every photo shown makes the small copy off the main thread (480 px wide, WebP, in files/ beside the other photos)", 'new Thread(() -> saveSmallPhoto(forSmall), "wall-small")' in main and "WALL_SMALL = \"wall-small.webp\"" in main
      and "WallFirstFrame.SMALL_W" in main)
check("the picture of the home (HomeSnapshot) is taken only with the full photo and the apps bar: onPause and the idle tick", main.count("WallFirstFrame.snapshotWorthKeeping(fullPhotoShown, appsShown, wallTrimmed)") == 2)
check("the timing line: resume to the first photo frame, under UrTVHome, once per return, with the RAM tier", 'android.util.Log.i("UrTVHome", "resume to first photo frame: "' in main and "photoLogged" in main and "Ram.tier(this)" in main)
check("the scrims and the apps bar are not behind the photo: they are in the backdrop and the root with the look's solid colour (never black), as before", "root.setBackgroundColor(GlassSource.look(this).base);" in main and "backdrop.addView(scrim" in main)
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "WallFirstFrame.java"), str(ROOT / "tests/WallFirstFrameRun.java")], capture_output=True, text=True)
        check("WallFirstFrame compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.WallFirstFrameRun"], capture_output=True, text=True, timeout=60).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            for n, v in lines:
                check(n, v == "ok")
            check("every case ran", len(lines) == 5, out)
else:
    check("java is installed", False)
sys.exit(1 if failed else 0)
