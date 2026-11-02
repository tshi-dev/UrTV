#!/usr/bin/env python3
"""The home screen's greeting (GreetingText.java): which words for which part of the day, and the weather line,
on tests/greeting_cases.tsv. Needs javac; without it, skipped with a note.   Run: python3 tests/greeting_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if not shutil.which("javac"):
    print("SKIP greeting: no javac here")
    sys.exit(0)
cases = [l.split("\t") for l in (ROOT / "tests/greeting_cases.tsv").read_text(encoding="utf-8").split("\n")
         if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"),
                    str(ROOT / "android/launcher/src/main/java/sa/urtv/home/GreetingText.java"),
                    str(ROOT / "android/launcher/src/main/java/sa/urtv/home/DigitsText.java"),
                    str(ROOT / "android/launcher/src/main/java/sa/urtv/home/ClockText.java"),
                    str(ROOT / "tests/GreetingRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.GreetingRun",
                          str(ROOT / "tests/greeting_cases.tsv")], check=True, capture_output=True).stdout
got = out.decode("utf-8").split("\n")
failed = 0
for c, g in zip(cases, got):
    n = {"part": 6, "weather": 6, "sun": 4, "seen": 3}[c[0]]   # the kind and its inputs; then what is expected, then a note
    want, note = c[n], c[n + 1]
    ok = g == want
    print(("PASS " if ok else "FAIL ") + f"greeting {c[0]}: {note}" + ("" if ok else f"  -> {g!r}, wanted {want!r}"))
    failed += not ok
if len(got) < len(cases):
    print("FAIL fewer answers than cases")
    failed += 1
sys.exit(1 if failed else 0)
