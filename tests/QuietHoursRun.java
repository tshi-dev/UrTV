package sa.urtv.home.tilawah;

/** tests/quiet_hours_test.py runs this: one line per case, «name<TAB>ok|bad». */
public class QuietHoursRun {
    static void out(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "bad")); }

    public static void main(String[] a) {
        int from = 23 * 60, to = 5 * 60;
        out("23:00 to 05:00 holds at midnight and at 04:59", QuietHours.inside(0, from, to) && QuietHours.inside(4 * 60 + 59, from, to) && QuietHours.inside(23 * 60, from, to));
        out("not at 05:00, noon or 22:59", !QuietHours.inside(5 * 60, from, to) && !QuietHours.inside(12 * 60, from, to) && !QuietHours.inside(22 * 60 + 59, from, to));
        out("a window inside one day (13:00 to 15:00)", QuietHours.inside(14 * 60, 13 * 60, 15 * 60) && !QuietHours.inside(15 * 60, 13 * 60, 15 * 60));
        out("from = to is no window", !QuietHours.inside(100, 100, 100));
        out("the adhan inside: heard by default", !QuietHours.silencesAdhan(true, true, QuietHours.ADHAN_HEARD, 4));
        out("the adhan inside: Fajr only keeps Fajr", !QuietHours.silencesAdhan(true, true, QuietHours.ADHAN_FAJR_ONLY, 0) && QuietHours.silencesAdhan(true, true, QuietHours.ADHAN_FAJR_ONLY, 5));
        out("the adhan inside: silent", QuietHours.silencesAdhan(true, true, QuietHours.ADHAN_SILENT, 0));
        out("off, or outside: never silenced", !QuietHours.silencesAdhan(false, true, QuietHours.ADHAN_SILENT, 0) && !QuietHours.silencesAdhan(true, false, QuietHours.ADHAN_SILENT, 0));
        out("a dua inside the quiet hours does not play", DuaPlan.decide(0, 0, true, false, false, false, true, true) == DuaPlan.Go.SKIP_QUIET);
        out("outside them it plays", DuaPlan.decide(0, 0, true, false, false, false, true, false) == DuaPlan.Go.PLAY);
        out("a reason before the quiet hours is said first (standby)", DuaPlan.decide(0, 0, false, false, false, false, true, true) == DuaPlan.Go.SKIP_STANDBY);
    }
}
