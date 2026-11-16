package sa.urtv.screensaver;

import java.util.Arrays;

/** tests/prayer_fix_test.py: PrayerFix on a plain JVM */
public class PrayerFixRun {
    public static void main(String[] a) {
        String doc = "{\"v\":1,\"adjustments\":["
            + "{\"id\":\"sa-isha\",\"where\":{\"country\":\"SA\"},\"from\":\"2027-02-08\",\"to\":\"2027-03-09\",\"minutes\":{\"isha\":5,\"fajr\":-2},\"source\":\"x\"},"
            + "{\"id\":\"sa-riyadh\",\"where\":{\"country\":\"SA\",\"city\":\"الرياض\"},\"from\":\"2027-02-10\",\"to\":\"2027-02-12\",\"minutes\":{\"isha\":3},\"source\":\"x\"},"
            + "{\"id\":\"big\",\"where\":{\"country\":\"KW\"},\"from\":\"2027-01-01\",\"to\":\"2027-12-31\",\"minutes\":{\"maghrib\":45,\"sunrise\":4,\"asr\":1.5},\"source\":\"x\"}]}";
        Object d = MiniJson.parse(doc);
        p("COUNTRY", PrayerFix.forDay(d, "SA", "جدة", "2027-02-08"));
        p("CITY", PrayerFix.forDay(d, "SA", "الرياض", "2027-02-11"));
        p("BEFORE", PrayerFix.forDay(d, "SA", "جدة", "2027-02-07"));
        p("LASTDAY", PrayerFix.forDay(d, "sa", "جدة", "2027-03-09"));
        p("OTHER", PrayerFix.forDay(d, "AE", "دبي", "2027-02-11"));
        p("CLAMP", PrayerFix.forDay(d, "KW", "الكويت", "2027-05-01"));
        p("NODOC", PrayerFix.forDay(null, "SA", "جدة", "2027-02-11"));
        p("BADDOC", PrayerFix.forDay(MiniJson.parse("{\"v\":1,\"adjustments\":7}"), "SA", "جدة", "2027-02-11"));
        int[] own = {0, 0, 0, 0, 2}, fix = {-2, 0, 0, 0, 5};
        p("OWNON", PrayerFix.combine(true, own, fix));
        p("OWNOFF", PrayerFix.combine(false, own, fix));
        System.out.println("APPLIEDON " + PrayerFix.applied(true, own, fix));
        System.out.println("APPLIEDNONE " + PrayerFix.applied(true, new int[] {-2, 0, 0, 0, 5}, fix).isEmpty());
        System.out.println("FRAG " + PrayerAdj.fragment(false || PrayerAdj.any(fix), PrayerFix.combine(false, new int[5], fix)));
        System.out.println("FRAGNONE [" + PrayerAdj.fragment(false || PrayerAdj.any(new int[5]), PrayerFix.combine(false, new int[5], new int[5])) + "]");
    }

    static void p(String k, int[] v) { System.out.println(k + " " + Arrays.toString(v)); }
}
