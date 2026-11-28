#!/usr/bin/env python3
"""Second review pass, F121 and F122: «كنبة»'s side cards are a second WebView page. (F121) A trim of the home's memory lets it go with the home's card. (F122) «UrTV النظيف»
is bare: no side cards while the mask is on, and they come back when it goes."""
import sys
from pathlib import Path
s = (Path(__file__).resolve().parents[1] / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
failed = 0
def check(n, ok):
    global failed
    print(("PASS " if ok else "FAIL ") + n); failed += not ok
trim = s.split("public void onTrimMemory(int level) {")[1].split("\n    }\n")[0]
check("F121: a trim of the memory drops the side cards beside the home's card (when the home is behind another app)", "homeCard.destroy();" in trim and "if (sideCards != null) sideCards.drop();" in trim
      and trim.index("homeCard.destroy();") < trim.index("sideCards.drop();"))
rs = s.split("private void refreshSideCards() {")[1].split("\n    }\n")[0]
check("F122: with the clean home on, refreshSideCards draws nothing and drops what is there", rs.lstrip().startswith("if (cleanShown) { if (sideCards != null) sideCards.drop(); return; }"))
ac = s.split("private void applyClean() {")[1].split("\n    }\n")[0]
check("F122: applyClean asks for the side cards again (so they go with the mask and come back without it)", "refreshSideCards();" in ac and ac.index("homeCard.hide(on);") < ac.index("refreshSideCards();"))
sys.exit(1 if failed else 0)
