#!/usr/bin/env python3
"""docs/SETTINGS-TREE.md is what the code says now (.github/tools/settings_tree.py makes it from SettingsHub, Prefs, docs/SETTINGS.md, the phone page's
«set» keys and the control centre's rows): a settings change without the tree made again fails here.   python3 tests/settings_tree_test.py"""
import re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


r = subprocess.run([sys.executable, "-I", str(ROOT / ".github/tools/settings_tree.py"), "--check"], capture_output=True, text=True)
check("docs/SETTINGS-TREE.md is current (python3 .github/tools/settings_tree.py makes it again)", r.returncode == 0, (r.stdout + r.stderr).strip())
tree = (ROOT / "docs/SETTINGS-TREE.md").read_text(encoding="utf-8")
hub = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsHub.java").read_text(encoding="utf-8")
rows = [re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m.group(1), 16)), x)
        for x in re.findall(r'\{"([^"]+)", [A-Z]+\}', hub[hub.index("static final String[][] ROWS"):hub.index("static final String[][] HEADERS")])]
lines = {re.sub(r" \[[^]]*\]$", "", re.sub(r"^—+ ", "", l)) for l in tree.splitlines() if l.startswith("—")}
missing = [x for x in rows if x not in lines and x != "بحث في الإعدادات"]
check("every row of the settings is in the tree, once under its section", not missing, missing)
check("the 4.4 look rows with where they show, their choices, the default apart from an example command and the values it accepts", all(x in tree for x in (
      "——— التخطيط [الرئيسية]\n    الخيارات: تلفزيونك / كنبة / كرة القدم\n    الأصل: تلفزيونك\n    مثال للوكيل: optimize/saver.sh homelayout classic", "      القيم المقبولة: classic|couch|football",
      "——— شكل التطبيقات [الرئيسية]\n    الخيارات: بنرات / أيقونات / مربعات ملونة\n    الأصل: مربعات ملونة", "مربعات / الشكل السابق\n    الأصل: الحالية")), "")
unesc = lambda x: re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m.group(1), 16)), x)
scopes = {unesc(k): v for k, v in re.findall(r'\{"([^"]+)", "(home|saver|both|tv)"\}', hub[hub.index("static final String[][] SCOPES"):])}
noscope = [x for x in rows if x not in scopes and x != "بحث في الإعدادات"]
check("4.4 the owner: every row says where it shows (SettingsHub.SCOPES: home, saver, both, tv), and the settings screen shows it at the row's end",
      not noscope and "SettingsHub.scopeWords(FocusKey.key(label))" in (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8"), noscope)
check("no row is left as «no command documented»: each has one, or is an action of the TV, or is the TV's only (the code has no command for it)", "لا أمر موثّق بعد" not in tree)
prefs = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java").read_text(encoding="utf-8")
panels = re.findall(r'\{"([a-z]+)", "', prefs[prefs.index("static final String[][] PANELS = {"):prefs.index("static final String[][] SAVER_PANELS")])
saver_cards = [p for p in panels if p != "nextprayer"]
doc = re.search(r"`optimize/saver\.sh card <([^>]+)>`", (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8")).group(1).split(", ")
check("docs/SETTINGS.md's card list is the app's own (Prefs.PANELS but nextprayer, a home-screen card), and card2 says it can't repeat the first card",
      doc == saver_cards and "the second card can't repeat the first" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8"), [doc, saver_cards])
check("where the phone page and the control centre reach a row", "صفحة الجوال: city" in tree and "مركز التحكم: city" in tree and "صفحة الجوال: fav_bar" in tree)
sys.exit(1 if failed else 0)
