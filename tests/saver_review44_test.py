#!/usr/bin/env python3
"""4.4 review fixes in the screensaver app (docs/dev/reviews/4.4.md F12-F15): source pins; each fails on the code before its fix."""
import re, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def src(n): return (APP / n).read_text(encoding="utf-8")
def body(s, head):
    i = s.index(head); j = s.find("\n    }\n", i); return s[i:j if j > 0 else len(s)]

# F15: the screensaver start never asks the home's provider on the UI thread
npb, prefs = src("NowPlayingBridge.java"), src("Prefs.java")
ko = body(npb, "static boolean kidsOn(")
check("F15: kidsOn (called by screenUrl on the UI thread) makes no provider query: it reads a kept copy and a thread refreshes it",
      "getContentResolver" not in ko and 'getBoolean("kids_on"' in ko and "new Thread(" in ko and "kidsAsk(" in ko)
check("F15: the blocking query is kidsAsk, used only by that thread", "static boolean kidsAsk(" in npb and "getContentResolver().query(Uri.parse(\"content://sa.urtv.home.nowplaying/kids\")" in body(npb, "static boolean kidsAsk(")
      and len(re.findall(r"kidsAsk\(", npb)) == 2)
check("F15: screenUrl still adds &kids=1 only with the kick-off on and kids on", 'kickoff() && NowPlayingBridge.kidsOn(c) ? "&kids=1" : ""' in prefs)

sys.exit(1 if failed else 0)
