#!/usr/bin/env python3
"""The prayer-time arithmetic of the adhan (android/launcher .../tilawah/PrayerMath.java) on a plain JVM: Isha stays an absolute
time past midnight, each country's method by its zone (against an independent solar calculation), the cache key and Aladhan's date go by the city's zone and name the city, «5:12 (+03)» parses. Needs javac;
without it, skipped with a note.   Run: python3 tests/prayer_math_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if not shutil.which("javac"):
    print("SKIP prayer math: no javac here")
    sys.exit(0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp,
                    str(ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/PrayerMath.java"),
                    str(ROOT / "tests/PrayerMathRun.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/ZoneFix.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/ZoneTable.java")], check=True)
    run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.PrayerMathRun", str(ROOT / "tests/prayer_gold.tsv"), str(ROOT / "tests/prayer_gold_countries.tsv")], capture_output=True)
out = run.stdout.decode("utf-8")
print(out, end="")
if run.returncode != 0 or "FAIL" in out:
    print(run.stderr.decode("utf-8", "replace"))
    sys.exit(1)
