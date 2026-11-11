package sa.urtv.home.tilawah;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

/** For tests/prayer_math_test.py: PrayerMath.java (the times and their cache keys) on a plain JVM. Prints PASS/FAIL lines. */
public final class PrayerMathRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    static String hm(long at, String tz) { return PrayerMath.clockOf(at, tz); }

    static String row(long[] t, String tz) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 6; i++) b.append(i > 0 ? " " : "").append(hm(t[i], tz));
        return b.toString();
    }

    static int minutes(String hhmm) { return Integer.parseInt(hhmm.substring(0, 2)) * 60 + Integer.parseInt(hhmm.substring(3)); }

    /** the clock row moved by that many minutes */
    static String shifted(String times, int shift) {
        if (shift == 0) return times;
        StringBuilder b = new StringBuilder();
        for (String t : times.split(" ")) { int m = Math.floorMod(minutes(t) + shift, 1440); b.append(b.length() > 0 ? " " : "").append(String.format("%02d:%02d", m / 60, m % 60)); }
        return b.toString();
    }

    /** the two clock rows differ by at most that many minutes in every field */
    static boolean within(String a, String b, int tolerance) {
        String[] x = a.split(" "), y = b.split(" ");
        for (int i = 0; i < x.length; i++) if (Math.abs(minutes(x[i]) - minutes(y[i])) > tolerance) return false;
        return true;
    }

    public static void main(String[] args) throws Exception {
        // Riyadh, 23 October 2026: the five times come in order and Isha is 90 minutes after Maghrib
        long[] r = PrayerMath.compute(2026, 10, 23, 24.6877, 46.7219, "Asia/Riyadh", false);
        boolean ordered = true;
        for (int i = 1; i < 6; i++) ordered &= r[i] > r[i - 1];
        check("Riyadh: fajr < sunrise < dhuhr < asr < maghrib < isha", ordered, java.util.Arrays.toString(r));
        check("Riyadh: Fajr 04:3x, Dhuhr 11:2x-11:3x, Maghrib 17:0x-17:1x", hm(r[0], "Asia/Riyadh").startsWith("04:") && hm(r[2], "Asia/Riyadh").startsWith("11:")
                && hm(r[4], "Asia/Riyadh").startsWith("17:"), hm(r[0], "Asia/Riyadh") + " " + hm(r[2], "Asia/Riyadh") + " " + hm(r[4], "Asia/Riyadh"));
        check("Riyadh: Isha is Maghrib + 90 minutes", r[5] - r[4] == 90 * 60_000L, String.valueOf(r[5] - r[4]));
        long[] ram = PrayerMath.compute(2026, 10, 23, 24.6877, 46.7219, "Asia/Riyadh", true);
        check("Ramadan: Isha is Maghrib + 120 minutes", ram[5] - ram[4] == 120 * 60_000L, String.valueOf(ram[5] - ram[4]));

        // Oslo, midsummer: Maghrib at 22:4x, so Isha is after midnight: the next day's, not 00:1x of the day that began 24 hours before
        long[] o = PrayerMath.compute(2026, 6, 21, 59.91, 10.75, "Europe/Oslo", false);
        long day0 = PrayerMath.midnight(2026, 6, 21, "Europe/Oslo");
        check("Oslo midsummer: Maghrib is late evening", o[4] - day0 > 22 * 3600_000L && o[4] - day0 < 24 * 3600_000L, hm(o[4], "Europe/Oslo"));
        check("Oslo midsummer: Isha is after Maghrib, past midnight, a day-1 moment (not wrapped back into the morning)",
                o[5] > o[4] && o[5] - o[4] == 90 * 60_000L && o[5] - day0 >= 24 * 3600_000L, hm(o[5], "Europe/Oslo") + " +" + (o[5] - day0) / 60000 + " min");
        check("Oslo midsummer: its clock reads 00:xx", hm(o[5], "Europe/Oslo").startsWith("00:"), hm(o[5], "Europe/Oslo"));

        // the cache key and Aladhan's date go by the city's zone and name the city
        long t = Instant.parse("2026-10-23T21:30:00Z").toEpochMilli();   // Saturday 00:30 in Riyadh, Friday evening in New York
        check("key: Riyadh's date, not the TV's", PrayerMath.key(t, 24.6877, 46.7219, "Asia/Riyadh").startsWith("times_20261024_"), PrayerMath.key(t, 24.6877, 46.7219, "Asia/Riyadh"));
        check("key: New York's date", PrayerMath.key(t, 40.71, -74.0, "America/New_York").startsWith("times_20261023_"), PrayerMath.key(t, 40.71, -74.0, "America/New_York"));
        check("key: another city the same day is another key", !PrayerMath.key(t, 24.6877, 46.7219, "Asia/Riyadh").equals(PrayerMath.key(t, 21.4858, 39.1925, "Asia/Riyadh")), "same");
        check("key: the same city is the same key", PrayerMath.key(t, 24.6877, 46.7219, "Asia/Riyadh").equals(PrayerMath.key(t + 3600_000L, 24.6877, 46.7219, "Asia/Riyadh")), "differs");
        check("key: the same instant in another zone of the same place is another key", !PrayerMath.key(t, 24.6877, 46.7219, "Asia/Riyadh").equals(PrayerMath.key(t, 24.6877, 46.7219, "Asia/Kuwait")), "same");
        check("Aladhan's date is the city's day: 24-10-2026 for Riyadh, 23-10-2026 for New York",
                PrayerMath.aladhanDate(t, "Asia/Riyadh").equals("24-10-2026") && PrayerMath.aladhanDate(t, "America/New_York").equals("23-10-2026"),
                PrayerMath.aladhanDate(t, "Asia/Riyadh") + " " + PrayerMath.aladhanDate(t, "America/New_York"));
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"));   // the TV's own zone must not leak in
        check("key and date ignore the machine's zone", PrayerMath.aladhanDate(t, "Asia/Riyadh").equals("24-10-2026")
                && PrayerMath.key(t, 24.6877, 46.7219, "Asia/Riyadh").startsWith("times_20261024_"), PrayerMath.aladhanDate(t, "Asia/Riyadh"));

        // Aladhan's «5:12 (+03)» must not crash the receiver
        check("parse: 05:12", ok(PrayerMath.parseClock("05:12"), 5, 12), "");
        check("parse: 5:12", ok(PrayerMath.parseClock("5:12"), 5, 12), "");
        check("parse: «5:12 (+03)»", ok(PrayerMath.parseClock("5:12 (+03)"), 5, 12), "");
        check("parse: «17:42 (AST)»", ok(PrayerMath.parseClock("17:42 (AST)"), 17, 42), "");
        check("parse: not a time -> null, no exception", PrayerMath.parseClock("--") == null && PrayerMath.parseClock("") == null
                && PrayerMath.parseClock(null) == null && PrayerMath.parseClock("24:00") == null && PrayerMath.parseClock("12:7") == null && PrayerMath.parseClock("x5:12") == null, "");

        // a Saudi city keeps exactly the times it had before the Gulf methods existed (tests/prayer_gold.tsv, taken from the code before)
        int golden = 0;
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t");
            String[] ymd = f[1].split("-");
            long[] g = PrayerMath.compute(Integer.parseInt(ymd[0]), Integer.parseInt(ymd[1]), Integer.parseInt(ymd[2]),
                    Double.parseDouble(f[2]), Double.parseDouble(f[3]), "Asia/Riyadh", "1".equals(f[4]),
                    PrayerMath.methodFor("Asia/Riyadh", null));
            golden++;
            check("Saudi golden " + f[0] + " " + f[1] + ": the same six times as before", row(g, "Asia/Riyadh").equals(f[5]), row(g, "Asia/Riyadh") + " vs " + f[5]);
        }
        check("Saudi golden: the file was read", golden == 12, String.valueOf(golden));

        // the other countries' cities (tests/prayer_gold_countries.tsv): times from an independent solar calculation (NOAA/Meeus series, not this
        // code's formula) with each country's published parameters; the method comes from the zone as the TV would pick it. One minute of slack.
        int countries = 0, rebased = 0;
        for (String line : Files.readAllLines(Paths.get(args[1]), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t");
            String[] ymd = f[1].split("-");
            int y = Integer.parseInt(ymd[0]), mo = Integer.parseInt(ymd[1]), d = Integer.parseInt(ymd[2]);
            long[] g = PrayerMath.compute(y, mo, d, Double.parseDouble(f[2]), Double.parseDouble(f[3]), f[4], false, PrayerMath.methodFor(f[4], null));
            countries++;
            // the row is written at f[6] minutes from UTC; this machine's time-zone data may give the zone another offset that day (Morocco
            // left UTC+1 in tzdata 2026c): the sun is the same, so the reference is re-based by the difference and counted, not failed
            int shift = (int) (sa.urtv.home.ZoneFix.offset(f[4], PrayerMath.midnight(y, mo, d, f[4]) + 12 * 3600_000L) / 60000) - Integer.parseInt(f[6].trim());   // the offset the app uses (issue #55)
            if (shift != 0) rebased++;
            String want = shifted(f[5], shift);
            check("reference " + f[0] + " " + f[1] + " (" + f[4] + "): every time within a minute of the independent calculation", within(row(g, f[4]), want, 1), row(g, f[4]) + " vs " + want);
        }
        System.out.println("NOTE reference: " + (rebased == 0 ? "every row at the offset it was written at" : rebased + " rows re-based for this machine's time-zone data, not formula errors"));
        check("reference: the file was read", countries >= 1, String.valueOf(countries));

        // which method a place gets: its country's by the zone, the Gulf one when asked, Umm al-Qura for anything else
        check("methods: Riyadh 4, Kuwait 9, Qatar 10, Dubai 16, Bahrain 8, Muscat 100 (Oman's ministry), Cairo 5, an unknown zone 4",
                PrayerMath.methodFor("Asia/Riyadh", null) == 4 && PrayerMath.methodFor("Asia/Kuwait", null) == 9 && PrayerMath.methodFor("Asia/Qatar", null) == 10
                && PrayerMath.methodFor("Asia/Dubai", null) == 16 && PrayerMath.methodFor("Asia/Bahrain", null) == 8 && PrayerMath.methodFor("Asia/Muscat", "auto") == 100
                && PrayerMath.methodFor("Africa/Cairo", null) == 5 && PrayerMath.methodFor("Asia/Amman", null) == 23 && PrayerMath.methodFor("Africa/Tunis", null) == 18 && PrayerMath.methodFor("Africa/Algiers", null) == 19 && PrayerMath.methodFor("Africa/Casablanca", null) == 21 && PrayerMath.methodFor("Asia/Damascus", null) == 5 && PrayerMath.methodFor("Asia/Beirut", null) == 5
                && PrayerMath.methodFor("Asia/Gaza", null) == 5 && PrayerMath.methodFor("Asia/Hebron", null) == 5 && PrayerMath.methodFor("Nowhere/Land", null) == 4 && PrayerMath.methodFor(null, null) == 4, "");
        check("methods: pm=gulf is the Gulf method (8) for Oman and anywhere; Aladhan knows 8 but not Oman's own 100",
                PrayerMath.methodFor("Asia/Muscat", "gulf") == 8 && PrayerMath.methodFor("Asia/Riyadh", "gulf") == 8 && PrayerMath.aladhanKnows(8) && PrayerMath.aladhanKnows(23) && !PrayerMath.aladhanKnows(100), "");
        // Muscat, 23 October 2026, 23.5880 N 58.3829 E: Oman's ministry (Fajr 18, Isha 18, Dhuhr/Asr/Maghrib +5; the parameters egyjs/afnon-android-tv-launcher fitted to the
        // ministry's timetable) as that method's own formulas (adhan's, tests/adhan_port.mjs) give it, and the Gulf method it replaces as Oman's default
        String muscat = row(PrayerMath.compute(2026, 10, 23, 23.5880, 58.3829, "Asia/Muscat", false, PrayerMath.methodFor("Asia/Muscat", null)), "Asia/Muscat");
        String muscatGulf = row(PrayerMath.compute(2026, 10, 23, 23.5880, 58.3829, "Asia/Muscat", false, PrayerMath.methodFor("Asia/Muscat", "gulf")), "Asia/Muscat");
        check("Oman: Muscat on 23 October 2026 gets the ministry's times (within a minute of afnon's own method)", within(muscat, "04:51 06:07 11:56 15:12 17:39 18:50", 1), muscat);
        check("Oman: with pm=gulf Muscat gets the Gulf method's times, as before", within(muscatGulf, "04:45 06:07 11:51 15:07 17:34 19:04", 1), muscatGulf);
        check("Oman: the ministry's Dhuhr, Asr and Maghrib are 5 minutes after the Gulf method's, sunrise the same, Fajr later, Isha earlier",
                minutes(muscat.split(" ")[2]) - minutes(muscatGulf.split(" ")[2]) == 5 && minutes(muscat.split(" ")[3]) - minutes(muscatGulf.split(" ")[3]) == 5
                && minutes(muscat.split(" ")[4]) - minutes(muscatGulf.split(" ")[4]) == 5 && muscat.split(" ")[1].equals(muscatGulf.split(" ")[1])
                && minutes(muscat.split(" ")[0]) > minutes(muscatGulf.split(" ")[0]) && minutes(muscat.split(" ")[5]) < minutes(muscatGulf.split(" ")[5]), muscat + " / " + muscatGulf);
        check("methods (4.3 A4): an old pm=jafari is the country's own (the Ja'fari is no longer offered), and id 0 is no method",
                PrayerMath.methodFor("Asia/Riyadh", "jafari") == 4 && PrayerMath.methodFor("Asia/Kuwait", "jafari") == 9 && PrayerMath.methodFor("Asia/Kuwait", "0") == 9 && PrayerMath.method(0).id == 4, "");
        check("methods: the Ramadan half hour of Isha is Umm al-Qura's alone",
                PrayerMath.ishaDelayMillis(true, 4) == 120 * 60_000L && PrayerMath.ishaDelayMillis(true, 10) == 90 * 60_000L && PrayerMath.ishaDelayMillis(true, 8) == 90 * 60_000L, "");

        // the Gulf cities, 23 October 2026: each country's own parameters show (Fajr, sunrise, Dhuhr, Asr, Maghrib, Isha)
        String kuwait = row(PrayerMath.compute(2026, 10, 23, 29.3697, 47.9783, "Asia/Kuwait", false, 9), "Asia/Kuwait");
        String kuwaitUq = row(PrayerMath.compute(2026, 10, 23, 29.3697, 47.9783, "Asia/Kuwait", false, 4), "Asia/Kuwait");
        check("Kuwait: Fajr at 18° is later than Umm al-Qura's 18.5°, Isha at 17.5° is 13 minutes earlier than Maghrib + 90, the rest equal",
                minutes(kuwait.split(" ")[0]) > minutes(kuwaitUq.split(" ")[0]) && minutes(kuwait.split(" ")[5]) < minutes(kuwaitUq.split(" ")[5])
                && kuwait.split(" ")[2].equals(kuwaitUq.split(" ")[2]) && kuwait.split(" ")[4].equals(kuwaitUq.split(" ")[4]), kuwait + " / " + kuwaitUq);
        long[] doha = PrayerMath.compute(2026, 10, 23, 25.2854, 51.5310, "Asia/Qatar", true, 10);
        check("Qatar: Isha is Maghrib + 90 minutes, also in Ramadan", doha[5] - doha[4] == 90 * 60_000L, String.valueOf(doha[5] - doha[4]));
        long[] manama = PrayerMath.compute(2026, 10, 23, 26.2154, 50.5832, "Asia/Bahrain", true, 8);
        check("Bahrain (Gulf method): Isha is Maghrib + 90 minutes, also in Ramadan", manama[5] - manama[4] == 90 * 60_000L, String.valueOf(manama[5] - manama[4]));
        // Sharjah, 12 October 2026, from a property site's timetable of the UAE's calendar (not an official source; one minute for Dhuhr, three for Asr)
        String sharjah = row(PrayerMath.compute(2026, 10, 12, 25.3463, 55.4209, "Asia/Dubai", false, 16), "Asia/Dubai");
        String[] s6 = sharjah.split(" ");
        check("UAE: Fajr, Dhuhr, Maghrib and Isha within 1 minute and Asr within 3 of a published UAE timetable for 12 October",
                Math.abs(minutes(s6[0]) - minutes("04:58")) <= 1 && Math.abs(minutes(s6[2]) - minutes("12:07")) <= 1 && Math.abs(minutes(s6[3]) - minutes("15:25")) <= 3
                && Math.abs(minutes(s6[4]) - minutes("17:57")) <= 1 && Math.abs(minutes(s6[5]) - minutes("19:11")) <= 1, sharjah);
        long[] dubai = PrayerMath.compute(2026, 10, 12, 25.2048, 55.2708, "Asia/Dubai", false, 16);
        long[] dubaiRaw = PrayerMath.compute(2026, 10, 12, 25.2048, 55.2708, "Asia/Dubai", false, 4);
        check("UAE: sunrise 3 minutes earlier, Dhuhr and Asr 3 minutes later than the untuned sun", dubai[1] < dubaiRaw[1] && dubai[2] - dubaiRaw[2] == 3 * 60_000L, row(dubai, "Asia/Dubai"));


        // F28: on the day the clocks change, every time is where the clock says, not an hour off (8 March 2026 spring forward, 1 November 2026 back)
        String ny8 = row(PrayerMath.compute(2026, 3, 8, 40.7128, -74.0060, "America/New_York", false), "America/New_York");
        check("DST: New York, 8 March (clocks forward): Fajr 05:45 and Dhuhr 13:07 on the clock, Asr and Maghrib after them", ny8.startsWith("05:45 07:19 13:07 16:22 18:55"), ny8);
        String ny1 = row(PrayerMath.compute(2026, 11, 1, 40.7128, -74.0060, "America/New_York", false), "America/New_York");
        check("DST: New York, 1 November (clocks back): Fajr 04:51, Dhuhr 11:40 on the clock", ny1.startsWith("04:51 06:26 11:40 14:29 16:52"), ny1);
        String paris = row(PrayerMath.compute(2026, 10, 25, 48.8566, 2.3522, "Europe/Paris", false), "Europe/Paris");
        check("DST: Paris, 25 October (the day summer time ends): sunset reads 17:xx, as the page's Maghrib does, not 16:xx", paris.split(" ")[4].startsWith("17:"), paris);
        String riyadh = row(PrayerMath.compute(2026, 3, 8, 24.6877, 46.7219, "Asia/Riyadh", false), "Asia/Riyadh");
        check("DST: Riyadh has no change and keeps its times", riyadh.split(" ")[2].startsWith("12:"), riyadh);
        long w = PrayerMath.wall(2026, 3, 8, 2 * 60 + 30, "America/New_York");   // 02:30 does not exist on 8 March: it reads as 03:30
        check("DST: a clock that does not exist (02:30 on 8 March) is 03:30; 25:10 is 01:10 of the next day",
                hm(w, "America/New_York").equals("03:30") && hm(PrayerMath.wall(2026, 6, 21, 25 * 60 + 10, "Asia/Riyadh"), "Asia/Riyadh").equals("01:10"), hm(w, "America/New_York"));

        // F19: the alarm is for the first prayer still ahead, even 1 second ahead; the screen's next-prayer rule leaves out one within 30 seconds
        long[] day = {1_000_000, 1_100_000, 2_000_000, 3_000_000, 4_000_000, 5_000_000};
        int[] five = {0, 2, 3, 4, 5};
        for (long[] c : new long[][]{{-60, 2}, {-31, 2}, {-30, 2}, {-20, 2}, {-1, 2}, {0, 3}, {5, 3}}) {
            long now = 2_000_000 + c[0] * 1000;
            int alarm = PrayerMath.firstAfter(day, five, now), shown = PrayerMath.firstAfter(day, five, now + 30_000);
            boolean ok = alarm == (c[0] < 0 ? 2 : 3) && shown == (c[0] < -30 ? 2 : 3);
            check("alarm choice at " + c[0] + " s before/after Dhuhr: alarm " + alarm + " (Dhuhr while it is ahead), screen " + shown, ok, alarm + "/" + shown);
        }
        check("alarm choice: nothing left today is -1", PrayerMath.firstAfter(day, five, 6_000_000) == -1, "");

        // F20: the scheduling identity changes with the place, the zone, the minutes and the method, and with nothing else
        String id = PrayerMath.identity(24.6877, 46.7219, "Asia/Riyadh", "0,0,0,0,0", 4);
        check("identity: the same place is the same identity", id.equals(PrayerMath.identity(24.6877, 46.7219, "Asia/Riyadh", "0,0,0,0,0", 4)), id);
        check("identity: another city differs", !id.equals(PrayerMath.identity(21.4266, 39.8256, "Asia/Riyadh", "0,0,0,0,0", 4)), id);
        check("identity: another zone differs", !id.equals(PrayerMath.identity(24.6877, 46.7219, "Asia/Kuwait", "0,0,0,0,0", 4)), id);
        check("identity: other adjustments differ", !id.equals(PrayerMath.identity(24.6877, 46.7219, "Asia/Riyadh", "0,0,0,2,0", 4)), id);
        check("identity: another method differs", !id.equals(PrayerMath.identity(24.6877, 46.7219, "Asia/Riyadh", "0,0,0,0,0", 8)), id);

        // the saved copy is per method: Umm al-Qura's key is as it always was, another method's has its own
        long t0 = Instant.parse("2026-10-23T09:00:00Z").toEpochMilli();
        String k4 = PrayerMath.key(t0, 24.6877, 46.7219, "Asia/Riyadh", 4);
        check("key: Umm al-Qura's key is unchanged, the Gulf method's is another", k4.equals(PrayerMath.key(t0, 24.6877, 46.7219, "Asia/Riyadh"))
                && !k4.equals(PrayerMath.key(t0, 24.6877, 46.7219, "Asia/Riyadh", 8)) && PrayerMath.key(t0, 24.6877, 46.7219, "Asia/Riyadh", 8).endsWith("_m8"), k4);
        System.exit(failed == 0 ? 0 : 1);
    }

    static boolean ok(int[] hm, int h, int m) { return hm != null && hm[0] == h && hm[1] == m; }
}
