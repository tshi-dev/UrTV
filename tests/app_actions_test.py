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
opts = src("AppOptions.java")

check("AppActions has no Android in it", "import android" not in acts)
check("the two names, in Arabic, and no «إيقاف إجباري» anywhere in the home", 'DETAILS = S.n("تفاصيل التطبيق"), DELETE = S.n("حذف التطبيق")' in acts
      and "إيقاف إجباري" not in main + acts.split("*/", 1)[1] and "forceStop" not in main)
# the app's options (docs/UX-REVIEW.md, v5): «انقل», «أخفِ», «اقفل», then ★ and ⓘ; the app's uninstall is on ⓘ's screen (Android's own)
check("the options are AppOptions' box: the three rows, then ★ (in or out of the favourites) and ⓘ", "AppOptions.show(this, anchor," in menu
      and all(f"o.{k} = " in menu for k in ("move", "hide", "lock", "favourite", "info")))
check("ⓘ is Android's app-details screen for that package (its uninstall asks to confirm there; nothing removed by us)",
      "new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(\"package\", pkg, null))" in menu
      and "PackageInstaller" not in main and "uninstall(" not in main)
check("ⓘ opens through the family lock (MENU_APP: «قفل الإعدادات», the TV's settings' lock and the app's own), never straight",
      "guarded(ParentalPolicy.MENU_APP, pkg, AppActions.DETAILS, () -> launch(details))" in menu and "startActivity" not in menu)
check("MENU_APP asks about @urtv and @tvsettings", "case MENU_APP:" in policy and "t.add(OWN_SETTINGS);\n                t.add(TV_SETTINGS);" in policy.split("case MENU_APP:")[1][:400])
check("kids mode moves and hides nothing (the lock, ★ and ⓘ stay, behind the lock)", "if (!kids) {\n            o.move = " in menu)
check("the whole box opens behind the family lock's MENU, from a long OK on the app and from the remote's Menu key on it (elsewhere Menu is the Inputs)",
      "guarded(ParentalPolicy.MENU, null, S.t(\"قائمة التطبيق\"), () -> showAppMenuNow(anchor, pkg, inDock));" in main
      and "card.setOnLongClickListener(v -> { optionsHintUsed(); showAppMenu(v, app.pkg, inDock); return true; });" in main
      and "getCurrentFocus().getTag() instanceof App" in main and "showAppMenu(f, ((App) f.getTag()).pkg, dockCards.contains(f));" in main
      and "showInputs(pills.get(0), ParentalPolicy.MENU_KEY);" in main)
check("no tile beside the favourites: the bar is the apps only", "optionsTile" not in main and "pickingOptions" not in main)
check("the hint «اضغط مطوّلاً للخيارات» beside a favourite's name, the first few times only, never once the box was opened",
      'OPTIONS_HINT = S.n("اضغط مطوّلاً للخيارات")' in main and "seen < OPTIONS_HINT_TIMES" in main and "optionsHint(inDock);" in main
      and 'putInt("options_hint", OPTIONS_HINT_TIMES).putInt("options_hint_en", OPTIONS_HINT_TIMES).apply();' in main
      and 'S.en() ? "options_hint_en" : "options_hint"' in main)   # step 27: counted per language, so the English home shows it too
settings_src = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
check("nothing is long-press-only: the settings' «التطبيقات والمفضلة» move, hide, change the favourites, lock (the family lock's page) and show the details",
      all(f'row("{r}"' in settings_src for r in ("تعديل المفضلة", "ترتيب كل التطبيقات", "ترتيب التطبيقات المفضلة", "قفل التطبيقات", "تفاصيل التطبيقات"))
      and "ACTION_MANAGE_APPLICATIONS_SETTINGS" in settings_src)
check("D-pad: ▲ ▼ walk the rows to the icon row, ◀ ▶ only between ★ and ⓘ (★ at the start: the right in Arabic), no wrap",
      "star.setNextFocusLeftId(Dir.rtl() ? info.getId() : star.getId());" in opts and "info.setNextFocusRightId(Dir.rtl() ? star.getId() : info.getId());" in opts
      and "v.setNextFocusLeftId(v.getId()); v.setNextFocusRightId(v.getId());" in opts)
check("OK on a row or an icon closes the box, then acts", "v.setOnClickListener(x -> { if (pw[0] != null) pw[0].dismiss(); if (action != null) action.run(); });" in opts)
check("the box is the app's width as it shows focused, above it (under it when there is no room), the look's colours and focus",
      "int w = Math.round(card.getWidth() * scale)" in opts and "GlassPaint.focused(a, l, GlassStyle.over(l.focus, dialog), RADIUS)" in opts
      and "GlassStyle.over(l.dialog, l.base)" in opts)
import re as _re
g = {k: int(v) for k, v in _re.findall(r"(PAD|GAP|BUTTON_H|RADIUS|BOX_RADIUS|ICON|ABOVE) = (\d+)", opts.split("static final int", 1)[1].split(";", 1)[0].replace("static final int", ""))}
check("spacing (the owner's rule): the box's padding is at least 16 px at 1080p (8 dp), its gaps on the 8 px scale (4 dp steps), each row 12 dp in from its edge, the box's corner its buttons' plus its padding",
      g.get("PAD", 0) >= 8 and g.get("GAP", 0) % 4 == 0 and g.get("ABOVE", 0) % 4 == 0 and g.get("BOX_RADIUS") == g.get("RADIUS", 0) + g.get("PAD", 0)
      and "r.setPadding(px(12, d), 0, px(12, d), 0);" in opts, g)
check("the other menus (the «المخفية» list) have no row", "showMenu(hiddenCard, labels, actions);" in main and "Collections.<View>emptyList()" in main)
check("the icons are there (Material, Apache 2.0)", all((ROOT / f"android/launcher/src/main/res/drawable/{n}.xml").exists()
      for n in ("ic_info", "ic_move", "ic_hide", "ic_lock", "ic_star", "ic_star_filled")))
if not shutil.which("javac"):
    print("SKIP app actions: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/app_actions_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(HOME / "AppActions.java"), str(ROOT / "tests/AppActionsRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.AppActionsRun", str(ROOT / "tests/app_actions_cases.tsv")],
                         check=True, capture_output=True).stdout.decode("utf-8").split("\n")
for c, g in zip(cases, out):
    check(f"app actions: {c[5]}", g == c[4], f"{g!r}, wanted {c[4]!r}")
check("an answer for every case", len(out) - 1 >= len(cases), (len(out), len(cases)))
sys.exit(1 if failed else 0)
