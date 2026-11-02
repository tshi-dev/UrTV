#!/usr/bin/env python3
"""«تعديل المفضلة» on the home screen: the pure state machine (FavEdit.java: pick up, move, drop, cancel, remove, undo), the Arabic direction of
the arrows, the pace of a held key, the gate that guards entering the mode, and the adhan rule over its keys, on tests/fav_edit_cases.tsv; and
the wiring, read from the source (it needs Android): the mode saves through the store «ترتيب المفضلة» and dock.sh share, the old arrange
screen is reached from the settings only, the adhan is asked first.   Run: python3 tests/fav_edit_test.py   (the cases need javac)"""
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

main, edit, dock, receiver, arrange = src(HOME / "MainActivity.java"), src(HOME / "FavEdit.java"), src(HOME / "Dock.java"), src(HOME / "DockReceiver.java"), src(HOME / "DockActivity.java")
settings = src(APP / "SettingsActivity.java")

# ---- the wiring
check("FavEdit has no Android in it (the test runs it without the SDK)", "import android" not in edit)
leave = main.split("private void leaveEdit()")[1].split("\n    }\n")[0]
check("BACK saves through Dock.save, the store «ترتيب المفضلة» and dock.sh use", "Dock.save(this, row)" in leave)
check("…and nowhere else writes the order: no preference of its own in the mode", not re.search(r'putString\("dock"', main.split("«تعديل المفضلة»")[1].split("private void saveDock")[0]))
check("«ترتيب المفضلة» saves through Dock.save", "Dock.save(this, row)" in arrange)
check("dock.sh's SET_DOCK saves through Dock.save", re.search(r"Dock\.save\(c, row\)", receiver) is not None)
check("Dock.save keeps the order in the \"dock\" preference and tells the home screen", 'putString("dock"' in dock.split("static void save")[1] and "CHANGED" in dock.split("static void save")[1])
check("an unchanged row is not saved (no top-up kept by a visit)", "if (e.changed())" in leave)
check("the mode asks the settings' lock before it starts (ParentalPolicy.MENU_ARRANGE, «قفل الإعدادات»); in kids mode it does not start",
      re.search(r"private void beginEdit\(String pkg\) \{\s*if \(kidsNoEdit\(\)\) return;\s*guarded\(ParentalPolicy\.MENU_ARRANGE, null, S\.t\(\"تعديل المفضلة\"\), \(\) -> startEdit\(pkg\)\);", main) is not None)
check("startEdit is reached only through beginEdit", len(re.findall(r"startEdit\(", main)) == 2)
check("the adhan rule is asked before the mode sees a key",
      main.index("if (adhanTakes(e)) return true;") < main.index("if (edit != null && editKey(e)) return true;"))
keyfn = main.split("private boolean editKey(KeyEvent e)")[1].split("private void applyEdit")[0]
check("the mode leaves the volume and power keys to Android (AdhanKeys.reserved)", "AdhanKeys.reserved(k)) return false" in keyfn)
check("a key release is consumed, so OK never clicks the app under it", "ACTION_DOWN) return true" in keyfn)
check("a held key steps at the pace the home screen already uses (SIDE_REPEAT_MS)", keyfn.count("SIDE_REPEAT_MS") >= 3)
check("the direction comes from the row's layout direction (FavEdit.step)", "FavEdit.step(k, dockRow.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL)" in keyfn)
check("the hint line is the one asked for", "اختر تطبيقًا لتحريكه · × للحذف · رجوع للإنهاء" in edit)
check("«تراجع» lasts 5 seconds", "UNDO_MS = 5000" in edit)
check("the row changing under the mode (an app installed, dock.sh) ends it without saving: buildApps abandons it",
      "if (edit != null) abandonEdit();" in main.split("private void buildApps()")[1][:200])
check("another screen coming up (onPause) or Home leaves the mode, saving", main.split("protected void onPause()")[1].split("\n    }\n")[0].count("leaveEdit()") == 1 and "if (edit != null) leaveEdit();   // Home while editing" in main)
# the old arrange screen: from the settings only
check("the long-press menu no longer opens the old arrange screen", "DockActivity" not in main)
check("the long-press menu has «تعديل المفضلة» for a favourite and not «ترتيب المفضلة…»",
      '"تعديل المفضلة"' in main.split("private void showAppMenuNow")[1].split("showMenu(anchor")[0] and "ترتيب المفضلة…" not in main)
others = [p.name for p in HOME.glob("*.java") if p.name not in ("DockActivity.java",) and re.search(r"\bDockActivity\b", src(p).replace("`am start -n sa.urtv.home/.DockActivity`", ""))]
check("no launcher code starts DockActivity (only the manifest and the settings reach it)", not [n for n in others if re.search(r"DockActivity\.class|\.DockActivity\"", src(HOME / n))], others)
check("the settings' «ترتيب التطبيقات المفضلة» opens the old arrange screen", "sa.urtv.home.DockActivity" in settings and 'row("ترتيب التطبيقات المفضلة"' in settings)
check("the settings have «تعديل المفضلة», which asks the home screen for the mode", 'row("تعديل المفضلة"' in settings and '"edit_favourites"' in settings and 'EXTRA_EDIT = "edit_favourites"' in main)
check("nothing else in the app starts DockActivity (the screensaver app's only way in is the settings row)",
      len(re.findall(r"sa\.urtv\.home\.DockActivity", "".join(src(p) for p in APP.glob("*.java")))) == 1)
d = main[main.index("public boolean dispatchKeyEvent"):]
check("keys: the adhan rule first, then the remote's shortcut keys (one that acts ends the edit, saved), then «تعديل المفضلة»",
      d.index("if (adhanTakes(e)) return true;") < d.index("remoteKeys.decide(") < d.index("if (edit != null && editKey(e)) return true;")
      and "if (edit != null) leaveEdit();   // a shortcut key" in d)
check("the long-press menu: «تعديل المفضلة» on a favourite, and no «ترتيب المفضلة…» (that list is the settings' only)",
      'labels.add(S.t("تعديل المفضلة"))' in main and "ترتيب المفضلة…" not in main)
# ---- «تراجع» from the TV test: UP from the tile row did not reach the chip and the seconds ran out. The keys are the mode's own (editKey),
# and under them Android's own focus search is wired so that no key that reaches the views can leave the row or miss the chip
fn = lambda name: main.split(name)[1].split("\n    }\n")[0]
keyfn_ = fn("private boolean editKey(KeyEvent e)")
check("«تراجع»: UP and Back in the mode are handled in editKey (edit.up, edit.back), not left to Android",
      "k == KeyEvent.KEYCODE_DPAD_UP" in keyfn_ and "applyEdit(edit.up(0, now))" in keyfn_ and "applyEdit(edit.back(now))" in keyfn_ and keyfn_.rstrip().endswith("return true;"))
wire = fn("private void wireEditFocus()")
check("«تراجع»: while it shows, UP from every tile of the row is the chip (nextFocusUp), else the tile itself",
      "c.setNextFocusUpId(chip ? undoChip.getId() : c.getId())" in wire and "for (View c : dockCards)" in wire)
check("«تراجع»: from the chip DOWN is the focused tile, and UP, LEFT and RIGHT stay on it",
      "undoChip.setNextFocusDownId(tile.getId())" in wire and all(f"undoChip.setNextFocus{w}Id(undoChip.getId())" in wire for w in ("Up", "Left", "Right")))
check("«تراجع»: a tile's DOWN is its ×, and from the × UP is the tile", "c.setNextFocusDownId(x != null ? x.getId() : c.getId())" in wire and "x.setNextFocusUpId(c.getId())" in wire)
paint = fn("private void paintEdit()")
check("«تراجع»: every redraw of the mode wires it before it moves the focus, and the focus goes to the chip when the model is on it",
      paint.index("wireEditFocus();") < paint.index("requestFocus()") and "e.onUndo() && chip ? undoChip" in paint)
check("«تراجع»: a focus request the chip refuses (not laid out yet) is made again once it is",
      "!target.requestFocus() && target == undoChip" in paint and "undoChip.post(" in paint and "edit.onUndo()" in paint)
check("«تراجع»: a rewiring of the screen while the mode is on (the row, the pills) ends with the mode's own wiring",
      fn("private void wireFocus()").rstrip().split("\n")[-2].strip() == "if (edit != null) wireEditFocus();   // a rewiring while «تعديل المفضلة» is on (the row, the pills) keeps the mode's own"
      and fn("private void wireFocus()").rstrip().endswith("if (appsEdit != null) wireAppsEditFocus();"))
check("«تراجع»: leaving the mode wires the row's own ways again (up to the pills)", "wireFocus();" in fn("private void endEditUi(int focus)"))
check("«تراجع»: Back while it shows undoes (FavEdit.back), checked case by case below", "if (undoAvailable(now)) return undo(now);" in edit.split("int back(long now)")[1].split("\n    }\n")[0])
dock_sh = (ROOT / "optimize/dock.sh").read_text(encoding="utf-8")
check("dock.sh keeps the agent's way (apps, set, show, reset) and knows nothing of the edit mode",
      all(a in dock_sh for a in ("send LIST_APPS", "send SET_DOCK", "send GET_DOCK", "send RESET_DOCK")) and "edit_favourites" not in dock_sh and "FavEdit" not in dock_sh)

if not shutil.which("javac"):
    print("SKIP fav edit cases: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in src(ROOT / "tests/fav_edit_cases.tsv").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(HOME / "FavEdit.java"), str(HOME / "ParentalPolicy.java"), str(HOME / "AdhanKeys.java"),
                    str(ROOT / "tests/FavEditRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.FavEditRun", str(ROOT / "tests/fav_edit_cases.tsv")],
                         check=True, capture_output=True).stdout.decode("utf-8").split("\n")
for c, g in zip(cases, out):
    want, note = c[-2], c[-1]
    check(f"fav edit: {c[0]} {note}", g == want, f"{g!r}, wanted {want!r}")
check("an answer for every case", len(out) - 1 == len(cases), (len(out), len(cases)))
sys.exit(1 if failed else 0)
