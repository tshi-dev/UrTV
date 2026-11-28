#!/usr/bin/env python3
"""The audit's missing test (X, 27 Nov «شكل البطاقات»): the card size and corners chosen in the settings reach the page's CSS, the whole way:
the row's values (Prefs) -> the page's URL fragment (#csize=, #ccorner=) -> the classes the page puts on <html> -> the CSS rules that draw them.
Nothing in this chain was pinned: a renamed class or fragment would have left the two rows doing nothing."""
import re, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
prefs = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java").read_text(encoding="utf-8")
tpl = (ROOT / "src/template.html").read_text(encoding="utf-8")
failed = 0
def check(n, ok, d=""):
    global failed
    print(("PASS " if ok else "FAIL ") + n + ("" if ok else f"  -> {d}")); failed += not ok
sizes = re.search(r"CARD_SIZES\s*=\s*\{([^}]*)\}", prefs).group(1)
corners = re.search(r"CARD_CORNERS\s*=\s*\{([^}]*)\}", prefs).group(1)
sz = re.findall(r'"(\w+)"', sizes); cr = re.findall(r'"(\w+)"', corners)
check("the settings offer the sizes s, m, l and the corners style, sharp, soft, round", sz == ["s", "m", "l"] and cr == ["style", "sharp", "soft", "round"], (sz, cr))
check("the URL fragment carries a size other than the middle one and a corner other than «as the style» (and nothing otherwise)",
      '("m".equals(cardSize()) ? "" : "&csize=" + cardSize())' in prefs and '("style".equals(cardCorner()) ? "" : "&ccorner=" + cardCorner())' in prefs)
check("the page reads #csize= and #ccorner= and puts csz-<size> / ccr-<corner> on <html> for exactly the values the settings can send",
      "_Q.get('csize')" in tpl and "_Q.get('ccorner')" in tpl and "z === 's' || z === 'l'" in tpl and "r === 'sharp' || r === 'soft' || r === 'round'" in tpl
      and "classList.add('csz-' + z)" in tpl and "classList.add('ccr-' + r)" in tpl)
missing = [c for c in ("html.csz-s ", "html.csz-l ", "html.ccr-sharp ", "html.ccr-soft ", "html.ccr-round ") if c not in tpl]
check("every class the page can set has its CSS rule (the card's zoom and its radius)", not missing, missing)
check("the card's size changes its zoom (smaller below 1, larger above 1) and the corners are never a pill",
      re.search(r"html\.csz-s \.week\{zoom:\.\d+\}", tpl) and re.search(r"html\.csz-l \.week\{zoom:1\.\d+\}", tpl)
      and not re.search(r"html\.ccr-\w+ \.week\{[^}]*border-radius:(9\d\d|\d{3,})px", tpl))
sys.exit(1 if failed else 0)
