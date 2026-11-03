#!/usr/bin/env python3
"""«التلفزيون المكتوم يبقى مكتومًا» (step 27): the TV's audio service unmutes at SCREEN_ON; the home remembers the mute at SCREEN_OFF / shutdown and mutes again once
after the wake. The rule on a JVM (tests/MuteKeepRun.java), then the wiring in the source. Run: python3 tests/mute_keep_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


keep = (HOME / "MuteKeep.java").read_text(encoding="utf-8")
rule = (HOME / "MuteKeepRule.java").read_text(encoding="utf-8")
app = (HOME / "HomeApp.java").read_text(encoding="utf-8")
man = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
check("the rule has no Android in it", "import android" not in rule)
check("the mute is remembered at SCREEN_OFF and at SHUTDOWN (and the 'done' flag reset), and checked after SCREEN_ON and at the boot", "ACTION_SCREEN_OFF" in keep and "ACTION_SHUTDOWN" in keep and "ACTION_SCREEN_ON" in keep
      and 'woke(app, "screen_on")' in keep and 'woke(app, "boot")' in keep and 'putBoolean("done", false)' in keep)
check("a restore mutes with the media stream's mute (ADJUST_MUTE), once (done), and logs under UrTVMute with fixed words", "ADJUST_MUTE" in keep and 'putBoolean("done", true)' in keep and 'TAG = "UrTVMute"' in keep and "muted_again" in keep)
check("the adhan is asked (MusicService.carryAdhanActive) and the volume compared before any mute", "carryAdhanActive()" in keep and "volume(app)" in keep)
check("a newer wake cancels the checks of an older one", "if (mine != wake) return;" in keep)
check("started with the process (Application), declared in the manifest", "MuteKeep.start(this);" in app and 'android:name=".HomeApp"' in man)
check("no key is read for this (the accessibility service stays blind to everything but the Settings key)", "onKeyEvent" not in keep and "KeyEvent" not in keep)
if not shutil.which("javac"):
    print("SKIP mute keep rule: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "MuteKeepRule.java"), str(ROOT / "tests/MuteKeepRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.MuteKeepRun"], capture_output=True, text=True).stdout
print(out.strip())
check("every MuteKeepRule case ran", "ALL PASS" in out, out[-200:])
sys.exit(1 if failed else 0)
