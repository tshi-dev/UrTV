#!/usr/bin/env python3
"""«تعديل وقت الصلاة» on a plain JVM: the screensaver's side (PrayerAdj.java: what the receiver accepts, the saved text, the fragment,
the labels) and the launcher's side (PrayerAdjust.java with PrayerMath.java: the minutes added to the five prayers, the sunrise left,
an Isha moved past midnight). Needs javac; without it, skipped with a note.   Run: python3 tests/prayer_adj_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if not shutil.which("javac"):
    print("SKIP prayer adjustment: no javac here")
    sys.exit(0)
failed = False
for run_class, sources in (
        ("sa.urtv.screensaver.PrayerAdjRun", ["android/app/src/main/java/sa/urtv/screensaver/PrayerAdj.java", "android/app/src/main/java/sa/urtv/screensaver/DigitsText.java", "tests/PrayerAdjRun.java"]),
        ("sa.urtv.home.tilawah.PrayerAdjustRun", ["android/launcher/src/main/java/sa/urtv/home/tilawah/PrayerAdjust.java",
                                                  "android/launcher/src/main/java/sa/urtv/home/tilawah/PrayerMath.java", "tests/PrayerAdjustRun.java",
                                                  "android/launcher/src/main/java/sa/urtv/home/ZoneFix.java", "android/launcher/src/main/java/sa/urtv/home/ZoneTable.java"])):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *[str(ROOT / s) for s in sources]], check=True)
        run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, run_class], capture_output=True)
    out = run.stdout.decode("utf-8")
    print(out, end="")
    if run.returncode != 0 or "FAIL" in out:
        print(run.stderr.decode("utf-8", "replace"))
        failed = True
sys.exit(1 if failed else 0)
