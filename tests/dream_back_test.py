#!/usr/bin/env python3
"""4.4 beta 4 (Xiaomi A9): the Quran radio (or a recitation) stopped after the screensaver. The screensaver took the sound for good while it showed;
«تلاوة» watched 60 s for it to end, gave up, and nothing asked again when the screensaver ended, so it stayed paused. Now MusicService hears the
dream start and end (ACTION_DREAMING_STARTED/STOPPED), marks a sound lost while it showed, and when it ends takes the sound back
(FocusRule.afterDream: lost in the dream, still waiting for the sound, the person still wants it, at most 2 h ago). FocusRule (no Android) on
cases, then the wiring.
Run: python3 tests/dream_back_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TIL = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

ms = (TIL / "MusicService.java").read_text(encoding="utf-8")
rc = ms[ms.index("private final BroadcastReceiver dream = new BroadcastReceiver()"):]
rc = rc[:rc.index("\n    };\n")]
check("MusicService listens for the dream's start and end, and lets go of it", "registerReceiver(dream, new IntentFilter(Intent.ACTION_DREAMING_STARTED));" in ms
      and "registerReceiver(dream, new IntentFilter(Intent.ACTION_DREAMING_STOPPED));" in ms and "unregisterReceiver(dream);" in ms)
check("a sound paused by a focus loss while the dream shows is marked, with its time", "if (dreaming && resumeOnFocus) { lostInDream = true; lostInDreamAt = SystemClock.elapsedRealtime(); }" in ms)
check("the dream's end asks FocusRule.afterDream with what the person wants, and the mark is cleared either way",
      "FocusRule.afterDream(lostInDream, resumeOnFocus, wish.wants() && player != null && prepared && !playing, SystemClock.elapsedRealtime() - lostInDreamAt)" in rc
      and rc.index("lostInDream = false;") < rc.index("if (!back) return;"))
check("taken back only with the focus granted, and then it plays as after any focus gain", "if (!requestFocus())" in rc and "player.start();" in rc and "playing = true;" in rc
      and "resumeOnFocus = false;" in rc and "playbackStarted();" in rc and rc.index("if (!requestFocus())") < rc.index("player.start();"))

if not shutil.which("javac"):
    print("SKIP dream back: no javac here")
    sys.exit(1 if failed else 0)
H = 3600_000
CASES = [
    ("lost while the screensaver showed, 5 min ago, still wanted: taken back", f"1,1,1,{5 * 60_000}", "1"),
    ("lost in the dream 1 h 59 min ago: taken back", f"1,1,1,{2 * H - 60_000}", "1"),
    ("lost in the dream 3 h ago (it ran all night): not started", f"1,1,1,{3 * H}", "0"),
    ("lost before the dream (another app's sound): not here", f"0,1,1,{60_000}", "0"),
    ("the person paused or stopped it meanwhile: not wanted", f"1,1,0,{60_000}", "0"),
    ("already back (not waiting for the sound): nothing", f"1,0,1,{60_000}", "0"),
]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(TIL / "FocusRule.java"), str(ROOT / "tests/DreamBackRun.java")], capture_output=True, text=True)
    check("FocusRule compiles alone", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.DreamBackRun", *[c[1] for c in CASES]], capture_output=True, text=True).stdout.split()
        for (name, _, want), got in zip(CASES, out + [""] * len(CASES)):
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
