#!/usr/bin/env python3
"""The owner's TV test of 159 (and 160): source checks of the TV-side fixes the browser test cannot reach. The pairing screen keeps phone
control on when the screensaver comes over it (only the person leaving, or Back, turns it off) and still keeps the screen awake; «ابدأ» in
«إعداد تلفزيوني» gets the focus after layout and the answer to the press is brought into sight; an adhan on a muted TV leaves it muted (never
at volume 0); the TV's real screensaver delay in seconds reaches the phone. Run: python3 tests/fix159_test.py"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return (ROOT / p).read_text(encoding="utf-8")


HOME = "android/launcher/src/main/java/sa/urtv/home/"
pa = text(HOME + "PhoneActivity.java")
pause = re.search(r"protected void onPause\(\) \{.*?\n    \}", pa, re.S).group(0)
check("3. pairing: leaving with nobody connected stops control only when the person left (Home, another app) or the screen finished",
      "control.stop(\"owner\")" in pause and "(left || isFinishing())" in pause, pause)
check("3. pairing: «left» comes from Android's user-leaving hint (the screensaver does not give it) and is cleared on every return",
      re.search(r"void onUserLeaveHint\(\) \{\s*super\.onUserLeaveHint\(\);\s*left = true;", pa) is not None
      and re.search(r"void onResume\(\) \{\s*super\.onResume\(\);\s*left = false;", pa) is not None)
check("3. pairing: the screen is still kept awake while control is on, and let go on pause", "keepAwake(control.isOn());" in pa and "keepAwake(false);" in pause)

su = text(HOME + "setup/SetupActivity.java")
check("setup: «ابدأ» is focused after the screen is laid out (one OK presses it)",
      "start.post(new Runnable() { @Override public void run() { start.requestFocus(); } });" in su and "\n        start.requestFocus();" not in su)
check("setup: the press's answer (the status line) is brought into sight", "status.requestRectangleOnScreen(" in su)

ms = text(HOME + "tilawah/MusicService.java")
up = re.search(r"private void adhanVolumeUp\(\) \{.*?\n    \}", ms, re.S).group(0)
back = re.search(r"private void adhanVolumeBack\(\) \{.*?\n    \}", ms, re.S).group(0)
check("2. the adhan on a muted TV: unmuted first to read the real level (Android says 0 while muted), the mute remembered on disk",
      "audio.isStreamMute(AudioManager.STREAM_MUSIC)" in up and "ADJUST_UNMUTE" in up and "prefs.setSavedTvMuted(muted);" in up
      and up.index("ADJUST_UNMUTE") < up.index("savedVolume = audio.getStreamVolume"))
check("2. …and muted again when the volume goes back (not when the person turned it during the adhan)",
      "if (AdhanVolume.muteAgain(prefs.savedTvMuted(), to)) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);" in back
      and "prefs.setSavedTvMuted(false);" in back)
unmute = [p.name for p in (ROOT / "android").rglob("*.java") if re.search(r"ADJUST_UNMUTE|ADJUST_TOGGLE_MUTE|setStreamMute", p.read_text(encoding="utf-8"))]
check("2. nothing else unmutes: only the adhan (MusicService) and the phone's own «كتم» key (PhoneActions)", sorted(unmute) == ["MusicService.java", "PhoneActions.java"], unmute)
vol = [p.name for p in (ROOT / "android").rglob("*.java") if "setStreamVolume(" in p.read_text(encoding="utf-8")]
check("2. only the adhan sets the volume", vol == ["MusicService.java"], vol)

cp = text("android/app/src/main/java/sa/urtv/screensaver/ConfigProvider.java")
act = text(HOME + "phone/PhoneActions.java")
check("7. the TV's screensaver delay goes to the phone in seconds too (45 s is not a whole minute)",
      'out.putString("idle_s", String.valueOf(Prefs.idleMs(c) / 1000));' in cp and 'm.put("saver_idle_s", b.getString("idle_s"));' in act)

print("\n" + (f"{failed} failed" if failed else "fix 159: all passed"))
raise SystemExit(1 if failed else 0)
