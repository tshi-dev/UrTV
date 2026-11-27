#!/usr/bin/env python3
"""4.4 beta 4, review F89: «أضف من فلاشة أو التنزيلات» on a normal phone photo. The decode read the screen's long-side limit as a short-side one, so a 12 MP photo was decoded whole (about 48 MB)
and its 3 to 5 MB JPEG sent through Binder (about 1 MB at most): «ثبّت أو حدّث…» or an out-of-memory. PhotoSize does the sizes (sub-sample by the long side, fit exactly, a JPEG that fits one call)
and PhotoAddActivity uses it. Run: python3 tests/photo_size_test.py"""
import shutil, subprocess, sys, tempfile
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
        check("every check ran", len(out) == 9, len(out))
sys.exit(1 if failed else 0)
