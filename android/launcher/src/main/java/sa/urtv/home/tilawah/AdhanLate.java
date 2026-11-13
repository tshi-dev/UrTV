package sa.urtv.home.tilawah;

/**
 * Hotfix 4.3 «الأذان الساعة 12 الليل»: an adhan alarm is judged by the prayer time it was set for (its «due»), no Android in here (tests/adhan_late_test.py runs it). An alarm that arrives
 * hours late (the TV in real standby, an overdue Isha delivered when it wakes, a clock that jumped at boot before the network time) or early must never sound: the sound is for the prayer's own
 * minute. Judged: a due time was given, it is at most MAX_LATE_MS ago, at most MAX_EARLY_MS ahead, and it is within NEAR_MS of one of the place's own prayer times (yesterday's, today's and tomorrow's,
 * so an Isha after midnight counts).
 */
final class AdhanLate {
    private AdhanLate() { }

    static final long MAX_LATE_MS = 10 * 60_000L, MAX_EARLY_MS = 60_000L, NEAR_MS = 2 * 60_000L;

    /** null = it may sound; else the reason, in the words of the log: «no time», «came N min late», «came N min early», «not near a prayer time» */
    static String skipReason(long due, long now, long[] prayerTimes) {
        if (due <= 0) return "the alarm has no prayer time";
        if (now - due > MAX_LATE_MS) return "the alarm for " + due + " came " + minutes(now - due) + " min late";
        if (due - now > MAX_EARLY_MS) return "the alarm for " + due + " came " + minutes(due - now) + " min early";
        if (prayerTimes != null) {
            boolean near = false;
            for (long t : prayerTimes) near |= t > 0 && Math.abs(t - due) <= NEAR_MS;
            if (!near) return "the alarm for " + due + " is not near a prayer time of this place";
        }
        return null;
    }

    private static long minutes(long ms) { return Math.max(1, ms / 60_000L); }
}
