#!/usr/bin/env python3
"""4.4 compact (the owner: «It's good to have big stuff, but they're just toooo much»): every text size of the setup, update, install, rescue and
add-on screens, read from the source, against android/launcher/.../Compact.java: titles 24 sp, rows and buttons 16-18 sp, second lines 13-14 sp, never
under 13; buttons 44 dp; a window 560 dp wide; the QR of a setup page 88 dp. Before/after pictures: docs/mockups/compact-setup/.
    python3 tests/compact_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


def args_of(s, i):
    depth, start, out, j, instr = 0, i + 1, [], i, None
    while j < len(s):
        c = s[j]
        if instr:
            if c == "\\": j += 2; continue
            if c == instr: instr = None
        elif c in "\"'": instr = c
        elif c in "([{": depth += 1
        elif c in ")]}":
            depth -= 1
            if depth == 0: out.append(s[start:j].strip()); return out
        elif c == "," and depth == 1: out.append(s[start:j].strip()); start = j + 1
        j += 1
    return out


def sizes(src, arg=1):
    """every literal sp of a text(…) call (the size is its second argument; SetupProgressView's text(sp, bold, colour) its first) and of setTextSize"""
    out = []
    for m in re.finditer(r"(?<![\w.])text\(", src):
        a = args_of(src, m.end() - 1)
        if len(a) > arg and re.fullmatch(r"\d+", a[arg]):
            out.append((int(a[arg]), src.count("\n", 0, m.start()) + 1))
    out += [(int(m.group(1)), src.count("\n", 0, m.start()) + 1) for m in re.finditer(r"COMPLEX_UNIT_SP, (\d+)\)", src)]
    return out


compact = text(HOME / "Compact.java")
check("Compact: a 560 dp window, 44 dp buttons, a 24 sp title, 18 sp buttons and rows at most, 13 sp at least, an 88 dp QR",
      "WINDOW_DP = 560, BUTTON_DP = 44, TITLE_SP = 24, BUTTON_SP = 18, BODY_MAX_SP = 18, MIN_SP = 13" in compact and "QR_DP = 88" in compact)
SCREENS = {
    "UpdateActivity": HOME / "UpdateActivity.java", "InstallSaverActivity": HOME / "InstallSaverActivity.java", "ExtrasActivity": HOME / "ExtrasActivity.java",
    "FactoryActivity": HOME / "setup/FactoryActivity.java", "SetupActivity": HOME / "setup/SetupActivity.java",
    "RescueActivity": ROOT / "android/rescue/src/main/java/sa/urtv/home/setup/RescueActivity.java", "UpdateCover": ROOT / "android/app/src/main/java/sa/urtv/screensaver/UpdateCover.java",
}
for name, path in SCREENS.items():
    got = sizes(text(path))
    bad = [(sp, ln) for sp, ln in got if sp < 13 or sp > 24 or (18 < sp < 24)]
    check(f"{name}: every text 13 to 18 sp, a title 24 sp, nothing between (no 20, 22, 28, 30, 34)", got and not bad, bad)
pv = text(HOME / "setup/SetupProgressView.java")
got = sizes(pv, arg=0)
bad = [(sp, ln) for sp, ln in got if sp < 13 or (sp > 20 and sp != 36)]
check("the setup's progress: 13 to 20 sp (the step's name 20), the percentage the one big number (36 sp, was 64)", got and not bad, bad)
draw = re.findall(r"label\(c, [^;]*?, (\d+), (?:look|0x)", pv)
check("the dialog's drawing: its words 13 to 17 sp", draw and all(13 <= int(x) <= 17 for x in draw), draw)
check("the setup's steps are one row of words above the work (no 232 dp rail beside it in a 560 dp window)",
      "stepper.setOrientation(HORIZONTAL);" in pv and "dp(232)" not in pv and "setOrientation(VERTICAL);\n        setLayoutDirection(Dir.layout());\n        stepper" in pv)
sa, ua, fa, ia, ea = (text(SCREENS[n]) for n in ("SetupActivity", "UpdateActivity", "FactoryActivity", "InstallSaverActivity", "ExtrasActivity"))
ra = text(SCREENS["RescueActivity"])
check("buttons 44 dp: the setup's, the language's, the update's, the factory step's, the install's, the add-on's, the rescue app's",
      "t.setMinHeight(dp(sa.urtv.home.Compact.BUTTON_DP));" in sa and "b.setMinHeight(dp(sa.urtv.home.Compact.BUTTON_DP));" in sa and "dp(Compact.BUTTON_DP)" in ua
      and "t.setMinHeight(dp(sa.urtv.home.Compact.BUTTON_DP));" in fa and "button.setMinHeight(dp(Compact.BUTTON_DP));" in ia and "new LinearLayout.LayoutParams(0, dp(Compact.BUTTON_DP), 1f)" in ea
      and "new LinearLayout.LayoutParams(-1, dp(44))" in ra and "dp(56)" not in sa + ua and "dp(64)" not in ea and "dp(84)" not in ra)
check("windows 560 dp wide in the safe area: the setup's every page, the update's, the factory step's; the install, add-on and rescue words in a 560 dp column",
      "WINDOW_DP = sa.urtv.home.Compact.WINDOW_DP, WINDOW_WIDE_DP = sa.urtv.home.Compact.WINDOW_DP" in sa and "Math.min(dp(Compact.WINDOW_DP)" in ua
      and "Math.min(dp(sa.urtv.home.Compact.WINDOW_DP)" in fa and "(sw - dp(Compact.WINDOW_DP)) / 2" in ia and "(sw - dp(Compact.WINDOW_DP)) / 2" in ea and "(sw - dp(560)) / 2" in ra)
check("the QR of the setup's start (rescue, the computer's way) 88 dp on the left; the done screen's packed report not under 160 dp (a dense code a phone must read)",
      sa.count("sa.urtv.home.Compact.QR_DP);") == 2 and "static final int QR_DP = 160;" in sa)
check("the focus ring stays: the setup's and the update's buttons keep GlassPaint.card, the rescue app its 4 dp ring",
      "sa.urtv.home.GlassPaint.card(t, look, 14);" in sa and "GlassPaint.card(p, look, 14);" in ua and "g.setStroke(has ? dp(4) : 0, 0xFF7FB2FF)" in ra)
check("the before/after pictures are in docs/mockups/compact-setup/", len(list((ROOT / "docs/mockups/compact-setup").glob("*.png"))) >= 6 if (ROOT / "docs/mockups/compact-setup").is_dir() else False)
sys.exit(1 if failed else 0)
