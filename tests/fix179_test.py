#!/usr/bin/env python3
"""Build 179's re-check (tester step 23): the numbers pinned. 1 dp = 2 px at the tester's 1080p.   python3 tests/fix179_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
RES = ROOT / "android/launcher/src/main/res"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


main = text(HOME / "MainActivity.java")
pills = re.findall(r"sideLlp\(-2, dp\((?:Pill|NowCard)\.HEIGHT\), 0, [-\w]+, dp\((\d+)\), [-\w]+\)", main)
check("the round buttons at the top of the home: 24 px apart (12 dp), every one added the same way", len(pills) == 6 and set(pills) == {"12"}, pills)
row = text(RES / "layout/item_row.xml")
check("Tilawah rows: 32 px of padding above and below the text (16 dp), 32 at the start, 32 at the end; rows 16 px apart",
      'android:paddingTop="16dp"' in row and 'android:paddingBottom="16dp">' in row and 'android:paddingEnd="16dp"' in row
      and all('android:dividerHeight="8dp"' in text(RES / f"layout/{f}.xml") for f in ("activity_main", "activity_adhan"))
      and 'android:verticalSpacing="8dp"' in text(RES / "layout/activity_surah.xml"))
pin = text(HOME / "PinUi.java")
check("PIN pad: no focus scale on a key (it ate 4 px of the gap beside it); the gaps between keys are one value, 24 px",
      "scaleX" not in pin and text(HOME / "PinPrompt.java").count("dp(a, 12)") >= 5)
settings = text(APP / "SettingsActivity.java")
check("settings pages: 64 px above the first line, a group's header 32 above and 16 under it (the first row was 124 px down)",
      "list.setPadding(dp(8), dp(32), dp(8), dp(16));" in settings and "t.setPadding(dp(10), dp(16), dp(10), dp(8));" in settings and "head.setPadding(0, 0, 0, dp(16));" in settings)
mk = text(HOME / "MarketsActivity.java")
check("markets: the page ends at the safe area (5 % under it), so no list starts under y 1026 and the lists scroll in",
      "page.setPadding(dp(60), dp(28), dp(60), Math.round(getResources().getDisplayMetrics().heightPixels * 0.05f));" in mk)
check("markets: stock, volume and watchlist rows have 24 px above and below their text (box wider than 600 px)", mk.count("r.setPadding(dp(16), dp(12), dp(16), dp(12));") >= 3 and "r.setPadding(dp(16), dp(8), dp(16), dp(8));" not in mk)
check("markets: pressing «أخفِ/أظهر المبالغ» keeps «محفظتي»: the tab's own re-draw does not let the focus that falls on the tabs switch the tab",
      "private boolean rebuilding;" in mk and "if (has && k != tab && !rebuilding) want(k);" in mk and "if (k == tab) {\n            rebuilding = true;" in mk)
tpl = text(ROOT / "src/template.html")
check("football games card: a club's name takes two lines and the league label wraps to two, nothing is cut with «…» before a third line",
      re.search(r"\.ftm \.nm\{white-space:normal;[^}]*-webkit-line-clamp:2", tpl) is not None
      and re.search(r"\.fct span:first-child\{[^}]*-webkit-line-clamp:2", tpl) is not None)
check("dist matches the source (src/build.py)", text(ROOT / "dist/tv.html").count("-webkit-line-clamp:2") >= 2 and text(ROOT / "dist/tv-en.html").count("-webkit-line-clamp:2") >= 2)
sys.exit(1 if failed else 0)
