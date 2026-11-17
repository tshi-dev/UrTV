#!/usr/bin/env python3
"""4.3.4: «hidden apps don't show: there is no way to see or unhide them». Three causes, each pinned here:
1. the hide toast sent people to «تعديل التطبيقات», a button the app's menu does not have (it is «انقل»), and «المخفية» is only the last place
   of that mode's grid, after every app: the settings' «ترتيب كل التطبيقات» now has «التطبيقات المخفية», the mode opened on «المخفية» with its list;
2. the toast names that way;
3. the mode asked from the settings ended at once when an app had changed meanwhile: onNewIntent started it, then onResume built the grid
   again (appsStale) and abandoned it; the grid is now built first and the mode starts once it is done.
Run: python3 tests/hidden_apps_way_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

main = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
settings = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
words = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Words.java").read_text(encoding="utf-8")
home_tsv = (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8")
saver_tsv = (ROOT / "i18n/en/saver.tsv").read_text(encoding="utf-8")
edit = (ROOT / "android/launcher/src/main/java/sa/urtv/home/AppsEdit.java").read_text(encoding="utf-8")

asked = main.split("private boolean appsRequested(Intent i) {")[1].split("\n    }\n")[0]
start = main.split("private void startAppsEdit(String pkg) {")[1].split("\n    }\n")[0]

# 1. the way in
check("the settings offer «التطبيقات المخفية» beside «تعديل التطبيقات» and «رجوع للترتيب الأصلي», each to its own extras",
      '{"تعديل التطبيقات", "التطبيقات المخفية", "رجوع للترتيب الأصلي"}, (d, i) -> openAppsOrder(i == 2, i == 1))' in settings
      and '.putExtra("apps_hidden", hidden)' in settings)
check("its words are in the table and in English", 'S.n("التطبيقات المخفية")' in words and "التطبيقات المخفية\tHidden apps\n" in saver_tsv)
check("the home reads it with edit_apps", 'EXTRA_APPS_HIDDEN = "apps_hidden"' in main
      and "wantHiddenList = i.getBooleanExtra(EXTRA_APPS_HIDDEN, false);" in asked)
check("the mode opens with the focus on «المخفية» (order.size() is its place in AppsEdit)",
      "int focus = toHidden ? order.size()" in start and "at == order.size()" in edit)
check("…and its list at once", "if (toHidden) grid.post(" in start and "showHiddenList();" in start.split("if (toHidden) grid.post(")[1])
check("nothing hidden: it says so, and no empty mode", 'if (toHidden && hiddenFound.isEmpty())' in start and 'S.t("ما فيه تطبيقات مخفية")' in start
      and "ما فيه تطبيقات مخفية\tNo hidden apps" in home_tsv)
check("the request is used once (a later «تعديل التطبيقات» opens on its app)", "wantHiddenList = false;" in start)

# 2. the toast
check("the hide toast names the way that exists", 'S.t("أُخفي، ويرجع من الإعدادات: «التطبيقات المخفية»")' in main
      and "«تعديل التطبيقات»\"))" not in main.split("o.hide = () -> {")[1].split("};")[0])

# 3. the race
check("an app changed while the settings were in front: the grid is built before the mode, not after it by onResume",
      asked.index("if (appsStale) { appsStale = false; buildApps(); }") < asked.index("beginPendingAppsEdit();"))
check("…and the mode starts when that grid is done", "gridDone = true;\n        beginPendingAppsEdit();" in main
      and "if (!gridDone) { wantAppsEdit = true; return; }" in start)
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
