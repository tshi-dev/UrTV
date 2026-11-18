#!/usr/bin/env python3
"""The digits of the Java screens: DigitsText.java (the one converter, both ways) on tests/digits_text_cases.tsv, the screensaver app's copy is
the same file with its package, and no Java file outside the helpers turns digits by hand.   Run: python3 tests/digits_text_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

a = (HOME / "DigitsText.java").read_text(encoding="utf-8")
b = (APP / "DigitsText.java").read_text(encoding="utf-8")
norm = lambda s: re.sub(r"public (static|final)", r"\1", s).replace("package sa.urtv.home;", "package X;").replace("package sa.urtv.screensaver;", "package X;")
check("the screensaver app's DigitsText is the launcher's, with its package", norm(a).replace("public ", "") == norm(b).replace("public ", ""), "the two copies differ")
check("Digits.java (the launcher's state) reads the setting like the other settings, through ConfigSnapshot, and keeps it",
      "ConfigSnapshot.ui(c)" in (HOME / "Digits.java").read_text(encoding="utf-8") and "putBoolean(\"latin\"" in (HOME / "Digits.java").read_text(encoding="utf-8"))
for act in ["MainActivity", "TasksActivity", "DockActivity", "tilawah/MainActivity", "tilawah/SurahActivity", "tilawah/PlayerActivity", "tilawah/AdhanActivity"]:
    check(f"launcher {act}: starts with Digits.init (its numbers follow the setting)", "Digits.init(this)" in (HOME / (act + ".java")).read_text(encoding="utf-8"))
check("settings: every number and label goes through conv() and the setting is read at each render",
      "latinDigits = DigitsText.isLatin(prefs.digits());" in (APP / "SettingsActivity.java").read_text(encoding="utf-8")
      and "DigitsText.convert(s, latinDigits)" in (APP / "SettingsActivity.java").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP digits text: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/digits_text_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "DigitsText.java"), str(ROOT / "tests/DigitsTextRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.DigitsTextRun", str(ROOT / "tests/digits_text_cases.tsv")],
                         check=True, capture_output=True).stdout
got = out.decode("utf-8").split("\n")
for c, g in zip(cases, got):
    check(f"DigitsText {'latin' if c[0] == '1' else 'hindi'}: {c[3]}", g == c[2], f"{c[1]!r} -> {g!r}, wanted {c[2]!r}")
sys.exit(1 if failed else 0)
