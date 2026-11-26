#!/usr/bin/env python3
"""4.4 beta 4: a home with «الآفاق» on never stays black. When no scene decodes (every one in a row, or the only one), SceneHome logs it
and tells the home (Backdrop.blank), which shows its wall photos; the first scene that draws hands the backdrop back (Backdrop.drawn).
Run: python3 tests/scene_blank_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
scene, main = (H / "SceneHome.java").read_text(encoding="utf-8"), (H / "MainActivity.java").read_text(encoding="utf-8")
miss = scene.split("if (photo == null || (withClock && glass == null)) {")[1].split("failRun = 0;")[0]
check("a scene that cannot be decoded is logged (its pack, id, tier) and counted", 'Log.w("SceneHome"' in miss and "failRun++;" in miss and "sc.id" in miss)
check("the home is told once when none draws: every scene failed in a row, or the only one", "failRun >= Math.max(1, scenes.size()) && !blankSaid && backdrop != null" in miss and "backdrop.blank();" in miss)
check("a scene that draws afterwards hands the backdrop back (once)", "if (blankSaid && backdrop != null) { blankSaid = false; backdrop.drawn(); }" in scene)
check("it still goes on to the next scene (never stops trying)", "index = at;" in miss and "schedule();" in miss)
b = main.split("sceneHome.backdrop(new SceneHome.Backdrop() {")[1].split("});")[0]
check("blank: the wall photos are shown again and the next one is made (scenesOn off so nextWallpaper runs)", "sceneBlank = true; scenesOn = false;" in b and "wallBack.setVisibility(View.VISIBLE)" in b and "nextWallpaper();" in b)
check("drawn: the photos go again, the layers emptied", "sceneBlank = false; scenesOn = true;" in b and "setImageDrawable(null)" in b and "INVISIBLE" in b)
check("the minute tick leaves the photos alone while blank (no hide, no flip of scenesOn)", "int show = on && !sceneBlank ? View.INVISIBLE : View.VISIBLE;" in main and "if (on != scenesOn && !sceneBlank) {" in main)
sys.exit(1 if failed else 0)
