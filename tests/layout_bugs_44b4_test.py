#!/usr/bin/env python3
"""4.4 beta 4, three known bugs of the home's layout (docs/dev/REGRESSIONS.md «Known bugs»; the supervisor's a, f, h), each seen on the emulator's
shots (.github/screens/shoot.sh: bug-a-dock-few-apps, bug-h-sand-reminder, bug-h-sand-letters):
  (a) a dock of three apps left a wide empty bar (tester step 76): the bar is as wide as its tiles;
  (f) the iqama bar was drawn over the apps page's first row (tester step 71): the page stops under the bar;
  (h) «رملي»: the reciter letter index was nearly invisible, and the reminder card lay over the clock (tester step 37).
    python3 tests/layout_bugs_44b4_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
main = (H / "MainActivity.java").read_text(encoding="utf-8")
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


fit = main[main.index("    private void fitTray() {"):]
fit = fit[:fit.index("\n    }\n") + 6]
check("(a) in «بطاقات» the bar is as wide as its tiles and its own padding, placed where the apps are; wider than the screen it keeps the full width",
      "if (FavBar.TILES.equals(appsLook) && parent.getWidth() > 0) {" in fit and "w += c.getMeasuredWidth() + m.leftMargin + m.rightMargin;" in fit
      and "if (w > dockRow.getPaddingLeft() + dockRow.getPaddingRight() && w < room) want = w;" in fit
      and "int g = FavBar.alignGravity(appsAlignNow) & Gravity.HORIZONTAL_GRAVITY_MASK;" in fit, fit[:300])
check("(a) «كمّل المشاهدة» under it moves with it: the same frame (the bar's box and the row both have FavBar.SIDE at each side), the bar's own start and end",
      "int left = g == Gravity.LEFT ? 0 : g == Gravity.RIGHT ? free : free / 2;" in fit and "int l = bar(FavBar.SIDE) + left, r = bar(FavBar.SIDE) + free - left;" in fit
      and "hero.addView(bottom, lp(-1, -2, Gravity.BOTTOM, bar(FavBar.SIDE), 0, bar(FavBar.SIDE)" in main and "content.addView(tvRowsBox, llp(-1, -2, bar(FavBar.SIDE), 0, bar(FavBar.SIDE), 0));" in main)
check("(a) it follows the row: each layout of the tiles (one post at a time), the look, the alignment, and «كمّل المشاهدة» coming or going",
      "if (!trayFitQueued) { trayFitQueued = true; handler.post(this::fitTray); }" in main and main.count("fitTray();") >= 3
      and "fitTray();   // the bar is the screen's width while the row is under it" in main)
import re
fav = (H / "FavBar.java").read_text(encoding="utf-8")
tile_w, pad = (int(re.search(rf"\b{k} = (\d+)", fav).group(1)) for k in ("TILE_W", "PAD_SIDE"))
zoom = float(re.search(r"FOCUS_SCALE = ([\d.]+)f", fav).group(1))
check("(a) a focused tile still fits in the snug bar: its growth on each side is under the bar's own padding (FavBar's TILE_W, FOCUS_SCALE, PAD_SIDE)",
      tile_w * (zoom - 1) / 2 < pad, (tile_w, zoom, pad))
row = main[main.index("    private void scrollToRow(View tileBox) {"):]
row = row[:row.index("\n    }\n") + 6]
check("(f) the apps page's first row: the tray stops under the iqama bar while it shows (the bar keeps the top edge)",
      "int barH = adhanBar.getVisibility() == View.VISIBLE ? Math.max(adhanBar.getHeight(), dp(26)) : 0;" in row
      and "tray.getTop() - barH, max)" in row)
check("(f) the bar coming or going while that row is up moves the page at once",
      "if (was != now && f != null && gridCards.contains(f) && f.getParent() instanceof View && gridFirst == 0) scrollToRow((View) f.getParent());" in main)
til = (H / "tilawah/MainActivity.java").read_text(encoding="utf-8")
check("(h) the reciter letters wear the look's own text and focus ink (R.color.sand: ?attr/gText, ?attr/gOnFocus), not a fixed cream",
      "t.setTextColor(getColorStateList(R.color.sand));" in til and "0xFFE7DCC4" not in til
      and "?attr/gText" in (ROOT / "android/launcher/src/main/res/color/sand.xml").read_text(encoding="utf-8"))
card = (H / "ReminderCard.java").read_text(encoding="utf-8")
check("(h) the reminder card stands 16 dp under the clock column (never above 60 dp) and goes up only as far as the screen's bottom needs",
      "if (y > 0) top = Math.max(top, y + dp(16));" in card and "if (card.getHeight() > 0 && room > 0) top = Math.min(top, room);" in card
      and "card.post(this::place);" in card and "reminder.under(new ReminderCard.Under() {" in main and "shownBottom((ViewGroup) clockBlock)" in main)
check("(h) the clock column ends at its lowest drawn line: the big clock leaves the small one INVISIBLE and lifts the date (the emulator: the card 72 dp low, over the apps)",
      "if (c.getVisibility() != View.VISIBLE || c.getHeight() == 0) continue;" in main
      and "low = Math.max(low, Math.round(c.getTop() + inner + c.getTranslationY()));" in main and "return low == 0 ? g.getHeight() : low;" in main)
check("(h) with the big clock it keeps clear of the digits: beside them under the clock column, as wide as that room allows, its two buttons one over the other when narrow; too narrow there, under the digits (the emulator: over the digits, then over the apps row)",
      "return ScenePlan.fixedBox(dm.widthPixels, dm.heightPixels, beside, Dir.rtl());" in main and "int side = Dir.rtl() ? parent.getWidth() - big[2] : big[0];" in card
      and "if (floor > 0 && card.getHeight() > 0 && above < top && above >= (y > 0 ? y + dp(8) : dp(16))) top = above;" in card and "@Override public int floor() {" in main
      and "if (room >= dp(200)) width = Math.min(width, room);" in card and "else top = Math.max(top, big[3] + dp(16));" in card and "stack(width < dp(320));" in card)
shoot = (ROOT / ".github/screens/shoot.sh").read_text(encoding="utf-8")
check("the letters' shot opens تلاوة by its button (not exported: an adb start is refused)", 'focus_to "تلاوة وأذان" 10 DPAD_LEFT' in shoot)
check("the emulator's shots of them", all(f"shot {n} " in shoot for n in ("bug-a-dock-few-apps", "bug-h-sand-reminder", "bug-h-sand-letters")))
sys.exit(1 if failed else 0)
