package sa.urtv.home.tilawah;

/**
 * «البث المباشر من الحرم»: a LINK in «تلاوة وأذان» ← الإذاعات to the broadcaster's own live page or channel (the Saudi Broadcasting Authority's
 * Quran channel, the official Makkah feed), opened in its own app or the browser with ACTION_VIEW. Nothing is streamed, copied or re-hosted in
 * this app, and no aggregator is used. The row shows only while URL holds an https link on one of the broadcaster's hosts below: it is EMPTY
 * because no official link could be opened and confirmed when this was written (sba.sa, aloula.sa and YouTube were out of reach), so today the
 * row does not show. To turn it on, put the official page here (a link checked by hand on the broadcaster's site), on one of OFFICIAL_HOSTS.
 * Plain Java on purpose: tests/haram_live_test.py runs it on a JVM.
 */
final class HaramLive {
    private HaramLive() { }

    /** The broadcaster's own live page or channel; "" = not confirmed yet, the row stays hidden. */
    static final String URL = "";

    /** SBA's own sites and the YouTube app that holds its official channel; a link anywhere else is never shown */
    static final String[] OFFICIAL_HOSTS = {"sba.sa", "aloula.sa", "youtube.com"};

    /** May the row show this link? https only, on an official host or a subdomain of one (www.aloula.sa), nothing else. */
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
