#!/usr/bin/env python3
"""Review F93: «أذاني من ملف» copies the file into the adhan and sets the alarm off the screen's thread (the copy took up to 5 minutes of audio, the alarm asks the screensaver for the place)."""
import re, sys
from pathlib import Path
B = Path(__file__).resolve().parents[1] / "android/launcher/src/main/java/sa/urtv/home/tilawah"
act = (B / "AudioActivity.java").read_text(encoding="utf-8")
use = (B / "AudioUse.java").read_text(encoding="utf-8")
failed = 0
def check(n, ok):
    global failed
    print(("PASS " if ok else "FAIL ") + n)
    failed += not ok
copy = act[act.index("private void copy(File f)"):]
copy = copy[:copy.index('"audio-copy").start()')]
ui = copy[copy.index("runOnUiThread"):]
check("AudioUse.set is not called inside runOnUiThread", "AudioUse.set(" in copy and "AudioUse.set(" not in ui)
setf = use[use.index("static String set("):]
setf = setf[:setf.index("if (\"dua\"")]
check("AudioUse.set schedules the alarm on its own thread (scheduleSoon)", "scheduleSoon(c)" in setf and "AdhanScheduler.schedule(c)" not in setf)
sys.exit(1 if failed else 0)
