#!/usr/bin/env python3
"""4.4 the consistency sweep (docs/mockups/sweep/): what it fixed stays fixed.   python3 tests/sweep_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


s = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
check("«المنطقة»: a region's count of cities is in the digits of the setting (it read «(32)» beside «١٢ ساعة»)",
      'DigitsText.convert(String.valueOf(regions[i].cities.length), latinDigits)' in s and 'cities.length + ")"' not in s)
bare = [m.group(0) for m in re.finditer(r'"\s*\(" \+ [a-zA-Z_.\[\]]+\.length \+ "\)"', s)]
check("no other count in brackets bypasses the digits setting in the settings", not bare, bare)

HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
check("sweep2, the city prompt: ONE glass strip (8 dp corners on a ~24 dp strip, never a capsule) with plain words in it, only the focused choice filled",
      "nudge.setBackground(GlassPaint.fill(this, opaque(l.dialog), NUDGE_RADIUS, l.line));" in main and "nudgeAsk.setBackground(null);" in main
      and "nudgeCard.setBackground(focusOnly(NUDGE_FOCUS_RADIUS));" in main and "nudgeLater.setBackground(focusOnly(NUDGE_FOCUS_RADIUS));" in main
      and "static final float NUDGE_RADIUS = 8, NUDGE_FOCUS_RADIUS = 6;" in main and "nudgeAsk.setBackground(opaqueTile(" not in main)
nudge = main[main.index("private void buildHomeNudge("):main.index("private void snoozeNudge()")]
check("sweep3 (the owner: «the notification, make it small»): one short line at the home's secondary size (12 sp), small plain buttons, at most 40 % of the screen",
      "static final int NUDGE_SP = 12;" in main and "static final float NUDGE_MAX = 0.40f;" in main and nudge.count("text(NUDGE_SP,") == 3
      and 'static final String CITY_ASK = S.n("اختر مدينتك للطقس والصلاة");' in main and "Math.round(screenW * NUDGE_MAX)" in main
      and "nudgeCard.setPadding(dp(10), dp(4), dp(10), dp(4));" in main and "nudgeLater.setPadding(dp(10), dp(4), dp(10), dp(4));" in main)
check("sweep3: never cut with «…» (the question is short; a longer notice wraps to two lines)",
      "nudgeAsk.setMaxLines(2);" in nudge and "TruncateAt" not in nudge and "nudgeAsk.setEllipsize" not in main and "وين أنت؟ اختر مدينتك عشان الطقس والصلاة" not in main)
check("sweep3: on the top row, right after the round buttons (16 px at 1080p), its middle on the row's middle, never over the weather line or the clock",
      "static final int NUDGE_CLEAR_PX = 16;" in main and main.count("keepNudgeClear())") == 3
      and "dx = barLeft ? b[0] + pillBar.getWidth() + gap - n[0] : b[0] - gap - (n[0] + nudge.getWidth());" in main
      and "dy = b[1] + pillBar.getHeight() / 2f - (n[1] + nudge.getHeight() / 2f);" in main and "dx += Math.min(0f, w[0] - gap - right);" in main)
cc = (HOME / "ControlCenter.java").read_text(encoding="utf-8")
check("sweep2, the control centre: «تخصيص», «شكل اللوحة», «إعادة الترتيب», «تم» are words (no outline at rest), an 8 dp focus fill, never a capsule",
      "static final int WORD_RADIUS = 8;" in cc and "kind == PILL || kind == PRIMARY ? WORD_RADIUS : 12" in cc and "kind == PILL && !f" not in cc
      and "kind == TOGGLE || kind == PILL ?" not in cc)
bar = (HOME / "PillBar.java").read_text(encoding="utf-8")
check("sweep2, the top row: its bar never rounder than 8 dp, a labelled button 6 dp (no capsule round «الجوال»)",
      "static final float MAX_RADIUS = 8f;" in bar and "Math.min(m.radius / 2f, MAX_RADIUS) * dp" in bar and "bg.setCornerRadius(dp(6));" in (HOME / "Pill.java").read_text(encoding="utf-8"))
check("sweep2, the settings hint: the rows' secondary size (14 sp; couch-tv its info line, 1.4375 rem) in the secondary colour, no longer 12 sp",
      "static final int HINT_SP = 14;" in s and "hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, HINT_SP);" in s and "hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);" not in s
      and "hint.setTextColor(CouchStyle.SUB);" in s and "HINT = 1.4375f," in (APP / "CouchStyle.java").read_text(encoding="utf-8"))
check("sweep2 option 2 (the owner), 4.4 beta 4 reversed for «كنبة» (the owner, typed): «الساعة: زجاجية» with a card BESIDE the clock shows the regular clock (the glass clock again without cards); with the couch strip under it the glass clock stays (GlassClockRule)",
      'static boolean glassClock(String homeClock, boolean cardsShown, boolean inRow) { return GlassClockRule.glass(homeClock, cardsShown, inRow); }' in main
      and "boolean glass = glassClock(cfg.homeClock, homeCard != null && homeCard.shownCards() > 0, homeCard != null && homeCard.inRow());" in main and "boolean bigClock = glass;" in main)
sys.exit(1 if failed else 0)
