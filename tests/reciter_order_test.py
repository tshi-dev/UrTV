#!/usr/bin/env python3
"""«القرّاء» in «تلاوة وأذان» (step 27, item 20): the favourite reciters first under «المفضلون», the letter index at the list's start edge, the dimmed letters, the saved
list in the export. The rules on a JVM (tests/ReciterOrderRun.java), then the wiring in the source. Run: python3 tests/reciter_order_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


act = text(HOME / "tilawah/MainActivity.java")
rcv = text(HOME / "tilawah/MusicReceiver.java")
lay = text(ROOT / "android/launcher/src/main/res/layout/activity_main.xml")
check("the order has no Android in it", "import android" not in text(HOME / "tilawah/ReciterOrder.java"))
check("the tab lists the reciters as ReciterOrder orders them (a heading «المفضلون», ★ before a favourite's name), the reciter selection (index into the catalogue, OK opens its surahs) is unchanged",
      "ReciterOrder.order(reciterIds()" in act and 'Row.header(sa.urtv.home.S.t("المفضلون"))' in act and '(it.fav ? "★ " : "") + reciter.name' in act and "putExtra(\"reciter\", row.index)" in act)
check("long OK on a reciter toggles ★ (the repeat that follows and the OK that ends it open nothing), saved in the home app's prefs", "event.getRepeatCount() == 1) { longFav = true; toggleFavourite(); return true; }" in act
      and "setReciterFavs(ReciterOrder.toggle(" in act and 'sp.getString("reciter_favs", "")' in text(HOME / "tilawah/Prefs.java"))
check("the letter column is at the list's start edge (first child of a row with the list), 40 dp wide, hidden on the other tabs, built from the alphabet of the language", 'android:id="@+id/letters"' in lay and lay.index("@+id/letters") < lay.index("@+id/list")
      and "ReciterOrder.alphabet(sa.urtv.home.S.en())" in act and "letters.setVisibility(tab == TAB_RECITERS ? View.VISIBLE : View.GONE);" in act)
check("a letter no reciter starts with is dimmed and cannot take the focus (the D-pad skips it); focusing or OK on a lit one jumps the list to the first reciter with it", "setFocusable(letterLit[i])" in act and "setAlpha(letterLit[i] ? 1f : 0.3f)" in act
      and "if (has) jumpToLetter(which);" in act and "list.setSelectionFromTop(reciterOffset + at, 0)" in act)
check("the key toward the column enters it from the list, the other key leaves it; the column keeps the safe area (a 40 dp column, 28 letters sharing the list's height)", "keyCode == sa.urtv.home.Dir.backKey() && enterLetters()" in act
      and "keyCode == sa.urtv.home.Dir.forwardKey()) focusList(" in act and "new LinearLayout.LayoutParams(-1, 0, 1f)" in act)
check("the home app's receiver: RECITER_FAV add|remove one id and RECITER_FAVS a whole list (\"-\" none), both only for ids the catalogue has; the status line carries reciter_favs", '"sa.urtv.home.RECITER_FAV"' in rcv and '"sa.urtv.home.RECITER_FAVS"' in rcv
      and 'error: unknown reciter' in rcv and '"|reciter_favs=" + prefs.reciterFavs()' in rcv)
sh = text(ROOT / "optimize/saver.sh")
check("saver.sh reciter-fav add|remove <id> checks the id's letters before it sends anything", "reciter-fav)" in sh and "*[!A-Za-z0-9_-]*" in sh and "RECITER_FAV --es op $op --es id $id" in sh)
sm = text(ROOT / "optimize/settings_move.py")
check("the export carries reciter_favs (read from the status line, written with RECITER_FAVS), validated as a list of plain ids", '"reciter_favs"' in sm and "RECITER_FAVS --es ids" in sm and "def read_reciter_favs" in sm)
if not shutil.which("javac"):
    print("SKIP reciter order: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "tilawah/ReciterOrder.java"), str(ROOT / "tests/ReciterOrderRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.ReciterOrderRun"], capture_output=True, text=True).stdout
print(out.strip())
check("every ReciterOrder case ran", "ALL PASS" in out, out[-200:])
sys.exit(1 if failed else 0)
