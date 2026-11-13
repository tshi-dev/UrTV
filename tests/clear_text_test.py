#!/usr/bin/env python3
"""«نص أوضح» (4.3.1): one setting that makes the grey words and numbers of the home and the screensaver as clear as the main text, in every look.

    python3 tests/clear_text_test.py

The contrast of every look with it on is checked for real in tests/glass_test.py (GlassRun --clear); the page's rewrite of its greys in tests/page.test.mjs. Here: the plumbing."""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


prefs, rcv, st, hub = text(SAVER / "Prefs.java"), text(SAVER / "SaverReceiver.java"), text(SAVER / "SettingsActivity.java"), text(SAVER / "SettingsHub.java")
gs_a, gs_h, look = text(SAVER / "GlassStyle.java"), text(HOME / "GlassStyle.java"), text(HOME / "Look.java")
page, sh = text(ROOT / "src/template.html"), text(ROOT / "optimize/saver.sh")

check("the setting is off by default, kept as clearText, and reaches both apps as «text=clear» in the tweaks string and the page as #clear=1 (only when on: the address is as before)",
      'boolean clearText() { return sp.getBoolean("clearText", false); }' in prefs and 'if (clear) s.append(";text=clear");' in prefs
      and "&& sync && !clear) return \"\";" in prefs and '(clearText() ? "&clear=1" : "")' in prefs)
check("GlassStyle (both copies, identical but the package): «text=clear» is read, clearText() says it, every look comes out with its secondary text as the main text and the focused tile's too, and the repaint key knows it",
      all('else if ("text".equals(k)) clear = "clear".equals(v);' in g and "public static boolean clearText()" in g and "l.light, l.base, l.scrim, l.scrimMin, l.text, l.text, l.accent, l.warn, l.panel, l.line, l.dialog, l.focus, l.onFocus, l.onFocus, l.hint, l.ring" in g
          and "return clearer(tinted(plain(look, light)));" in g and "return clearer(light ? LIGHT : DARK);" in g and '(tweakClear ? "clear|" : "")' in g for g in (gs_a, gs_h))
      and gs_a.replace("sa.urtv.screensaver", "P").replace("sa.urtv.home", "P") == gs_h.replace("sa.urtv.screensaver", "P").replace("sa.urtv.home", "P"))
check("the home's own palette (the date, the weather, the lines under the clock) follows: its soft colour is its ink when on", "GlassStyle.clearText() ? new Palette(p.text, p.ink, p.ink," in look)
check("the page: #clear=1 rewrites the sheet's grey text colours (not a border or a fill), --dim and the football and markets tokens to the full ink, once, before anything shows; the gold, green and red stay",
      "clear: _Q.get('clear') === '1'" in page and "if(CONFIG.clear){" in page and "(?<![-\\w])(color:)#(?:B9AE96|A79C85|9C9279|8F8670|CFC5AD|D8CDB6|E6DCC4|9FB8A8)" in page and "(--dim:)#C3B8A2" in page)
check("saver.sh cleartext on|off, validated by the receiver, shown by `show`, moved by settings_move (clear_text)",
      "  cleartext) " in sh and "optimize/saver.sh cleartext on" in sh.split("set -euo")[0] and 'getStringExtra("cleartext")' in rcv and "setClearText" in rcv and '"\\ncleartext: "' in rcv
      and '"clear_text"' in text(ROOT / "optimize/settings_move.py") and 'es("cleartext", flag(v))' in text(ROOT / "optimize/settings_move.py"))
check("the screensaver's settings have the row «نص أوضح» (on says «مفعّل…», off «متوقف»), in its category and in the English table, with no «·» or «•»",
      'row("نص أوضح", prefs.clearText() ? "مفعّل الكلام الرمادي بلون النص الكامل" : "متوقف"' in st and '{"نص أوضح", SAVER}' in hub
      and "نص أوضح\tClearer text" in text(ROOT / "i18n/en/saver.tsv") and not any(c in "نص أوضح مفعّل الكلام الرمادي بلون النص الكامل" for c in "·•"))
check("docs: SETTINGS.md and FEATURES.md say it", "optimize/saver.sh cleartext " in text(ROOT / "docs/SETTINGS.md") and "optimize/saver.sh cleartext " in text(ROOT / "docs/FEATURES.md"))
sys.exit(1 if failed else 0)
