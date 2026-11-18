#!/usr/bin/env python3
"""4.3.5 (an LG-branded CVTE Android 9 set: the LG logo, then a black screen, the remote doing nothing): on a TV without a working system WebView
`new WebView` throws, and the home's card made one at every start, so the home died at every start. Every WebView of the home is made by
WebViews.make (null when it cannot be, never tried again in that process); the card stays off and the home draws without it; «المباريات» says so
and closes. The startup picture never waits for a card that was not made (HomeSnapshot.LIVE_WAIT_MS caps it, and loading is set only on a load).
Run: python3 tests/webview_guard_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

wv = (HOME / "WebViews.java").read_text(encoding="utf-8")
card = (HOME / "HomeCard.java").read_text(encoding="utf-8")
matches = (HOME / "MatchesActivity.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
tsv = (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8")

raw = [str(p.relative_to(ROOT)) for p in HOME.rglob("*.java") if p.name != "WebViews.java" and "new WebView(" in re.sub(r"//[^\n]*", "", p.read_text(encoding="utf-8"))]
check("no `new WebView(` in the home outside WebViews.make", not raw, raw)
check("WebViews.make catches what a missing or broken WebView throws, and returns null",
      "catch (RuntimeException | LinkageError e)" in wv and "return null;" in wv and "return new WebView(c);" in wv)
check("…and after one failure it does not try again in that process", "if (broken) return null;" in wv and "broken = true;" in wv)
check("…its log line names the exception's class only (nothing of the TV)", 'e.getClass().getName()' in wv and "e.getMessage()" not in wv)
create = card.split("private boolean create(String u) {")[1].split("\n    }\n")[0] if "private boolean create(String u) {" in card else ""
check("HomeCard.create says whether it made the card", "WebView made = WebViews.make(activity);" in create and "if (made == null) return false;" in create
      and create.rstrip().endswith("return true;"))
check("…and the card's start stops there (no web.onResume on a card that was not made)", "if (!create(want)) return;" in card)
check("«loading» (what the startup picture waits for) is set only once a page is loading", create.index("loading = true;") > create.index("if (made == null) return false;"))
check("the startup picture has its own time cap", "waited >= HomeSnapshot.LIVE_WAIT_MS" in main)
check("«المباريات» without a WebView: says so and closes, never a crash",
      "web = WebViews.make(this);" in matches and "finish();\n            return;" in matches.split("web = WebViews.make(this);")[1].split("web.setBackgroundColor")[0])
check("…its words in English too", "هذا التلفزيون ما فيه متصفح النظام، فالصفحة ما تفتح\t" in tsv)
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
