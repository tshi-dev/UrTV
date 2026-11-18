#!/usr/bin/env python3
"""The sleep timer after a restart (F30): its alarm is gone, so it must not be shown as running, and it never starts late.
SleepWindow.java runs for real (tests/SleepWindowRun.java); the wiring is read from the source and the manifest.
Run: python3 tests/sleep_boot_test.py"""
import shutil, subprocess, sys, tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

A = "{http://schemas.android.com/apk/res/android}"
timer = (HOME / "SleepTimer.java").read_text(encoding="utf-8")
check("the end is read through SleepWindow, with the time since boot", "SleepWindow.running(" in timer and "SystemClock.elapsedRealtime()" in timer)
check("a timer saved before a restart is forgotten at boot, and nothing is started", "static void forgetAfterBoot" in timer
      and "setExact" not in timer[timer.index("static void forgetAfterBoot"):timer.index("static int minutesLeft")])
app = ET.parse(ROOT / "android/launcher/src/main/AndroidManifest.xml").getroot().find("application")
rec = next((r for r in app.findall("receiver") if r.get(A + "name") == ".SleepBootReceiver"), None)
actions = set() if rec is None else {a.get(A + "name") for a in rec.find("intent-filter").findall("action")}
check("manifest: the boot receiver exists, is for the system/shell only and hears the boot", rec is not None
      and rec.get(A + "permission") == "android.permission.DUMP" and "android.intent.action.BOOT_COMPLETED" in actions, actions)
check("the boot receiver calls forgetAfterBoot", "SleepTimer.forgetAfterBoot(c)" in (HOME / "SleepBootReceiver.java").read_text(encoding="utf-8"))

recv = (HOME / "SleepReceiver.java").read_text(encoding="utf-8")
check("4.4 review F6: the alarm's receiver holds goAsync() and finishes it when the thread that asks the add-on ends (on every path)",
      "goAsync()" in recv and "SleepTimer.fire(c, " in recv and "pr.finish()" in recv and "finally {" in timer
      and "if (!later && end != null) end.run();" in timer and timer.count("end.run()") == 2)
if not shutil.which("javac"):
    print("SKIP sleep window: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "SleepWindow.java"), str(ROOT / "tests/SleepWindowRun.java")], check=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SleepWindowRun"], check=True, capture_output=True).stdout.decode()
rows = [l.split("\t") for l in out.split("\n") if l]
for name, res in rows:
    check("sleep window: " + name, res == "ok")
check("every check ran", len(rows) == 5, len(rows))
sys.exit(1 if failed else 0)
