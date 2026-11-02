#!/usr/bin/env python3
"""The app's long-press menu on our home: the row of small buttons under its last item (AppActions.java, tests/app_actions_cases.tsv):
«تفاصيل التطبيق» opens Android's app-details screen, «حذف التطبيق» Android's own uninstall with its own confirm, never for a system app
or our two apps, none in kids mode; no «إيقاف إجباري» (a normal app cannot force-stop another, and the TV's details screen takes no focus
extra). Then the wiring, read from the source: both open through the family lock, the D-pad reaches them, the permission is declared.
Run: python3 tests/app_actions_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def src(n): return (HOME / n).read_text(encoding="utf-8")
main, acts, policy = src("MainActivity.java"), src("AppActions.java"), src("ParentalPolicy.java")
man = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
menu = main.split("private void showAppMenuNow(")[1].split("\n    }\n")[0]
buttons = main.split("private List<View> appButtons(")[1].split("\n    }\n")[0]
show = main.split("private void showMenu(View anchor, List<String> labels, List<Runnable> actions, List<View> buttons) {")[1].split("\n    }\n")[0]

check("AppActions has no Android in it", "import android" not in acts)
check("the two names, in Arabic, and no «إيقاف إجباري» anywhere in the home", 'DETAILS = "تفاصيل التطبيق", DELETE = "حذف التطبيق"' in acts
      and "إيقاف إجباري" not in main + acts.split("*/", 1)[1] and "forceStop" not in main)
check("the row goes under the last item of the app menu («الرقابة الأبوية»)", menu.rstrip().endswith("showMenu(anchor, labels, actions, appButtons(pkg, kids));")
      and menu.index('"الرقابة الأبوية') < menu.index("appButtons(pkg, kids)"))
check("«تفاصيل التطبيق» is Android's app-details screen for that package",
      "new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(\"package\", pkg, null))" in buttons)
check("«حذف التطبيق» is Android's own ACTION_DELETE (the system asks to confirm; nothing removed by us, no silent uninstall)",
      "new Intent(Intent.ACTION_DELETE, Uri.fromParts(\"package\", pkg, null))" in buttons and "PackageInstaller" not in main and "uninstall(" not in main)
check("both open through the family lock (MENU_APP: «قفل الإعدادات», the TV's settings' lock and the app's own), never straight",
      buttons.count("guarded(ParentalPolicy.MENU_APP, pkg,") == 2 and "startActivity" not in buttons)
check("MENU_APP asks about @urtv and @tvsettings", "case MENU_APP:" in policy and "t.add(OWN_SETTINGS);\n                t.add(TV_SETTINGS);" in policy.split("case MENU_APP:")[1][:400])
check("a system app (or an update of one) is told by its flags, and one that cannot be read is treated as system",
      "FLAG_SYSTEM | android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP" in main and "return true;   // not known: not offered" in main)
check("the manifest declares REQUEST_DELETE_PACKAGES (Android 9+ drops ACTION_DELETE without it)", 'android.permission.REQUEST_DELETE_PACKAGES' in man)
check("D-pad: ▼ from the last item reaches the first button, ▲ from a button goes back, ◀ ▶ walk the row right to left, no wrap",
      "buttons.get(0).getId()" in show and "b.setNextFocusUpId(last.getId());" in show and "b.setNextFocusDownId(b.getId());" in show
      and "setNextFocusRightId(buttons.get(Math.max(0, k - 1)).getId())" in show and "setNextFocusLeftId(buttons.get(Math.min(buttons.size() - 1, k + 1)).getId())" in show
      and "row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);" in show)
check("OK on a button closes the menu, then acts (like the items)", "b.setOnClickListener(v -> { pw.dismiss(); ((Runnable) v.getTag()).run(); });" in show)
check("the popup moves up by the row's height, so it still sits above the tile", "- extra, Gravity.START)" in show)
check("the other menus (the «المخفية» list) have no row", "showMenu(hiddenCard, labels, actions);" in main and "Collections.<View>emptyList()" in main)
check("the two icons are there (Material, Apache 2.0)", all((ROOT / f"android/launcher/src/main/res/drawable/{n}.xml").exists() for n in ("ic_info", "ic_delete")))
if not shutil.which("javac"):
    print("SKIP app actions: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/app_actions_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "AppActions.java"), str(ROOT / "tests/AppActionsRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.AppActionsRun", str(ROOT / "tests/app_actions_cases.tsv")],
                         check=True, capture_output=True).stdout.decode("utf-8").split("\n")
for c, g in zip(cases, out):
    check(f"app actions: {c[5]}", g == c[4], f"{g!r}, wanted {c[4]!r}")
check("an answer for every case", len(out) - 1 >= len(cases), (len(out), len(cases)))
sys.exit(1 if failed else 0)
