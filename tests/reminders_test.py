#!/usr/bin/env python3
"""«المهام» with a time (docs/PROPOSALS-NOV2.md §4): the logic runs for real (Reminders, TaskList, ReminderKeys in the screensaver app; ReminderPlan and
ReminderEdit in the home on tests/ReminderRun.java and tests/ReminderHomeRun.java), and the wiring that needs Android is read from the source:
the alarm is the adhan's kind and is put back after a reboot, the card is silent by default, the saver.sh flags, the phone page's date and time
fields and its 64 KB budget. The page's card is tests/reminder_page.test.mjs. Needs javac for the first part.   Run: python3 tests/reminders_test.py"""
import json, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
MAN = ROOT / "android/launcher/src/main/AndroidManifest.xml"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
text = lambda p: p.read_text(encoding="utf-8")

# ---- the wiring, from the source
man = text(MAN)
rcv = man.split('android:name=".ReminderReceiver"')[1].split("</receiver>")[0]
check("the alarm's receiver comes back after a boot, a clock or zone change and an update (the adhan's actions) and takes the shell's reschedule",
      all(a in rcv for a in ("BOOT_COMPLETED", "QUICKBOOT_POWERON", "TIME_SET", "TIMEZONE_CHANGED", "MY_PACKAGE_REPLACED", "REMINDER_RESCHEDULE")))
check("only the shell and the system can send to it (DUMP)", 'android:permission="android.permission.DUMP"' in rcv)
sched = text(HOME / "ReminderScheduler.java")
check("the alarm is the adhan's kind: exact while idle when allowed, else the idle-safe inexact one, never a plain set()",
      "setExactAndAllowWhileIdle" in sched and "canScheduleExactAlarms" in sched and "setAndAllowWhileIdle" in sched and ".set(" not in sched)
check("its request code is its own (the adhan's is 47)", "REQUEST = 48" in sched and "REQUEST = 47" in text(HOME / "tilawah/AdhanScheduler.java"))
check("the exact-alarm permission is held", "android.permission.SCHEDULE_EXACT_ALARM" in man)
recv = text(HOME / "ReminderReceiver.java")
check("the alarm is put back after every card, and a boot only puts it back", "ReminderScheduler.schedule(c);" in recv and recv.index("ACTION.equals") < recv.index("ReminderStore.read"))
check("a TV in standby shows nothing and nothing is made up", "screenOn(c)" in recv and "isInteractive" in recv)
check("the card waits for the adhan", "NowPlaying.adhanLive()" in recv and "ADHAN_WAIT" in recv)
check("over another app it is a 5-second text notice: no overlay permission, no focus", "Toast" in recv and "SYSTEM_ALERT_WINDOW" not in man and "TYPE_APPLICATION_OVERLAY" not in recv + text(HOME / "ReminderCard.java"))
check("the notice's log never carries the task's words", 'Log.i("UrTVReminder", "notice shown")' in recv)
main = text(HOME / "MainActivity.java")
check("the home draws the card on the alarm and when it comes back to the front, and hands it the keys first", "ReminderReceiver.SHOW" in main and "reminder.refresh()" in main and "reminder.key(e)" in main)
check("the card is the topmost view of the home", main.index("new ReminderCard(this, root)") > main.index("grid.setClipChildren"))
check("the card is silent: a chime only for a reminder that asks, once", "row.sound && chimed.add(" in text(HOME / "ReminderCard.java") and "t.sound = false" not in text(HOME / "ReminderCard.java"))
check("a reminder is silent by default everywhere (the chime is off until asked)", "boolean sound" in text(APP / "Task.java") and 'getStringExtra("sound")' in text(APP / "SaverReceiver.java") and '"on".equals(i.getStringExtra("sound"))' in text(APP / "SaverReceiver.java"))
check("the two apps carry the same ReminderKeys", text(HOME / "ReminderKeys.java").split("\n", 1)[1] == text(APP / "ReminderKeys.java").split("\n", 1)[1])
dream = text(APP / "ScreenDream.java")
check("the screensaver gives the first press to the card and stays", "reminderKeys.decide(" in dream and "NowPlayingBridge.reminderCard()" in dream and "reminderKey(" in dream)
bridge = text(APP / "NowPlayingBridge.java")
check("the page can only mark done, snooze, say a card is up and ask for the chime", all(f in bridge for f in ("reminderDone(long id)", "reminderSnooze(long id, int minutes)", "reminderShown(boolean on)", "reminderChime()")))
check("the screen's end clears the card flag", "NowPlayingBridge.clearCard()" in dream)
prefs, prov = text(APP / "Prefs.java"), text(APP / "ConfigProvider.java")
check("a task with no reminder is saved as it always was", "if (t.due > 0) o.put(\"due\"" in prefs)
check("a repeating reminder is rolled when the list is read, under the same lock", "Reminders.needsRoll(v, now)" in prefs and "synchronized (TASKS_LOCK)" in prefs)
check("the provider gives the reminder columns after the old five, takes them on insert and update", '"at", "id", "due", "bf", "rp", "snd", "wh", "sn"' in prov and 'v.getAsLong("due")' in prov and 'getAsInteger("snooze")' in prov)
check("a change to a reminder starts a screen that is up over", "private void changed()" in prov and "Prefs.CHANGED" in prov)
check("the page gets the open reminders as #rem=", '"&rem="' in prefs and "remindersParam()" in prefs)
tasks_ui = text(HOME / "TasksActivity.java")
check("«المهام» has «ذكّرني» beside a task, and OK still finishes it", "TaskReminderActivity" in tasks_ui and "markDone(t, id)" in tasks_ui)
check("the reminder page is declared, not exported", 'android:name=".TaskReminderActivity"' in man and re.search(r'TaskReminderActivity"\s+android:exported="false"', man))
sh = text(ROOT / "optimize/saver.sh")
check("saver.sh tasks add takes --at, --before, --repeat, --sound, --where and tells the home's alarm", all(f in sh for f in ("--at)", "--before)", "--repeat)", "--sound)", "--where)", "REMINDER_RESCHEDULE")))
check("saver.sh checks the date shape and the choices before sending anything", "0|10|30|60" in sh and "no|daily|weekly" in sh and "YYYY-MM-DD HH:MM" in sh)
check("an old `tasks add 'نص'` still goes the same way", 'send TASKS "--es op add --es text $text$extra"' in sh)

# ---- the phone page: a date and a time beside the text box, under 64 KB
ph = ROOT / "android/launcher/src/main/assets/phone"
ar, en = text(ph / "index.html"), text(ph / "index.en.html")
check("the phone page has a date field and a time field, labelled", 'id="dd" type="date" aria-label=' in ar and 'id="tt" type="time" aria-label=' in ar)
check("they make the text a task with its time: the body is {text, due}", "body = { text: t, due: due }" in ar and "due > Date.now()" in ar)
check("both phone pages stay under 64 KB", len(ar.encode()) < 64 * 1024 and len(en.encode()) < 64 * 1024, (len(ar.encode()), len(en.encode())))
schema = text(HOME / "phone/Schema.java")
check("the phone's /api/text takes a due time only as a plausible number", '"text", "mode", "search", "due"' in schema and 'bad due' in schema)
pa = text(HOME / "phone/PhoneActions.java")
check("a task with a time from the phone goes to the screensaver's list whatever is on the TV, then the alarm follows", "reminder request" in pa and "ReminderScheduler.schedule(app)" in pa)

# ---- the logic
if not shutil.which("javac"):
    print("SKIP reminders: no javac here")
    sys.exit(1 if failed else 0)
def run(tmp, files, main):
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + [str(f) for f in files], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, main], check=True, capture_output=True).stdout.decode("utf-8")
    return [l.split("\t") for l in out.split("\n") if l]
with tempfile.TemporaryDirectory() as tmp:
    rows = run(tmp, [APP / "Task.java", APP / "TaskList.java", APP / "Reminders.java", APP / "ReminderKeys.java", ROOT / "tests/ReminderRun.java"], "sa.urtv.screensaver.ReminderRun")
for n, r in rows: check("reminders: " + n, r == "ok")
check("every reminder check ran", len(rows) >= 40, len(rows))
with tempfile.TemporaryDirectory() as tmp:
    rows = run(tmp, [HOME / "ReminderPlan.java", HOME / "ReminderEdit.java", ROOT / "tests/ReminderHomeRun.java"], "sa.urtv.home.ReminderHomeRun")
for n, r in rows: check("home reminders: " + n, r == "ok")
check("every home check ran", len(rows) >= 20, len(rows))
sys.exit(1 if failed else 0)
