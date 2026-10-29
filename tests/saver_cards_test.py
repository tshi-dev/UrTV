#!/usr/bin/env python3
"""The screensaver's save of the home cards (SaverReceiver.set, which the phone page reaches through ConfigProvider) on a plain JVM against
Robolectric's android-all jar: a card, then «بدون» (none), three cards, the empty string, a card, none again: each is what is stored.
The owner's TV test twice said «بدون» from the phone page did not reach the TV; this keeps the screensaver's half of that path honest.
Needs javac and the cached android-all jar (the one tests/android_compile.sh keeps; SKIP without either, never a failure).
Run: python3 tests/saver_cards_test.py"""
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
cache = Path(os.environ.get("URTV_CACHE") or os.environ.get("XDG_CACHE_HOME") or Path.home() / ".cache") / ("" if os.environ.get("URTV_CACHE") else "urtv")
jars = sorted(cache.glob("android-all-*.jar")) if cache.is_dir() else []
jar = os.environ.get("URTV_ANDROID_JAR") or (str(jars[-1]) if jars else "")
if not (shutil.which("javac") and shutil.which("java") and jar and Path(jar).is_file()):
    print("SKIP the saver cards cases: no javac or no cached android-all jar (run tests/android_compile.sh once online)")
    sys.exit(0)
tmp = tempfile.mkdtemp()
stub = Path(tmp) / "src/sa/urtv/screensaver"
stub.mkdir(parents=True)
(stub / "BuildConfig.java").write_text('package sa.urtv.screensaver; public class BuildConfig { public static final String VERSION_NAME = "t", MARKETING = "4.0", BRANCH = "x"; public static final int VERSION_CODE = 1; }')
shutil.copy(ROOT / "tests/SaverCardsRun.java", stub / "SaverCardsRun.java")
r = subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", jar, "-d", tmp + "/out", "-sourcepath", f"{ROOT}/android/app/src/main/java:{tmp}/src", str(stub / "SaverCardsRun.java")], capture_output=True, text=True)
if r.returncode:
    print("FAIL the saver cards run does not compile\n" + r.stderr[-800:])
    sys.exit(1)
out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp", f"{tmp}/out:{jar}", "sa.urtv.screensaver.SaverCardsRun"], capture_output=True, text=True).stdout
lines = [l for l in out.splitlines() if l.startswith(("PASS ", "FAIL "))]
print("\n".join(lines))
bad = [l for l in lines if l.startswith("FAIL ")]
if len(lines) != 6 or bad:
    print("FAIL every step of the sequence ran and passed", out[-300:])
    sys.exit(1)
print("ALL PASS")
