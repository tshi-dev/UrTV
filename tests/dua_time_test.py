#!/usr/bin/env python3
"""4.4 the adhan's previews and the duas' times, from an audit:
  - a dua «بعد العشاء بساعتين» that falls after midnight played never (its alarm fired, the receiver looked from today and found tomorrow's);
    now the days start at yesterday, and it belongs to its prayer's day;
  - a clock time on a DST day was an hour off (midnight + 10 h); now the city's own clock reads it (Day.wall, DuaDays in the place's zone);
  - an alarm and its dua's moment are the same within three minutes (the prayer times may be corrected after the alarm was set: Aladhan's, a
    city, «تعديل وقت الصلاة»), and the duas are set again with the adhan whenever the times change;
  - the duas' Hijri date moves with «فرق التاريخ الهجري», as the home's;
  - a second preview of another muezzin replaces the first (it kept playing the first); a prayer's row plays its adhan whole as a preview at that
    prayer's level (a real adhan meanwhile takes its place: it used to be swallowed); the control centre previews that prayer's own muezzin.
DuaRule runs for real with made-up days (a London summer, a Cairo DST day).
    python3 tests/dua_time_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
T = H / "tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")
days, rec, svc, prev, tab = read(T / "DuaDays.java"), read(T / "DuaReceiver.java"), read(T / "MusicService.java"), read(T / "AdhanPreview.java"), read(T / "MainActivity.java")
check("the days start at yesterday", "if (k < -1 || k > DuaRule.HORIZON) return null;" in days and "for (int k = -1; k <= HORIZON; k++)" in read(T / "DuaRule.java"))
check("a clock time is the city's clock that day", "w.set(Calendar.HOUR_OF_DAY, minutes / 60);" in days and "d.wall != null ? d.wall.applyAsLong(clock)" in read(T / "DuaRule.java"))
check("the Hijri date moves with «فرق التاريخ الهجري»", "HijriShift.fromFragment(" in days and "HijriShift.date(noonShifted, hijriShift)" in days)
check("the receiver: the same within three minutes", "d.rule.next(due - DuaPlan.SAME_MS - 1, days)" in rec and "if (!DuaPlan.same(t, due)) continue;" in rec)
check("the duas are set again when the times change (Aladhan's, a new place)", "DuaScheduler.schedule(c);   // 4.4: the duas' too" in read(T / "PrayerTimes.java") and read(T / "AdhanSync.java").count("DuaScheduler.schedule(c);") == 2)
check("a preview of another muezzin replaces the first; the same one only gets its time again", "if (adhanPreview && previewSeconds > 0 && wanted == adhanVoice) {" in svc and "if (duaNow == null && !adhanPreview) return;" in svc)
check("a prayer's row: the adhan whole, as a preview, at that prayer's level", "MusicService.adhan(this, voice, Adhan.name(voice), WHOLE_PREVIEW_S, row.index);" in tab and "WHOLE_PREVIEW_S = 600" in tab)
check("the control centre: that prayer's own muezzin and level", "public static void play(Context c, int prayer)" in prev and "p.playFor(c, prayer)" in prev and "AdhanPreview.prayerOf(next)" in read(H / "ControlCenter.java"))

if shutil.which("javac"):
    run = r"""package sa.urtv.home.tilawah;
import java.util.*;
public class DtRun { static void out(String n, boolean ok) { System.out.println(n + "\t" + (ok ? "ok" : "bad")); }
static DuaRule.Days london(final long day0, final int first) {   // made-up days: Isha at 23:00, midnight every 24 h from day0; day(-1) is known from `first`
  return k -> {
    if (k < first || k > DuaRule.HORIZON) return null;
    long m = day0 + k * 86_400_000L;
    long[] p = {m + 3 * 3600_000L, 0, m + 13 * 3600_000L, m + 17 * 3600_000L, m + 21 * 3600_000L, m + 23 * 3600_000L};
    return new DuaRule.Day(((k % 7) + 7) % 7, 2026, 6, 10 + k, 1447, 12, 10 + k, m, p);
  };
}
public static void main(String[] a) {
  long day0 = 1_781_049_600_000L;   // a made-up midnight
  DuaRule r = new DuaRule(); r.at = DuaRule.AFTER; r.prayer = 5; r.offset = 120;   // «كل يوم، بعد العشاء بساعتين»: 01:00 after midnight
  long due = day0 + 3600_000L;   // today 01:00, from yesterday's Isha
  out("after midnight: the alarm's own moment is found from yesterday's Isha", r.next(due - 1, london(day0, -1)) == due);
  out("without yesterday (an old caller): the next is tomorrow's, as before", r.next(due - 1, london(day0, 0)) == due + 86_400_000L);
  DuaRule w = new DuaRule(); w.day = DuaRule.WEEKDAYS; w.weekdays = 1 << 4; w.at = DuaRule.AFTER; w.prayer = 5; w.offset = 120;   // Thursday's, after Isha
  out("it belongs to its prayer's day: Thursday's falls on Friday 01:00", w.next(day0 + 3 * 86_400_000L, london(day0, -1)) == day0 + 5 * 86_400_000L + 3600_000L);
  // a DST day: midnight is 23 h before the next one; the wall clock says 10:00 at midnight + 9 h
  final long m0 = day0;
  DuaRule.Days dst = k -> k != 0 ? null : new DuaRule.Day(5, 2026, 4, 24, 1447, 11, 7, m0, new long[6], minutes -> m0 + (minutes >= 60 ? (minutes - 60) * 60_000L : minutes * 60_000L));
  DuaRule c = new DuaRule(); c.at = DuaRule.AT_CLOCK; c.clock = 600;
  out("a clock time on a DST day is the wall clock's (10:00, not midnight + 10 h)", c.next(m0 - 1, dst) == m0 + 9 * 3600_000L);
  DuaRule.Days plain = k -> k != 0 ? null : new DuaRule.Day(5, 2026, 4, 24, 1447, 11, 7, m0, new long[6]);
  out("a day with no wall clock (the tests' days): midnight + minutes, as before", c.next(m0 - 1, plain) == m0 + 10 * 3600_000L);
  out("an alarm and a moment within three minutes are the same", DuaPlan.same(due, due + 2 * 60_000L) && DuaPlan.same(due, due - 3 * 60_000L) && !DuaPlan.same(due, due + 4 * 60_000L) && !DuaPlan.same(0, due));
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "DtRun.java").write_text(run)
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "DuaRule.java"), str(T / "DuaPlan.java"), str(Path(tmp, "DtRun.java"))], capture_output=True, text=True)
        check("DuaRule and DuaPlan compile on their own", r.returncode == 0, r.stderr[-800:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.DtRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 6, out)
sys.exit(1 if failed else 0)
