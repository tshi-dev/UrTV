package sa.urtv.home.tilawah;

/** tests/dua_test.py runs this: one line per case, «name<TAB>ok|bad». Days are made up: day k starts at k*DAY, Sunday first. */
public class DuaRuleRun {
    static final long DAY = 86_400_000L, MIN = 60_000L;
    static void out(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "bad")); }

    /** a year of days: weekday k%7 (0 Sunday), Gregorian 2026 from Jan 1 (31-day months for simplicity), Hijri 1448 from Muharram 1 (30-day months);
     *  prayers: Fajr 5:00, sunrise 6:20, Dhuhr 12:00, Asr 15:30, Maghrib 18:00, Isha 19:30 */
    static final DuaRule.Days DAYS = k -> {
        long m = k * DAY;
        long[] p = {m + 300 * MIN, m + 380 * MIN, m + 720 * MIN, m + 930 * MIN, m + 1080 * MIN, m + 1170 * MIN};
        return new DuaRule.Day(k % 7, 2026, k / 31 % 12 + 1, k % 31 + 1, 1448 + k / 360, k / 30 % 12 + 1, k % 30 + 1, m, p);
    };

    static DuaRule rule(String text) { return DuaRule.parse(text); }

    public static void main(String[] a) {
        DuaRule every = rule("d=0;a=0;c=600;r=1");   // every day 10:00
        out("every day at 10:00: today when it is still before", every.next(9 * 60 * MIN, DAYS) == 600 * MIN);
        out("every day at 10:00: tomorrow once it passed", every.next(600 * MIN, DAYS) == DAY + 600 * MIN);

        DuaRule friday = rule("d=1;w=32;a=0;c=600;r=1");   // Friday (bit 5)
        out("every Friday at 10:00: the first Friday is day 5", friday.next(0, DAYS) == 5 * DAY + 600 * MIN);
        out("every Friday: the next one a week later", friday.next(5 * DAY + 600 * MIN, DAYS) == 12 * DAY + 600 * MIN);
        DuaRule sunTueThu = rule("d=1;w=21;a=1;c=0;p=0;o=30;r=1");   // Sun, Tue, Thu, 30 min before Fajr
        out("Sun, Tue, Thu, 30 minutes before Fajr", sunTueThu.next(0, DAYS) == 270 * MIN && sunTueThu.next(270 * MIN, DAYS) == 2 * DAY + 270 * MIN
                && sunTueThu.next(2 * DAY + 270 * MIN, DAYS) == 4 * DAY + 270 * MIN && sunTueThu.next(4 * DAY + 270 * MIN, DAYS) == 7 * DAY + 270 * MIN);
        DuaRule asr = rule("d=0;a=2;p=3;o=10;r=1");
        out("after Asr by 10 minutes follows that day's Asr", asr.next(0, DAYS) == 940 * MIN);
        DuaRule atMaghrib = rule("d=0;a=2;p=4;o=0;r=1");
        out("at Maghrib itself (no minutes)", atMaghrib.next(0, DAYS) == 1080 * MIN);

        DuaRule greg = rule("d=2;y=2026;m=2;n=14;a=0;c=480;r=0");   // 14 Feb 2026 08:00, once
        long g = greg.next(0, DAYS);
        out("a Gregorian date, once: that day", g == (31 + 13) * DAY + 480 * MIN);
        out("a Gregorian date, once: nothing after it", greg.next(g, DAYS) == 0);
        DuaRule hijri = rule("d=3;m=12;n=9;a=2;p=4;o=0;r=1");   // 9 Dhu al-Hijjah after Maghrib, every year
        long h = hijri.next(0, DAYS);
        out("a Hijri date, every year: 9 Dhu al-Hijjah at Maghrib", h == (11 * 30 + 8) * DAY + 1080 * MIN);
        out("the look-ahead covers a whole Hijri year (355 days) and more", DuaRule.HORIZON >= 356);
        DuaRule hijriSoon = rule("d=3;m=1;n=5;a=0;c=60;r=1");
        out("a Hijri date, every year: the next year's when this year's passed", hijriSoon.next(4 * DAY + 60 * MIN, DAYS) == 364 * DAY + 60 * MIN);
        DuaRule hijriOnce = rule("d=3;y=1449;m=1;n=1;a=0;c=0;r=0");
        out("a Hijri date in another year, once", hijriOnce.next(0, DAYS) == 360 * DAY);

        out("a broken rule is refused when read", rule("d=1;w=0;a=0;c=600;r=1") == null && rule("d=2;y=0;m=2;n=14;a=0;c=0;r=0") == null
                && rule("d=0;a=0;c=1440;r=1") == null && rule("d=0;a=2;p=6;o=0;r=1") == null && rule("d=0;a=1;p=0;o=181;r=1") == null
                && rule("d=3;m=12;n=31;a=0;c=0;r=1") == null && rule("x") == null && rule(null) == null);
        out("the text goes and comes back the same", rule(asr.text()).text().equals(asr.text()) && rule(hijri.text()).text().equals(hijri.text()));
        out("a newer key in the text is ignored", rule("d=0;a=0;c=600;r=1;zz=4") != null);
        DuaRule noTimes = rule("d=0;a=2;p=3;o=10;r=1");
        out("a day whose prayer times are not known is skipped", noTimes.next(0, k -> new DuaRule.Day(k % 7, 2026, 1, 1, 1448, 1, 1, k * DAY, k == 0 ? null : new long[]{0, 0, 0, k * DAY + 930 * MIN, 0, 0})) == DAY + 940 * MIN);
        out("no day at all: none", every.next(0, k -> null) == 0);

        out("plays on time with the TV on and nothing else", DuaPlan.decide(1000, 1000, true, false, false, false, true) == DuaPlan.Go.PLAY);
        out("a missed one is not made up (over 3 minutes late)", DuaPlan.decide(0, DuaPlan.LATE_MS + 1, true, false, false, false, true) == DuaPlan.Go.SKIP_LATE);
        out("standby: not played", DuaPlan.decide(0, 0, false, false, false, false, true) == DuaPlan.Go.SKIP_STANDBY);
        out("never over the adhan", DuaPlan.decide(0, 0, true, true, false, true, true) == DuaPlan.Go.SKIP_ADHAN);
        out("another app's sound: left alone by default", DuaPlan.decide(0, 0, true, false, true, false, true) == DuaPlan.Go.SKIP_OTHER_APP);
        out("another app's sound: played over it when the person chose that", DuaPlan.decide(0, 0, true, false, true, true, true) == DuaPlan.Go.PLAY);
        out("its file was deleted: nothing plays", DuaPlan.decide(0, 0, true, false, false, false, false) == DuaPlan.Go.SKIP_NO_SOUND);
    }
}
