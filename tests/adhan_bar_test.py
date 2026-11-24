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
# 4.4 beta 4: the lead (10 or 15, 10 the default; the bar stays until the adhan), through the setting, the fragment, the page and the command
_a, _h, _t = (ROOT / "android/app/src/main/java/sa/urtv/screensaver").joinpath("Prefs.java").read_text(encoding="utf-8"), (ROOT / "android/launcher/src/main/java/sa/urtv/home/ConfigSnapshot.java").read_text(encoding="utf-8"), (ROOT / "src/template.html").read_text(encoding="utf-8")
_r = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SaverReceiver.java").read_text(encoding="utf-8")
_ok = ('sp.getInt("adhanLead", 10) == 15 ? 15 : 10' in _a and '(adhanLead() == 15 ? "&adhanlead=15" : "")' in _a
       and '(fragment.contains("&adhanlead=15")' in _h and "adhanLead: _Q.get('adhanlead') === '15' ? 15 : 10" in _t and "toAdhan <= CONFIG.adhanLead" in _t
       and 'return "error: adhanlead is one of 10 15";' in _r and "adhanlead) set1 adhanlead" in (ROOT / "optimize/saver.sh").read_text(encoding="utf-8")
       and "snap.adhanLead()" in (ROOT / "android/launcher/src/main/java/sa/urtv/home/AdhanBar.java").read_text(encoding="utf-8"))
print(("PASS " if _ok else "FAIL ") + "adhan bar lead: stored (10 default), sent only when 15, read by the home and the page, set by saver.sh adhanlead, and checked by the receiver")
failed += not _ok
sys.exit(1 if failed else 0)
