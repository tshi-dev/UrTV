#!/usr/bin/env python3
"""«توهّج لون التطبيق» (4.3.1): the glow behind the focused app is OFF by default (new installs and old ones), has three states
(مطفأ / خفيف: a third of the strength / مفعّل), and while off the home makes no glow view at all.
Needs javac for the Java half; the plumbing checks run without it.   Run: python3 tests/glow_mode_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "GlowLook.java"), str(HOME / "GlassStyle.java"),
                        str(ROOT / "tests/GlowModeRun.java")], check=True, capture_output=True)
        rows = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.GlowModeRun"], check=True, capture_output=True, text=True).stdout.split("\n")
    r = [x.split(":", 2) for x in rows if x]
    check("nothing set, empty or without the key: off, no strength (the default)", all(r[i][0] == "off" and float(r[i][1]) == 0 for i in (0, 1, 2)), r[:3])
    check("light is a third of the strength", r[3][0] == "light" and abs(float(r[3][1]) - 1 / 3) < 0.01, r[3])
    check("on is the full strength", r[4][0] == "on" and float(r[4][1]) == 1.0, r[4])
    check("off and an unknown word are off", r[5][0] == "off" and r[6][0] == "off", r[5:7])
    check("the three states differ in the repaint key (a change repaints the home at once)", len({r[2][2], r[3][2], r[4][2]}) == 3, [x[2] for x in r[2:5]])
    check("it sits beside «نص أوضح» without disturbing it", r[7][0] == "on" and r[7][2].startswith("clear|"), r[7])
else:
    print("SKIP glow mode (java half): no javac here")

prefs, rcv, st, a_act = text(SAVER / "Prefs.java"), text(SAVER / "SaverReceiver.java"), text(SAVER / "SettingsActivity.java"), text(SAVER / "AppearanceActivity.java")
main, sh, mv = text(HOME / "MainActivity.java"), text(ROOT / "optimize/saver.sh"), text(ROOT / "optimize/settings_move.py")
check("Prefs: the stored default is off, and only light/on count", 'getString("glowMode", "off")' in prefs and '"light".equals(v) ? v : "off"' in prefs)
check("Prefs: an all-default look is still the empty string only while the glow is off", '"off".equals(glowMode())) return ""' in prefs and '";glow="' in prefs)
check("saver.sh and SaverReceiver: glow off|light|on, shown by `show`", "glow) case" in sh and "off|light|on" in sh and '"glow: " + ' not in rcv and 'glow: " + p.glowMode()' in rcv)
check("settings_move: the setting is exported and imported", '"glow"' in mv and 'es("glow", v)' in mv)
check("a row in the settings and three tiles in «المظهر»", "توهّج لون التطبيق" in st and "Mock.GLOW" in a_act and '{"off", "light", "on"}' in a_act)
check("the home: no glow view is made while it is off (created only by applyGlowMode)",
      "glow = new View" in main and main.count("glow = new View") == 1 and "private void applyGlowMode" in main
      and main.index("glow = new View") > main.index("private void applyGlowMode"))
check("the home: no animation while off (fadeGlow and setGlow check the view)", "glow != null && glowAlphaTo != alpha" in main and "if (glow == null) return;" in main)
check("the home: the mode is applied from the look's repaint, without a restart", main.index("applyGlowMode();", main.index("private void applyLook")) < main.index("lightNow = light;", main.index("private void applyLook")))
check("the strength scales both alphas", "CENTRE_ALPHA * glowShare" in main and "MID_ALPHA * glowShare" in main)
sys.exit(1 if failed else 0)
