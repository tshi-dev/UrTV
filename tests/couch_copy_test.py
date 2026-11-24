#!/usr/bin/env python3
"""4.4 beta 4 (the owner: «كنبة» as couch-tv's own design), step 1 beyond the faces (tests/couch_faces_test.py): the bar in «أيقونات» ends as
couch-tv's does, with «الإعدادات» (OK opens our settings) and «كل التطبيقات» (their «المزيد»), each with its name under the bar while focused,
the size and a soft lift as the focus (no ring); a new favourite goes before them. With the big clock and the cards in their row, the date sits
centred just above the digits (couch-tv's date over its clock), the zone and prayer lines with it; other layouts keep their lift.
    python3 tests/couch_copy_test.py"""
import sys
from pathlib import Path

H = Path(__file__).resolve().parents[1] / "android/launcher/src/main/java/sa/urtv/home"
main = (H / "MainActivity.java").read_text(encoding="utf-8")
failed = 0
def check(name, ok):
    global failed
    print(("PASS " if ok else "FAIL ") + name)
    failed += not ok

add = main[main.index("    private void addAllAppsTile() {"):]
add = add[:add.index("\n    }\n") + 6]
check("«أيقونات» ends with the two tiles too (not only «زجاجي»), «الإعدادات» first", "boolean icons = FavBar.ICONS.equals(appsLook);" in add
      and "if (!glassWay() && !icons) return;" in add and "if (icons) addSettingsTile(corner);" in add)
st = main[main.index("    private void addSettingsTile(int corner) {"):main.index("    private int sh() {")]
check("«الإعدادات»: a flat frosted square with the gear, no ring; OK opens our settings; the focus is the size and a soft lift",
      "R.drawable.ic_settings" in st and "openOurSettings(false)" in st and "setStroke" not in st and "scaleTo(v, has ? FavBar.ZOOM_SCALE : 1f);" in st)
check("both keep their place at the end: re-added after a reorder, a new favourite goes before them",
      "if (settingsBox != null) dockRow.addView(settingsBox);" in main and main.index("if (settingsBox != null) dockRow.addView(settingsBox);") < main.index("if (allAppsBox != null) { dockRow.addView(allAppsBox); relayDock(); }")
      and "View endFirst = settingsBox != null && settingsBox.getParent() == dockRow ? settingsBox : allAppsBox;" in main)
check("their names show under the bar while focused, as an app's", "f == allAppsCard || f == settingsCard" in main and 'else if (f == settingsCard) zoomName.setText(S.t("الإعدادات"));' in main)
lift = main[main.index("    private void applyLift() {"):]
lift = lift[:lift.index("\n    }\n") + 6]
check("the date centred just above the big clock's digits in «كنبة» (the cards in their row), the zone and prayer lines with it; otherwise no sideways move",
      "if (bigClockOn && homeCard != null && homeCard.inRow() && date.getWidth() > 0) {" in lift and "ScenePlan.fixedBox(dm.widthPixels, dm.heightPixels, false, Dir.rtl())" in lift
      and "dx = (box[0] + box[2]) / 2f - date.getWidth() / 2f - x0;" in lift and "dy = box[1] - date.getHeight() - dp(10) - y0;" in lift and "float dy = bigClockOn ?" in lift
      and "v.setTranslationX(dx);" in lift)
sys.exit(1 if failed else 0)
