package sa.urtv.home.tilawah;

/** For tests/adhan_late_test.py: AdhanLate's verdicts. One line per check: name TAB ok|FAIL. */
public final class AdhanLateRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    static final long H = 3_600_000L, M = 60_000L;

    public static void main(String[] a) {
        long day = 1_800_000_000_000L;   // a day's midnight in some zone; the five times of three days
        long[] times = new long[15];
        long[] clock = {5 * H + 20 * M, 12 * H + 5 * M, 15 * H + 30 * M, 18 * H + 10 * M, 19 * H + 40 * M};
        for (int d = 0; d < 3; d++) for (int i = 0; i < 5; i++) times[d * 5 + i] = day + (d - 1) * 24 * H + clock[i];
        long dhuhr = day + clock[1];
        check("on time: it sounds (no skip reason)", AdhanLate.skipReason(dhuhr, dhuhr + 5_000, times) == null);
        check("within 10 minutes late: it sounds", AdhanLate.skipReason(dhuhr, dhuhr + 9 * M, times) == null);
        check("3 hours late (the overdue Isha delivered when the TV wakes): skipped, the log line says how late", AdhanLate.skipReason(dhuhr, dhuhr + 3 * H, times).equals("the alarm for " + dhuhr + " came 180 min late"));
        check("more than 10 minutes late: skipped", AdhanLate.skipReason(dhuhr, dhuhr + 11 * M, times) != null);
        check("2 minutes early (a clock that jumped at boot): skipped", AdhanLate.skipReason(dhuhr, dhuhr - 2 * M, times).contains("min early"));
        check("under a minute early: it sounds", AdhanLate.skipReason(dhuhr, dhuhr - 30_000, times) == null);
        check("an alarm with no prayer time (an older version's): skipped", AdhanLate.skipReason(0, dhuhr, times) != null && AdhanLate.skipReason(-5, dhuhr, times) != null);
        long other = dhuhr + 2 * H;   // another date's / not a prayer's moment: 2 h after Dhuhr
        check("a due time that is not near a prayer of this place (another date, a wrong zone): skipped even when on time", AdhanLate.skipReason(other, other, times).contains("not near a prayer time"));
        long nearOff = dhuhr + 90_000;
        check("within 2 minutes of a prayer's time: it sounds (Aladhan's minute against ours)", AdhanLate.skipReason(nearOff, nearOff, times) == null && AdhanLate.skipReason(dhuhr + 3 * M, dhuhr + 3 * M, times) != null);
        long ishaAfterMidnight = day + 24 * H + 15 * M;   // an Isha past midnight belongs to the day before: yesterday's list has it
        long[] hi = times.clone();
        hi[4] = ishaAfterMidnight;   // yesterday's Isha, at 00:15 today
        check("an Isha after midnight (high latitudes, Ramadan) is a real time: it sounds at 00:15", AdhanLate.skipReason(ishaAfterMidnight, ishaAfterMidnight + 20_000, hi) == null);
        check("and a stale midnight alarm with no such prayer: skipped", AdhanLate.skipReason(day + 24 * H, day + 24 * H, times) != null);
        check("no list of times (not known): only the lateness rules apply", AdhanLate.skipReason(dhuhr, dhuhr, null) == null && AdhanLate.skipReason(dhuhr, dhuhr + H, null) != null);
        check("the zone's offset is in minutes (the identity carries it)", SchedIdentity.offsetMinutes(3 * H) == 180 && SchedIdentity.offsetMinutes(-5 * H - 30 * M) == -330 && SchedIdentity.offsetMinutes(0) == 0);
        System.exit(0);
    }
}
