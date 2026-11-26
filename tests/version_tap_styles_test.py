#!/usr/bin/env python3
"""The owner's pass (seven OKs on the version lines, GameRelease) must work the same in every settings style and layout, with nothing visible that hints at it.
SettingsActivity.render builds the version note once, for every style («أزرار», «كنبة», «tvOS»; the side list and 4.3.2's screens of buttons): the note is
wired to versionTap wherever the row «تحديث صفحة شاشة التوقف» is on the screen, row() adds that row to the list in the classic and the couch ways, the note is
focusable and clickable (a remote's OK clicks it), and no style, layout or look test sits between the row and the wiring.
Run: python3 tests/version_tap_styles_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

st = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
hub = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsHub.java").read_text(encoding="utf-8")
wire = "if (last.getParent() == list) { TextView ver = note(last, Updater.versionLines(this)); ver.setFocusable(true); ver.setClickable(true); ver.setOnClickListener(v -> versionTap()); }"
check("the version note is wired to versionTap, focusable and clickable, in the one place every style builds the update section", st.count(wire) == 1)
i = st.index(wire)
before = st[st.rindex('View last = row("تحديث صفحة شاشة التوقف"', 0, i):i]
check("nothing about the style (classic, couch, tvOS, the look) stands between the row and the wiring", not re.search(r"\b(classic|cs|ts|tiles|sideFocus)\b\s*(!=|==|\)|\?)|if \((cs|ts|classic)", before), before[:200])
row = st[st.index("private View row(String label"):st.index("private View offscreen()")]
check("row() puts the row in the list in the couch way and in the classic (and tvOS) way", row.count("list.addView(couch, clp);") == 1 and row.count("list.addView(card, lp);") == 1)
check("a row of another section is not built, so the note exists only where the row is (never twice)", "if (!on(cat)) return offscreen();" in row)
check("the row is one section's row in both layouts (the side list and 4.3.2): «تحديث صفحة شاشة التوقف» is UPDATE and «update», never left out", '{"تحديث صفحة شاشة التوقف", UPDATE}' in hub and '{"تحديث صفحة شاشة التوقف", "update"}' in hub)
note = st[st.index("private TextView note(View last"):st.index("return t;", st.index("private TextView note(View last"))]
check("note() puts the lines in the list (the last row scrolls them into view); nothing on them says there is a pass", "list.addView(t);" in note and "setBackground" not in note and "setTag" not in note)
tap = st[st.index("private void versionTap()"):st.index("private void versionTap()") + 400]
check("versionTap counts seven OKs, each within four seconds", "now - verAt > 4000" in tap and "verTaps < 7" in tap)
sys.exit(1 if failed else 0)
