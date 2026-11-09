#!/usr/bin/env python3
"""The recitation and the adhan at a normal level: Loudness.java (no Android) says how much the player lifts them, MusicService puts a
LoudnessEnhancer (a gain with a limiter) on its own audio session. Checked: the gain is for the surah, the radio and the adhan only, never
above +9 dB; it is made for every player and released with it; a TV without the effect plays as before; and it never touches the TV's
volume or mute: the adhan's own level is still the person's choice and never above it, and nothing new unmutes.
Needs javac for the Java part; without it that part is skipped with a note.
Run: python3 tests/loudness_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

loud = (T / "Loudness.java").read_text(encoding="utf-8")
service = (T / "MusicService.java").read_text(encoding="utf-8")
prefs = (T / "Prefs.java").read_text(encoding="utf-8")

check("Loudness is pure: no android import", "import android" not in loud)
start = service.split("private void startUrl(")[1].split("\n    }\n")[0]
check("every player gets the lift, on its own session, for what it plays",
      service.count("Playback.create(") == 1 and "louder = lift(player.getAudioSessionId(), Loudness.targetMb(source));" in start   # 4.3 A2: MediaPlayer or ExoPlayer (Playback)
      and start.index("Playback.create(") < start.index("lift(") < start.index("prepareAsync") if "prepareAsync" in start else False)
rel = service.split("private void releasePlayer(")[1].split("\n    }\n")[0]
check("the effect is released with the player (startUrl releases the old one first)", "louder.release()" in rel and "louder = null;" in rel
      and start.index("releasePlayer();") < start.index("lift("))
lift = service.split("private static LoudnessEnhancer lift(")[1].split("\n    }\n")[0]
check("a TV without the effect plays as before: any failure gives null, nothing thrown", "catch (RuntimeException | UnsatisfiedLinkError" in lift and "return null;" in lift)
check("the lift is setTargetGain on our session only: no volume, no mute, no global session 0",
      "setTargetGain(mb)" in lift and not re.search(r"setStreamVolume|adjustStreamVolume|ADJUST_|setMasterVolume|LoudnessEnhancer\(0\)", re.sub(r"/\*.*?\*/|//[^\n]*", "", lift + loud, flags=re.S)))
up = service.split("private void adhanVolumeUp(")[1].split("\n    }\n")[0]
check("the adhan's own level is still the person's choice, never above it", "int want = Math.round(max * prefs.adhanVolume() / 100f);" in up
      and "Math.max(1, Math.min(max, want))" in up)
check("ducking and the fades still work through setVolume (the limiter is behind them)", "player.setVolume(0.2f, 0.2f)" in service and "AdhanVoices.gain(" in service)
check("tests/run.sh runs this test", "tests/loudness_test.py" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP loudness: no javac here")
    sys.exit(1 if failed else 0)
consts = re.search(r"static final int SOURCE_NONE = 0[^;]*;", prefs).group(0)
with tempfile.TemporaryDirectory() as tmp:
    d = Path(tmp)
    (d / "Prefs.java").write_text("package sa.urtv.home.tilawah; final class Prefs { " + consts + " }")
    (d / "LoudnessRun.java").write_text(
        "package sa.urtv.home.tilawah; public final class LoudnessRun { public static void main(String[] a) {"
        " System.out.println(Loudness.targetMb(Prefs.SOURCE_SURAH) + \" \" + Loudness.targetMb(Prefs.SOURCE_RADIO) + \" \" + Loudness.targetMb(Prefs.SOURCE_ADHAN)"
        " + \" \" + Loudness.targetMb(Prefs.SOURCE_NONE) + \" \" + Loudness.targetMb(99) + \" \" + Loudness.GAIN_MB + \" \" + Loudness.MAX_MB); } }")
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "Loudness.java"), str(d / "Prefs.java"), str(d / "LoudnessRun.java")], check=True, capture_output=True)
    got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.LoudnessRun"], check=True, capture_output=True, text=True).stdout.split()
surah, radio, adhan, none, other, gain, cap = map(int, got)
check("the surah, the radio and the adhan are lifted by the same gain", surah == radio == adhan == gain > 0, got)
check("nothing else is lifted", none == 0 and other == 0, got)
check("the gain is a real lift and never more than +9 dB", 300 <= gain <= cap <= 900, got)

print("\n" + (f"{failed} failed" if failed else "loudness: all passed"))
sys.exit(1 if failed else 0)
