package sa.urtv.home.tilawah;

/**
 * 4.4 «ساعات الهدوء»: a night window (for example 23:00 to 05:00) in which no dua or reminder plays; the adhan in it is the person's choice: heard
 * (the default), Fajr only, or silent (its time, card, bar and notice stay). No Android in it (tests/QuietHoursRun.java runs it).
 */
final class QuietHours {
    private QuietHours() { }

    static final int ADHAN_HEARD = 0, ADHAN_FAJR_ONLY = 1, ADHAN_SILENT = 2;

    /** minute of the day `now` (0..1439) is inside from..to; a window that passes midnight wraps; from == to is no window */
    static boolean inside(int now, int from, int to) {
        if (from == to) return false;
        return from < to ? now >= from && now < to : now >= from || now < to;
    }

    /** the adhan of this prayer is silent because of the quiet hours */
    static boolean silencesAdhan(boolean on, boolean inside, int policy, int prayer) {
        if (!on || !inside) return false;
        if (policy == ADHAN_SILENT) return true;
        return policy == ADHAN_FAJR_ONLY && prayer != PrayerSound.FAJR;
    }
}
