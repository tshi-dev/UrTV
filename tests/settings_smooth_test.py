#!/usr/bin/env python3
"""4.4 the owner: «smoother» settings, in both layouts (the side list and 4.3.2). Measured from the code (no TV here): what one render builds, what a
focus move does, the search. Prints the numbers.   python3 tests/settings_smooth_test.py"""
import collections, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


st, hub = ((APP / f).read_text(encoding="utf-8") for f in ("SettingsActivity.java", "SettingsHub.java"))
rowfn = st[st.index("    private View row(String label, String value, int icon, final Runnable onClick) {"):].split("\n    }\n")[0]
views_per_row = len(re.findall(r"= new (?:android\.widget\.)?(?:LinearLayout|ImageView|TextView)\(this\)", rowfn)) + 1   # and the mark (onMark)
side = collections.Counter(c for _, c in re.findall(r'\{"([^"]+)", ([A-Z]+)\}', hub[hub.index("static final String[][] ROWS = {"):hub.index("static final String[][] HEADERS = {")]))
total = sum(side.values())
print(f"NUMBERS one render, before: every row of every section built, {total} rows, about {total * views_per_row} views")
print(f"NUMBERS one render, after: the section's own rows only, in every style (one tree, 4.4 beta 4): {min(side.values())} to {max(side.values())} rows "
      f"(mean {total / len(side):.1f}); a screen of buttons builds none (it built {total})")
side_ms = re.search(r"SIDE_MS = (\d+)", st)
print(f"NUMBERS a focus move in the side list, before: a whole render ({total} rows); after: none until the focus rests {side_ms.group(1) if side_ms else '?'} ms")
check("a row of another section is not built: its action is kept (the search runs it) and row() gives one shared view, never added",
      "if (onClick != null) actions.put(FocusKey.key(label), onClick);\n        if (!on(cat)) return offscreen();" in rowfn
      and rowfn.index("if (!on(cat)) return offscreen();") < rowfn.index("new LinearLayout(this);", rowfn.index("actions.put")) and 'n.setTag("name");' in st)
check("the side list opens a section once the focus rests on its item, so holding the arrow builds nothing on the way",
      "side.removeCallbacks(sideOpen);" in st and "side.postDelayed(sideOpen, SIDE_MS);" in st and "static final long SIDE_MS = 120;" in st)
paint = st[st.index("    private void paintState(TextView v"):].split("\n    }\n")[0]
check("a focus change only recolours the on/off word (the span's colour, drawn again): no new text, no layout",
      "if (g != null && g.length == 1) { if (g[0].color != green) { g[0].color = green; v.invalidate(); } return; }" in paint
      and "!text.contentEquals(v.getText())" in paint and "implements android.text.style.UpdateAppearance" in st)
check("the focused row: a short scale (1.02) on the one motion (Motion: 200 ms, couch-tv's curve; none with «الوضع الخفيف»), lifted over its neighbours, instead of anything laid out again",
      "static final float FOCUS_SCALE = 1.02f;" in st and "static final long FOCUS_MS = Motion.MS;" in st
      and "v.animate().scaleX(has ? FOCUS_SCALE : 1f).scaleY(has ? FOCUS_SCALE : 1f).setDuration(Motion.lite(SettingsActivity.this) ? 0 : FOCUS_MS).setInterpolator(Motion.ease()).start();" in rowfn and "v.setTranslationZ(has ? dp(4) : 0);" in rowfn)
search = st[st.index("    private void searchSettings() {"):].split("\n    }\n")[0]
check("the search: the rows found as the words are typed, once the typing rests, from the list in memory, in a recycled list",
      "h.postDelayed(look_, SEARCH_MS);" in search and "SettingsHub.search(in.getText().toString().trim())" in search
      and "super.getView(i, old, parent)" in search and "static final long SEARCH_MS = 120;" in st)
check("nothing from the disk or the network in render() or a row (the fonts are read once and kept)",
      not re.search(r"getAssets\(\)|openFileInput|new URL\(|HttpURLConnection|BitmapFactory", st[st.index("    private void render() {"):].split("\n    }\n")[0] + rowfn)
      and "Typeface tf = fonts.get(name);" in st)
sys.exit(1 if failed else 0)
