#!/usr/bin/env python3
"""«تحكم من الجوال»: the pairing screen on the TV and the phone page's tabs. The pairing screen (the owner, on the TV test of 3.145: it should fit one page with no scrolling, and the
address and the code should stand out; the code sat at the very bottom edge). Two columns: the address and the 6-digit code each in a
large box of their own on the right, the QR code on the left. Checked in the source.   Run: python3 tests/pairing_screen_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
act = (ROOT / "android/launcher/src/main/java/sa/urtv/home/PhoneActivity.java").read_text(encoding="utf-8")
page = (ROOT / "android/launcher/src/main/assets/phone/index.html").read_text(encoding="utf-8")
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

on = act[act.index("private View renderOn()"):].split("\n    }\n")[0]
offer = on[on.index("shownToken = o.token;"):on.index("update();")]
check("two columns, right to left: the address and the code first (the right), then the QR code (the left)",
      "body.setOrientation(LinearLayout.HORIZONTAL);" in offer and offer.index("body.addView(info") < offer.index("body.addView(qrCol"))
check("the address stands out: large (28), gold, in its own framed box", 'boxed(ltr(control.address() + "/p"), 28, GOLD, 0f)' in offer)
check("the code stands out: very large (48), in its own framed box, its six digits read as two threes", "boxed(ltr(spaced(o.code)), 48, TEXT, 0.08f)" in offer
      and 'code.substring(0, 3) + " " + code.substring(3)' in act)
check("a value's box: a glass panel of its own (GlassPaint.panel, the glass look), one line", "t.setBackground(GlassPaint.panel(this, look, 16));" in act and "t.setSingleLine(true);" in act)
check("the QR code a little smaller (240 dp), beside the values, not over them", "new LinearLayout.LayoutParams(dp(240), dp(240))" in offer and "dp(300), dp(300)" not in act)
check("the screen is wide enough for both on one TV screen (up to 1000 dp, the screen less 96 dp), with less room above and below",
      "Math.min(Math.round(getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density) - 96, 1000)" in act
      and "col.setPadding(0, dp(20), 0, dp(16));" in act)
# a rough height at 1080p and 720p (both 540 dp tall): text is about 1.4 times its size; the taller of the two columns, with one phone
txt = lambda sp: sp * 1.4
info = (txt(15) + 6) + (txt(28) + 22 + 14) + (txt(15) + 6) + (txt(48) + 22 + 8) + (txt(13) + 14) + (txt(16) + 4) + (46 + 8) + (4 + 52 + 8)
qr = txt(15) + 8 + 240
page_h = 20 + 16 + txt(26) + max(info, qr) + 8 + 2 * txt(12) + 6
check(f"one page on a 1080p or 720p TV: about {page_h:.0f} of its 540 dp, with a phone connected", page_h <= 540, page_h)
check("the phones and «إيقاف» go in the right column under the code, not under the whole screen", "below = info;" in offer
      and "below.addView(stop, sp);" in on and "below.addView(row, rowParams());" in on)
check("the countdown is in the right column, under the code", offer.index("info.addView(countdown") > offer.index("boxed(ltr(spaced(o.code))"))
check("on the phone, the code box takes the space of «123 456» too (7 characters), and only the digits are sent",
      'id="code" inputmode="numeric" autocomplete="off" maxlength="7"' in page and "var c = digits($('code').value).replace(/\\D/g, '');" in page)
# ---- the phone page's tabs (the owner: the bottom icons need their own design): our own line drawings, one style, in the text's colour
check("the four tabs have their own drawn icons (remote, apps, now, settings) in the page's markup, no emoji or text glyphs",
      all(f'<g id="ico-{k}">' in page for k in ("remote", "apps", "now", "settings")) and "🎮" not in page
      and not re.search(r"\\['(remote|apps|now|settings)', '[^']+', '", page))
check("…drawn in the text's colour, so the dark and the light look and the gold «on» tab all take them (4.0 look: the «on» tab's word and drawing in gold)",
      "#bar button svg {" in page and "stroke:currentColor;" in page and "fill:none;" in page and "#bar button.on { color:var(--gold);" in page)
check("…copied into each tab from the page's own drawings (cloneNode; no HTML built from text, no address)",
      "var g = $('ico-' + name).cloneNode(true);" in page and "b.appendChild(icon(t[0]));" in page and "innerHTML" not in page and "w3.org" not in page)
sys.exit(1 if failed else 0)
