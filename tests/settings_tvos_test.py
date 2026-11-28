#!/usr/bin/env python3
"""4.4 beta 2 «شكل شاشة الإعدادات: tvOS» (the owner approved docs/mockups/settings-style/, a2145c5c): the third style of the settings screen. The
4.3.2 screen's logic (its screens, Back up a screen, the same rows, search and actions) drawn in the tvOS manner (TvosStyle): one list of large
glass rows, the focused one white with dark words and a little bigger, the shown section's icon large in a glass square on the other side with
its line; on a weak TV (Motion.lite) by itself the light version: flat, nothing grows. Read from the code.   python3 tests/settings_tvos_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
st, prefs, ts, recv = read(APP / "SettingsActivity.java"), read(APP / "Prefs.java"), read(APP / "TvosStyle.java"), read(APP / "SaverReceiver.java")
saver = read(ROOT / "optimize/saver.sh")

check("«tvOS» is the third choice of «شكل شاشة الإعدادات» (stored tvos), from the one list the row, the command and the receiver read",
      'SETTINGS_STYLES = {"buttons", "couch", "tvos"};' in prefs and 'SETTINGS_STYLE_NAMES = {"أزرار", "كنبة", "tvOS"};' in prefs
      and "Prefs.SETTINGS_STYLES" in recv and "settingsstyle buttons|couch|tvos" in saver)
check("an update changes nothing: the style read the first time is the screen the TV shows (never tvOS by itself)",
      'v = couchOn() || sp.getBoolean("uiOneForm", false) || SIDE_FOR_NEW_SETUPS && FreshInstall.is(ctx) ? "couch" : "buttons";' in prefs and "static final boolean SIDE_FOR_NEW_SETUPS = true;" in prefs)   # 4.4 the owner: the side list for new setups (on; R-470)
check("the 4.3.2 logic: classic stays true with tvOS (its screens, Back up a screen, search), TvosStyle only draws",
      'classic = !"couch".equals(prefs.settingsStyle());' in st and 'if ("tvos".equals(prefs.settingsStyle())) ts = new TvosStyle(this);' in st
      and "if (ts != null) { tvosHead(); return; }" in st)
check("one list: a section a line on the first screens, ▲ ▼ along it, ◀ ▶ stay",
      "boolean alone = (wide && i == 0) || ts != null;" in st and "t.setNextFocusUpId(tiles.get(Math.max(0, i - 1)).getId());" in st
      and "t.setNextFocusLeftId(t.getId());" in st)
check("the rows at the start side, the section's icon in its glass square on the other side with its line; the side follows the focused section",
      "panes.addView(scroll, new LinearLayout.LayoutParams(width - sideWidth - gapPx, -1));\n            panes.addView(sideScroll, sideLp);" in st
      and "square.setBackground(ts.glyphBox(" in st and "tvosAbout.setText(conv(SettingsHub.about(cat)));" in st and "if (has) tvosShow(cat);" in st)
check("the tvOS colours on every card and dialog of the screen (one Look: white words, glass rows, the focused one white with dark words)",
      "look = ts.look(look);" in st and st.count("couchVeil(); tvosVeil();") == 3 and "0xFFFFFFFF, 0xFF111111, 0x8C000000" in ts)
check("a weak TV by itself: Motion.lite gives the light version: a stronger veil, a flat icon square, nothing grows (rows and sections)",
      "light = Motion.lite(c);" in ts and "float grow() { return light ? 1f : GROW; }" in ts and "int veil() { return light ? VEIL_LIGHT : VEIL; }" in ts
      and "if (ts != null && ts.light) { v.animate().cancel(); v.setScaleX(1f); v.setScaleY(1f); v.setTranslationZ(0); }" in st
      and "setDuration(ts.light ? 0 : FOCUS_MS)" in st)
check("720p and the large font: the icon's square is a part of the side (never wider than it), its line wraps; the rows are today's (they wrap)",
      "Math.min(Math.round(sideWidth * TvosStyle.GLYPH_SIDE), dp(TvosStyle.GLYPH_MAX_DP))" in st and "GLYPH_SIDE = 0.78f" in ts)
check("no file, network or provider work added: TvosStyle is colours and numbers only", not any(w in ts for w in ("File", "ContentResolver", "Thread", "http", "query(")))
check("no «·» or «•» in the tvOS words", "·" not in ts and "•" not in ts)
sys.exit(1 if failed else 0)
