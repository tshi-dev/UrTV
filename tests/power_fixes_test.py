#!/usr/bin/env python3
"""The two fixes of the power audit (docs/POWER.md): the photo rotation of the home screen is only armed while home is in front, and the
music service does not stay foreground after a command that started nothing. Pure logic with javac; the rest as source checks.
Run: python3 tests/power_fixes_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
L = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(L / "tilawah/IdleCommand.java"), str(ROOT / "tests/IdleCommandRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.IdleCommandRun"], check=True, capture_output=True, text=True).stdout.split()
    check("IdleCommand.leavesNothing: nothing loaded and no timer -> stop; playing, a timer, radio/adhan/stop, loading -> stay", out == ["true", "false", "false", "false", "false", "false"], str(out))
else:
    print("SKIP idle command: no javac here")

svc = (L / "tilawah/MusicService.java").read_text(encoding="utf-8")
body = svc[svc.index("public int onStartCommand"):svc.index("public IBinder onBind")]
check("onStartCommand asks IdleCommand before it returns, and radio, adhan and stop are not 'may leave nothing'",
      body.index("IdleCommand.leavesNothing") < body.index("return START_NOT_STICKY;\n    }") and
      all(a in body[body.index("mayLeaveNothing ="):body.index("IdleCommand.leavesNothing")] for a in ("ACTION_PLAY_RADIO", "ACTION_PLAY_ADHAN", "ACTION_STOP")), body[-700:])
idle = body[body.index("IdleCommand.leavesNothing"):]
idle = idle[:idle.index("return START_NOT_STICKY;")]
stop_idle = svc[svc.index("private void stopIdle()"):svc.index("void stopPlayback()")]
check("the idle branch stops without stopPlayback() or save() (they wipe the remembered surah and position)",
      "stopIdle()" in idle and "stopPlayback" not in idle and "save()" not in stop_idle and "stopPlayback" not in stop_idle
      and "foregroundStarted = false" in stop_idle and "stopForeground(true)" in stop_idle and "stopSelf()" in stop_idle, idle + stop_idle)
check("PAUSE and SLEEP 0 with nothing loaded (IdleCommand true) reach only stopIdle: prefs untouched",
      "pause(); break;" in body and "setSleep(" in body and "prefs.save" not in stop_idle)
check("the adhan path is untouched by the check (PLAY_ADHAN is excluded, its case is unchanged)", "case ACTION_PLAY_ADHAN:\n                playAdhan(" in body)

main = (L / "MainActivity.java").read_text(encoding="utf-8")
sched = main[main.index("private void scheduleWallpaper"):main.index("private void scheduleWallpaper") + 300]
check("scheduleWallpaper does nothing when home is not in front", re.search(r"removeCallbacks\(rotateWallpaper\);\s*if \(!resumed\) return;", sched), sched)
check("onResume sets resumed first, onPause clears it", re.search(r"onResume\(\) \{\s*super\.onResume\(\);\s*(?:if \(sceneHome != null\) sceneHome\.start\(\);\s*)?resumed = true;", main) and re.search(r"onPause\(\) \{\s*super\.onPause\(\);\s*(?:if \(sceneHome != null\) sceneHome\.stop\(\);[^\n]*\s*)?resumed = false;", main))
check("a slow own-photo check does not change the wallpaper after home was paused", "if (!isDestroyed() && resumed) nextWallpaper();" in main)
sys.exit(1 if failed else 0)
