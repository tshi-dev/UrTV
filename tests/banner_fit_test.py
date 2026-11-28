#!/usr/bin/env python3
"""4.4 beta 4: some apps' banners were cut on their home tiles. A banner fills its 16:9 tile, so one of another shape (a 2:1 strip, a 4:3 or square
picture) lost its edges or its name. Now, by its own shape (BannerFit: more than 6 % of a side would be cut), such a banner is drawn whole in the
middle of a 16:9 picture (MainActivity.fitted), so the tile's fill cuts nothing; a banner of the usual shape fills the tile exactly as before.
BannerFit (no Android) on shapes, then the wiring.
Run: python3 tests/banner_fit_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

main = (H / "MainActivity.java").read_text(encoding="utf-8")
fit = main[main.index("private Drawable fitted(Drawable d, boolean banner)"):]
fit = fit[:fit.index("\n    }\n")]
check("fitted: a banner of another shape is drawn whole (the smaller scale), on a 16:9 picture", "boolean whole = banner && BannerFit.contain(iw, ih, w, h);" in fit
      and "float f = whole ? Math.min(w / (float) iw, h / (float) ih) : banner ? Math.max(w / (float) iw, h / (float) ih)" in fit
      and "int bw = banner ? w :" in fit and "bh = banner ? h :" in fit)
check("fitted: a whole banner is always redrawn (also a small one), a usual one only when it is larger than the tile, as before", "if (f >= 1f && !whole) return d;" in fit)
check("banners still go through fitted(banner, true), and the tile still fills with CENTER_CROP", 'artCache.put("b:" + a.pkg, banner = fitted(banner, true))' in main
      and "} else if (app.isBanner || app.composed) {   // a banner, or the icon already composed on its tile (TileArt): full-bleed\n            img.setScaleType(ImageView.ScaleType.CENTER_CROP);" in main)

if not shutil.which("javac"):
    print("SKIP banner fit: no javac here")
    sys.exit(1 if failed else 0)
W, T = 480, 270
CASES = [
    ("320 x 180, the usual banner: fills", f"320,180,{W},{T}", "0"),
    ("a banner 2 % off the shape: fills (a hairline)", f"320,176,{W},{T}", "0"),
    ("a 2:1 strip: whole", f"400,200,{W},{T}", "1"),
    ("4:3: whole", f"320,240,{W},{T}", "1"),
    ("square: whole", f"256,256,{W},{T}", "1"),
    ("a tall picture: whole", f"180,320,{W},{T}", "1"),
    ("no size: as before (fills)", f"0,0,{W},{T}", "0"),
]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(H / "BannerFit.java"), str(ROOT / "tests/BannerFitRun.java")], capture_output=True, text=True)
    check("BannerFit compiles alone", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.BannerFitRun", *[c[1] for c in CASES]], capture_output=True, text=True).stdout.split()
        for (name, _, want), got in zip(CASES, out + [""] * len(CASES)):
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
