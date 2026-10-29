package sa.urtv.home.tilawah;

import java.util.Arrays;

/**
 * For tests/place_choice_test.py: the place of the adhan alarm when the screensaver's settings cannot be read (the TV kills its process and
 * refuses to start it from the background). A failed read must never change the scheduled times for a place that is not the bundled one.
 */
public final class PlaceChoiceRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    static final String CAIRO = "city=%D8%A7%D9%84%D9%82%D8%A7%D9%87%D8%B1%D8%A9&lat=30.0444&lon=31.2357&tz=Africa%2FCairo&adj=2,0,0,0,0&digits=latin";
    static final String KUWAIT = "city=x&lat=29.3759&lon=47.9774&tz=Asia%2FKuwait";
    static final String RIYADH = "city=r&lat=24.6877&lon=46.7219&tz=Asia%2FRiyadh";

    /** what Place makes of a pick: the times of a day and the alarm identity; null = the bundled place (nothing to use) */
    static Object[] place(PlaceChoice.Pick p) {
        if (p.fragment == null) return null;
        PlaceFragment f = PlaceFragment.parse(p.fragment);
        int method = PrayerMath.methodFor(f.tz, f.pm);
        long[] t = PrayerMath.compute(2026, 10, 29, f.lat, f.lng, f.tz, false, method);
        long[] at = t.clone();
        PrayerAdjust.shift(at, f.adj);
        return new Object[] {Arrays.toString(at), PrayerMath.identity(f.lat, f.lng, f.tz, PrayerAdjust.text(f.adj), method)};
    }

    public static void main(String[] args) {
        // the screensaver answered, then could not be read: the place is the last good one and the times do not move
        Object[] good = place(PlaceChoice.pick(CAIRO, null));
        PlaceChoice.Pick failedRead = PlaceChoice.pick(null, CAIRO);
        Object[] after = place(failedRead);
        check("a fresh read is used and is not the remembered one", !PlaceChoice.pick(CAIRO, KUWAIT).remembered && CAIRO.equals(PlaceChoice.pick(CAIRO, KUWAIT).fragment));
        check("a failed read uses the last good place, said to be the remembered one", failedRead.remembered && CAIRO.equals(failedRead.fragment));
        check("the remembered place makes the same times as the fresh one (Cairo with its own adjustment); that a failed read never moves the alarm is PlaceMemoryRun's device", Arrays.equals(good, after));
        Object[] kuwait = place(PlaceChoice.pick(KUWAIT, null)), kuwaitFailed = place(PlaceChoice.pick(null, KUWAIT));
        check("the same for Kuwait", Arrays.equals(kuwait, kuwaitFailed));
        Object[] riyadh = place(PlaceChoice.pick(RIYADH, null));
        check("the bundled place's times are not Cairo's or Kuwait's (so the old fallback WAS wrong for them)", !riyadh[0].equals(good[0]) && !riyadh[0].equals(kuwait[0]) && !riyadh[1].equals(good[1]));
        check("a fragment without coordinates is no answer: the last good place stays", PlaceChoice.pick("city=x&greeting=1", CAIRO).remembered && PlaceChoice.pick("", CAIRO).remembered && PlaceChoice.pick("lat=1", CAIRO).remembered);
        check("a new city read after a failure replaces the remembered one", !PlaceChoice.pick(KUWAIT, CAIRO).remembered && KUWAIT.equals(PlaceChoice.pick(KUWAIT, CAIRO).fragment));
        PlaceChoice.Pick none = PlaceChoice.pick(null, null);
        check("the very first install with nothing read and no provider has no fragment: Place uses the bundled place", none.fragment == null && !none.remembered && PlaceChoice.pick("", null).fragment == null);
        check("the adjustment of the fragment is kept in the remembered place (-20..20 minutes)", PlaceFragment.parse(CAIRO).adj[0] == 2 && PlaceFragment.parse(KUWAIT).adj[0] == 0);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
