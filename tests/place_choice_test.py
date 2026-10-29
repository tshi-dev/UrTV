#!/usr/bin/env python3
"""The adhan alarm's place when the screensaver cannot be read (the owner's TCL kills its process and refuses to start it from the background
while another app starts: «callee doesn't have OP_ASSOC_START_permission», «Failed to find provider info»). A failed read must keep the last good
place, so the alarm is never set for the bundled one (Riyadh); only an install that never read anything uses the bundled place. The pure choice
(PlaceChoice/PlaceFragment on a plain JVM), then source checks of the wiring.
Needs javac for the Java cases. Run: python3 tests/place_choice_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return (ROOT / p).read_text(encoding="utf-8")


T = "android/launcher/src/main/java/sa/urtv/home/tilawah/"
H = "android/launcher/src/main/java/sa/urtv/home/"
if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    srcs = [str(ROOT / T / n) for n in ("PlaceChoice.java", "PlaceFragment.java", "PrayerMath.java", "PrayerAdjust.java")] + [str(ROOT / "tests/PlaceChoiceRun.java")]
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + srcs, check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.PlaceChoiceRun"], capture_output=True, text=True).stdout
    for line in out.splitlines():
        if line.startswith(("PASS ", "FAIL ")):
            print(line)
            failed += line.startswith("FAIL ")
    check("every PlaceChoice case ran", "ALL PASS" in out, out[-200:])
else:
    print("SKIP the Java cases: no javac")

place = text(T + "Place.java")
check("a failed read (null cursor, exception, null fragment) goes to the last good place before the bundled one",
      "PlaceChoice.pick(fragment, remembered(c))" in place and "if (pick.fragment == null) return bundled(c);" in place and "return get(c, fresh);" in place)
check("the last good fragment is kept in memory and in this app's own preferences, written only when it changed",
      'getSharedPreferences("place", Context.MODE_PRIVATE)' in place and "if (fragment.equals(lastGood)) return;" in place and "if (!pick.remembered) remember(c, pick.fragment);" in place)
check("a read that failed and used the last good place says so once per streak (tag UrTVAdhan, fixed words, no place)",
      'Log.i("UrTVAdhan", "place read failed: the last good place is used")' in place and "if (usedRemembered && !failing)" in place and "UrTVAdhan" not in text(T + "AdhanSync.java"))
sync = text(T + "AdhanSync.java")
on = sync[sync.index("public static void onConfig"):sync.index("/** The shell asks")]
check("a failed read (null fragment) decides nothing about the alarm: the identity is not touched and nothing is rescheduled",
      "if (fragment == null) return;" in on and on.index("if (fragment == null) return;") < on.index("identity(p)") and on.index("if (fragment == null) return;") < on.index("AdhanScheduler.schedule"))
check("the alarm is set from the very place its identity was made from (onConfig and the shell's reschedule)",
      "AdhanScheduler.schedule(c, p);" in on and "Place p = Place.get(c);\n        sp.edit().putString(\"sched_identity\", identity(p)).apply();\n        AdhanScheduler.schedule(c, p);" in sync)
sch = text(T + "AdhanScheduler.java")
check("the scheduler takes the place the caller judged by (nextAlarm for that place); the one-argument form reads it once",
      "static void schedule(Context c) { schedule(c, Place.get(c)); }" in sch and "PrayerTimes.nextAlarm(c, place)" in sch and "static Next nextAlarm(Context c, Place place)" in text(T + "PrayerTimes.java"))
snap = text(H + "ConfigSnapshot.java")
check("the home's settings read keeps the last good values when a read fails (not the defaults), and asks again at the next call",
      "return last != null ? last : s;" in snap and "if (s.fragment != null) { last = s; lastAt = now; return s; }" in snap)
fa = text(T + "AdhanReceiver.java")
check("the alarm receiver still sets the next alarm through the scheduler (so the place it uses is the remembered one when the read fails)", fa.count("AdhanScheduler.schedule(context)") == 2)
frag = text(T + "PlaceFragment.java")
check("the fragment parser and the choice are pure (no Android in them)", "android." not in frag and "android." not in text(T + "PlaceChoice.java"))
print("ALL PASS" if not failed else f"{failed} FAILED")
sys.exit(1 if failed else 0)
