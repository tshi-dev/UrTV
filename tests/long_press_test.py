#!/usr/bin/env python3
"""R-503 (docs/research/LAUNCHERS-GITHUB.md idea 6): a long press that waited for a key repeat never came on remotes that send none, nor on TCL's
Android 12 while a key filter is on (it drops the repeats). LongPress on a plain JVM, then the wiring: the long OK on a reciter (★) acts on the first
repeat as before, or on the release by its hold time; the home's other long presses (Settings key, Back «hold») already decide on the release.
Needs javac for the Java cases. Run: python3 tests/long_press_test.py"""
import re
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


lp = HOME / "LongPress.java"
check("LongPress exists", lp.exists())
if lp.exists() and shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(lp), str(ROOT / "tests/LongPressRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.LongPressRun"], capture_output=True, text=True).stdout
    for line in out.splitlines():
        if line.startswith(("PASS ", "FAIL ")):
            print(line)
            failed += line.startswith("FAIL ")
    check("every LongPress case ran", "ALL PASS" in out, out[-200:])
else:
    print("SKIP the Java cases: no javac")
til = (HOME / "tilawah/MainActivity.java").read_text(encoding="utf-8")
check("the reciter's long OK: the first repeat as before, else the release by its hold time (and that release opens nothing)",
      "event.getRepeatCount() == 1) { longFav = true; toggleFavourite(); return true; }" in til
      and "LongPress.onRelease(event.getEventTime(), event.getDownTime(), false)) { toggleFavourite(); return true; }" in til
      and til.index("ACTION_UP && longFav") < til.index("LongPress.onRelease("))
rk = (HOME / "RemoteKeys.java").read_text(encoding="utf-8")
be = (HOME / "BackExit.java").read_text(encoding="utf-8")
check("the Settings key and Back «hold» keep deciding on the release by the hold time", "LONG_MS = 500" in rk and "eventTime - downTime >= HOLD_MS" in be)
print("ALL PASS" if not failed else f"{failed} FAILED")
raise SystemExit(1 if failed else 0)
