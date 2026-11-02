package sa.urtv.home.tilawah;

/** For tests/haram_live_test.py: which links the «البث المباشر من الحرم» row may open (HaramLive.java), on a plain JVM. */
public final class HaramLiveRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("today the link is empty, so the row does not show", HaramLive.URL.isEmpty() && !HaramLive.shown(HaramLive.URL), HaramLive.URL);
        check("an https page of the broadcaster's own hosts would show",
                HaramLive.shown("https://www.aloula.sa/live/x") && HaramLive.shown("https://sba.sa/") && HaramLive.shown("https://aloula.sa")
                && HaramLive.shown("https://www.youtube.com/@channel/live") && HaramLive.shown("https://WWW.ALOULA.SA/x"), "");
        check("never: http, another host, a look-alike host, a user@host trick, empty or null",
                !HaramLive.shown("http://www.aloula.sa/live") && !HaramLive.shown("https://makkahlive.net/") && !HaramLive.shown("https://aloula.sa.example.com/")
                && !HaramLive.shown("https://notaloula.sa/") && !HaramLive.shown("https://aloula.sa@evil.example/") && !HaramLive.shown("https://")
                && !HaramLive.shown("") && !HaramLive.shown(null), "");
        if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    }
}
