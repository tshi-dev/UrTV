#!/usr/bin/env python3
"""The times of day of the launcher's screens (ClockText.java, and GreetingText.sun on it): the 12-hour clock as always, and the
24-hour one of «ساعة ٢٤», in both kinds of digits, on tests/clock_text_cases.tsv. Needs javac; without it, skipped with a note.
Run: python3 tests/clock_text_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if not shutil.which("javac"):
    print("SKIP clock text: no javac here")
    sys.exit(0)
cases = [l.split("\t") for l in (ROOT / "tests/clock_text_cases.tsv").read_text(encoding="utf-8").split("\n")
         if l and not l.startswith("#")]
home = ROOT / "android/launcher/src/main/java/sa/urtv/home"
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(home / "ClockText.java"), str(home / "GreetingText.java"),
                    str(home / "DigitsText.java"), str(ROOT / "tests/ClockTextRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.ClockTextRun",
                          str(ROOT / "tests/clock_text_cases.tsv")], check=True, capture_output=True).stdout
got = out.decode("utf-8").split("\n")
failed = 0
for c, g in zip(cases, got):
    kind, inp, mode, digits, want, note = c
    ok = g == want
    print(("PASS " if ok else "FAIL ") + f"clock {kind} {inp!r} {mode}: {note}" + ("" if ok else f"  -> {g!r}, wanted {want!r}"))
    failed += not ok
if len(got) < len(cases):
    print("FAIL fewer answers than cases")
    failed += 1
sys.exit(1 if failed else 0)
