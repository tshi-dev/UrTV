package sa.urtv.home.tilawah;

/**
 * 4.4 «الأدعية والتذكيرات»: when one plays, with no Android in it (tests/DuaRuleRun.java runs it on made-up days). A rule is a day and a time.
 *   the day: every day; days of the week (one or several); a Gregorian date; a Hijri date (Umm al-Qura). «يتكرر» on: every such day (a date:
 *            every year on it); off: the next one only, then the reminder turns itself off.
 *   the time: a clock time; or before or after a prayer by some minutes («بعد العصر بـ ١٠ دقائق», «قبل المغرب»), from that day's own times,
 *            so it follows the prayer through the year.
 * The caller gives the days (Days): their weekday, both dates and the prayer times in the city's zone.
 */
final class DuaRule {
    static final int EVERY_DAY = 0, WEEKDAYS = 1, GREGORIAN = 2, HIJRI = 3;
    static final int AT_CLOCK = 0, BEFORE = 1, AFTER = 2;
    /** how far ahead a next time is looked for: a yearly Hijri date comes round in at most 355 days */
    static final int HORIZON = 400;
    /** before or after a prayer by at most three hours */
    static final int MAX_OFFSET = 180;

    int day = EVERY_DAY;
    /** WEEKDAYS: bit 0 Sunday … bit 6 Saturday */
    int weekdays;
    /** GREGORIAN or HIJRI: the year (only for one that does not repeat), the month 1-12, the day 1-31 */
    int year, month, date;
    int at = AT_CLOCK;
    /** AT_CLOCK: minutes from midnight */
    int clock;
    /** BEFORE/AFTER: the prayer (PrayerTimes' index: 0 Fajr, 1 sunrise, 2 Dhuhr, 3 Asr, 4 Maghrib, 5 Isha) and by how many minutes */
    int prayer, offset;
    boolean repeat = true;

    /** One day as the city sees it. */
    static final class Day {
        final int weekday;   // 0 Sunday … 6 Saturday
        final int gy, gm, gd, hy, hm, hd;
        final long midnight;   // this day's 00:00 in the city's zone
        final long[] prayers;   // the six times of that day (PrayerTimes.at), 0 when not known
        Day(int weekday, int gy, int gm, int gd, int hy, int hm, int hd, long midnight, long[] prayers) {
            this.weekday = weekday; this.gy = gy; this.gm = gm; this.gd = gd; this.hy = hy; this.hm = hm; this.hd = hd; this.midnight = midnight; this.prayers = prayers;
        }
    }

    /** today is 0, tomorrow 1, … */
    interface Days { Day day(int fromToday); }

    /** the rule is whole: a weekday picked, a real date, a time in range */
    boolean valid() {
        if (day < EVERY_DAY || day > HIJRI || at < AT_CLOCK || at > AFTER) return false;
        if (day == WEEKDAYS && (weekdays & 0x7F) == 0) return false;
        if ((day == GREGORIAN || day == HIJRI) && (month < 1 || month > 12 || date < 1 || date > (day == HIJRI ? 30 : 31))) return false;
        if ((day == GREGORIAN || day == HIJRI) && !repeat && year < 1) return false;
        if (at == AT_CLOCK && (clock < 0 || clock >= 24 * 60)) return false;
        if (at != AT_CLOCK && (prayer < 0 || prayer > 5 || offset < 0 || offset > MAX_OFFSET)) return false;
        return true;
    }

    boolean on(Day d) {
        switch (day) {
            case EVERY_DAY: return true;
            case WEEKDAYS: return (weekdays & (1 << d.weekday)) != 0;
            case GREGORIAN: return d.gm == month && d.gd == date && (repeat || d.gy == year);
            case HIJRI: return d.hm == month && d.hd == date && (repeat || d.hy == year);
            default: return false;
        }
    }

    /** the moment on that day, 0 when that day's prayer time is not known */
    long time(Day d) {
        if (at == AT_CLOCK) return d.midnight + clock * 60_000L;
        long p = d.prayers == null || prayer >= d.prayers.length ? 0 : d.prayers[prayer];
        if (p <= 0) return 0;
        return at == BEFORE ? p - offset * 60_000L : p + offset * 60_000L;
    }

    /** The next moment strictly after `after`, or 0 when there is none within HORIZON days (a date that does not come, a broken rule). */
    long next(long after, Days days) {
        if (!valid()) return 0;
        for (int k = 0; k <= HORIZON; k++) {
            Day d = days.day(k);
            if (d == null) return 0;
            if (!on(d)) continue;
            long t = time(d);
            if (t > after) return t;
        }
        return 0;
    }

    // ---- kept as text in DuaStore: "d=1;w=34;y=0;m=0;n=0;a=2;c=0;p=3;o=10;r=1"

    String text() {
        return "d=" + day + ";w=" + weekdays + ";y=" + year + ";m=" + month + ";n=" + date + ";a=" + at + ";c=" + clock + ";p=" + prayer + ";o=" + offset + ";r=" + (repeat ? 1 : 0);
    }

    /** null when the text is not one of ours */
    static DuaRule parse(String s) {
        if (s == null) return null;
        DuaRule r = new DuaRule();
        try {
            for (String part : s.split(";")) {
                int eq = part.indexOf('=');
                if (eq <= 0) return null;
                int v = Integer.parseInt(part.substring(eq + 1));
                switch (part.substring(0, eq)) {
                    case "d": r.day = v; break;
                    case "w": r.weekdays = v; break;
                    case "y": r.year = v; break;
                    case "m": r.month = v; break;
                    case "n": r.date = v; break;
                    case "a": r.at = v; break;
                    case "c": r.clock = v; break;
                    case "p": r.prayer = v; break;
                    case "o": r.offset = v; break;
                    case "r": r.repeat = v != 0; break;
                    default: break;   // a newer key: ignored
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return r.valid() ? r : null;
    }

    DuaRule copy() { return parse(text()); }
}
