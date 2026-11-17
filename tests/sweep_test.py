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
sys.exit(1 if failed else 0)
