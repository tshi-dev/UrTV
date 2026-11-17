#!/usr/bin/env python3
"""4.4, the tester's build 413 (Home 100 % in normal use; T9 with the owner's real remote: the add-on's Home key catch 3/3, Google TV never started):
1. the Home key catch is ON by default on a Google TV (launcherx installed), only while the person's home is ours, the window watch kept as the
   fallback; a switch in the settings («زر الرئيسية يفتح تلفزيونك مباشرة») and `dock.sh home-key on|off|status`;
2. the Home step is judged only after Android bound the add-on (up to 10 s), and the result page reads the role and the add-on again for 10 s;
3. the fight guard pauses at the 6th burst in 4 s (HomeWatchRun runs it; here the constant);
4. the intro's first focus is the reading area itself, not its first button («English»).
Read from the source. Run: python3 tests/build413_test.py"""
import subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def read(p): return (ROOT / p).read_text(encoding="utf-8")
eng = read("android/launcher/src/main/java/sa/urtv/home/setup/SetupEngine.java")
fw = eng.split("Result finishWatch(boolean watchOk) throws IOException {")[1].split("\n    }\n")[0]
check("2. the Home step is judged after the add-on is bound (up to 10 s), then the key", "if (watchOk && boundWithin(BOUND_WAIT_MS) && homeKeyOpensOurs())" in fw and "BOUND_WAIT_MS = 10_000;" in eng)
bw = eng.split("boolean boundWithin(int ms) throws IOException {")[1].split("\n    }\n")[0]
check("2. bound is Android's own answer, read once a second", "dumpsys accessibility | grep -m1 'Bound services:'" in bw and "sleeper.sleep(1000);" in bw and "if (waited >= ms) return false;" in bw)
sa = read("android/launcher/src/main/java/sa/urtv/home/setup/SetupActivity.java")
hl = sa.split("private void homeLines(")[1].split("\n    }\n")[0]
check("2. the result page reads the role and the add-on again once a second, up to 10 s, until both are fine", "homeLines(roleLine, watchLine, RESULT_RECHECK);" in sa and "RESULT_RECHECK = 10;" in sa
      and "if ((ours && bound) || left <= 0" in hl and "postDelayed(" in hl and "left - 1" in hl)
check("3. the guard window is 4 s", "static final long WINDOW_MS = 4_000," in read("android/launcher/src/main/java/sa/urtv/home/HomeWatch.java"))
box = read("android/launcher/src/main/java/sa/urtv/home/Box.java")
fs = box.split("void focusSelf() {")[1].split("\n        }\n")[0]
check("4. the reading area takes the focus itself, not its first button", "scroll.focusSelf();" in box and fs.index("FOCUS_BEFORE_DESCENDANTS") < fs.index("requestFocus()") < fs.index("FOCUS_AFTER_DESCENDANTS"))
sys.exit(1 if failed else 0)
