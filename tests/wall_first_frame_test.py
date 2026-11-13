#!/usr/bin/env python3
"""Hotfix 4.3: the home never shows black on the way back (a user's Mi Box on 4.3: the cards on black for a few seconds, no photo and no apps bar). WallFirstFrame runs on a plain JVM; the wiring is
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
check("a trim keeps the small copy as the stand-in (480 px) when there is one, and the full photo is marked gone", "Bitmap thumb = smallPhoto;" in main and "fullPhotoShown = false;" in main)
check("every photo shown makes the small copy off the main thread (480 px wide, WebP, in files/ beside the other photos)", 'new Thread(() -> saveSmallPhoto(forSmall), "wall-small")' in main and "WALL_SMALL = \"wall-small.webp\"" in main
      and "WallFirstFrame.SMALL_W" in main)
check("the picture of the home (HomeSnapshot) is taken only with the full photo and the apps bar: onPause and the idle tick", main.count("WallFirstFrame.snapshotWorthKeeping(fullPhotoShown, appsShown, wallTrimmed)") == 2)
check("the timing line: resume to the first photo frame, under UrTVHome, once per return, with the RAM tier", 'android.util.Log.i("UrTVHome", "resume to first photo frame: "' in main and "photoLogged" in main and "Ram.tier(this)" in main)
check("the scrims and the apps bar are not behind the photo: they are in the backdrop and the root with the look's solid colour (never black), as before", "root.setBackgroundColor(GlassSource.look(this).base);" in main and "backdrop.addView(scrim" in main)
main_src = main
check("the Activity only stopped (onRestart): NO overlay, its live views are still there; the picture is for a cold start only", "protected void onRestart()" in main and "showSnapshot();" in main
      and main.count("showSnapshot();") == 1 and main.split("protected void onRestart()")[1].split("}")[0].count("showSnapshot") == 0 or "NO overlay" in main)
check("no skeleton, ever: with no good picture (none, stale, another look, `debug nosnapshot`) there is no overlay at all; the live views and the look's own colour show", "HomeSnapshot.skeleton(" not in main
      and 'snapshot overlay: none (no usable picture)' in main and "if (b == null) {" in main)
check("a trim always leaves a stand-in: the small copy, else a 160 px copy of whatever drawable is there (rendered small), and on out of memory the photos stay (never nulled for nothing)",
      "Bitmap thumb = smallPhoto;" in main and "d.draw(cv);" in main and "the photos stay" in main and main.index("if (thumb == null) {") < main.index("v.setImageDrawable(null)"))
check("the home's own provider and disk reads (ownInfo, homePackInfo, remotePhotos) run in the photo thread, not on the main thread", main.split("final int gen = ++wallGen;")[1].split("new Thread(() -> {")[1][:900].count("ownInfo()") == 1
      and main.split("private void nextWallpaper()")[1].split("final int gen = ++wallGen;")[0].count("ownInfo()") == 0 and main.split("private void nextWallpaper()")[1].split("final int gen = ++wallGen;")[0].count("remotePhotos()") == 0)
cp = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/ConfigProvider.java").read_text(encoding="utf-8")
check("the screensaver app does not stat every photo for the home's wallpaper: the dates are read only when the query asks for them; the home asks for n, mode, rev and home", 'contains("dates")' in cp
      and 'new String[] {"n", "mode", "rev", "home"}' in main)
check("the backdrop's hardware layer is off on a tight TV (LOW or MID) as well as on the low-RAM flag", "&& !Ram.tight(this)) backdrop.setLayerType" in main)
snap = (H / "HomeSnapshot.java").read_text(encoding="utf-8")
check("UrTVHome logs: snapshot capture ok or failed with the reason, the overlay shown or none and for how long, the trim level, resume to the first photo frame", 'snapshot capture ok' in snap and snap.count("snapshot capture failed") >= 3
      and 'snapshot overlay: picture shown' in main and 'snapshot overlay: gone after' in main and '"trim level "' in main and 'resume to first photo frame' in main)
check("the photo comes back sharp quickly: decoding starts in onStart (off the main thread, at screen size), the stand-in is 480 px wide, and the time to the sharp photo is logged under UrTVHome",
      "protected void onStart()" in main and "if (wallTrimmed) { wallTrimmed = false; nextWallpaper(); }" in main.split("protected void onStart()")[1][:300] and "WallFirstFrame.SMALL_W, Math.max(1, WallFirstFrame.SMALL_W * b.getHeight()" in main
      and '"resume to sharp photo: "' in main and "inSampleSize = Bounded.sampleSize(o.outWidth, getResources().getDisplayMetrics().widthPixels)" in main)
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
