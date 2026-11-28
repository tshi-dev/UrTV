#!/usr/bin/env python3
"""R-501 (docs/research/LAUNCHERS-GITHUB.md idea 4): a TV's startup manager (Skyworth, Vidda) turns our own boot receivers off, so the adhan, the
reminders and the boot heals never run after a reboot. The home puts back every receiver of its own that the manifest enables and that is now off:
at its first show in a process, then 5 s and 15 s later. ReceiverState on a plain JVM, then the wiring in the source.
Needs javac for the Java cases. Run: python3 tests/receiver_heal_test.py"""
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


state = HOME / "ReceiverState.java"
check("ReceiverState exists", state.exists())
if state.exists() and shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(state), str(ROOT / "tests/ReceiverHealRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.ReceiverHealRun"], capture_output=True, text=True).stdout
    for line in out.splitlines():
        if line.startswith(("PASS ", "FAIL ")):
            print(line)
            failed += line.startswith("FAIL ")
    check("every ReceiverState case ran", "ALL PASS" in out, out[-200:])
else:
    print("SKIP the Java cases: no javac")
if state.exists():
    st = state.read_text(encoding="utf-8")
    check("the states are Android's (COMPONENT_ENABLED_STATE_*: 0 default, 1 on, 2 off, 3 off by the user, 4 off until used)",
          "DEFAULT = 0, ENABLED = 1, DISABLED = 2, DISABLED_USER = 3, DISABLED_UNTIL_USED = 4;" in st)
heal = (HOME / "ReceiverHeal.java").read_text(encoding="utf-8") if (HOME / "ReceiverHeal.java").exists() else ""
check("only our own package's receivers are read (another app's never)", "pm.getPackageInfo(c.getPackageName(), PackageManager.GET_RECEIVERS | PackageManager.MATCH_DISABLED_COMPONENTS)" in heal)
check("the one write: back to the manifest's state, without killing the app", heal.count("setComponentEnabledSetting(") == 1
      and "COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP" in heal and "ReceiverState.heal(r.enabled," in heal)
check("at the first show in a process, then 5 s and 15 s later, off the UI thread", "AT_MS = {0, 5_000, 15_000}" in heal and "startedThisProcess" in heal
      and "new Thread(" in heal)
check("a fixed word and a count in the log", 'TAG = "UrTVReceivers"' in heal and re.search(r'Log\.\w\(TAG, "[^"]*" \+ n\)', heal) is not None)
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
check("the home starts it when it shows", "ReceiverHeal.onStart(this);" in main)
print("ALL PASS" if not failed else f"{failed} FAILED")
raise SystemExit(1 if failed else 0)
