#!/usr/bin/env python3
"""4.4 beta 3 (a Bahrain user: Umm al-Qura at every start until «المدينة» was opened): a place of one's own saved without its zone took the TV's zone
at every read, wrong at a cold start before the TV has set it. Its zone and country now come from the nearest built-in city (OwnPlace), found once and
saved (Prefs.setCity, cityTz, cityCountry), so the first page drawn already has the place's calculation (#pm). The TV's zone only when nothing is near.
Needs javac (skipped with a note without).   Run: python3 tests/own_place_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
prefs = (SRC / "Prefs.java").read_text(encoding="utf-8")
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def body(sig):
    return prefs.split(sig, 1)[1].split("\n    }\n", 1)[0]


set_city = body("void setCity(String name, String lat, String lon, String tz, String country) {")
check("setCity: a place of one's own without a zone takes the nearest built-in city's zone and country before it is saved",
      "builtin == null && (tz == null || tz.isEmpty())" in set_city and "OwnPlace.near(lat, lon)" in set_city
      and set_city.index("OwnPlace.near") < set_city.index('putString("cityTz", tz)'))
tz = body("String cityTz() {")
check("cityTz: the nearest built-in city's zone before the TV's (an older saved place is migrated once)",
      "ownPlace(c)" in tz and tz.index("ownPlace(c)") < tz.index("TimeZone.getDefault()"))
cc = body("String cityCountry() {")
check("cityCountry: a place of one's own without a zone takes the nearest city's country, one with its own zone keeps that zone's",
      'sp.getString("cityTz", "").isEmpty() ? ownPlace(c) : null' in cc and cc.index("ownPlace(c)") < cc.index("Location.codeOf(cityTz())"))
own = body("private String[] ownPlace(String[] c) {")
check("ownPlace saves only what is missing (a zone or country given with the place is never replaced)",
      'if (sp.getString("cityTz", "").isEmpty()) e.putString("cityTz"' in own and 'if (sp.getString("cityCountry", "").isEmpty()) e.putString("cityCountry"' in own)
check("the fragment's method is the saved country's (PrayerMethod.fragment with cityCountry and cityTz)",
      "PrayerMethod.fragment(prayerMethod(), cityCountry(), cityTz())" in prefs)
op = (SRC / "OwnPlace.java").read_text(encoding="utf-8")
check("OwnPlace has no Android in it and remembers its last answer (no scan at every settings read)", "import android" not in op and "private static volatile Object[] last;" in op)

CASES = [
    ("26.2235,50.5876", "Asia/Bahrain BH"),    # Manama, a point of one's own
    ("26.1300,50.5600", "Asia/Bahrain BH"),    # Riffa
    ("29.3700,47.9800", "Asia/Kuwait KW"),
    ("25.2900,51.5300", "Asia/Qatar QA"),
    ("24.7000,46.7000", "Asia/Riyadh SA"),     # a Saudi list city carries no zone: Riyadh's
    ("0.0000,-30.0000", "none"),              # the middle of the Atlantic: the TV's zone, as before
    ("x,y", "none"), ("91,0", "none"),
]
javac, java = shutil.which("javac"), shutil.which("java")
if not (javac and java):
    print("SKIP OwnPlace.java: no javac here (CI's Build APKs compiles it)")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run([javac, "-encoding", "UTF-8", "-d", tmp, str(SRC / "OwnPlace.java"), str(SRC / "Cities.java"), str(ROOT / "tests/OwnPlaceRun.java")],
                       capture_output=True, text=True)
    check("OwnPlace.java compiles", r.returncode == 0, r.stderr[-600:])
    if r.returncode == 0:
        r = subprocess.run([java, "-cp", tmp, "sa.urtv.screensaver.OwnPlaceRun"] + [a for a, _ in CASES], capture_output=True, text=True)
        got = dict(l.split(" -> ", 1) for l in r.stdout.splitlines() if " -> " in l)
        for a, want in CASES:
            check(f"{a} -> {want}", got.get(a) == want, got.get(a))
sys.exit(1 if failed else 0)
