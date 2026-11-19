#!/usr/bin/env python3
"""4.4 beta 2 the full look «tvOS» on the home (the owner approved docs/mockups/tvos-preset/ and the joint full-look proposal): the preset «tvOS»
(after «كنبة»: the layout tvos, the photos, the plain clock, the tvOS bar, the tvOS settings screen; the rest the person's own); the layout tvos
only through it; the cards in one row exactly as wide as the bar, above it; each slot as CardFit decides (the provider's card_fit, never a copy of
the table); and on a weak TV nothing grows and the bar is flat. The geometry runs on a JVM (tests/TvosRowRun.java).   python3 tests/tvos_home_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
prefs, st, recv, prov = read(APP / "Prefs.java"), read(APP / "SettingsActivity.java"), read(APP / "SaverReceiver.java"), read(APP / "ConfigProvider.java")
main, card, box, bar = read(HOME / "MainActivity.java"), read(HOME / "HomeCard.java"), read(HOME / "CardBox.java"), read(HOME / "GlassBar.java")
page = read(ROOT / "src/template.html")

# ---- the look
check("the preset «tvOS» after «كنبة»: the layout tvos, the photos, the plain clock, the tvOS bar; the rest the person's own (no forced dark)",
      'PRESETS = {"tv", "couch", "tvos",' in prefs and '{"tvos", "photos", "plain", "glass", null, null, null, null, null, null, null, null},' in prefs)
check("a full design: it sets the tvOS settings screen, and «شكلك المعتاد» gives the person's back", 'm.put("tvos", "tvos");' in prefs and "FULL_DESIGN_STYLE.get(key)" in prefs)
check("the layout tvos only through the full look: «التخطيط» lists it only while it is the layout; saver.sh homelayout tvos is refused with the way",
      'if (Prefs.TVOS_LAYOUT.equals(Prefs.HOME_LAYOUTS[i]) && !Prefs.TVOS_LAYOUT.equals(prefs.homeLayout())) continue;' in st
      and 'if (Prefs.TVOS_LAYOUT.equals(homeLayout)) return "error: tvos comes with the full look: optimize/saver.sh preset tvos";' in recv)
check("its picture in the app, small", (ROOT / "android/app/src/main/assets/presets/tvos.jpg").stat().st_size < 40_000)

# ---- the cards
check("each slot's CardFit decision for the look and the background is a provider column (card_fit), the only copy the home and the page read",
      '"card_fit", "apps_align"});' in prov and "CardFit.decide(cards.get(i), p.cardLook(), p.homeBg())" in prov and 'String cardLook() { return homeLayout(); }' in prefs
      and 'getColumnIndex("card_fit")' in card and "CardFit" not in card.replace("CardFit:", "").replace("CardFit decision", ""))
check("the home: the layout tvos is a row (as couch), the page told tvos, the decisions and how many show",
      'tvos = "tvos".equals(layout);' in card and "if (tvos) row = true;" in card and '"&tvos=1&fit=" + android.net.Uri.encode(fit) + "&slots=" + tvosSlots()' in card
      and "if (row && homeCard != null && homeCard.tvos()) return CardBox.tvosRow(" in main)
check("the page: html.tvos glass rectangles (flat on a weak TV), #fit= as each slot's class (two wide, four days), rowOverflow for a row with more cards than slots",
      "if(_Q.get('tvos') === '1') document.documentElement.classList.add('tvos');" in page and "html.tvos.lowram .week" in page
      and "if(FIT[c.i] === 'wide') c.el.classList.add('fwide');" in page and "html.tvos .mid .week.fwide{grid-column:span 2}" in page
      and "html.tvos .week.frow4 ul{display:grid;grid-template-columns:repeat(4,minmax(0,1fr))" in page and "function rowOverflow(slots){" in page
      and "setInterval(() => { if(document.hidden) return; k = (k + 1) % turn.length; show(); }, 20 * 1000);" in page)

if not shutil.which("javac"):
    print("SKIP the geometry: no javac here")
else:
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "FavBar.java"), str(HOME / "CardBox.java"), str(ROOT / "tests/TvosRowRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.TvosRowRun"], check=True, capture_output=True).stdout.decode().split("\n")
    rows = {(int(l.split()[1]), float(l.split()[2])): [int(x) for x in l.split()[3:]] for l in out if l.startswith("row ")}
    for (h, f), (w, rh, bottom, title, side, gap, slots) in rows.items():
        sw = 1920 if h == 1080 else 1280
        check(f"{h}p, font {f}: the row exactly as wide as the bar's tray (SIDE each side), ending {gap} px above the name and the bar",
              w == sw - 2 * side and h - bottom == title - gap, (w, sw, side, bottom, title, gap))
    check("a title and three lines at 1080 (about 254 px), taller with the large font so nothing is cut", rows[(1080, 1.0)][1] == 254 and rows[(1080, 1.3)][1] > rows[(1080, 1.0)][1], rows)
    check("three cards across; two at 720p with the large font (the third takes turns)", rows[(1080, 1.0)][6] == 3 and rows[(1080, 1.3)][6] == 3 and rows[(720, 1.0)][6] == 3 and rows[(720, 1.3)][6] == 2, rows)

# ---- a weak TV
check("a weak TV: the focused tvOS icon grows at once like every icon (the owner, 19 Nov: no ring, so the size is the focus), the bar is a flat fill with its edge and reads no picture",
      "float s = has ? (inDock ? (zoom ? FavBar.ZOOM_SCALE : FavBar.FOCUS_SCALE) : 1.1f) : 1f;" in main
      and "if (glassy && Ram.low(MainActivity.this)) { v.animate().cancel(); v.setScaleX(s); v.setScaleY(s); }" in main and "scaleTo(v, has ? FavBar.ZOOM_SCALE : 1f);" in main
      and "glassBar.flat(Ram.low(this));" in main and "if (!glassWay() || loader.isShutdown() || Ram.low(this)) return;" in main and "if (backdrop != null && !flat) {" in bar)
sys.exit(1 if failed else 0)
