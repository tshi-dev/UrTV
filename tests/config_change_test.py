#!/usr/bin/env python3
"""4.4 beta 4 (the tester: `wm size 1280x720` while the home was up left only the clock and the weather): the home declares configChanges for the
screen's size and density, so Android does not make it again, and its layout is built from the screen's pixels. A change of the screen's size or
density now makes the home again (recreate), and nothing else does. The frozen-screen watchdog counts a focus move as a frame (a zoom is drawn by
the render thread with no draw pass: five false «frozen-screen recovered» lines in one round).
Run: python3 tests/config_change_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

main = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
man = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
check("the manifest still lets the home handle size and density itself (so the home must make itself again)", "screenSize" in man.split("MainActivity")[1][:900] if "MainActivity" in man else False)
oc = main.split("protected void onCreate(Bundle b) {")[1][:700]
check("onCreate remembers the size (dp) and density the layout is built for", all(t in oc for t in ("builtWidthDp = built.screenWidthDp;", "builtHeightDp = built.screenHeightDp;", "builtDensityDpi = built.densityDpi;")))
cc = main.split("public void onConfigurationChanged(android.content.res.Configuration c) {")[1].split("\n    }\n")[0]
check("a change of width, height or density makes the home again; before it is built, nothing", "if (builtWidthDp == 0) return;" in cc
      and "c.screenWidthDp != builtWidthDp || c.screenHeightDp != builtHeightDp || c.densityDpi != builtDensityDpi) recreate();" in cc)
check("nothing else (the dark look, the keyboard) makes it again", cc.count("recreate()") == 1)
check("the watchdog counts a focus move as a frame", "addOnGlobalFocusChangeListener((o, n) -> lastDrawAt = SystemClock.uptimeMillis());" in main.split("private void watchKey()")[1][:900])
check("the date's lift is measured from the clock's row (clockRow's top plus the clock's own), never the clock's own 0: the date stays under the greeting",
      "int clockTop = (clock.getParent() instanceof View ? ((View) clock.getParent()).getTop() : 0) + clock.getTop();" in main
      and "-Math.max(0, date.getTop() - clockTop)" in main and "left.addView(clockRow," in main and "clockRow.addView(clock);" in main)
sys.exit(1 if failed else 0)
