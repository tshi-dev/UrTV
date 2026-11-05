#!/usr/bin/env python3
"""«تحديث صفحة شاشة التوقف» (was «تحديث الشاشة الآن»): people took the page's update for the apps' one (rows 421, 441). The row's name says it
is the screensaver's page, its line points to «تحديث تلفزيوني» (same category) for the two apps, and «تحديث تلفزيوني» says it updates the apps.
Read from the source and the docs; the old name is left nowhere a person or an agent reads it as a row.   Run: python3 tests/update_row_test.py"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
text = lambda p: p.read_text(encoding="utf-8")
st, hub = text(APP / "SettingsActivity.java"), text(APP / "SettingsHub.java")
render = st[st.index("    private void render() {"):].split("\n    }\n")[0]
row = re.search(r'View last = row\("([^"]+)", "([^"]+)", R\.drawable\.ic_sync, new Runnable\(\) \{ public void run\(\) \{ updateNow\(\); \} \}\);', render)
check("the page's update row is «تحديث صفحة شاشة التوقف» and still runs the same update (updateNow)", row and row.group(1) == "تحديث صفحة شاشة التوقف", row and row.group(0))
check("its line says the page only and points to «تحديث تلفزيوني» for the two apps",
      row and "فقط" in row.group(2) and "لتحديث التطبيقين: «تحديث تلفزيوني»" in row.group(2), row and row.group(2))
check("«تحديث تلفزيوني» says it updates the two apps", 'row("تحديث تلفزيوني", "تحديث التطبيقين: ' in render)
check("both rows sit in the same category («تحديث تلفزيوني»), so «في نفس القسم» is true",
      '{"تحديث تلفزيوني", UPDATE}' in hub and '{"تحديث صفحة شاشة التوقف", UPDATE}' in hub)
check("the version lines still show under it", "if (last.getParent() == list) note(last, Updater.versionLines(this));" in render)
old = [str(p.relative_to(ROOT)) for p in list((ROOT / "android").rglob("*.java")) + [ROOT / "README.md", ROOT / "docs/TEST-PLAN.md"]
       if "تحديث الشاشة الآن" in text(p)]
check("the old name is gone from the apps, the README and the test plan", not old, old)
for d in ("SETTINGS.md", "FEATURES.md"):
    t = text(ROOT / "docs" / d)
    check(f"docs/{d}: the new name, and the old one only as «was»/«كان اسمه»",
          "تحديث صفحة شاشة التوقف" in t and all(("was «تحديث الشاشة الآن»" in line or "كان اسمه «تحديث الشاشة الآن»" in line)
                                               for line in t.splitlines() if "تحديث الشاشة الآن" in line))
sys.exit(1 if failed else 0)
