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
check("the tray is a HorizontalScrollView with the bar's own glass, 218 high, in the safe area (120 from the sides, 54 from the bottom)",
      "tray = new android.widget.HorizontalScrollView(this);" in main and "new LinearLayout.LayoutParams(-1, bar(FavBar.TRAY_H))" in main
      and "bar(FavBar.SIDE), 0, bar(FavBar.SIDE), bar(FavBar.BOTTOM)" in main and "tray.setClipToPadding(true);" in main)
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
check("no tile beside the bar", "optionsTile" not in main)
check("the only move: 40 px down in 250 ms for a widget, back up the same way", "void lowerTray(boolean down)" in main
      and "bar(FavBar.WIDGET_DROP)" in main and "setDuration(FavBar.DROP_MS)" in main)
if not shutil.which("javac"):
    print("SKIP fav bar: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "FavBar.java"), str(ROOT / "tests/FavBarRun.java")], check=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.FavBarRun"], check=True, capture_output=True).stdout.decode().split("\n")
for line in out:
    if not line or line.startswith("nameX") or line.startswith("tile1080"): continue
    f = line.split(); h = int(f[0]); v = dict(zip(f[1::2], f[2::2])) if False else None
    nums = re.findall(r"(\w+) ([-\d.]+(?: [-\d.]+)*)", line)
    tray = list(map(int, line.split(" tray ")[1].split(" fits ")[0].split()))
    g = {k: float(line.split(f" {k} ")[1].split()[0]) for k in ("fits", "five", "width", "topRoom", "sideRoom", "liftedTopRoom", "dropped", "screen")}
    if h == 1080:
        check("1080p: the tray is x 120 to 1800 and y 808 to 1026", tray == [120, 1800, 808, 1026], tray)
    check(f"{h}p: the tiles and their padding fill the tray's height", g["fits"] == 1, g)
    check(f"{h}p: five tiles and their gaps fit the tray's width", g["five"] <= g["width"], g)
    check(f"{h}p: the focused tile, lifted and grown with its ring, stays at least 8 px (1080p) inside the tray's top and sides",
          g["topRoom"] >= 8 * h / 1080 - 1 and g["sideRoom"] >= 8 * h / 1080 - 1, g)
    check(f"{h}p: a tile picked up to move stays inside the tray", g["liftedTopRoom"] >= 0, g)
    check(f"{h}p: dropped 40 px for a widget, the tray is still on the screen", g["dropped"] <= g["screen"], g)
check("1080p: a tile is the spec's 304", [l for l in out if l.startswith("tile1080")][0].split()[1] == "304")
nx = [l for l in out if l.startswith("nameX")][0].split()[1:]
check("the name centres over its tile and never leaves its row", [float(x) for x in nx] == [400.0, 0.0, 1480.0], nx)
sys.exit(1 if failed else 0)
