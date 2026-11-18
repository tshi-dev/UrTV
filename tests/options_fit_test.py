#!/usr/bin/env python3
"""4.3.4: a long OK on an app showed its options box with the words cut off. The box was exactly the app's width (AppOptions), each row is one
line, and «صفحته في المتجر», «إلغاء التثبيت», «افتح القفل» need more than a small tile (more again with the TV's font made bigger); its rows
were a fixed 36 dp too. Now (OptionsFit): as wide as its widest row (never narrower than the app, never past the screen), kept on the screen,
and a row as tall as its line. The rule runs on a JVM; the box is checked to use it.
Run: python3 tests/options_fit_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

opts = (HOME / "AppOptions.java").read_text(encoding="utf-8")
show = opts.split("static PopupWindow show(")[1].split("\n    }\n")[0]
row = opts.split("private static View row(")[1].split("\n    }\n")[0]

check("the rows are one line each (so the box must fit them)", "t.setSingleLine(true);" in row)
check("the box's width comes from its widest row, measured in the font it is drawn with",
      "t.getPaint().measureText(t.getText().toString())" in show and "OptionsFit.width(Math.round(card.getWidth() * scale), widest + 2 * pad, screenW, margin)" in show)
check("…the row's measure counts its pads, the icon and its gap", "px(12 + 12 + ICON + 8 + 4, d)" in show
      and "r.setPadding(px(12, d), 0, px(12, d), 0);" in row and "gp.setMarginEnd(px(8, d));" in row)
check("the box is placed by OptionsFit.left (never off the screen)", "showAtLocation(card, Gravity.NO_GRAVITY, OptionsFit.left(cx, w, screenW, margin), y)" in show)
check("rows and icons take the measured height; the box's height adds them up",
      show.count("rowH") >= 6 and "int h = 2 * pad + (rows.size() + 1) * rowH + rows.size() * px(GAP, d);" in show)
check("the probe and the rows use the same text size", "probe.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP);" in show
      and "t.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP);" in row)

if not shutil.which("javac"):
    print("SKIP the rule on a JVM: no javac")
    sys.exit(1 if failed else 0)
cases = [
    ("width 200 150 1920 32", 200, "a tile wider than its words: the app's width, as before"),
    ("width 200 330 1920 32", 330, "«صفحته في المتجر» needs more than the tile: the box grows"),
    ("width 200 5000 1920 32", 1856, "never past the screen less its margins"),
    ("width 0 0 0 32", 1, "a screen not measured yet: never zero or less"),
    ("left 960 300 1920 32", 810, "centred on the app"),
    ("left 100 330 1920 32", 32, "an app at the start: moved in to the margin"),
    ("left 1880 330 1920 32", 1558, "an app at the end: moved in to the margin"),
    ("row 72 30 16", 72, "a normal font: the grid's 36 dp button"),
    ("row 72 70 16", 86, "a bigger TV font: the row grows with its line"),
]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-d", tmp, str(HOME / "OptionsFit.java"), str(ROOT / "tests/OptionsFitRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.OptionsFitRun"] + [c[0] for c in cases], check=True, capture_output=True, text=True).stdout.split()
for (arg, want, name), got in zip(cases, out):
    check(f"{name} ({arg} -> {want})", int(got) == want, got)
check("an answer for every case", len(out) == len(cases), out)
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
