package sa.urtv.home.tilawah;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import sa.urtv.home.ZoneFix;
import sa.urtv.home.ZoneFixPeek;

/** For tests/zone_fix_test.py: ZoneFix with fake TV zone data (an old TV's Morocco at +01, a vendor's +02 and +00), the page's word, Calendar,
 *  the prayer times and the reminders' day steps. One line per check: name TAB ok|FAIL. */
public final class ZoneFixRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    static final long H = 3600_000L, MIN = 60_000L;
    static long utc(int y, int mo, int d, int h, int mi) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        c.clear();
        c.set(y, mo - 1, d, h, mi, 0);
        return c.getTimeInMillis();
    }

    public static void main(String[] a) {
        String CASA = "Africa/Casablanca";
        long nov11 = utc(2026, 11, 11, 12, 0), mar1 = utc(2026, 3, 1, 12, 0), jun1 = utc(2026, 6, 1, 12, 0), sep19 = utc(2026, 9, 19, 12, 0);
        // the TV's data, three ways wrong for Morocco
        for (final long fake : new long[] {H, 2 * H, 0}) {
            ZoneFixPeek.platform((id, t) -> CASA.equals(id) || "Africa/El_Aaiun".equals(id) ? fake : TimeZone.getTimeZone(id).getOffset(t));
            ZoneFixPeek.forget();
            check("Morocco with the TV's data at " + fake / H + " h: +00 now (since 2026-09-20), +01 before, +00 in Ramadan 2026",
                ZoneFix.offset(CASA, nov11) == 0 && ZoneFix.offset(CASA, sep19) == H && ZoneFix.offset(CASA, jun1) == H && ZoneFix.offset(CASA, mar1) == 0
                && ZoneFix.offset("Africa/El_Aaiun", nov11) == 0);
            Calendar c = Calendar.getInstance(ZoneFix.zone(CASA), Locale.US);
            c.setTimeInMillis(utc(2026, 11, 11, 16, 52));
            boolean clock = c.get(Calendar.HOUR_OF_DAY) == 16 && c.get(Calendar.MINUTE) == 52 && c.get(Calendar.DAY_OF_MONTH) == 11;
            c.clear();
            c.set(2026, Calendar.NOVEMBER, 11, 0, 0, 0);
            check("  the clock reads 16:52 at 16:52 UTC, and Safi's midnight is 00:00 UTC (a Calendar both ways)", clock && c.getTimeInMillis() == utc(2026, 11, 11, 0, 0));
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            f.setTimeZone(ZoneFix.zone(CASA));
            check("  a date format the same", "2026-11-11 23:30".equals(f.format(new java.util.Date(utc(2026, 11, 11, 23, 30)))));
            long[] p = PrayerMath.compute(2026, 11, 11, 32.2994, -9.2372, CASA, false, 3);   // Safi, Muslim World League
            ZoneFixPeek.platform((id, t) -> TimeZone.getTimeZone(id).getOffset(t));
            check("  Safi's prayer times are the same moments as with the right data (the table)", java.util.Arrays.equals(p, PrayerMath.compute(2026, 11, 11, 32.2994, -9.2372, CASA, false, 3)));
            ZoneFixPeek.platform((id, t) -> CASA.equals(id) ? fake : TimeZone.getTimeZone(id).getOffset(t));
            Calendar dh = Calendar.getInstance(ZoneFix.zone(CASA), Locale.US);
            dh.setTimeInMillis(p[2]);
            check("  Dhuhr in Safi on 11 Nov 2026 reads about 12:2x local (the sun's noon at 9.2 W, +00), not an hour later", dh.get(Calendar.HOUR_OF_DAY) == 12 && dh.get(Calendar.MINUTE) >= 10 && dh.get(Calendar.MINUTE) <= 35);
        }
        ZoneFixPeek.platform((id, t) -> TimeZone.getTimeZone(id).getOffset(t));
        ZoneFixPeek.forget();
        // a zone the table does not have, the TV's data off by an hour: the page's word moves it near the moment told, not far from it
        final String RIY = "Asia/Riyadh";
        ZoneFixPeek.platform((id, t) -> RIY.equals(id) ? 4 * H : TimeZone.getTimeZone(id).getOffset(t));
        check("no word from the page: the TV's own (+04, wrong)", ZoneFix.offset(RIY, nov11) == 4 * H);
        ZoneFix.report(RIY, nov11, 180);
        check("the page says +03 at that moment: +03 then, and a day later; the TV's again two days later",
            ZoneFix.offset(RIY, nov11) == 3 * H && ZoneFix.offset(RIY, nov11 + 24 * H) == 3 * H && ZoneFix.offset(RIY, nov11 + 48 * H) == 4 * H);
        check("the zone's object follows it (a Calendar reads 15:00 at 12:00 UTC)", calHour(RIY, nov11) == 15);
        check("another zone is not moved by Riyadh's word", ZoneFix.offset("Asia/Dubai", nov11) == 4 * H);
        ZoneFix.report(RIY, nov11, 240);
        check("the page agrees with the TV: nothing moves", ZoneFix.offset(RIY, nov11) == 4 * H);
        ZoneFixPeek.forget();
        ZoneFix.report("Not/AZone", nov11, 60);
        ZoneFix.report(RIY, nov11, 15 * 60);
        ZoneFix.report(RIY, 0, 180);
        ZoneFix.report("x|1|60");
        ZoneFix.report("Asia/Riyadh|abc|60");
        check("a bad word is ignored: an unknown zone, an offset past 14 h, no moment, a broken «zone|ms|minutes»", ZoneFix.offset(RIY, nov11) == 4 * H);
        ZoneFix.report(RIY + "|" + nov11 + "|180");
        check("«zone|ms|minutes» as ConfigProvider gives it", ZoneFix.offset(RIY, nov11) == 3 * H);
        ZoneFixPeek.forget();
        ZoneFixPeek.platform((id, t) -> TimeZone.getTimeZone(id).getOffset(t));
        check("a zone nobody moves is the TV's own object (nothing changes for Riyadh, Dubai, Cairo's data where right)",
            ZoneFix.zone(RIY).getClass() == TimeZone.getTimeZone(RIY).getClass() && ZoneFix.offset(RIY, nov11) == 3 * H);
        // the other zones of the table, at moments their old rules got wrong
        check("Amman and Damascus +03 in winter (2022 rule)", ZoneFix.offset("Asia/Amman", utc(2027, 1, 15, 12, 0)) == 3 * H && ZoneFix.offset("Asia/Damascus", utc(2027, 1, 15, 12, 0)) == 3 * H);
        check("Tehran +03:30 in summer (no DST since 2022)", ZoneFix.offset("Asia/Tehran", utc(2027, 7, 1, 12, 0)) == 210 * MIN);
        check("Mexico City -06 in summer (no DST since 2022)", ZoneFix.offset("America/Mexico_City", utc(2027, 7, 1, 12, 0)) == -6 * H);
        check("Cairo +03 in summer (DST back in 2023), +02 in winter", ZoneFix.offset("Africa/Cairo", utc(2027, 7, 1, 12, 0)) == 3 * H && ZoneFix.offset("Africa/Cairo", utc(2027, 1, 15, 12, 0)) == 2 * H);
        check("Juba +02 (2021)", ZoneFix.offset("Africa/Juba", utc(2027, 1, 15, 12, 0)) == 2 * H);
        check("Almaty +05 (2024)", ZoneFix.offset("Asia/Almaty", utc(2027, 1, 15, 12, 0)) == 5 * H);
        check("Edmonton -06 in winter (permanent DST from 2026-11-01)", ZoneFix.offset("America/Edmonton", utc(2027, 1, 15, 12, 0)) == -6 * H);
        check("Dublin: +00 in winter, +01 in summer (its negative DST read either way)", ZoneFix.offset("Europe/Dublin", utc(2027, 1, 15, 12, 0)) == 0 && ZoneFix.offset("Europe/Dublin", utc(2027, 7, 1, 12, 0)) == H);
        check("past the table (2046): the TV's own data", ZoneFix.offset("Asia/Amman", utc(2047, 1, 15, 12, 0)) == TimeZone.getTimeZone("Asia/Amman").getOffset(utc(2047, 1, 15, 12, 0)));
        // a daily reminder in Safi keeps its wall time across days with the TV's +01 data
        ZoneFixPeek.platform((id, t) -> CASA.equals(id) ? H : TimeZone.getTimeZone(id).getOffset(t));
        long due = utc(2026, 11, 11, 20, 0);   // 20:00 in Safi (+00)
        long next = ZoneFixPeek.step(due, 1, ZoneFix.zone(CASA));
        check("a reminder at 20:00 in Safi, a day later: 20:00 in Safi again (20:00 UTC), not 19:00", next == utc(2026, 11, 12, 20, 0) && calHour(CASA, next) == 20);
        long rd = utc(2026, 9, 19, 19, 0);   // 20:00 in Safi at +01, the day before +00 for good
        check("across 2026-09-20 (+01 to +00): 20:00 stays 20:00 on the wall", calHour(CASA, ZoneFixPeek.step(rd, 1, ZoneFix.zone(CASA))) == 20);
    }

    static int calHour(String id, long t) {
        Calendar c = Calendar.getInstance(ZoneFix.zone(id), Locale.US);
        c.setTimeInMillis(t);
        return c.get(Calendar.HOUR_OF_DAY);
    }
}
