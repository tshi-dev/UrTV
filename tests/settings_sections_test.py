#!/usr/bin/env python3
"""4.3.2 «الإعدادات تكون مضبوطة»: every option lives in the one section it belongs to, in a set order, found by a search, and the old section keys still land.
4.3.3: ten sections (docs/mockups/SETTINGS-MAP.md), and the city chosen in one place, «الموقع والطقس» ← «المدينة», which every old way to it opens.

    python3 tests/settings_sections_test.py

SettingsHub (ROWS, ORDER, search, canon) runs for real on a JVM (tests/SettingsSearchRun.java); the wiring in SettingsActivity is read from the source
(tests/settings_hub_test.py checks the rest). Needs javac for the JVM part."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
hub = (APP / "SettingsHub.java").read_text(encoding="utf-8")
if not shutil.which("javac"):
    print("SKIP settings sections: no javac here")
    sys.exit(0)
QUERIES = ["مدينه", "صوت", "جوال", "هجري", "لون", "اسواق", "الأذان", "ثواني", "نوم", "zzzz", "", "الاذان", "pin"]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "SettingsHub.java"), str(APP / "FocusKey.java"), str(ROOT / "tests/SettingsSearchRun.java")],
                   check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.SettingsSearchRun", *QUERIES],
                         check=True, capture_output=True).stdout.decode("utf-8").split("\n")
search = {}
for l in out:
    f = l.split("\t")
    if f[0] == "search": search[f[1]] = [x.split("=") for x in f[2:]]
canon = {f[1]: f[2] for f in (l.split("\t") for l in out) if f[0] == "canon"}
rows = [(f[1], f[2], int(f[3])) for f in (l.split("\t") for l in out) if f[0] == "row"]

check("the sections (4.3.3): the search, then the ten in the owner's list; no shortcut beside them, none is deeper",
      re.findall(r'\{(?:SEARCH|LANG|PLACE|LOOK|APPS|BUTTONS|PRAY|CONTENT|SAVER|FAMILY|UPDATE), "([^"]+)"', hub[hub.index("CATEGORIES"):hub.index("OLD")])
      == ["بحث في الإعدادات", "اللغة", "الموقع والطقس", "الشكل والمشاهد", "التطبيقات", "الأزرار السريعة", "الصلاة والأذان", "المباريات والأسواق", "شاشة التوقف",
          "العائلة والأمان", "حول"])
# every row of the hub is in the order of its own section, once; only the two shortcut rows (no screen of their own) are not
ranked = [r for r in rows if r[2] >= 0]
check("every row is in its section's order (only the search's own row is not)",
      sorted(r[0] for r in rows if r[2] < 0) == ["بحث في الإعدادات"], [r[0] for r in rows if r[2] < 0])
orders = {}
blk = hub[hub.index("static final String[][] ORDER"):]
blk = blk[:blk.index("};")]
for m in re.finditer(r"\{(LANG|PLACE|LOOK|APPS|BUTTONS|PRAY|CONTENT|SAVER|FAMILY|UPDATE), ((?:H \+ )?\"[^}]*)\},", blk):
    orders[m.group(1).lower()] = [e for h, e in re.findall(r'(H \+ )?"([^"]+)"', m.group(2)) if not h]
by = {}
for n, c, _ in rows: by.setdefault(c, []).append(n)
check("ORDER holds each row of its section exactly once (and its headers), nothing stale, nothing left out",
      all(sorted(v) == sorted(by.get(k, [])) for k, v in orders.items()) and set(orders) == set(by) - {"search"},
      {k: sorted(set(by.get(k, [])) ^ set(v)) for k, v in orders.items()})
check("the places the owner asked for (4.3.3): the clock's look and the photos in «الشكل والمشاهد», the language, digits, months and 12/24 in «اللغة», the city and the weather in «الموقع والطقس», the favourites in «التطبيقات»",
      all(n in by["look"] for n in ("حجم الساعة", "إظهار الثواني", "نص أوضح", "توهّج لون التطبيق", "صوري", "البطاقة الأولى"))
      and all(n in by["lang"] for n in ("اللغة", "الأرقام", "أسماء الأشهر الميلادية", "نظام الساعة")) and sorted(by["place"]) == sorted(["المدينة", "الدولة", "التحية والطقس", "الرطوبة والرياح"])
      and all(n in by["content"] for n in ("التصنيفات", "اللغات", "مسلسلات مختارة", "أقل تقييم"))
      and all(n in by["apps"] for n in ("تطبيقات في الشريط", "ترتيب كل التطبيقات", "قفل التطبيقات"))
      and sorted(by["family"]) == sorted(["الرقابة الأبوية", "تصفية المواقع", "تحكم من الجوال"]), {k: len(v) for k, v in by.items()})
check("the Hijri shift is a row (it was a shell command only), in «اللغة»", 'row("فرق التاريخ الهجري"' in st and "فرق التاريخ الهجري" in by["lang"])
check("search: a typed word finds the row by its name (الأذان first), by an alias (صوت → الأذان), by its section's name",
      search["الأذان"][0][0] == "الأذان" and ["الأذان", "pray"] in search["صوت"] and ["تحكم من الجوال", "family"] in search["جوال"] and ["فرق التاريخ الهجري", "lang"] in search["هجري"]
      and ["مركز التحكم", "buttons"] in search["نوم"] and ["إظهار الثواني", "look"] in search["ثواني"])
check("search: letters are folded (الاذان = الأذان, اسواق finds الأسواق), an alias works in Latin letters (pin), nothing for a word nobody knows or for nothing",
      search["الاذان"] == search["الأذان"] and any(r[0] == "صفحة الأسواق" for r in search["اسواق"]) and ["الرقابة الأبوية", "family"] in search["pin"]
      and search["zzzz"] == [] and search[""] == [])
check("search: never answers with the search's own row, and «مدينة» finds the one city row", all(r[0] != "بحث في الإعدادات" for v in search.values() for r in v)
      and search["مدينه"][0] == ["المدينة", "place"] and all(r[0] != "تغيير المدينة" for r in search["مدينه"]))
check("the old section keys land in their new section (a stored or passed one keeps working); a current key stays; an unknown one is nothing",
      canon.get("cards") == "saver" and canon.get("more") == "content" and canon.get("clock") == "lang" and canon.get("listen") == "pray" and canon.get("home") == "apps"
      and canon.get("shows") == "content" and canon.get("phone") == "family" and canon.get("parental") == "family" and canon.get("control") == "buttons"
      and canon.get("city") == "place" and canon.get("adhan") == "pray" and canon.get("report") == "update" and canon.get("look") == "look" and canon.get("nope") == "null", canon)
check("the activity: a section is opened from a link by its canonical key, and the search opens the row's section with the focus on that row",
      "SettingsHub.canon(getIntent().getStringExtra(\"screen\"))" in st and "forceWant = found.get(i)[0];" in st and "open(found.get(i)[1]);" in st
      and "SettingsHub.search(query)" in st and 'row("بحث في الإعدادات"' in st)
city = st[st.index("private void cityRow()"):].split("\n    }\n")[0]
check("4.3.3 the city in one place: every way to it (pickCity from the home's «المدينة», the control center, the question bar; a new intent too) opens «الموقع والطقس» on «المدينة» with its chooser",
      'screen = SettingsHub.PLACE;' in city and 'forceWant = "المدينة";' in city and "if (askCity) cityRow();" in st
      and "if (askCity) root.post(new Runnable() { public void run() { pickCity(); } });" in st
      and 'in.removeExtra("pickCity"); cityRow(); render(); pickCity(); }' in st
      and st.count('row("المدينة"') == 1 and 'row("تغيير المدينة"' not in st)
check("the old entry points are untouched: the remote's Settings key and the control center open SettingsActivity (no extra), the city pill passes pickCity",
      'putExtra("pickCity", true)' in (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
      and 'SAVER_PKG + ".SettingsActivity"' in (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
      and 'getBooleanExtra("pickCity", false)' in st)
sys.exit(1 if failed else 0)
