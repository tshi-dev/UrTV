#!/usr/bin/env python3
"""The favourites tray's rendered check (.github/screens/tray_check.py): its rules, run here on made-up dumps and pixels - a good tray passes, each fault the
tester found (a tile outside, a sliver, the name in the wrong place or following the focus, the tray past the safe area) fails - and the wiring of the CI job
that runs it on the emulator.   python3 tests/tray_check_test.py"""
import importlib.util
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location("tray_check", ROOT / ".github/screens/tray_check.py")
tc = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tc)
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def node(cls, r, text="", focusable=False, focused=False):
    return f'<node class="{cls}" text="{text}" content-desc="" focusable="{str(focusable).lower()}" focused="{str(focused).lower()}" bounds="[{r[0]},{r[1]}][{r[2]},{r[3]}]"/>'


def dump(tiles, label, tray=(128, 768, 1792, 986), focus=0):
    ns = [node("android.widget.HorizontalScrollView", tray, focusable=True)]
    for i, r in enumerate(tiles):
        ns.append(node("android.widget.FrameLayout", r, focusable=True, focused=i == focus))
    ns.append(node("android.widget.TextView", label, "name"))
    ns.append(node("android.widget.TextView", (label[0] - 300, label[1] + 20, label[0] - 10, label[3]), "hint"))
    return "<hierarchy>" + "".join(ns) + "</hierarchy>"


def ar_tiles(shift=0):   # the first tile at the right, 304 wide, 24 apart, 24 inside the tray; the focused one grown (+12 each side, lifted 6)
    xs = [1464 - i * 328 + shift for i in range(5)]
    t = [(x, 792, x + 304, 962) for x in xs]
    t[0] = (xs[0] - 12, 780, xs[0] + 316, 974)
    return t


def tray_for(n):
    return (128, 986 - tc.tray_height(n), 1792, 986)


def en_n_tiles(n, first_focus=True):   # n whole tiles from the left, tile_size(n) wide, 24 apart, 24 inside the tray, centred in its height
    w, h = tc.tile_size(n)
    y = 986 - tc.tray_height(n) + 24   # the tray is as high as its tiles with 24 above and below (step 28)
    t = [(152 + k * (w + 24), y, 152 + k * (w + 24) + w, y + h) for k in range(n)]
    t[0] = (t[0][0] - 6, t[0][1] - 6, t[0][2] + 6, t[0][3] + 6) if first_focus else t[0]
    return t


check("tile_size: 5 is 304x171, 7 is 210x118, 10 is 140x78 at 1080p (the bar-size setting)", [tc.tile_size(n) for n in (5, 7, 10)] == [(304, 171), (210, 118), (140, 78)], [tc.tile_size(n) for n in (5, 7, 10)])
for n in (7, 10):
    f, m = tc.analyse(dump(en_n_tiles(n), (152, 704, 256, 752), tray=tray_for(n)), "en", "first", n=n, favs=n)
    check(f"a good tray of {n} whole tiles passes (24 apart, 24 inside, the name at the left inner padding)", not f and m.get("whole") == n, (f, m.get("whole")))
t = en_n_tiles(7)
f, _ = tc.analyse(dump(t[:6] + [(t[6][0] + 20, t[6][1], t[6][2] + 20, t[6][3])], (152, 704, 256, 752), tray=tray_for(7)), "en", "first", n=7, favs=7)
check("7 set, one tile 20 px out of its place: found (not 24 apart)", any("not 24" in x for x in f), f)
f, _ = tc.analyse(dump(en_n_tiles(7)[:5], (152, 704, 256, 752), tray=tray_for(7)), "en", "first", n=7, favs=7)
check("7 set but only 5 whole tiles show: found", any("whole tiles" in x for x in f), f)
good_label = (1664, 704, 1768, 752)
f, m = tc.analyse(dump(ar_tiles(), good_label), "ar", "first")
check("a good tray in Arabic passes: five whole tiles inside the inner rect, the focused one with room to spare, the name at the inner padding", not f, f)
f, _ = tc.analyse(dump(ar_tiles()[:4] + [(128, 792, 168, 962)], good_label), "ar", "first")
check("a sliver of a sixth tile (40 px) is found", any("sliver" in x for x in f), f)
f, _ = tc.analyse(dump(ar_tiles(24), good_label), "ar", "first")
check("tiles not 24 px inside the tray are found", any("inner rect" in x or "closer than" in x for x in f), f)
f, _ = tc.analyse(dump(ar_tiles(), (1700, 704, 1790, 752)), "ar", "first")
check("the name not at the start edge's inner padding is found (right edge 1790, not 1768)", any("name's right edge" in x for x in f), f)
f, _ = tc.analyse(dump(ar_tiles(), (152, 704, 256, 752)), "ar", "first")
check("the mirrored name (above the leftmost tile, the owner's report) is found", any("name's right edge" in x for x in f), f)
f, _ = tc.analyse(dump(ar_tiles(), good_label, tray=(120, 768, 1800, 1066)), "ar", "first")
check("a tray past the safe area (x 120..1800 and a bottom of 1066) is found", any("safe area" in x for x in f), f)
low = [(r[0], r[1] + 40, r[2], r[3] + 40) for r in ar_tiles()]
f, _ = tc.analyse(dump(low, (1664, 744, 1768, 792), tray=(128, 808, 1792, 986)), "ar", "first", lowered=True)
check("lowered: the tray at 808..1026 is inside the safe area, ends at 1026 and its tiles are whole inside it", not f, f)
en_tiles = [(tc.W - r[2], r[1], tc.W - r[0], r[3]) for r in ar_tiles()]
f, _ = tc.analyse(dump(en_tiles, (152, 704, 256, 752)), "en", "first")
check("a good tray in English passes: the name at the left, the first tile at the left", not f, f)
check("the name is in one place: the same top and bottom with the focus on the first and on the last tile, whatever the words",
      not tc.same_place([1664, 704, 1768, 752], [1600, 704, 1768, 752]) and tc.same_place([1664, 704, 1768, 752], [1500, 640, 1768, 688]))

try:
    from PIL import Image, ImageDraw
    tray = (128, 768, 1792, 986)
    a = Image.new("RGB", (tc.W, tc.H), (40, 60, 90))
    b = a.copy()
    check("pixels: the strips beside the tray are equal when nothing is drawn outside it", not tc.strips_same(a, b, tray))
    ImageDraw.Draw(b).rectangle((10, 800, 110, 960), fill=(240, 240, 240))
    check("pixels: a tile drawn left of the tray (x 10..110) makes the strips differ", bool(tc.strips_same(a, b, tray)))
except ImportError:
    print("SKIP the pixel cases: no Pillow")

wf = (ROOT / ".github/workflows/tray.yml").read_text(encoding="utf-8")
check("the CI job: builds the debug apps, builds stub apps, starts an API 34 TV emulator at 1080p, runs tray_check.py, uploads the dumps and captures even on failure",
      "assembleDebug" in wf and "make_stubs.sh" in wf and "api-level: 34" in wf and "target: android-tv" in wf and "profile: tv_1080p" in wf and "tray_check.py" in wf
      and "actions/upload-artifact@v4" in wf and "if: always()" in wf)
check("the CI job runs on every push to next and work branches and on pull requests into next, and a failing check fails the run (no continue-on-error)",
      "pull_request" in wf and "branches: [next]" in wf and "continue-on-error" not in wf and "work/**" in wf)
src = (ROOT / ".github/screens/tray_check.py").read_text(encoding="utf-8")
check("the rendered check also takes the 8 pictures of the owner's question: sand and dark glass, bar size 5, 7, 10 and all on 12 apps, each judged by analyse() and for a name cut with «…»",
      "def shots(out, report, failures):" in src and 'for look in ("sand", "glass"):' in src and '(("5", 5), ("7", 7), ("10", 8), ("all", 8))' in src and "seed_dock(12)" in src and "tray-{look}-{setting}.png" in src and "«…»" in src)
f, _ = tc.analyse(dump(en_n_tiles(7), (152, 704, 256, 752), tray=(128, 768, 1792, 986)), "en", "first", n=7, favs=7)
check("step 28: 7 tiles in a tray that kept its 218 height (an empty band under them): found", any("high" in x for x in f) and any("not 24 and 24" in x for x in f), f)
ns_h = [{"cls": "android.widget.TextView", "text": "Stub 1", "desc": "", "focusable": False, "focused": False, "rect": (152, 764, 236, 819)},
        {"cls": "android.widget.TextView", "text": "Hold OK for options", "desc": "", "focusable": False, "focused": False, "rect": (268, 760, 520, 819)}]
check("step 28: the hint beside the name (16 sp, as big as the name) is not taken for the name", tc.name_label(ns_h, (128, 837, 1792, 986))["text"] == "Stub 1")
sys.exit(1 if failed else 0)
