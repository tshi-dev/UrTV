#!/usr/bin/env python3
"""4.3.3 (7) published prayer corrections (urtv-data prayer/adjustments.json, PrayerFix): by country, a city's entry first, within their days,
held to −20..20 like the person's own, never over a prayer the person moved, sent through the same adj= the page and the home read, and said
on the settings screen. Runs PrayerFix on a plain JVM."""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
fails = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  [{detail}]"))
    if not ok:
        fails.append(name)


prefs = (APP / "Prefs.java").read_text(encoding="utf-8")
st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
cf = (APP / "ContentFile.java").read_text(encoding="utf-8")
check("the fragment's adj= is the combined one, and only one adj= is made", "+ prayerAdjFragment()" in prefs and "PrayerAdj.fragment(prayerAdjOn(), prayerAdj())" not in prefs
      and "PrayerAdj.fragment(on || PrayerAdj.any(fix), PrayerFix.combine(on, own, fix))" in prefs)
check("today is the city's day, the city is the saved one, the country the saved one", 'getTimeZone(cityTz())' in prefs and "cityCountry(), city()[0]" in prefs)
check("the file: the kept copy at once, fetched in the background at most twice a day, a bad answer never replaces it",
      "ContentFile.refresh(ctx, PrayerFix.PATH, PrayerFix.NAME, 12 * 3600_000L)" in prefs and "valid(MiniJson.parse(r.text())) == null) return;" in cf
      and "tmp.renameTo(f)" in cf and 'DataHost.NEW + path' in cf and "new Thread(" in cf)
check("the settings screen says when a correction is applied, and that the person's own wins", "PrayerFix.applied(prefs.prayerAdjOn(), prefs.prayerAdj(), prefs.prayerFix())" in st
      and "تعديلك لأي صلاة يبقى هو المعتمد" in st)

if not shutil.which("javac"):
    print("SKIP the run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        srcs = [str(APP / n) for n in ("PrayerFix.java", "PrayerAdj.java", "MiniJson.java", "DigitsText.java")] + [str(ROOT / "tests/PrayerFixRun.java")]
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *srcs], capture_output=True, text=True)
        check("PrayerFix compiles on a plain JVM", r.returncode == 0, r.stderr[-800:])
        if r.returncode == 0:
            out = dict(l.split(" ", 1) for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.PrayerFixRun"], capture_output=True, text=True).stdout.splitlines() if " " in l)
            check("a country's correction on its first day", out.get("COUNTRY") == "[-2, 0, 0, 0, 5]", out)
            check("a city's entry wins over its country's for that prayer, the country's stays for the others", out.get("CITY") == "[-2, 0, 0, 0, 3]", out)
            check("not before its first day; on its last day; the country code in any case", out.get("BEFORE") == "[0, 0, 0, 0, 0]" and out.get("LASTDAY") == "[-2, 0, 0, 0, 5]", out)
            check("another country: nothing", out.get("OTHER") == "[0, 0, 0, 0, 0]", out)
            check("held to 20, the sunrise never moved, half minutes not applied", out.get("CLAMP") == "[0, 0, 0, 20, 0]", out)
            check("no file or a broken one: nothing", out.get("NODOC") == "[0, 0, 0, 0, 0]" and out.get("BADDOC") == "[0, 0, 0, 0, 0]", out)
            check("the person's own minutes win for their prayer; the correction for the others", out.get("OWNON") == "[-2, 0, 0, 0, 2]", out)
            check("the setting off: the correction alone", out.get("OWNOFF") == "[-2, 0, 0, 0, 5]", out)
            check("the screen names only the corrections really applied", "الفجر" in out.get("APPLIEDON", "") and "العشاء" not in out.get("APPLIEDON", "")
                  and out.get("APPLIEDNONE") == "true", out)
            check("the page and the home get it as adj= (their parsers unchanged)", out.get("FRAG") == "&adj=-2,0,0,0,5" and out.get("FRAGNONE") == "[]", out)

print(f"\n{len(fails)} failed" if fails else "\nall passed")
sys.exit(1 if fails else 0)
