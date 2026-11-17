#!/usr/bin/env python3
"""4.4 the owner: «the same easing everywhere». Every move of our screens takes Motion.MS (200 ms) on couch-tv's cubic-bezier(.2,.8,.2,1); with
«الوضع الخفيف» a move is instant and only a fade runs. A photo's or scene's cross-fade, the home's first arrival, a game and the kick-off notice keep
their own time (content, not a move of the screen).   python3 tests/motion_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
for d in (HOME, APP):
    m = read(d / "Motion.java")
    check(f"{d.name}: Motion is 200 ms on cubic-bezier(.2,.8,.2,1)", "static final long MS = 200;" in m and "new PathInterpolator(0.2f, 0.8f, 0.2f, 1f)" in m)
check("«الوضع الخفيف»: a move is instant (a fade instead): the home by its tier, the settings by the setting (on, or auto on a weak TV)",
      "static long move(Context c) { return Ram.low(c) ? 0 : MS; }" in read(HOME / "Motion.java")
      and 'return "on".equals(p.lite()) || ("auto".equals(p.lite()) && Prefs.lowRam(c));' in read(APP / "Motion.java"))
ui = {HOME / "ControlCenter.java": None, HOME / "PillBar.java": None, HOME / "Pill.java": None, APP / "SettingsActivity.java": None, APP / "CouchStyle.java": None}
left = {p.name: len(re.findall(r"new (?:android\.view\.animation\.)?(?:Decelerate|Accelerate|AccelerateDecelerate|Linear)Interpolator\(", read(p))) for p in ui}
check("no other curve in the moves of the control centre, the top row's pills and the settings", not any(left.values()), left)
main = read(HOME / "MainActivity.java")
others = re.findall(r"new (?:android\.view\.animation\.)?(?:Decelerate|Accelerate|AccelerateDecelerate)Interpolator\([^)]*\)", main)
check("the home: one other curve only, its first arrival (a content sequence, staggered), every focus, label, glow and tray move on Motion", len(others) == 1
      and main.count("Motion.ease()") >= 14 and "setDuration(has ? 320 : 200)" not in main and "setDuration(160)" not in main, (len(others), main.count("Motion.ease()")))
check("the control centre comes in on Motion, and with «الوضع الخفيف» it fades where it stands instead of sliding",
      "final boolean fade = wideNow || Ram.low(anchor.getContext());" in read(HOME / "ControlCenter.java") and "SLIDE_MS = (int) Motion.MS, FOCUS_MS = (int) Motion.MS;" in read(HOME / "ControlCenter.java"))
check("the pills and the top row open on Motion", "private static final long MS = Motion.MS;" in read(HOME / "Pill.java") and "OPEN_MS = Motion.MS" in read(HOME / "PillBar.java")
      and "anim.setInterpolator(Motion.ease());" in read(HOME / "PillBar.java"))
check("both settings styles' focus on Motion (couch-tv's style already was its curve), none with «الوضع الخفيف»",
      "static final long FOCUS_MS = Motion.MS;" in read(APP / "SettingsActivity.java") and "setDuration(Motion.lite(v.getContext()) ? 0 : MS)" in read(APP / "CouchStyle.java"))
sys.exit(1 if failed else 0)
