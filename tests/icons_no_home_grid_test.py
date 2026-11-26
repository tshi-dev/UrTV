#!/usr/bin/env python3
"""4.4 beta 4 (the owner's TV, build 620, «أيقونات»): one DOWN on the home landed in the apps grid under the row, as if «كل التطبيقات» had
opened. couch-tv's home is one row ending with «المزيد»: in «أيقونات» the home has no apps grid (the row's «كل التطبيقات» opens them), DOWN
from the row goes to the cards row or stays, and the start focus is the first favourite. «تعديل التطبيقات» shows the grid while it is on;
every other look keeps its grid as before.
    python3 tests/icons_no_home_grid_test.py"""
import sys
from pathlib import Path

H = Path(__file__).resolve().parents[1] / "android/launcher/src/main/java/sa/urtv/home"
main = (H / "MainActivity.java").read_text(encoding="utf-8")
failed = 0
def check(name, ok):
    global failed
    print(("PASS " if ok else "FAIL ") + name)
    failed += not ok

def body(sig):
    b = main[main.index(sig):]
    return b[:b.index("\n    }\n") + 6]

check("the rule: the grid on the home unless «أيقونات», and always while «تعديل التطبيقات» is on",
      "static boolean homeGrid(String appsLook, boolean appsEditOn) { return appsEditOn || !FavBar.ICONS.equals(appsLook); }" in main
      and "private boolean homeGrid() { return homeGrid(appsLook, appsEdit != null); }" in main)
w = body("    private void wireFocus() {")
check("wireFocus hides the grid and its label in «أيقونات» and wires no way into it (g = 0: DOWN goes to the cards row or stays)",
      "grid.setVisibility(onHome ? View.VISIBLE : View.GONE);" in w and "if (!onHome) appsLabel.setVisibility(View.GONE);" in w
      and "g = onHome ? gridCards.size() : 0;" in w and w.index("boolean onHome = homeGrid();") < w.index("g = onHome"))
check("the dock's DOWN still uses g (so with no grid it is the cards row or the tile itself)",
      ": g > 0 ? gridCards.get(Math.min(i, g - 1)).getId() : c.getId());" in w)
e = body("    private void endAppsEditUi(String focusPkg) {")
check("leaving «تعديل التطبيقات» in «أيقونات» puts the focus on the row, not on a hidden grid tile",
      "if (f != null && gridCards.contains(f) && homeGrid()) f.requestFocus();" in e and e.index("wireFocus();") < e.index("homeGrid()"))
check("the start focus is the first favourite when nothing was focused before",
      "else if (!dockCards.isEmpty()) dockCards.get(0).requestFocus();" in main)
sys.exit(1 if failed else 0)
