package sa.urtv.home.tilawah;

/** For tests/haram_live_test.py: which links the «البث المباشر من الحرم» row may open (HaramLive.java), on a plain JVM. */
public final class HaramLiveRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("two live channels, Makkah then Madinah, both shown (https on youtube.com)", HaramLive.LIVE.length == 2
                && HaramLive.LIVE[0][1].equals("https://www.youtube.com/@SaudiQuranTv/live") && HaramLive.LIVE[1][1].equals("https://www.youtube.com/@SaudiSunnahTv/live")
                && HaramLive.shown(HaramLive.LIVE[0][1]) && HaramLive.shown(HaramLive.LIVE[1][1]), "");
        java.util.Set<String> all = new java.util.HashSet<>(java.util.Arrays.asList(HaramLive.YOUTUBE, HaramLive.SMARTTUBE, HaramLive.SMARTTUBE_BETA));
        check("opened in YouTube, then SmartTube, then the browser (null at the end)", java.util.Arrays.asList(HaramLive.YOUTUBE, HaramLive.SMARTTUBE, HaramLive.SMARTTUBE_BETA, null).equals(HaramLive.openOrder(all::contains)), "" + HaramLive.openOrder(all::contains));
        check("4.3: the browser gets SBA's own Aloula page (Makkah: quran, Madinah: sunna), an app the YouTube /live page; both may be shown",
                HaramLive.linkFor(0, null).equals("https://aloula.sba.sa/live/quran") && HaramLive.linkFor(1, null).equals("https://aloula.sba.sa/live/sunna")
                && HaramLive.linkFor(0, HaramLive.YOUTUBE).equals(HaramLive.LIVE[0][1]) && HaramLive.linkFor(1, HaramLive.SMARTTUBE).equals(HaramLive.LIVE[1][1])
                && HaramLive.shown(HaramLive.PAGES[0]) && HaramLive.shown(HaramLive.PAGES[1]), "");
        check("no YouTube: SmartTube first; neither: the browser alone", java.util.Arrays.asList(HaramLive.SMARTTUBE, null).equals(HaramLive.openOrder(HaramLive.SMARTTUBE::equals))
                && java.util.Collections.singletonList((String) null).equals(HaramLive.openOrder(p -> false)) && java.util.Collections.singletonList((String) null).equals(HaramLive.openOrder(null)), "");
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
