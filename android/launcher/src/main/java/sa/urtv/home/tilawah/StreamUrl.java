package sa.urtv.home.tilawah;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * A live station's address, made ready for the player before it gets it (tester step 19, the owner's TCL):
 * - radiojar (the Saudi Quran Radio) answers https://stream.radiojar.com/… with a 302 to plain http://n0x.radiojar.com/…, and the TV's player
 *   never followed it (buffering for ever, no error). We follow the redirects ourselves and hand the player the final address. An https -> http
 *   step is taken only to a host the app allows cleartext for (network_security_config.xml: radiojar.com); any other is refused.
 * - backup.qurango.net answers 500 (177 bytes) at random. A 5xx is an error here, so the player's caller tries again after a pause that grows
 *   (backoffMs), and says «تعذّر» only after the last try.
 * - that final http address is handed to the player without being requested by us (its token may be single use).
 * Plain Java (java.net): tests/stream_url_test.py runs it against a local server.
 */
final class StreamUrl {
    private StreamUrl() { }

    static final int MAX_REDIRECTS = 5;
    /** tries in all for a station (the first and two more), with a growing wait between them */
    static final int TRIES = 3;

    /** The hosts network_security_config.xml lets the app reach over plain http (and so the only ones an https -> http redirect may go to). */
    static boolean cleartextAllowed(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        return h.equals("radiojar.com") || h.endsWith(".radiojar.com");
    }

    /** The wait before try n+1 (n = tries made so far, from 1): 2 s, then 4 s, then 8 s at most. */
    static long backoffMs(int triesMade) { return 2000L << Math.max(0, Math.min(2, triesMade - 1)); }

    /** True when one more try is allowed after this many failed ones. */
    static boolean again(int triesMade) { return triesMade < TRIES; }

    /** The status that is a passing server failure (tried again), not a wrong address. */
    static boolean passing(int status) { return status >= 500 && status <= 599; }

    /**
     * Follows the redirects of a GET (headers only: the body is never read), and returns the address that answers 2xx. IOException for a 5xx
     * (passing: try again), another 4xx, too many redirects, or a step to http that is not allowed. Only http and https.
     */
    static String resolve(String address, int timeoutMs) throws IOException { return resolve(address, timeoutMs, StreamUrl::cleartextAllowed); }

    /** Which hosts plain http may reach (the app's: cleartextAllowed; the test's: its local server). */
    interface HttpHosts { boolean allowed(String host); }

    static String resolve(String address, int timeoutMs, HttpHosts http) throws IOException {
        String now = address;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            URL u = new URL(now);
            String scheme = u.getProtocol();
            if (!scheme.equals("https") && !(scheme.equals("http") && http.allowed(u.getHost()))) throw new IOException("not allowed: " + scheme + "://" + u.getHost());
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            try {
                c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(timeoutMs);
                c.setReadTimeout(timeoutMs);
                c.setRequestProperty("Icy-MetaData", "0");
                int status = c.getResponseCode();
                if (status >= 300 && status < 400) {
                    String to = c.getHeaderField("Location");
                    if (to == null || to.isEmpty()) throw new IOException("redirect without a Location");
                    now = new URL(u, to).toString();
                    // radiojar's http://nNN.radiojar.com/…?rj-tok=… address carries a token that one request can use up: asking it here (a GET) left
                    // the player's own request with nothing (buffering for ever, tester step 19/GO 178). An allowed cleartext step is handed over unasked.
                    URL next = new URL(now);
                    if (next.getProtocol().equals("http") && http.allowed(next.getHost())) return now;
                    continue;
                }
                if (status >= 200 && status < 300) return now;
                throw new IOException((passing(status) ? "server error " : "refused ") + status);
            } finally {
                c.disconnect();
            }
        }
        throw new IOException("too many redirects");
    }
}
