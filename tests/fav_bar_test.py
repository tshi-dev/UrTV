"""The favourites bar (FavBar.java, docs/PROPOSALS-NOV2.md §1): the tray's place and size, five tiles inside it, the focused tile's lift
and ring inside it (at least 8 px from its edges), the 40 px drop still on the screen; and the wiring in the home screen.
Run: python3 tests/fav_bar_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
check("the tray is a HorizontalScrollView with the bar's own glass, 218 high, in the safe area (128 from the sides, 54 from the bottom)",
      "tray = new TraySnap(this);" in main and "new LinearLayout.LayoutParams(-1, bar(FavBar.TRAY_H))" in main
      and "bar(FavBar.SIDE), 0, bar(FavBar.SIDE), bar(FavBar.REST_BOTTOM)" in main and "tray.setClipToPadding(true);" in main)
check("the same tray on every look: its colours are FavBar's, light or dark, never a look's own",
      "dockBand.setColor(light ? FavBar.FILL_LIGHT : FavBar.FILL_DARK);" in main and "dockBand.setColor(p.band);" not in main)
check("the favourites and the row under them take FavBar's cell (304 wide, 24 apart), not a share of the width",
      main.count("dockCell(") >= 6 and "dockRow.addView(tile, dockCell(i, dock.size()));" in main)
check("a focused favourite lifts 6 and grows 1.08 in 150 ms, its ring 3 px green (DGA on light, #8FE0B4 over a photo)",
      "translationY(has ? -bar(FavBar.LIFT) : 0)" in main and "FavBar.FOCUS_SCALE" in main and "setDuration(FavBar.MS)" in main
      and "FavBar.ring(lightNow)" in main)
check("the name is centred over the focused favourite, follows it as the tray scrolls, and hides when the focus leaves the bar",
      "placeName();" in main and "tray.setOnScrollChangeListener((v, x, y, ox, oy) -> placeName());" in main
      and "addOnGlobalFocusChangeListener((was, now) -> placeName())" in main and "nameRow.animate().alpha(inBar ? 1f : 0f)" in main)
bottom_adds = re.findall(r"bottom\.addView\((\w+)", main)
check("the tray always rests at y 768 (1080p): the first screen's bottom block holds only the name and the tray, so «أكمل المشاهدة» (its own row below) never lifts it",
      bottom_adds == ["titleRow", "tray"] and "content.addView(tvRowsBox" in main and "bottom.addView(tvRowsBox" not in main
      and "lp(-1, -2, Gravity.BOTTOM, bar(FavBar.SIDE), 0, bar(FavBar.SIDE), bar(FavBar.REST_BOTTOM))" in main, bottom_adds)
check("…and nothing else moves it: no translation or margin of the tray or its block but lowerTray's 40 px",
      main.count("tray.animate()") == 1 and "tray.setTranslationY" not in main and "bottomBlock.setTranslationY" not in main
      and "bottomBlock.animate()" not in main)
check("no tile beside the bar", "optionsTile" not in main)
check("the only move: 40 px down in 250 ms for a widget, back up the same way", "void lowerTray(boolean down)" in main
      and "bar(FavBar.WIDGET_DROP)" in main and "setDuration(FavBar.DROP_MS)" in main)
if not shutil.which("javac"):
    print("SKIP fav bar: no javac here")
    check("build 180: the tray cuts everything it holds to its rounded outline (a sixth tile was drawn outside it, x 0..128), and the name stays in one place: above the tray at its start edge, in line with the inner padding, never following a tile",
      "tray.setClipToOutline(true);" in main_src and "tray.setClipChildren(true);" in main_src and "o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), bar(FavBar.RADIUS))" in main_src
      and "nameRow.setTranslationX(0);" in main_src and "setMarginStart(bar(FavBar.PAD_SIDE))" in main_src and "FavBar.nameX(cardCentre" not in main_src)
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "FavBar.java"), str(ROOT / "tests/FavBarRun.java")], check=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.FavBarRun"], check=True, capture_output=True).stdout.decode().split("\n")
for line in out:
    if not line or line.startswith("nameX") or line.startswith("tile1080") or line.startswith("bar ") or line.startswith("shown ") or line.startswith("side ") or line.startswith("scroll "): continue
    f = line.split(); h = int(f[0]); v = dict(zip(f[1::2], f[2::2])) if False else None
    nums = re.findall(r"(\w+) ([-\d.]+(?: [-\d.]+)*)", line)
    tray = list(map(int, line.split(" tray ")[1].split(" fits ")[0].split()))
    g = {k: float(line.split(f" {k} ")[1].split()[0]) for k in ("fits", "five", "width", "padS", "padT", "topRoom", "sideRoom", "liftedTopRoom", "dropped", "safe", "screen")}
    if h == 1080:
        check("1080p: the tray rests at x 128 to 1792 and y 768 to 986", tray == [128, 1792, 768, 986], tray)
    check(f"{h}p: the tiles and their padding fill the tray's height", g["fits"] == 1, g)
    check(f"{h}p: five tiles, their gaps and a 24 px inset on both sides are exactly the tray's width (no room for a sixth, no sliver)", abs(g["five"] - g["width"]) <= 5, g)
    check(f"{h}p: the inset is 24 px (1080p) on all four sides", g["padS"] == g["padT"] == 24 * h / 1080 and g["padS"] == g["padT"], g)
    check(f"{h}p: the focused tile, lifted and grown with its ring, stays at least 8 px (1080p) inside the tray's top and sides",
          g["topRoom"] >= 8 * h / 1080 - 1 and g["sideRoom"] >= 8 * h / 1080 - 1, g)
    check(f"{h}p: a tile picked up to move stays inside the tray", g["liftedTopRoom"] >= 0, g)
    check(f"{h}p: dropped 40 px for a widget, the fully lowered tray still ends inside the safe area (y 1026 at 1080p)", g["dropped"] <= g["safe"], g)
sc = [int(x) for x in [l for l in out if l.startswith("scroll ")][0].split()[1:]]
check("the tray scrolls a focused tile whole into view with the inset: nothing when it is inside, one pitch (304 + 24) to the next tile, never past the ends, never when five or fewer",
      sc == [0, 0, -656, -328, 0], sc)
main_src = (HOME / "MainActivity.java").read_text(encoding="utf-8")
check("the tray is a TraySnap (FavBar.scrollDelta with the inset) so a focused tile is never flush with its edge", "tray = new TraySnap(this);" in main_src
      and "FavBar.scrollDelta(r.left, r.right, getScrollX(), getWidth(), bar(FavBar.PAD_SIDE), getChildAt(0).getWidth())" in main_src)
check("the tray's bottom is never lower than 1026: it rests 40 higher (REST_BOTTOM 94) and the lowering by WIDGET_DROP 40 ends exactly there",
      "BOTTOM = 54, REST_BOTTOM = BOTTOM + 40" in (HOME / "FavBar.java").read_text(encoding="utf-8") and "WIDGET_DROP = 40" in (HOME / "FavBar.java").read_text(encoding="utf-8"))
# «تطبيقات في الشريط»: N whole tiles, 24 gaps, 24 insets, the tray's box unchanged
bars = {}
for l in out:
    if l.startswith("bar "):
        t = l.split()
        bars[(int(t[1]), int(t[2]))] = {"tile": int(t[4]), "row": int(t[6]), "inner": int(t[8]), "slack": int(t[10]), "ratio": t[12] == "1", "ring": t[14] == "1", "fill": int(t[16])}
check("bar size: 5 shows the default 304, 7 shows 210, 10 shows 140 at 1080p ((inner 1616 - (N-1) x 24) / N, rounded down)", bars[(1080, 5)]["tile"] == 304 and bars[(1080, 7)]["tile"] == 210 and bars[(1080, 10)]["tile"] == 140, bars)
check("step 28: the tiles of every N (5 to 10) at 1080p and 720p, with the rounding's leftover pixels spread over the first ones, fill the tray's inner width exactly (no 2 px sliver of the next tile)", all(v["fill"] == 0 for v in bars.values()), bars)
check("bar size: for every N from 5 to 10 at 1080p and 720p the tiles fill the tray's inner width to within N pixels (whole tiles, 24 apart), never past it", all(0 <= v["slack"] <= n for (h, n), v in bars.items()), bars)
check("bar size: the tile keeps its 16:9 ratio, and the focused tile's ring stays inside the 24 px inset, for every N", all(v["ratio"] and v["ring"] for v in bars.values()), bars)
shown = [l for l in out if l.startswith("shown ")][0].split()[1:]
check("bar size: «5» is 5, «7» is 7 even with 3 favourites (the tray keeps its size), «10» is 10, «all» is the favourites up to 10 but never fewer than 5, a number out of range or a word is clamped / 5",
      shown == ["5", "7", "10", "8", "5", "10", "10", "5", "5", "5"], shown)
check("1080p: a tile is the spec's 304", [l for l in out if l.startswith("tile1080")][0].split()[1] == "304")
side = [l for l in out if l.startswith("side ")][0].split()[1:]
check("◀ ▶: English (left to right) ▶ goes to the next favourite and ◀ stays at the first; Arabic ▶ goes to the one before; the edges stay (tester step 20)",
      side == ["1", "-1", "1", "-1", "1", "-1"], side)
check("the home walks its rows with FavBar.sideStep on the row as drawn, and the favourites row is laid out by Dir",
      "FavBar.sideStep(index, row.getChildCount(), right, rtl)" in (HOME / "MainActivity.java").read_text(encoding="utf-8")
      and "dockRow.setLayoutDirection(Dir.layout());" in (HOME / "MainActivity.java").read_text(encoding="utf-8"))
nx = [l for l in out if l.startswith("nameX")][0].split()[1:]
check("the name centres over its tile and never leaves its row", [float(x) for x in nx] == [400.0, 0.0, 1480.0], nx)
sys.exit(1 if failed else 0)
