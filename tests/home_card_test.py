#!/usr/bin/env python3
"""The home card when the screensaver's settings can't be read: it keeps the last choice (HomeCardChoice.java), «بدون»
included, and shows the prayers only with nothing remembered. Needs javac (skipped with a note without).
Run: python3 tests/home_card_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/launcher/src/main/java/sa/urtv/home/HomeCardChoice.java"
B = "file:///android_asset/web/tv.html#"
# frag, panel (read now), saved frag, saved panel ("-" = nothing) -> the page
CASES = [
    ("city=Riyadh&panel=old&week=1", "matches", "-", "-", B + "city=Riyadh&panel=matches&home_panel=matches&widget=1"),   # read: its own panel/week dropped
    ("city=Riyadh", "none", "-", "-", "off"),                                                          # read: «بدون»
    ("-", "-", "-", "-", B + "panel=prayers&widget=1"),                                                # unreadable, nothing remembered
    ("-", "-", "city=Riyadh", "forecast", B + "city=Riyadh&panel=forecast&home_panel=forecast&widget=1"),                  # unreadable: the last card
    ("-", "-", "city=Riyadh", "none", "off"),                                                          # unreadable: «بدون» stays «بدون»
    ("city=Abha", "verse", "city=Riyadh", "none", B + "city=Abha&panel=verse&home_panel=verse&widget=1"),               # readable wins over the saved
    ("city=Abha", "-", "city=Riyadh", "verse", B + "city=Riyadh&panel=verse&home_panel=verse&widget=1"),                # half read: the saved one
    ("-", "verse", "-", "-", B + "panel=prayers&widget=1"),                                            # half read, nothing saved
    ("-", "-", "city=Riyadh", "-", B + "panel=prayers&widget=1"),                                      # half saved: not used
    ("city=Riyadh&panel=old", "verse,hourly,year", "-", "-", B + "city=Riyadh&panel=verse&home_panel=verse,hourly,year&widget=1"),   # up to three: the first as panel=, all as home_panel=
    ("-", "-", "city=Riyadh", "prayers,stocks", B + "city=Riyadh&panel=prayers&home_panel=prayers,stocks&widget=1"),  # unreadable: the last cards
    # «المهام» among the cards is a button on the home screen: the page is told (live=1), whichever place it has; no other card is
    ("city=Riyadh", "tasks", "-", "-", B + "city=Riyadh&panel=tasks&home_panel=tasks&widget=1&live=1"),
    ("city=Riyadh", "verse,tasks,year", "-", "-", B + "city=Riyadh&panel=verse&home_panel=verse,tasks,year&widget=1&live=1"),
    ("city=Riyadh", "verse,hourly,tasks", "-", "-", B + "city=Riyadh&panel=verse&home_panel=verse,hourly,tasks&widget=1&live=1"),
    ("-", "-", "city=Riyadh", "year,tasks", B + "city=Riyadh&panel=year&home_panel=year,tasks&widget=1&live=1"),    # unreadable: the last cards, live still
    ("city=Riyadh", "today,smart,stocks", "-", "-", B + "city=Riyadh&panel=today&home_panel=today,smart,stocks&widget=1"),   # «يومك» and the smart card may show a task: not live
    ("city=Riyadh", "none", "city=Riyadh", "tasks", "off"),                                                           # «بدون»: no card, so nothing live
]

# HomeCard.pause()/refresh() call pauseTimers()/resumeTimers(), which are process-wide: safe only while this card is the launcher's only WebView
launcher = list((ROOT / "android/launcher/src/main/java").rglob("*.java"))
makers = [p.name for p in launcher if "new WebView(" in p.read_text(encoding="utf-8")]
timers = [p.name for p in launcher if "pauseTimers" in p.read_text(encoding="utf-8").replace("pauseTimers()/resumeTimers()", "")]
# «المباريات» (MatchesActivity, docs/PROPOSALS-NOV2.md §1) is the second: never in front with the home, it resumes the timers in its onResume
# and pauses them in its onPause, so whichever of the two comes in front last leaves them running
ma = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MatchesActivity.java").read_text(encoding="utf-8")
ma_ok = ("protected void onResume() {\n        super.onResume();\n        if (web != null) { web.onResume(); web.resumeTimers(); }" in ma
         and "protected void onPause() {\n        super.onPause();\n        if (web != null) { web.onPause(); web.pauseTimers(); }" in ma)
card_ok = sorted(makers) == ["HomeCard.java", "MatchesActivity.java"] and sorted(timers) == ["HomeCard.java", "MatchesActivity.java"] and ma_ok
print(("PASS " if card_ok else "FAIL ") + "the launcher's WebViews are HomeCard's and «المباريات»'s, each pausing the process-wide timers when it goes and resuming them when it comes"
      + ("" if card_ok else f"  -> WebViews: {makers}, pauseTimers in: {timers}; revisit HomeCard.pause()/refresh() before adding another"))

javac, java = shutil.which("javac"), shutil.which("java")
if not (javac and java):
    print("SKIP HomeCardChoice.java: no javac here (CI's Build APKs compiles it)")
    sys.exit(0 if card_ok else 1)
failed = 0 if card_ok else 1
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run([javac, "-encoding", "UTF-8", "-d", tmp, str(SRC), str(ROOT / "tests/HomeCardChoiceRun.java")], capture_output=True, text=True)
    ok = r.returncode == 0
    print(("PASS " if ok else "FAIL ") + "HomeCardChoice.java compiles" + ("" if ok else "  -> " + r.stderr[-600:]))
    failed += not ok
    if ok:
        args = [a for c in CASES for a in c[:4]]
        r = subprocess.run([java, "-cp", tmp, "sa.urtv.home.HomeCardChoiceRun", *args], capture_output=True, text=True)
        got = r.stdout.splitlines()
        for c, g in zip(CASES, got):
            ok = g == c[4]
            print(("PASS " if ok else "FAIL ") + f"{c[:4]} -> {c[4]}" + ("" if ok else f"  -> {g}"))
            failed += not ok
        if len(got) != len(CASES):
            print("FAIL runner printed", len(got), "lines", r.stderr[-300:]); failed += 1
sys.exit(1 if failed else 0)
