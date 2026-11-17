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
check("sweep2, the city prompt: ONE glass strip (12 dp corners) with plain words in it, only the focused choice filled; no capsule of its own",
      "nudge.setBackground(GlassPaint.fill(this, opaque(l.dialog), NUDGE_RADIUS, l.line));" in main and "nudgeAsk.setBackground(null);" in main
      and "nudgeCard.setBackground(focusOnly(NUDGE_FOCUS_RADIUS));" in main and "nudgeLater.setBackground(focusOnly(NUDGE_FOCUS_RADIUS));" in main
      and "static final float NUDGE_RADIUS = 12, NUDGE_FOCUS_RADIUS = 8;" in main and "nudgeAsk.setBackground(opaqueTile(" not in main and "nudgeCard.setBackground(opaqueTile(" not in main)
check("sweep2, the weather line: the strip keeps 16 px (at 1080p) from it, moved sideways after each layout of either",
      "static final int NUDGE_CLEAR_PX = 16;" in main and main.count("keepNudgeClear())") == 2
      and "Math.round(NUDGE_CLEAR_PX * getResources().getDisplayMetrics().widthPixels / 1920f)" in main)


def shift(n, nw, w, ww, gap):   # MainActivity.nudgeShift, the same arithmetic
    if w + ww // 2 >= n + nw // 2:
        over = n + nw + gap - w
        return -over if over > 0 else 0
    over = w + ww + gap - n
    return over if over > 0 else 0


check("sweep2, nudgeShift: the case of the screens (the strip 830..1496, the line from 1500): 12 px to the left; far apart: no move; a line on the left: to the right",
      shift(830, 666, 1500, 270, 16) == -12 and shift(400, 300, 1500, 270, 16) == 0 and shift(300, 400, 100, 210, 16) == 26
      and "int over = n + nw + gap - w; return over > 0 ? -over : 0;" in main and "int over = w + ww + gap - n;" in main)
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
sys.exit(1 if failed else 0)
