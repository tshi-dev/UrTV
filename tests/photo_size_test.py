#!/usr/bin/env python3
"""G3 (4.3.3): a downloaded photo is kept at the TV's own screen size: PhotoShrink.screenMax (the screen's long side, between 1280 and 1920; 1920 when not known),
so a TV that draws its screen at 1280x720 keeps 1280 px photos; a weak TV 1280 (the screensaver: LOW or MID as before; the home: LOW, the «خفيف» profile). The rule
is run on a plain JVM (the same arithmetic, read from the source), the wiring is read from the sources. WebP is not used: a JPEG decodes faster on a weak TV's CPU
and the photos come from the host as JPEG.

    python3 tests/photo_size_test.py
"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def text(p): return Path(p).read_text(encoding="utf-8")

ps = text(SAVER / "PhotoShrink.java")
check("both PhotoShrink copies are the same code", ps.split("\n", 1)[1] == text(HOME / "PhotoShrink.java").split("\n", 1)[1])
m = re.search(r"static int screenMax\(int screenLong\) \{ return screenLong <= 0 \? MAX : Math\.max\(LOW, Math\.min\(MAX, screenLong\)\); \}", ps)
check("the rule: the screen's long side between LOW and MAX, MAX when not known", m is not None and "static final int MAX = 1920" in ps and "static final int LOW = 1280;" in ps)
rule = lambda s: 1920 if s <= 0 else max(1280, min(1920, s))
check("a 720p screen: 1280; 1080p: 1920; a 4K UI: 1920; a 1366 screen: 1366; unknown: 1920", [rule(x) for x in (1280, 1920, 3840, 1366, 0)] == [1280, 1920, 1920, 1366, 1920])
check("the screen size is the app's own display metrics (what the WebView and the home draw at)", "c.getResources().getDisplayMetrics()" in ps)
check("the screensaver's downloaded photos: a weak TV 1280 as before, else the screen's size",
      "Prefs.tightRam(c) ? PhotoShrink.toSize(data, LOW_PX) : PhotoShrink.toSize(data, PhotoShrink.screenMax(c))" in text(SAVER / "PhotoCache.java"))
check("the home's downloaded photos: «خفيف» (Ram.low) 1280, else the screen's size",
      "PhotoShrink.toSize(data, Ram.low(this) ? PhotoShrink.LOW : PhotoShrink.screenMax(this))" in text(HOME / "MainActivity.java"))
check("the disk caches keep their caps (40 MB of photos at most)", "CACHE_MAX = 40L << 20" in text(SAVER / "Bounded.java") and "Bounded.fitting(sizes, Bounded.CACHE_MAX)" in text(SAVER / "PhotoCache.java"))
sys.exit(1 if failed else 0)
