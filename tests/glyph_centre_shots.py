#!/usr/bin/env python3
"""Where a letter's INK stands in its tile (the owner's rule: every letter in the centre of its box, always). On the CI emulator `.github/screens/games-flow.sh` takes a shot and
a `uiautomator dump` of the word game; every letter tile and key is a view with the letter as its description (GameActivity.glyph), so its bounds are the tile's. This measures
the ink (the pixels that differ from the tile's own colour, inside the border) against the middle of those bounds: at most 2 px at 1080p, in both directions.

  python3 tests/glyph_centre_shots.py SHOT.png UIDUMP.xml     measure a real shot (exit 1 when a glyph is off by more than 2 px, or when no glyph was found)
  python3 tests/glyph_centre_shots.py                         self-test: the measuring itself on drawn images, and the source pins (no emulator, no font)
"""
import re, sys
import xml.etree.ElementTree as ET
from pathlib import Path

TOL = 2.0   # px at 1080p
ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1

def ink_centre(img, box, border=12):
    """(cx, cy) of the glyph's ink inside `box` (l, t, r, b), border px skipped; None when the box is empty of ink"""
    l, t, r, b = box
    inner = img.crop((l + border, t + border, r - border, b - border)).convert("RGB")
    w, h = inner.size
    px = inner.load()
    counts = {}
    for y in range(0, h, 3):
        for x in range(0, w, 3):
            counts[px[x, y]] = counts.get(px[x, y], 0) + 1
    bg = max(counts, key=counts.get)
    xs0, xs1, ys0, ys1 = w, -1, h, -1
    for y in range(h):
        for x in range(w):
            p = px[x, y]
            if abs(p[0] - bg[0]) + abs(p[1] - bg[1]) + abs(p[2] - bg[2]) > 90:
                if x < xs0: xs0 = x
                if x > xs1: xs1 = x
                if y < ys0: ys0 = y
                if y > ys1: ys1 = y
    if xs1 < 0: return None
    return (l + border + (xs0 + xs1 + 1) / 2, t + border + (ys0 + ys1 + 1) / 2)

def offsets(img, dump, one_letter=True):
    """[(letter, dx, dy)] in 1080p px, for every view of the dump whose description is one letter"""
    scale = img.size[1] / 1080.0
    out = []
    for n in ET.parse(dump).getroot().iter("node"):
        d = n.get("content-desc") or ""
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds") or "")
        if not m or len(d) != 1 or not "؀" <= d <= "ۿ": continue
        l, t, r, b = map(int, m.groups())
        if r - l < 40 or b - t < 40: continue
        c = ink_centre(img, (l, t, r, b), border=max(8, int(12 * scale)))
        if c: out.append((d, (c[0] - (l + r) / 2) / scale, (c[1] - (t + b) / 2) / scale))
    return out

def measure(png, dump):
    from PIL import Image
    res = offsets(Image.open(png), dump)
    bad = [(d, round(dx, 1), round(dy, 1)) for d, dx, dy in res if abs(dx) > TOL or abs(dy) > TOL]
    for d, dx, dy in res: print(f"  {d}  dx {dx:+.1f}  dy {dy:+.1f}")
    check(f"{png}: {len(res)} letters measured, every ink centre within {TOL} px of its tile's centre", len(res) >= 3 and not bad, bad)

if len(sys.argv) == 3:
    measure(sys.argv[1], sys.argv[2]); sys.exit(1 if failed else 0)

# ---- self-test: the measuring on drawn images
from PIL import Image, ImageDraw
def drawn(dx, dy):
    img = Image.new("RGB", (1920, 1080), (20, 40, 30)); d = ImageDraw.Draw(img)
    l, t, r, b = 300, 200, 460, 360
    d.rectangle((l, t, r, b), fill=(250, 240, 220), outline=(10, 10, 10), width=6)
    cx, cy = (l + r) / 2 + dx, (t + b) / 2 + dy
    d.rectangle((cx - 20, cy - 45, cx + 19, cy + 44), fill=(10, 10, 10))   # a glyph-like block, 40 x 90, centred on (cx, cy)
    return img, (l, t, r, b)
for dx, dy in ((0, 0), (1, -1), (6, 0), (0, -8)):
    img, box = drawn(dx, dy)
    c = ink_centre(img, box)
    ox, oy = c[0] - (box[0] + box[2]) / 2, c[1] - (box[1] + box[3]) / 2
    check(f"the measuring finds a glyph {dx},{dy} px off its tile's centre (within 1 px)", abs(ox - dx) <= 1 and abs(oy - dy) <= 1, (ox, oy))
img, box = drawn(0, 0)
check("a centred glyph passes the 2 px rule and one 6 px off does not", abs(ink_centre(img, box)[0] - 380) <= TOL and abs(ink_centre(drawn(6, 0)[0], box)[0] - 380) > TOL)
check("a tile with no glyph measures as empty (not a pass)", ink_centre(Image.new("RGB", (400, 400), (250, 240, 220)), (0, 0, 400, 400)) is None)

# ---- the placement maths on a plain JVM
import shutil, subprocess, tempfile
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        c = subprocess.run(["javac", "-nowarn", "-d", tmp, str(APP / "GlyphCentre.java"), str(ROOT / "tests/GlyphCentreTest.java")], capture_output=True, text=True)
        check("GlyphCentre compiles on a plain JVM", c.returncode == 0, c.stderr[-400:])
        if c.returncode == 0:
            r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.GlyphCentreTest"], capture_output=True, text=True)
            for line in r.stdout.splitlines():
                if line.startswith(("PASS", "FAIL")): print(line); failed += line.startswith("FAIL")
            check("the JVM test ran to the end", r.returncode == 0, r.stderr[-300:])
else:
    check("javac is there for the GlyphCentre test", False)

# ---- the source: every letter tile and key is drawn by its ink, never by the font's line box
read = lambda n: (APP / n).read_text(encoding="utf-8")
base, w5, kh, hive = read("GameActivity.java"), read("Word5Activity.java"), read("KhanatActivity.java"), read("LetterHiveActivity.java")
check("GameActivity.glyph: draws one letter by its ink bounds (GlyphCentre), as a view whose description is the letter (the emulator test finds it by that)",
      "GlyphCentre.origin(ink.left, ink.top, ink.right, ink.bottom, getWidth(), getHeight())" in base and "setContentDescription(s)" in base and "paint.getTextBounds(s, 0, s.length(), ink)" in base)
check("the word game: the five tiles of a row (typing, coloured) and every key are glyph views, not TextViews of the line box",
      w5.count("glyph(") >= 2 and "View lt = glyph(shownOf(k), 72, wordsColour);" in w5 and "t.addView(glyph(String.valueOf(shown.charAt(i)), tile * .62f" in w5 and "centered(String.valueOf(shown.charAt(i))" not in w5)
check("«خمس خانات»: the wheel's letter, the big letter and the small one are glyph views", "spinView = glyph(" in kh and "glyph(game.letter, fitSize" in kh and "glyph(game.letter, 144" in kh)
check("«خلية الحروف»: the cells' letters are centred on the glyph's ink (exactCenter), as before", "bounds.exactCenterX()" in hive and "bounds.exactCenterY()" in hive)
print("ok" if failed == 0 else "failed")
sys.exit(1 if failed else 0)
