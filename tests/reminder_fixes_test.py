#!/usr/bin/env python3
"""Reminders, the 4.2 review: R04 an unanswered repeating reminder keeps its future alarms (the home's alarm plans the next one from
the repeat and zone the screensaver hands it), R07 two reminders inside one grace each get theirs and an alarm delivered twice delivers
nothing twice (delivered keys kept apart from done), R08 repeats are calendar days in the city's zone (summer time keeps the hour; a
time the clocks jump over comes after the jump, one that comes twice is the first). Needs javac.   Run: python3 tests/reminder_fixes_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def text(p): return Path(p).read_text(encoding="utf-8")

store, recv, prov, prefs = text(HOME / "ReminderStore.java"), text(HOME / "ReminderReceiver.java"), text(APP / "ConfigProvider.java"), text(APP / "Prefs.java")
check("R04: the screensaver hands the repeat and its zone with each task", '"rp", "snd", "wh", "sn", "tz"}' in prov and "x.snooze, tz}" in prov)
check("R04: the home reads them (an older screensaver without a zone: the TV's)", 'cur.getColumnIndex("rp")' in store and 'cur.getColumnIndex("tz")' in store)
check("R04: the alarm is planned after every delivery from the list itself", recv.index("deliver(c, now);") < recv.index("ReminderScheduler.schedule(c);", recv.index("deliver(c, now);")))
check("R07: the receiver delivers the fresh ones, never showing.get(0)", "showing.get(0)" not in recv and "ReminderPlan.fresh(showing, delivered)" in recv)
check("R07: delivered keys are kept on the disk, apart from done", "ReminderStore.setDelivered(" in recv and '"delivered"' in store)
check("R08: the screensaver rolls and finishes in the city's zone", "Reminders.roll(v, now, Reminders.zone(cityTz()))" in prefs and "Reminders.zone(cityTz()));" in prefs.split("TaskList.markDone(tasks()")[1][:80])
check("R08: no fixed 24 h or 7 d step left in the repeats", "d += ((now - d) / p + 1) * p" not in text(APP / "Reminders.java"))

if not shutil.which("javac"):
    print("SKIP reminder fixes: no javac here")
    sys.exit(1 if failed else 0)
def run(files, main):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + [str(f) for f in files], check=True, capture_output=True)
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Duser.timezone=UTC", "-cp", tmp, main], check=True, capture_output=True).stdout.decode("utf-8")
    return [l.split("\t", 1) for l in out.split("\n") if l]
home = run([HOME / "ReminderPlan.java", HOME / "ZoneFix.java", HOME / "ZoneTable.java", ROOT / "tests/ReminderFixHomeRun.java"], "sa.urtv.home.ReminderFixHomeRun")
saver = run([APP / "Task.java", APP / "TaskList.java", APP / "Reminders.java", APP / "ZoneFix.java", APP / "ZoneTable.java", ROOT / "tests/ReminderDstRun.java"], "sa.urtv.screensaver.ReminderDstRun")
steps = {}
for side, rows in (("home", home), ("screensaver", saver)):
    for n, r in rows:
        if n == "steps": steps[side] = r; continue
        check(f"{side}: {n}", r == "ok", r)
check("the two apps step the same (the home's plan and the screensaver's roll agree)", steps.get("home") and steps.get("home") == steps.get("screensaver"), steps)
check("every check ran", len(home) >= 29 and len(saver) >= 15, (len(home), len(saver)))
print("\n" + (f"{failed} failed" if failed else "reminder fixes: all passed"))
sys.exit(1 if failed else 0)
