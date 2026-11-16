package sa.urtv.screensaver;

import java.util.Map;
import java.util.TreeMap;

/**
 * G3 (4.3.3): how much this app asks the network, for the tester (no Android in it: tests/netlog_test.py runs it on a plain JVM). Every request the app or its page makes is
 * counted by host (the WebView's requests in shouldInterceptRequest, Net, the photo downloads); when a request comes {@link #WINDOW_MS} or more after the window began, one
 * line goes to the log (tag UrTVNet) with the window's minutes, its requests by host and, from the Android side, the bytes the app received and sent in it (TrafficStats).
 * Nothing is written while nothing is asked, so a quiet TV writes no line: the next line covers the quiet time too (its minutes say how long). Local hosts (.invalid: the
 * page's own photos and packs served from the disk) and file/data URLs are not the network and are not counted. Both apps carry the same file (the launcher's is in sa.urtv.home).
 */
public final class NetLog {
    private NetLog() { }

    public static final long WINDOW_MS = 10 * 60_000L;

    /** told one finished window (the Android side adds the bytes and writes the line) */
    public interface Logger { void window(long minutes, int requests, String byHost); }
    public static volatile Logger logger;

    private static long start = -1;
    private static int total;
    private static final Map<String, Integer> HOSTS = new TreeMap<>();

    /** the host of a network URL, or null for one that is not the network (file:, data:, blob:, a local .invalid host) */
    static String host(String url) {
        if (url == null) return null;
        int s = url.indexOf("://");
        if (s <= 0) return null;
        String scheme = url.substring(0, s).toLowerCase(java.util.Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) return null;
        int e = s + 3;
        while (e < url.length() && "/?#".indexOf(url.charAt(e)) < 0) e++;
        String h = url.substring(s + 3, e);
        int at = h.lastIndexOf('@');
        if (at >= 0) h = h.substring(at + 1);
        int colon = h.lastIndexOf(':');
        if (colon > 0 && h.indexOf(']') < colon) h = h.substring(0, colon);
        h = h.toLowerCase(java.util.Locale.ROOT);
        return h.isEmpty() || h.endsWith(".invalid") ? null : h;
    }

    /** one request to url, now; the window's line when it is due */
    public static void count(String url) { count(url, System.currentTimeMillis()); }

    static void count(String url, long now) {
        String h = host(url);
        if (h == null) return;
        String[] done = null;
        synchronized (HOSTS) {
            if (start < 0 || now < start) start = now;
            else if (now - start >= WINDOW_MS) {
                done = new String[] {String.valueOf((now - start) / 60_000L), String.valueOf(total), byHost()};
                start = now; total = 0; HOSTS.clear();
            }
            total++;
            Integer n = HOSTS.get(h);
            HOSTS.put(h, n == null ? 1 : n + 1);
        }
        Logger l = logger;
        if (done != null && l != null) {
            try { l.window(Long.parseLong(done[0]), Integer.parseInt(done[1]), done[2]); } catch (RuntimeException ignored) { }
        }
    }

    /** "host 12, other 3", the most asked first */
    private static String byHost() {
        java.util.List<Map.Entry<String, Integer>> e = new java.util.ArrayList<>(HOSTS.entrySet());
        java.util.Collections.sort(e, (a, b) -> b.getValue() - a.getValue());
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, Integer> x : e) { if (s.length() > 0) s.append(", "); s.append(x.getKey()).append(' ').append(x.getValue()); }
        return s.toString();
    }

    /** the line as the log shows it (bytes from the Android side; -1 = not known) */
    public static String line(long minutes, int requests, String byHost, long rxBytes, long txBytes) {
        String per = minutes > 0 ? String.format(java.util.Locale.ROOT, "%.1f", requests / (double) minutes) : String.valueOf(requests);
        return "net " + minutes + " min: " + requests + " requests (" + per + "/min)" + (rxBytes >= 0 ? ", in " + kb(rxBytes) + ", out " + kb(txBytes) : "") + (byHost.isEmpty() ? "" : "; " + byHost);
    }

    private static String kb(long b) { return b >= 1L << 20 ? String.format(java.util.Locale.ROOT, "%.1f MB", b / 1048576.0) : b >= 1024 ? (b >> 10) + " KB" : b + " B"; }

    /** a test: the window starts again */
    static void reset() { synchronized (HOSTS) { start = -1; total = 0; HOSTS.clear(); } }
}
