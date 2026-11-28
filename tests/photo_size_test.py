#!/usr/bin/env python3
"""4.4 beta 4, review F89: «أضف من فلاشة أو التنزيلات» on a normal phone photo. The decode read the screen's long-side limit as a short-side one, so a 12 MP photo was decoded whole (about 48 MB)
and its 3 to 5 MB JPEG sent through Binder (about 1 MB at most): «ثبّت أو حدّث…» or an out-of-memory. PhotoSize does the sizes (sub-sample by the long side, fit exactly, a JPEG that fits one call)
and PhotoAddActivity uses it. Run: python3 tests/photo_size_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)[:500]))
    failed += not ok
rd = lambda p: Path(p).read_text(encoding="utf-8")

pa = rd(H / "PhotoAddActivity.java")
check("PhotoAddActivity: the JPEG is made by screenJpeg (long-side sub-sample, exact fit, the first quality that fits 700 KB), not by the icon editor's short-side decode",
      "static byte[] screenJpeg(File f, int maxLong)" in pa and "PhotoSize.sample(bounds.outWidth, bounds.outHeight, maxLong)" in pa and "PhotoSize.fit(" in pa and "PhotoSize.QUALITIES" in pa
      and "PhotoSize.MAX_JPEG_BYTES" in pa and "IconEditorActivity.decode(f, PhotoShrink.screenMax" not in pa)
check("an out-of-memory is said («ما قدرنا نقرأ الصورة»), never a crash", "catch (RuntimeException | OutOfMemoryError e)" in pa)
if not shutil.which("javac"):
    print("SKIP the sizes on a JVM: no javac")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "PhotoSize.java"), str(ROOT / "tests/PhotoSizeRun.java")], capture_output=True, text=True)
    check("PhotoSize compiles on a JVM", r.returncode == 0, r.stderr[-400:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.PhotoSizeRun"], capture_output=True, text=True).stdout.splitlines()
        for line in out:
            n, res = line.rsplit("\t", 1)
            check(n, res == "ok")
        check("every check ran", len(out) == 11, len(out))
H2 = H
check("both the screen's photo and the icon editor's pictures are turned upright after decoding (review F98)",
      "PhotoTurn.upright(f, b)" in (H2 / "PhotoAddActivity.java").read_text(encoding="utf-8") and "PhotoTurn.upright(f, BitmapFactory.decodeFile" in (H2 / "IconEditorActivity.java").read_text(encoding="utf-8"))
# G3 (4.3.3), put back (review F129): the screen's own size for a downloaded photo
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
ps = (SAVER / "PhotoShrink.java").read_text(encoding="utf-8")
check("G3: both PhotoShrink copies are the same code (the screensaver's and the home's)", ps.split("\n", 1)[1] == (H / "PhotoShrink.java").read_text(encoding="utf-8").split("\n", 1)[1])
check("G3: the rule: the screen's long side between LOW (1280) and MAX (1920), MAX when not known",
      re.search(r"static int screenMax\(int screenLong\) \{ return screenLong <= 0 \? MAX : Math\.max\(LOW, Math\.min\(MAX, screenLong\)\); \}", ps) is not None and "static final int MAX = 1920" in ps and "static final int LOW = 1280;" in ps)
rule = lambda n: 1920 if n <= 0 else max(1280, min(1920, n))
check("G3: a 720p screen 1280, 1080p 1920, a 4K UI 1920, a 1366 screen 1366, unknown 1920", [rule(x) for x in (1280, 1920, 3840, 1366, 0)] == [1280, 1920, 1920, 1366, 1920])
check("G3: the screensaver's downloaded photos: a weak TV 1280, else the screen's size", "Prefs.tightRam(c) ? PhotoShrink.toSize(data, LOW_PX) : PhotoShrink.toSize(data, PhotoShrink.screenMax(c))" in (SAVER / "PhotoCache.java").read_text(encoding="utf-8"))
check("G3: the home's downloaded photos: «خفيف» (Ram.low) 1280, else the screen's size", "PhotoShrink.toSize(data, Ram.low(this) ? PhotoShrink.LOW : PhotoShrink.screenMax(this))" in (H / "MainActivity.java").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
