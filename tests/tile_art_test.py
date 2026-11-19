#!/usr/bin/env python3
"""4.4 beta 2, the owner (build 525: «the icons need to be 100% perfect»; then «no outline, no small icons»): every app fills its tile. A banner as it is;
an adaptive icon's background over the whole tile and its foreground large in the middle; a legacy icon trimmed, fitted large, on its own edge
colour (a bare glyph: on LIGHT or DARK, whichever stands out); all from the icon at xxxhdpi. No outline at rest or on the focus: the focus is the
size, the lift and the shadow. Hard cases each have a rule here: a tiny icon, a wide logo, white on white, a transparent icon, a phone-only app.
The grid moves a whole row at a time. TileArt and GridWindow run on a JVM; MainActivity's wiring is read from the source.
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
comp = main[main.index("    private Drawable composeTile(Drawable icon) {"):main.index("    /** a legacy icon cut to its opaque pixels")]
check("no dark tint of the app's colour behind a tile any more (darken is gone)", "darken(" not in main)
check("an app with a banner: its banner fills the tile; with none, its icon composed to fill the tile, drawn full-bleed the same way",
      "} else if (app.isBanner || app.composed) {" in tile and "app.composed && !icons ? 0 : TileArt.LIGHT" in tile)
check("composed once per app on the apps' loading thread (appOf), cached, never on the main thread, from the icon at xxxhdpi",
      'Drawable t = artCache.get("t:" + a.pkg);' in main and "t = composeTile(sharpIcon(ri, pm));" in main and "getDrawableForDensity(id, TileArt.DENSITY, null)" in main
      and main.index("t = composeTile(sharpIcon(ri, pm));") < main.index("    /** On the main thread: the dock first") and "composeTile" not in tile)
check("the glow keeps the app's own colour (sampled from its icon before it is composed)", main.index("a.color = sampleColor(a.pkg, a.art);") < main.index("t = composeTile(sharpIcon(ri, pm));"))
check("an adaptive icon: its background layer over the whole tile, its foreground large in the middle (no small rounded square on a backing)",
      "Drawable bg = ad.getBackground(), fg = ad.getForeground();" in comp and "bg.setBounds(0, (h - w) / 2, w, (h - w) / 2 + w); bg.draw(c);" in comp and "fg.draw(c);" in comp and "clipPath" not in comp)
check("a legacy icon: trimmed first, then on its own edge colour; a bare glyph on the backing it stands out from",
      "Drawable cut = iw > 0 && ih > 0 ? trimmed(icon, iw, ih) : null;" in comp and "int bg = TileArt.edgeColour(edge);" in comp and "if (bg == 0) {" in comp and "TileArt.backing(" in comp)
check("no outline: the focus never shows the ring (only an app carried in «تعديل المفضلة» does); the focus is the size, the lift and the shadow",
      "shine.setAlpha(0f);   // the owner (19 Nov): no outline" in tile and "has && !zoom ? 1f : 0f" not in tile and "translationZ(z)" in tile and "FOCUS_INNER" not in main)
if not shutil.which("javac"):
    print("SKIP the rules: no javac here"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "TileArt.java"), str(HOME / "GridWindow.java"), str(ROOT / "tests/TileArtRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.TileArtRun"], check=True, capture_output=True).stdout.decode()
r = {l.split("\t")[0]: l.split("\t")[1:] for l in out.splitlines()}
check("kinds: a banner first; else an adaptive icon; else a wide logo (wider than 1.3) or a square icon (a phone-only app's icon is one of these)",
      r["kind"] == ["banner", "adaptive", "icon", "wide"], r["kind"])
tiny, big, wide, ad, none = (tuple(map(int, x.split("x"))) for x in r["box"])
check("a tiny legacy icon (32 px) is enlarged, at most 4 times (128), never blurred past that", tiny == (128, 128), tiny)
check("a legacy icon fills 85 % of the tile's height (191 -> 162)", big == (162, 162), big)
check("a wide logo fills 80 % of the width, its shape kept (400x100 -> 272x68 on a 340x191 tile)", wide == (272, 68), wide)
check("an adaptive icon's foreground is drawn at 1.2 of the tile's height (its logo about 80 % of it)", ad == (229, 229), ad)
check("an icon that cannot be measured still fills the square box", none == (162, 162), none)
check("a legacy icon's tile takes its own edge colour (white stays white, red stays red); a half-transparent edge (a bare glyph) takes none",
      r["edge"] == ["ffffffff", "0", "ffe50914"], r["edge"])
check("a bare glyph: white on the dark backing, dark on the light one (white on white never); nothing read: light",
      r["backing"] == ["ff2b2e36", "ffece9e3", "ff2b2e36", "ffece9e3"], r["backing"])
check("transparent padding is cut off (60 of 200 a side), a thin one (4 of 200) kept, nothing opaque left alone", r["trim"] == ["60,60,80,80", "null", "null"], r["trim"])
check("lightness: white 1, black 0", r["lum"] == ["100", "0"], r["lum"])
g = list(map(int, r["grid"]))
check("«كل التطبيقات»: whole rows only; the page moves by the fewest rows to keep the focused one whole, and only when it leaves the rows shown; up makes it first",
      g == [0, 1, 2, 1, 1], g)
fwin = main[main.index("    private void scrollToRow(View tileBox) {"):]
check("…the home scrolls by it (the first row shown at the safe top); from the tray, the first row as before",
      "gridFirst = GridWindow.first(tops, bottoms, at, gridFirst," in fwin and "glideTo(Math.max(0, Math.min(tops[gridFirst] - Math.round(screenH * GridWindow.SAFE_TOP), max)));" in fwin)
check("every grid row the same height: «المخفية» keeps the room of a tile's name under it", 'room.setText(" ");' in main and "hiddenBox.addView(room," in main)
sys.exit(1 if failed else 0)
