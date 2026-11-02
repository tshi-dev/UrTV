#!/usr/bin/env python3
"""No trailing «…» on a menu item (the owner: "why the dots"). Every item of the home's menus (the app's long-press menu, «المصادر», the
lists made with inputRow) and the menu texts of the screensaver's settings lists end in a word, not in «…». A text that is still working
(«جارٍ التحقق…») is a status, not an item, and keeps it. Read from the source.   Run: python3 tests/menu_dots_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

home = ROOT / "android/launcher/src/main/java/sa/urtv/home"
items = []
for f in sorted(home.rglob("*.java")):
    s = f.read_text(encoding="utf-8")
    items += [(f.name, m) for m in re.findall(r'(?:labels\.add|inputRow|row|button)\("([^"]*)"', s)]
dotted = [f"{n}: {t}" for n, t in items if t.endswith("…")]
check(f"the home's menu items and rows end in a word ({len(items)} read)", items and not dotted, dotted)
main = (home / "MainActivity.java").read_text(encoding="utf-8")
for name in ("تحديث تلفزيوني", "تلاوة وأذان", "إعدادات الأذان", "الرقابة الأبوية", "كل المصادر"):
    check(f"«{name}» is in the menu without dots", f'"{name}"' in main and f'"{name}…"' not in main)
saver = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
labels = re.findall(r'labels\[[^\]]*\] = "([^"]*)"', saver)
check(f"the screensaver settings' list items end in a word ({len(labels)} read)", labels and not [l for l in labels if l.endswith("…")], labels)
for name in ("بحث عن مدينة أو قرية", "تصفح الدوريات", "بحث عن أي فريق أو منتخب"):
    check(f"«{name}» is there without dots", f'"{name}"' in saver)
check("a status that is still working keeps its «…» (it is not an item)", '"جارٍ التحقق…"' in (home / "phone/UpdateText.java").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
