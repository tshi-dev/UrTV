#!/usr/bin/env python3
"""4.4 compact, then ONE BOX and ONE TEXT STYLE (the owner): every text size of the setup, update, install, rescue and
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


compact, box = text(HOME / "Compact.java"), text(HOME / "Box.java")
check("Compact: ONE box (560 x 600 dp), ONE text style (22 title, 17 body, 14 secondary), 44 dp buttons, an 88 dp QR",
      "BOX_W_DP = 560, BOX_H_DP = 600" in compact and "TITLE_SP = 22, BODY_SP = 17, SECONDARY_SP = 14" in compact and "BUTTON_DP = 44" in compact and "QR_DP = 88" in compact)
SCREENS = {
    "UpdateActivity": HOME / "UpdateActivity.java", "InstallSaverActivity": HOME / "InstallSaverActivity.java", "ExtrasActivity": HOME / "ExtrasActivity.java",
    "FactoryActivity": HOME / "setup/FactoryActivity.java", "SetupActivity": HOME / "setup/SetupActivity.java",
    "RescueActivity": ROOT / "android/rescue/src/main/java/sa/urtv/home/setup/RescueActivity.java", "UpdateCover": ROOT / "android/app/src/main/java/sa/urtv/screensaver/UpdateCover.java",
}
ALLOWED = {22, 17, 14}
for name, path in SCREENS.items():
    got = sizes(text(path))
    bad = [(sp, ln) for sp, ln in got if sp not in ALLOWED]
    check(f"{name}: ONE text style: every size is 22, 17 or 14 sp", got and not bad, bad)
pv = text(HOME / "setup/SetupProgressView.java")
got = sizes(pv, arg=0)
check("the setup's progress: only 22, 17 and 14 sp (the percentage in the title style, no 64 or 36 sp number)", got and all(sp in ALLOWED for sp, _ in got), [x for x in got if x[0] not in ALLOWED])
draw = re.findall(r"label\(c, [^;]*?, (\d+), (?:look|0x)", pv)
check("the dialog's drawing: its words 17 or 14 sp", draw and all(int(x) in (17, 14) for x in draw), draw)
check("the setup's steps are one row of words above the work (no 232 dp rail beside it)", "stepper.setOrientation(HORIZONTAL);" in pv and "dp(232)" not in pv)
srcs = {n: text(p) for n, p in SCREENS.items()}
check("ONE BOX: the setup, update, factory, install, add-on and rescue screens all are a Box (none sets its own window or full-screen column)",
      all("Box.make(" in srcs[n] for n in ("SetupActivity", "UpdateActivity", "FactoryActivity", "InstallSaverActivity", "ExtrasActivity", "RescueActivity"))
      and "WindowColumn" not in srcs["SetupActivity"] and "WINDOW_DP" not in "".join(srcs[n] for n in ("UpdateActivity", "FactoryActivity", "InstallSaverActivity", "ExtrasActivity")))
check("the box is always the same size: Compact.BOX_W_DP x BOX_H_DP (the safe area caps it), set once, never from its content",
      "new FrameLayout.LayoutParams(width(c), height(c), Gravity.CENTER)" in box and "Compact.BOX_W_DP * " in box and "Compact.BOX_H_DP * " in box
      and "box.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));" in box and "colLp" not in srcs["SetupActivity"])
check("the box's title is fixed above the words, which scroll inside it with a scroll bar that stays, and «⌄» while there is more",
      "title.setTextSize(TypedValue.COMPLEX_UNIT_SP, Compact.TITLE_SP);" in box and "scroll.setScrollbarFadingEnabled(false);" in box and 'more.setText("⌄");' in box
      and "more.setVisibility(canScrollVertically(1) ? View.VISIBLE : View.INVISIBLE)" in box)
rs = box[box.index("public boolean dispatchKeyEvent(KeyEvent e)"):box.index("View firstButtonInView()")]
check("READ BEFORE FOCUS: with the words longer than the box, the reading area takes the focus first; DOWN scrolls them; only at their end does DOWN go to the button",
      "if (scroll.overflows()) { scroll.scrollTo(0, 0); scroll.focusSelf(); }" in box and "else if (button != null) button.requestFocus();" in box
      and "if (canScrollVertically(1)) { smoothScrollBy(0," in rs and rs.index("canScrollVertically(1)") < rs.index("firstButtonInView()") and "b.requestFocus();" in rs)
check("every screen's first focus goes through the box (read before focus)",
      srcs["SetupActivity"].count("page.focusFirst(") >= 5 and "box.focusFirst(focus);" in srcs["UpdateActivity"] and "box.focusFirst(v);" in srcs["FactoryActivity"]
      and "box.focusFirst(button);" in srcs["InstallSaverActivity"] and "page.focusFirst(button);" in srcs["ExtrasActivity"] and "page.focusFirst(button);" in srcs["RescueActivity"])
check("the rescue app gets the box and its sizes from the home's own files (copied in)", "'Box.java', 'Compact.java'" in text(ROOT / "android/rescue/build.gradle"))
sa, ua, fa, ia, ea, ra = (srcs[n] for n in ("SetupActivity", "UpdateActivity", "FactoryActivity", "InstallSaverActivity", "ExtrasActivity", "RescueActivity"))
check("buttons 44 dp: the setup's, the language's, the update's, the factory step's, the install's, the add-on's, the rescue app's",
      "t.setMinHeight(dp(sa.urtv.home.Compact.BUTTON_DP));" in sa and "b.setMinHeight(dp(sa.urtv.home.Compact.BUTTON_DP));" in sa and "dp(Compact.BUTTON_DP)" in ua
      and "t.setMinHeight(dp(sa.urtv.home.Compact.BUTTON_DP));" in fa and "button.setMinHeight(dp(Compact.BUTTON_DP));" in ia and "new LinearLayout.LayoutParams(0, dp(Compact.BUTTON_DP), 1f)" in ea
      and "new LinearLayout.LayoutParams(-1, dp(44))" in ra)
check("the QR of the setup's start (rescue, the computer's way) 88 dp on the left; the done screen's packed report not under 160 dp (a dense code a phone must read)",
      sa.count("sa.urtv.home.Compact.QR_DP);") == 2 and "static final int QR_DP = 160;" in sa)
check("the focus ring stays: the setup's and the update's buttons keep GlassPaint.card, the rescue app its 4 dp ring",
      "sa.urtv.home.GlassPaint.card(t, look, 14);" in sa and "GlassPaint.card(p, look, 14);" in ua and "g.setStroke(has ? dp(4) : 0, 0xFF7FB2FF)" in ra)
check("the pictures of every page are in docs/mockups/compact-setup-v2/", len(list((ROOT / "docs/mockups/compact-setup-v2").glob("*.png"))) >= 8 if (ROOT / "docs/mockups/compact-setup-v2").is_dir() else False)
sys.exit(1 if failed else 0)
