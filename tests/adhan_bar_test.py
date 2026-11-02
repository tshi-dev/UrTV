#!/usr/bin/env python3
"""The home screen's bar before the adhan (AdhanBarText.java): the last 10 minutes before each prayer, then until the iqama,
on tests/adhan_bar_cases.tsv. Needs javac; without it, skipped with a note.   Run: python3 tests/adhan_bar_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if not shutil.which("javac"):
    print("SKIP adhan bar: no javac here")
    sys.exit(0)
cases = [l.split("\t") for l in (ROOT / "tests/adhan_bar_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
src = ROOT / "android/launcher/src/main/java/sa/urtv/home"
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(src / "AdhanBarText.java"), str(src / "GreetingText.java"), str(src / "ClockText.java"), str(src / "DigitsText.java"),
                    str(ROOT / "tests/AdhanBarRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.AdhanBarRun", str(ROOT / "tests/adhan_bar_cases.tsv")],
                         check=True, capture_output=True).stdout
got = out.decode("utf-8").split("\n")
failed = 0
for c, g in zip(cases, got):
    ok = g == c[8]
    print(("PASS " if ok else "FAIL ") + f"adhan bar: {c[9]}" + ("" if ok else f"  -> {g!r}, wanted {c[8]!r}"))
    failed += not ok
if len(got) < len(cases):
    print("FAIL fewer answers than cases")
    failed += 1
sys.exit(1 if failed else 0)
