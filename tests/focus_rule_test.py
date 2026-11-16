#!/usr/bin/env python3
"""4.3.3 G1: «تلاوة وأذان» and the radio keep playing behind the screensaver and the TV's settings until the person stops them. FocusRule (run for
real, tests/FocusRuleRun.java): a short loss of the TV's audio pauses until it comes back, «may duck» plays quieter, a loss for good pauses and is
watched for a minute and taken back when nothing else plays (never over a call); the adhan brings back a surah that another sound had paused;
a service Android refused the foreground notice gets it when our home or player comes to the front. The source checks say MusicService does
it that way. Needs javac for the run.   python3 tests/focus_rule_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

svc = (T / "MusicService.java").read_text(encoding="utf-8")
rule = (T / "FocusRule.java").read_text(encoding="utf-8")
home = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
player = (T / "PlayerActivity.java").read_text(encoding="utf-8")
recv = (T / "MusicReceiver.java").read_text(encoding="utf-8")
man = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

check("FocusRule is plain Java (no Android import)", "import android" not in rule)
check("the numbers match AudioManager's", all(f"{n} = {v};" in rule for n, v in (("GAIN", 1), ("LOSS", -1), ("LOSS_TRANSIENT", -2), ("LOSS_TRANSIENT_CAN_DUCK", -3), ("MODE_NORMAL", 0))))
check("the focus change goes through FocusRule.on", "FocusRule.on(change, ours)" in svc)
check("a loss for good starts the watch", "if (change == FocusRule.LOSS && resumeOnFocus) startFocusWatch();" in svc)
look = svc[svc.index("private void lookForFocus()"):]
look = look[:look.index("\n    }\n")]
check("the look reads the other players and the call mode", "audio.isMusicActive()" in look and "audio.getMode()" in look and "FocusRule.look(" in look)
check("taking back asks Android for the focus first", look.index("requestFocus()") < look.index("player.start()"))
check("a person's pause, stop or new play ends the watch",
      all(n in svc for n in ("endFocusWatch();\n        if (!foregroundStarted) foreground();",)) and svc.count("endFocusWatch();") >= 5)
check("the adhan counts a surah waiting for the focus", "FocusRule.onBeforeAdhan(playing, buffering && wish.wants(), resumeOnFocus && wish.wants())" in svc)
check("the screensaver is not a reason to stop: only the adhan stops at screen off",
      re.search(r"screenOff = new BroadcastReceiver\(\) \{\s*@Override public void onReceive\(Context c, Intent i\) \{\s*if \(source == Prefs.SOURCE_ADHAN\) finishAdhan\(\);\s*\}", svc) is not None)
check("the service stays a mediaPlayback foreground service with a MediaSession and a stop action",
      'android:foregroundServiceType="mediaPlayback"' in man and "FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK" in svc
      and "new MediaSession(" in svc and "command(ACTION_STOP)" in svc and "PlaybackState.ACTION_STOP" in svc)
check("a refused foreground notice is asked again when it plays", "if (!foregroundStarted) foreground();" in svc)
check("the home and the player ask it again from their onResume",
      "MusicService.keepForeground(this)" in home and "MusicService.keepForeground(this)" in player and "case ACTION_KEEP: break;" in svc)
check("music.sh status says foreground and focus_wait", "|foreground=" in recv and "|focus_wait=" in recv)

if not shutil.which("javac"):
    print("SKIP the rule's run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "FocusRule.java"), str(ROOT / "tests/FocusRuleRun.java")], capture_output=True, text=True)
        check("FocusRule compiles on its own (plain Java)", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.FocusRuleRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every focus case ran", len(out.splitlines()) == 18, len(out.splitlines()))
sys.exit(1 if failed else 0)
