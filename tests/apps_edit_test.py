#!/usr/bin/env python3
"""«تعديل التطبيقات» on the home screen: «كل التطبيقات» edited in place as «تعديل المفضلة» edits the favourites. The pure state machine
(AppsEdit.java: pick up, move across the grid, drop, cancel, hide with «تراجع», «المخفية», up into the favourites), the grid's saved order,
kids mode, the gate that guards entering the mode and the adhan rule over its keys, on tests/apps_edit_cases.tsv; and the wiring, read from
the source (it needs Android): the order, the hidden list dock.sh shares and the favourites are saved once, when the mode ends.
Run: python3 tests/apps_edit_test.py   (the cases need javac)"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def src(p): return p.read_text(encoding="utf-8")

main, edit, dock, receiver = src(HOME / "MainActivity.java"), src(HOME / "AppsEdit.java"), src(HOME / "Dock.java"), src(HOME / "DockReceiver.java")
settings, hub = src(APP / "SettingsActivity.java"), src(APP / "SettingsHub.java")
fn = lambda text, name: text.split(name)[1].split("\n    }\n")[0]

# ---- the model
check("AppsEdit has no Android in it (the test runs it without the SDK)", "import android" not in edit)
check("the hint line, in the style of «تعديل المفضلة»", 'HINT = S.n("اختر تطبيقًا لتحريكه · × للإخفاء · ارفعه للمفضلة · رجوع للإنهاء")' in edit)
check("«تراجع» lasts as long as in «تعديل المفضلة» (FavEdit.UNDO_MS)", "undoUntil = now + FavEdit.UNDO_MS;" in edit)

# ---- the order «كل التطبيقات» shows
show = fn(main, "private void showApps(List<App> all, String[] wanted, int gen)")
check("the grid is by name, then in the owner's saved order (AppsEdit.arrange); the favourites are topped up first, by package (AppsEdit.topUp, cases in tests/apps_edit_cases.tsv), never by label",
      show.index("Collections.sort(rest, (x, y) -> x.label.compareToIgnoreCase(y.label));") < show.index("AppsEdit.topUp(have, other, MIN_DOCK)")
      < show.index("rest = inSavedOrder(rest);") and "AppsEdit.arrange(names, Dock.appsOrder(this))" in main and "dock.add(rest.remove(0));" not in show)
check("the order is kept in the launcher's settings file as \"apps_order\"; nothing saved means by name, as always",
      'prefs(c).getString("apps_order", null)' in dock and 'e.putString("apps_order", String.join(",", order));' in dock)
check("«رجوع للترتيب الأصلي» removes only the order (what is hidden stays hidden) and redraws",
      'prefs(c).edit().remove("apps_order").apply();' in fn(dock, "static void resetAppsOrder(Context c)") and "CHANGED" in fn(dock, "static void resetAppsOrder(Context c)"))

# ---- one store with dock.sh
save = fn(dock, "static void saveApps(Context c, List<String> order, List<String> hide, List<String> show, List<String> row)")
check("hiding and showing write the same \"hidden\" and \"shown\" lists dock.sh hide / show do (hideOrShow), in one edit",
      'e.putString("hidden", String.join(",", hidden)).putString("shown", String.join(",", shown));' in save
      and "hidden.addAll(hide);" in save and "hidden.removeAll(show);" in save and "shown.removeAll(hide);" in save and "shown.addAll(show);" in save
      and 'prefSet(c, hide ? "hidden" : "shown")' in fn(dock, "static void hideOrShow(Context c, List<String> pkgs, boolean hide)"))
check("dock.sh hide / show and hidden still go through Dock (DockReceiver)", "Dock.hideOrShow(" in receiver and "Dock.leftOut(" in receiver)
check("«المخفية» lists the apps hidden with that list (Dock.hiddenApps, from the same scan as the grid)",
      "if (hidden.contains(p) && seenHidden.add(p) && hiddenOut != null) hiddenOut.add(ri);" in dock and "Dock.hiddenApps(this)" in main)
check("one save and one broadcast when the mode ends: the order (only when an app was moved), hidden and shown, the favourites",
      "Dock.saveApps(this, e.moved() ? order : null, e.hiddenNow(), e.shownNow(), row);" in fn(main, "private void leaveAppsEdit()")
      and save.count("e.apply();") == 1 and save.count("sendBroadcast") == 1)
check("nothing changed: nothing saved", "if (!e.changed()) return;" in fn(main, "private void leaveAppsEdit()"))
check("up into the favourites: at their end, never past Dock.MAX", "if (!row.contains(p) && row.size() < MAX_DOCK) row.add(p);" in fn(main, "private void leaveAppsEdit()")
      and "new AppsEdit(order, new ArrayList<>(hiddenFound.keySet()), cols, dockCards.size(), MAX_DOCK," in main)
check("full favourites say so", 'case AppsEdit.FAVOURITES_FULL:' in main and 'S.t("المفضلة ممتلئة (%s' in main)

# ---- entering, leaving, keys
check("«انقل» in the options of an app of «كل التطبيقات» starts «تعديل التطبيقات» on it (a favourite: «تعديل المفضلة»); not in kids mode",
      "if (!kids) {\n            o.move = () -> { if (inDock) beginEdit(pkg); else beginAppsEdit(pkg); };" in main)
check("the mode asks the settings' lock first, as «تعديل المفضلة» (ParentalPolicy.MENU_ARRANGE, «قفل الإعدادات»); kids mode never starts it",
      re.search(r'private void beginAppsEdit\(String pkg\) \{\s*if \(kidsNoEdit\(\)\) return;\s*guarded\(ParentalPolicy\.MENU_ARRANGE, null, S\.t\("تعديل التطبيقات"\), \(\) -> startAppsEdit\(pkg\)\);', main) is not None)
check("startAppsEdit is reached only through beginAppsEdit", len(re.findall(r"startAppsEdit\(", main)) == 2)
check("kids mode: no «تعديل المفضلة» either (it saved a row of the allowed apps only), the same rule (AppsEdit.allowed)",
      "if (!kids) {\n            o.move = " in main and "if (AppsEdit.allowed(Parental.state(this).kids)) return false;" in main)
check("one mode at a time", "if (edit != null || appsEdit != null || dockCards.isEmpty()) return;" in main and "if (appsEdit != null || edit != null) return;" in main)
check("the mode starts once the whole grid is built (addGridRows ran to its end)", "if (!gridDone) { wantAppsEdit = true; return; }" in main
      and "gridDone = true;\n        beginPendingAppsEdit();" in main)
d = main[main.index("public boolean dispatchKeyEvent"):]
check("keys: the adhan rule first, then the remote's shortcut keys (one that acts ends the mode, saved), then the mode",
      d.index("if (adhanTakes(e)) return true;") < d.index("remoteKeys.decide(") < d.index("if (appsEdit != null && appsEditKey(e)) return true;")
      and "if (appsEdit != null) leaveAppsEdit();   // and in «تعديل التطبيقات»" in d)
keyfn = fn(main, "private boolean appsEditKey(KeyEvent e)")
check("the mode leaves the volume and power keys to Android, eats a release, and steps a held key at the home's pace",
      "AdhanKeys.reserved(k)) return false" in keyfn and "ACTION_DOWN) return true" in keyfn and "SIDE_REPEAT_MS" in keyfn and keyfn.count("oncePerPress") == 2)
check("the arrows' direction comes from the grid's layout direction (FavEdit.step, Arabic)", "FavEdit.step(k, grid.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL)" in keyfn)
check("another screen (onPause) or Home leaves the mode, saving; a change of the apps under it (buildApps) ends it without saving",
      "if (appsEdit != null) leaveAppsEdit();" in fn(main, "protected void onPause()") and "if (appsEdit != null) leaveAppsEdit();" in fn(main, "protected void onNewIntent(Intent intent)")
      and "if (appsEdit != null) abandonAppsEdit();" in fn(main, "private void buildApps()"))
check("a rewiring while the mode is on ends with the mode's own wiring", fn(main, "private void wireFocus()").rstrip().endswith("if (appsEdit != null) wireAppsEditFocus();"))
check("a step of the carried app builds again only the rows that differ", "if (same) continue;" in fn(main, "private void syncGrid(List<String> order, boolean withHidden)"))

# ---- the settings
check("Settings → «التطبيقات والمفضلة» has «ترتيب كل التطبيقات», with «تعديل التطبيقات» and «رجوع للترتيب الأصلي»",
      'row("ترتيب كل التطبيقات"' in settings and '{"تعديل التطبيقات", "رجوع للترتيب الأصلي"}' in settings and '{"ترتيب كل التطبيقات", APPS}' in hub)
check("…which ask the home screen with the extras it reads", '.putExtra(reset ? "reset_apps_order" : "edit_apps", true)' in settings
      and 'EXTRA_EDIT_APPS = "edit_apps", EXTRA_RESET_APPS = "reset_apps_order"' in main)
check("…and «رجوع للترتيب الأصلي» asks the settings' lock too, and nothing in kids mode",
      'if (kidsNoEdit()) return true;\n            guarded(ParentalPolicy.MENU_ARRANGE, null, S.t("ترتيب كل التطبيقات")' in main)
move = src(ROOT / "optimize/settings_move.py")
check("settings_move does not move the favourites, so neither the order nor the hidden list (documented)",
      "dock" not in move.split("ALLOWED_TOP")[1].split(")")[0] and "apps_order" not in move)
check("documented: FEATURES.md, SETTINGS.md, CHANGELOG.md", all("تعديل التطبيقات" in src(ROOT / f) for f in ("docs/FEATURES.md", "docs/SETTINGS.md", "CHANGELOG.md")))

# ---- the cases, for real
if not shutil.which("javac"):
    print("SKIP apps edit cases: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in src(ROOT / "tests/apps_edit_cases.tsv").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(HOME / "AppsEdit.java"), str(HOME / "FavEdit.java"), str(HOME / "ParentalPolicy.java"),
                    str(HOME / "AdhanKeys.java"), str(ROOT / "tests/AppsEditRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.AppsEditRun", str(ROOT / "tests/apps_edit_cases.tsv")],
                         check=True, capture_output=True).stdout.decode("utf-8").split("\n")
for c, g in zip(cases, out):
    want, note = c[-2], c[-1]
    check(f"apps edit: {c[0]} {note}", g == want, f"{g!r}, wanted {want!r}")
check("an answer for every case", len(out) - 1 == len(cases), (len(out), len(cases)))
sys.exit(1 if failed else 0)
