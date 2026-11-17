#!/usr/bin/env python3
"""4.4 «مصادر البيانات» (الإعدادات ← حول), the owner's rule: where the data comes from is said once, in the settings, and on the cards it is
a tiny dim line at most (the page's side: tests/sources_rule.test.mjs). Here: the row is in «حول» in both screens (4.4 and 4.3.2's), each
source says what it gives and its terms and goes to a row that exists, the credits a licence asks for are named (Open-Meteo's CC BY, the
tafsir's publisher, the photos'), and the page's rule is 60% and half the light.
    python3 tests/sources_rule_test.py"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


src, st, hub = (APP / "Sources.java").read_text(encoding="utf-8"), (APP / "SettingsActivity.java").read_text(encoding="utf-8"), (APP / "SettingsHub.java").read_text(encoding="utf-8")
tpl = (ROOT / "src/template.html").read_text(encoding="utf-8")
rows = re.findall(r'\{"([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)"\}', src[src.index("LIST = {"):])
check("Sources: each source with what it gives, its terms and the row of what it gives", len(rows) >= 8, len(rows))
named = set(re.findall(r'row\("([^"]+)"', st)) | set(re.findall(r'(?:toggle|choice)Row\("([^"]+)"', st))
hub_rows = set(re.findall(r'\{"([^"]+)", (?:LANG|PLACE|LOOK|APPS|BUTTONS|PRAY|CONTENT|SAVER|FAMILY|UPDATE|SEARCH)\}', hub))
bad = [r[3] for r in rows if r[3] not in hub_rows]
check("Sources: every source goes to a row the settings have (SettingsHub.ROWS)", not bad, bad)
names = " ".join(r[0] for r in rows)
for who in ("Open-Meteo", "TheSportsDB", "Yahoo Finance", "TVmaze", "QuranEnc", "Wikidata", "Aladhan"):
    check(f"Sources: {who} is listed", who in names)
om = next((r for r in rows if r[0] == "Open-Meteo"), None)
check("Sources: Open-Meteo's CC BY, and that its name stays on screen", om and "CC BY" in om[2] and "يبقى" in om[2], om)
check("Sources: the tafsir's terms (unchanged, the publisher and source named)", any("الناشر" in r[2] for r in rows))
check("Sources: the photos' credits are pointed to («حقوق الصور»)", any("حقوق الصور" in r[1] for r in rows))
check("settings: the row «مصادر البيانات» in «حول», opening the list", 'row("مصادر البيانات"' in st and "showSources()" in st and '{"مصادر البيانات", UPDATE}' in hub)
check("settings: in 4.3.2's screen too, and in its order", '{"مصادر البيانات", "update"}' in hub and '"مسح الملفات المؤقتة", "مصادر البيانات"}' in hub)
check("settings: «مصادر البيانات», never «المصادر» (that is the TV's inputs on the home)", '"المصادر"' not in src and 'row("المصادر"' not in st)
check("settings: OK on a source lands on its row (the search's way: hubAt, forceWant, open)",
      re.search(r"hubAt = SettingsHub\.of\(src\[3\], null\); forceWant = src\[3\]; open\(hubAt\);", st) is not None)
check("page: a source line is 60% of its line and half its light (.srcl)", ".srcl{font-size:.6em;opacity:.5;" in tpl)
check("page: the weather's and the markets' lines follow it", re.search(r"\.wxsrc\{[^}]*\*\.6\)[^}]*opacity:\.5", tpl) and re.search(r"\.mkup\{[^}]*\*\.6\)[^}]*opacity:\.5", tpl))
sys.exit(1 if failed else 0)
