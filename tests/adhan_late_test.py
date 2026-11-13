#!/usr/bin/env python3
"""Hotfix 4.3 «الأذان الساعة 12 الليل»: an adhan alarm that arrives late, early, with no prayer time or not near one of the place's prayer times never sounds; it is skipped and logged under UrTVAdhan
(the alarm for <due> came <n> min late), with the adhan's start logged too (prayer, due, lateness, reason). The zone's offset is in the alarm's identity, so a later zone report sets the alarm again.
AdhanLate runs on a plain JVM; the wiring is read from AdhanReceiver.
    python3 tests/adhan_late_test.py"""
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
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


rc, pt, place = (T / "AdhanReceiver.java").read_text(encoding="utf-8"), (T / "PrayerTimes.java").read_text(encoding="utf-8"), (T / "Place.java").read_text(encoding="utf-8")
body = rc.split("public void onReceive")[1]
check("the receiver judges the alarm before it plays or notices: AdhanLate.skipReason(due, now, the times around today), logged, the note kept", "AdhanLate.skipReason(due, now, PrayerTimes.aroundToday(context))" in rc
      and 'Log.i("UrTVAdhan", "adhan skipped: " + late)' in rc and 'prefs.setLastAlarmNote("skipped: " + late, now)' in rc
      and body.index("AdhanLate.skipReason") < body.index("MusicService.adhan(") < body.index("AdhanNotice.show(") < body.index("AdhanSaver.start("))
check("a skipped alarm still sets the next one and refreshes (schedule and refresh stay after it)", body.rindex("AdhanScheduler.schedule(context);") > body.index("adhan skipped") and body.rindex("PrayerTimes.refresh(context, null);") > body.index("adhan skipped"))
check("every adhan start is logged under UrTVAdhan with its prayer, its due time, its lateness and the reason", 'Log.i("UrTVAdhan", "adhan start: prayer="' in rc and '" due=" + due' in rc and 'reason=alarm"' in rc)
check("the times around today are yesterday's, today's and tomorrow's (an Isha after midnight counts), the saved Aladhan times applied as next() reads them", "static long[] aroundToday(Context c)" in pt and "for (int d = -1; d <= 1; d++)" in pt and "t.apply(saved)" in pt.split("static long[] aroundToday")[1].split("}\n\n")[0])
check("the alarm's identity carries the zone's offset (a later zone report, ZoneFix, sets the alarm again)", '"|z" + SchedIdentity.offsetMinutes(sa.urtv.home.ZoneFix.zone(tz).getOffset(System.currentTimeMillis()))' in place)
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        srcs = [T / "AdhanLate.java", T / "SchedIdentity.java", ROOT / "tests/AdhanLateRun.java"]
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + [str(s) for s in srcs], capture_output=True, text=True)
        check("AdhanLate compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.AdhanLateRun"], capture_output=True, text=True, timeout=60).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            for n, v in lines:
                check(n, v == "ok")
            check("every case ran", len(lines) == 13, out)
else:
    check("java is installed", False)
sys.exit(1 if failed else 0)
