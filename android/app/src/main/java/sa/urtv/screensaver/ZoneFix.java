package sa.urtv.screensaver;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * 4.3 (issue #55): every native time of a city or of the TV (the clock, the date lines, the Hijri day, prayer times, reminders, the matches' times)
 * takes its zone from here, so it cannot disagree with the page. Why: the apps read the TV's own tz database, which can be years old, while the
 * page reads its WebView's (updated with Chrome). In Safi (Africa/Casablanca) the home's clock was an hour ahead: Morocco has been on +00 for good
 * since 2026-09-20 (tzdata 2026c+), and a TV with older data still says +01.
 *
 * The offset of a zone at a moment, in this order:
 * 1. ZoneTable (made from a recent tz database by .github/tools/zone_table.py) for the zones whose rules changed in recent years;
 * 2. else the TV's own data (java.util.TimeZone);
 * 3. then the page's word (report): the page tells, through the screensaver, the offset it uses for the city's zone at a moment; for moments
 *    within REPORT_WINDOW_MS of that one, a native offset that differs by the same amount is moved to the page's, and one line is logged.
 * zone(id) gives a java.util.TimeZone that answers with these offsets (the TV's own object when nothing needs a change). Plain Java: tests on a
 * JVM (tests/zone_fix_test.py). Both apps carry the same file (the screensaver's is in sa.urtv.screensaver).
 */
public final class ZoneFix {
    private ZoneFix() { }

    /** the page's offset is trusted for moments this close to the one it was told for (a day and a half: the page tells it every hour) */
    public static final long REPORT_WINDOW_MS = 36 * 3600_000L;

    private static Map<String, long[][]> table;   // zone -> {minutes since 1970 of each change}, {offset in minutes after it}; [0] is the first
    private static volatile Report report;
    private static volatile String logged = "";

    /** The page's word: at UTC `at`, zone `id` is `offsetMin` minutes ahead of UTC. */
    static final class Report {
        final String id; final long at; final int offsetMin;
        Report(String id, long at, int offsetMin) { this.id = id; this.at = at; this.offsetMin = offsetMin; }
    }

    /** Logs the one line when the page corrects a zone (Android's Log on the TV; nothing on a JVM). */
    interface Logger { void log(String line); }
    static volatile Logger logger;

    private static synchronized Map<String, long[][]> table() {
        if (table != null) return table;
        Map<String, long[][]> t = new HashMap<>();
        for (String row : ZoneTable.DATA) {
            String[] p = row.split("\\|", -1);
            String[] ch = p[2].isEmpty() ? new String[0] : p[2].split(",");
            long[] at = new long[ch.length + 1], off = new long[ch.length + 1];
            at[0] = Long.MIN_VALUE;
            off[0] = Long.parseLong(p[1]);
            for (int i = 0; i < ch.length; i++) {
                String[] x = ch[i].split(":");
                at[i + 1] = Long.parseLong(x[0]);
                off[i + 1] = Long.parseLong(x[1]);
            }
            t.put(p[0], new long[][] {at, off});
        }
        return table = t;
    }

    /** the zone's offset from ZoneTable at that moment, or null when the table does not cover it */
    static Long fromTable(String id, long utcMs) {
        long[][] z = id == null ? null : table().get(id);
        if (z == null || utcMs < ZoneTable.FROM_MS || utcMs >= ZoneTable.TO_MS) return null;
        long min = utcMs / 60_000L;   // FROM_MS is after 1970: never negative here (Math.floorDiv is Android 7+)
        int i = z[0].length - 1;
        while (i > 0 && z[0][i] > min) i--;
        return z[1][i] * 60_000L;
    }

    /** The TV's own tz data: java.util.TimeZone; a test gives a fake one (an old TV's +01, a vendor's +02 or +00). */
    interface Platform { long offset(String id, long utcMs); }
    static volatile Platform platform = new Platform() {
        @Override public long offset(String id, long utcMs) { return TimeZone.getTimeZone(id).getOffset(utcMs); }
    };

    /** the native offset (the table, else the TV's own data), before the page's word */
    static long nativeOffset(String id, long utcMs) {
        Long t = fromTable(id, utcMs);
        return t != null ? t : platform.offset(id, utcMs);
    }

    /** The offset, in ms, of zone `id` at UTC `utcMs`: the table, else the TV's data, moved to the page's word near the moment it was told. */
    public static long offset(String id, long utcMs) {
        long n = nativeOffset(id, utcMs);
        Report r = report;
        if (r == null || !r.id.equals(id) || Math.abs(utcMs - r.at) > REPORT_WINDOW_MS) return n;
        long d = r.offsetMin * 60_000L - nativeOffset(id, r.at);
        if (d == 0) return n;
        String line = id + " " + (nativeOffset(id, r.at) / 60_000L) + " -> " + r.offsetMin;
        if (!line.equals(logged)) {
            logged = line;
            Logger l = logger;
            if (l != null) l.log("zone " + id + ": the TV's data says " + (nativeOffset(id, r.at) / 60_000L) + " min, the page " + r.offsetMin + " min: the page's is used");
        }
        return n + d;
    }

    /**
     * The page's word (the screensaver's page, through ConfigProvider's column page_zone «zone|ms|minutes»): kept when it is a known zone, a
     * moment, and an offset between -14 h and +14 h in whole minutes; anything else is ignored. null forgets it.
     */
    public static void report(String id, long at, int offsetMin) {
        if (id == null || id.isEmpty() || at <= 0 || offsetMin < -14 * 60 || offsetMin > 14 * 60) return;
        if (!known(id)) return;
        report = new Report(id, at, offsetMin);
    }

    /** «zone|ms|minutes» as ConfigProvider gives it; ignored when it is not that */
    public static void report(String packed) {
        if (packed == null || !packed.matches("[A-Za-z0-9_+\\-/]{1,40}\\|\\d{1,15}\\|-?\\d{1,4}")) return;
        String[] p = packed.split("\\|");
        report(p[0], Long.parseLong(p[1]), Integer.parseInt(p[2]));
    }

    static void forget() { report = null; logged = ""; }

    private static boolean known(String id) {
        if (table().containsKey(id)) return true;
        for (String s : TimeZone.getAvailableIDs()) if (s.equals(id)) return true;
        return false;
    }

    /** a zone that needs nothing from here: not in the table and no page word that moves it */
    private static boolean plain(String id) {
        if (table().containsKey(id)) return false;
        Report r = report;
        return platform.offset(id, System.currentTimeMillis()) == TimeZone.getTimeZone(id).getOffset(System.currentTimeMillis())
            && (r == null || !r.id.equals(id) || r.offsetMin * 60_000L == platform.offset(id, r.at));
    }

    /** The zone to give a Calendar or a date format: the TV's own one when nothing moves it, else one that answers with offset(id, …). */
    public static TimeZone zone(String id) {
        if (id == null || id.isEmpty()) return TimeZone.getDefault();
        return plain(id) ? TimeZone.getTimeZone(id) : new Fixed(id);
    }

    /** The TV's own zone, the same way (a TV set to Casablanca with old data, too). */
    public static TimeZone tvZone() { return zone(TimeZone.getDefault().getID()); }

    /**
     * A java.util.TimeZone whose every answer is ZoneFix.offset. Calendar asks getOffset(ms) for the fields of a moment, and raw + (DST ? savings : 0)
     * for the moment of a wall time: raw is the offset now and the savings the other offset of the zone within a year (the zones here have at most
     * two at a time), so both ways agree.
     */
    static final class Fixed extends TimeZone {
        Fixed(String id) { setID(id); }

        @Override public int getOffset(long date) { return (int) offset(getID(), date); }

        @Override public int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis) {
            GregorianCalendar g = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            g.clear();
            g.set(Calendar.ERA, era);
            g.set(year, month, day, 0, 0, 0);
            long wall = g.getTimeInMillis() + millis;
            long o = offset(getID(), wall - offset(getID(), wall));   // the moment whose wall time this is
            return (int) o;
        }

        @Override public int getRawOffset() { return (int) offset(getID(), System.currentTimeMillis()); }

        @Override public void setRawOffset(int ms) { }

        @Override public boolean inDaylightTime(Date d) { return offset(getID(), d.getTime()) != getRawOffset(); }

        @Override public int getDSTSavings() {
            long now = System.currentTimeMillis(), raw = getRawOffset();
            for (long t = now - 366 * 86_400_000L; t < now + 366 * 86_400_000L; t += 86_400_000L) {
                long o = offset(getID(), t);
                if (o != raw) return (int) (o - raw);
            }
            return 0;
        }

        @Override public boolean useDaylightTime() { return getDSTSavings() != 0; }

        @Override public boolean hasSameRules(TimeZone other) { return other instanceof Fixed && other.getID().equals(getID()); }
    }
}
