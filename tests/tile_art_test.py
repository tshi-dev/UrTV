#!/usr/bin/env python3
"""4.4 beta 2, the owner's test of build 525 («the icons need to be 100% perfect; what I saw was baaad»): an app with no TV banner is drawn as its own
icon, large, on a flat neutral tile, never a dark tint of its colour; a banner fills the tile. The hard cases each have a rule here: no banner and a
tiny icon, a wide logo, a white icon on white, a transparent icon. TileArt runs on a JVM; MainActivity's wiring is read from the source.
    python3 tests/tile_art_test.py      (needs javac)"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
fav = (HOME / "FavBar.java").read_text(encoding="utf-8")
tile = main[main.index("    private View tile(App app, boolean inDock) {"):main.index("    /** Explicit D-pad targets")]
check("no dark tint of the app's colour behind a tile any more (darken is gone)", "darken(" not in main)
check("an app with a banner: its banner fills the tile; with none, its icon composed on a neutral tile, drawn full-bleed the same way",
      "} else if (app.isBanner || app.composed) {" in tile and "app.composed && !icons ? 0 : TileArt.LIGHT" in tile)
check("the composing is done once per app on the apps' loading thread (appOf), cached, never on the main thread",
      'Drawable t = artCache.get("t:" + a.pkg);' in main and "t = composeTile(raw);" in main and main.index("t = composeTile(raw);") < main.index("    /** On the main thread: the dock first")
      and "composeTile" not in tile)
check("the glow keeps the app's own colour (sampled from its icon before it is put on the tile)", main.index("a.color = sampleColor(a.pkg, a.art);") < main.index("t = composeTile(raw);"))
check("an adaptive icon is masked as a launcher shows it (its middle 72 of 108, in a rounded square), not shrunk inside its safe zone",
      "icon instanceof android.graphics.drawable.AdaptiveIconDrawable" in main and "TileArt.ADAPTIVE_BLEED" in main and "gc.clipPath(clip);" in main)
check("the backing is chosen from the icon's own opaque pixels (white on white and a bare glyph read)", "if (Color.alpha(p) < 128) continue;" in main and "TileArt.backing(" in main)
check("the focus reads on a light tile as on a dark one: the person's ring and a thin dark line inside it (inside the tile: the tray's room, tests/fav_bar_test.py, unchanged)",
      "new android.graphics.drawable.LayerDrawable(new Drawable[] {ring, inner});" in tile and "twoTone.setLayerInset(1, rw, rw, rw, rw);" in tile and "shine.setBackground(twoTone);" in tile
      and "FOCUS_INNER = 0x8C000000" in main and "FOCUS_SCALE = 1.14f" in fav)
if not shutil.which("javac"):
    print("SKIP the rules: no javac here"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "TileArt.java"), str(HOME / "GridWindow.java"), str(ROOT / "tests/TileArtRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.TileArtRun"], check=True, capture_output=True).stdout.decode()
r = {l.split("\t")[0]: l.split("\t")[1:] for l in out.splitlines()}
check("kinds: a banner first; else an adaptive icon; else a wide logo (wider than 1.3) or a square icon", r["kind"] == ["banner", "adaptive", "icon", "wide"], r["kind"])
tiny, big, wide, ad, none = (tuple(map(int, x.split("x"))) for x in r["box"])
check("a tiny icon (32 px) is enlarged, at most 3 times (96), never blurred past that", tiny == (96, 96), tiny)
check("a big icon fits 62 % of the tile's height (191 -> 118)", big == (118, 118), big)
check("a wide logo fits 72 % of the width and half the height, its shape kept (400x100 -> 245x61 on a 340x191 tile)", wide == (245, 61) and abs(wide[0] / wide[1] - 4) < 0.1, wide)
check("an adaptive icon's box is the same square as a square icon's", ad == (118, 118), ad)
check("an icon that cannot be measured still gets the square box", none == (118, 118), none)
check("white on white: a white icon gets the dark backing", r["backing"][0] == "ff2b2e36", r["backing"])
check("a transparent icon: a dark glyph on the light backing, a white glyph on the dark one", r["backing"][1] == "ffece9e3" and r["backing"][2] == "ff2b2e36", r["backing"])
check("a coloured icon on the light backing; nothing read: light", r["backing"][3] == "ffece9e3" and r["backing"][4] == "ffece9e3", r["backing"])
check("a legacy icon with wide transparent margins is cut to its visible part (a small gap kept); one with small margins, or nothing opaque, is left as it is",
      r["trim"] == ["57,57,86,86", "null", "null"], r["trim"])
check("…and the cutting is done on the loading thread too, before the icon is fitted", "Drawable trimmed = trimmed(icon, iw, ih);" in main)
g = list(map(int, r["grid"]))
check("«كل التطبيقات»: whole rows only; the page moves by the fewest rows to keep the focused one whole, and only when it leaves the rows shown; up makes it first",
      g == [0, 1, 2, 1, 1], g)
fwin = main[main.index("    private void scrollToRow(View tileBox) {"):]
check("…the home scrolls by it (the first row shown at the safe top, never a row cut at the top); from the tray, the first row as before",
      "gridFirst = GridWindow.first(tops, bottoms, at, gridFirst," in fwin and "glideTo(Math.max(0, Math.min(tops[gridFirst] - Math.round(screenH * GridWindow.SAFE_TOP), max)));" in fwin
      and "screenH * 0.42f" not in fwin[:fwin.index("    // ----")])
check("every grid row the same height: «المخفية» keeps the room of a tile's name under it", 'room.setText(" ");' in main and "hiddenBox.addView(room," in main)
check("lightness: white 1, black 0", r["lum"] == ["100", "0"], r["lum"])
sys.exit(1 if failed else 0)
