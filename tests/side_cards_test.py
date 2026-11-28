#!/usr/bin/env python3
"""4.4 beta 4 (the owner's idea, docs/mockups/couch-side-cards/): «كنبة»'s small cards down the left, A (the next prayer, the hours' weather) or B
(and the next match), OFF by default: the owner's pick is the one word Prefs.SIDE_CARDS_DEFAULT. Off, nothing of it exists on the home.
    python3 tests/side_cards_test.py"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
prefs = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java").read_text(encoding="utf-8")
recv = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SaverReceiver.java").read_text(encoding="utf-8")
side = (ROOT / "android/launcher/src/main/java/sa/urtv/home/SideCards.java").read_text(encoding="utf-8")
main = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
page = (ROOT / "src/template.html").read_text(encoding="utf-8")
sh = (ROOT / "optimize/saver.sh").read_text(encoding="utf-8")
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

check("off by default, and the owner's pick is one word (Prefs.SIDE_CARDS_DEFAULT)", 'static final String SIDE_CARDS_DEFAULT = "off";' in prefs
      and 'sp.getString("sideCards", SIDE_CARDS_DEFAULT)' in prefs and 'static final String[] SIDE_CARDS = {"off", "a", "b"};' in prefs)
check("off sends nothing to the page; a or b travels as #sidecards=", '("off".equals(sideCards()) ? "" : "&sidecards=" + sideCards())' in prefs)
check("the agent's way: saver.sh sidecards off|a|b, checked, and `show` says it", 'sidecards)set1 sidecards' in sh and '"error: sidecards is one of off, a, b"' in recv
      and 'p.setSideCards(sideCards)' in recv and '"\\nsidecards: " + p.sideCards()' in recv)
check("A is the next prayer and the hours' weather, B adds the next match", 'return "a".equals(option) ? "nextprayer,hourly" : "b".equals(option) ? "nextprayer,hourly,nextmatch" : null;' in side)
check("only in «كنبة», and nothing at all when off (no view, no page)", 'if (opt == null || !"couch".equals(layout)) return null;' in side and 'if (url == null) { drop(); return; }' in side)
check("the column is the mockup's: x 122, 340 wide, y 120 to 554 at 1080p (20 above the strip), scaled with the screen",
      "static final int X = 122, W = 340, TOP = 120, BOTTOM = 554, GAP = 20;" in side and "FavBar.px(W, h), FavBar.px(BOTTOM - TOP, h)" in side)
check("it never runs into the cards strip (run 22: with the plain clock the strip is higher): checked at every layout, hidden while its whole height does not end 20 above the strip, never cut",
      "parent.getViewTreeObserver().addOnGlobalLayoutListener(fitter);" in side and "parent.getViewTreeObserver().removeOnGlobalLayoutListener(fitter);" in side
      and "int vis = top < 0 || FavBar.px(BOTTOM + GAP, h) <= top ? View.VISIBLE : View.INVISIBLE;" in side
      and "() -> homeCard == null ? -1 : homeCard.stripTop()" in main and "int stripTop() {" in (ROOT / "android/launcher/src/main/java/sa/urtv/home/HomeCard.java").read_text(encoding="utf-8"))
check("it never takes the focus (the D-pad's ways on the home are unchanged)", "web.setFocusable(false);" in side and "FOCUS_BLOCK_DESCENDANTS" in side)
check("the settings are read on the loader, the view placed on the UI thread, with the home card's refreshes; dropped on destroy",
      "final String u = side.read();" in main and "loader.execute(() -> {" in main[main.index("private void refreshSideCards()"):]
      and main.count("refreshSideCards();") >= 2 and "if (sideCards != null) sideCards.drop();" in main)
check("the page: #side=1 is one narrow column, the cards share its height, no two side by side", "if(_Q.get('side') === '1') document.documentElement.classList.add('side');" in page
      and "html.side .mid,html.side .mid.n2,html.side .mid.n3{grid-template-columns:minmax(0,1fr)!important" in page)
sys.exit(1 if failed else 0)
