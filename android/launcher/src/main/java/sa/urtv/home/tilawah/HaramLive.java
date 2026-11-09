package sa.urtv.home.tilawah;

import java.util.ArrayList;
import java.util.List;

/**
 * «البث المباشر من الحرم»: LINKS in «تلاوة وأذان» ← الإذاعات to the Saudi Broadcasting Authority's own YouTube channels, live: Makkah on the Quran
 * channel (@SaudiQuranTv), Madinah on the Sunnah channel (@SaudiSunnahTv), both linked from sba.sa's own footer («القنوات التلفزيونية»). The
 * channel's /live address follows whatever it broadcasts now (a video id would change). Nothing is streamed, copied or re-hosted in this app:
 * OK opens the link in the installed YouTube app, else in SmartTube, else SBA's own Aloula page in the browser (ACTION_VIEW). A row shows only while its link is
 * https on one of OFFICIAL_HOSTS. Plain Java on purpose: tests/haram_live_test.py runs it on a JVM.
 */
final class HaramLive {
    private HaramLive() { }

    /** The two live channels, in the rows' order: [the title (Arabic, i18n/en/home.tsv), the official /live page]. */
    static final String[][] LIVE = {
        {"مكة المكرمة · قناة القرآن الكريم", "https://www.youtube.com/@SaudiQuranTv/live"},
        {"المدينة المنورة · قناة السنة النبوية", "https://www.youtube.com/@SaudiSunnahTv/live"},
    };

    /**
     * 4.3: the same two channels on SBA's own platform (Aloula, «البث المباشر»), for the browser when neither YouTube nor SmartTube is on the TV: its
     * page plays in a TV browser, where youtube.com does not. Aloula's channel list (read by .github/tools/aloula_probe.py in CI) gives no stream
     * address of its own (its player asks for one each time), so nothing is played inside our app.
     */
    static final String[] PAGES = {"https://aloula.sba.sa/live/quran", "https://aloula.sba.sa/live/sunna"};

    /** The address for one way of opening: an app (YouTube, SmartTube) gets the YouTube /live page, the browser (null) gets Aloula's own page. */
    static String linkFor(int which, String pkg) { return pkg == null ? PAGES[which] : LIVE[which][1]; }

    /** SBA's own sites and the YouTube app that holds its official channels; a link anywhere else is never shown */
    static final String[] OFFICIAL_HOSTS = {"sba.sa", "aloula.sa", "youtube.com"};

    /** The apps tried, in order: YouTube for Android TV, then SmartTube (stable, then beta); null at the end = the browser (any app for the link). */
    static final String YOUTUBE = "com.google.android.youtube.tv", SMARTTUBE = "org.smarttube.stable", SMARTTUBE_BETA = "org.smarttube.beta";

    interface Installed { boolean has(String pkg); }

    /** Which app gets the link first, then next if it refuses: the installed ones of YouTube and SmartTube, then null (no package: the browser). */
    static List<String> openOrder(Installed installed) {
        List<String> out = new ArrayList<>();
        for (String p : new String[] {YOUTUBE, SMARTTUBE, SMARTTUBE_BETA}) if (installed != null && installed.has(p)) out.add(p);
        out.add(null);
        return out;
    }

    /** May the row show this link? https only, on an official host or a subdomain of one (www.youtube.com), nothing else. */
    static boolean shown(String url) {
        if (url == null || !url.startsWith("https://")) return false;
        String rest = url.substring("https://".length());
        int end = rest.length();
        for (char c : new char[] {'/', '?', '#', ':'}) { int i = rest.indexOf(c); if (i >= 0 && i < end) end = i; }
        String host = rest.substring(0, end).toLowerCase(java.util.Locale.ROOT);
        if (host.isEmpty() || rest.substring(0, end).contains("@")) return false;
        for (String h : OFFICIAL_HOSTS) if (host.equals(h) || host.endsWith("." + h)) return true;
        return false;
    }
}
