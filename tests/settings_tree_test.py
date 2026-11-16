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
lines = {re.sub(r"^—+ ", "", l).split(":")[0] for l in tree.splitlines() if l.startswith("—")}
missing = [x for x in rows if x not in lines and x != "بحث في الإعدادات"]
check("every row of the settings is in the tree, once under its section", not missing, missing)
check("the 4.4 look rows with their choices and defaults", all(x in tree for x in ("—— التخطيط: تلفزيونك / كنبة / كرة القدم (الأصل: تلفزيونك)",
      "—— شكل التطبيقات: إطار / تكبير وظل (الأصل: إطار)", "مربعات (الأصل: الحالية)")), "")
check("where the phone page and the control centre reach a row", "صفحة الجوال: city" in tree and "مركز التحكم: city" in tree and "صفحة الجوال: fav_bar" in tree)
sys.exit(1 if failed else 0)
