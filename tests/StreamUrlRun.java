package sa.urtv.home.tilawah;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/** For tests/stream_url_test.py: StreamUrl against a local server (redirects, a 5xx, a step to a host that may not be reached over http). */
public final class StreamUrlRun {
    static int failed;
    static void check(String name, boolean ok, Object got) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + got));
        if (!ok) failed++;
    }
    static String tryResolve(String url, StreamUrl.HttpHosts h) {
        try { return StreamUrl.resolve(url, 3000, h); } catch (Exception e) { return "ERR " + e.getMessage(); }
    }

    public static void main(String[] a) throws Exception {
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = srv.getAddress().getPort();
        String base = "http://127.0.0.1:" + port;
        final AtomicInteger gets = new AtomicInteger();
        srv.createContext("/live", x -> { x.getResponseHeaders().add("Location", base + "/n05/stream?token=1"); x.sendResponseHeaders(302, -1); x.close(); });
        srv.createContext("/rel", x -> { x.getResponseHeaders().add("Location", "/n05/stream"); x.sendResponseHeaders(301, -1); x.close(); });
        srv.createContext("/n05", x -> { gets.incrementAndGet(); x.getResponseHeaders().add("Content-Type", "audio/mpeg"); x.sendResponseHeaders(200, 0); x.getResponseBody().write(new byte[1024]); x.close(); });
        srv.createContext("/busy", x -> { byte[] b = new byte[177]; x.sendResponseHeaders(500, b.length); x.getResponseBody().write(b); x.close(); });
        srv.createContext("/gone", x -> { x.sendResponseHeaders(404, -1); x.close(); });
        srv.createContext("/loop", x -> { x.getResponseHeaders().add("Location", base + "/loop"); x.sendResponseHeaders(302, -1); x.close(); });
        srv.createContext("/away", x -> { x.getResponseHeaders().add("Location", "http://localhost:" + port + "/n05/x"); x.sendResponseHeaders(302, -1); x.close(); });
        srv.start();
        final boolean[] flip = {false};
        StreamUrl.HttpHosts alternate = h -> { flip[0] = !flip[0]; return flip[0]; };   // true for the scheme check, false for "hand over unasked": the loop is followed
        StreamUrl.HttpHosts local = h -> h.equals("127.0.0.1"), app = StreamUrl::cleartextAllowed;
        try {
            check("a 302 is followed and the player gets the final address (radiojar's https -> http one)", (base + "/n05/stream?token=1").equals(tryResolve(base + "/live", local)), tryResolve(base + "/live", local));
            check("an allowed http step is handed over without our own request (a single-use token stays unused)", gets.get() == 0, gets.get());
            check("a relative Location is read against the address that gave it", (base + "/n05/stream").equals(tryResolve(base + "/rel", local)), tryResolve(base + "/rel", local));
            check("a 500 (qurango's 177 bytes) is a passing server error: tried again", tryResolve(base + "/busy", local).equals("ERR server error 500"), tryResolve(base + "/busy", local));
            check("a 404 is not tried again", tryResolve(base + "/gone", local).equals("ERR refused 404"), tryResolve(base + "/gone", local));
            check("redirects that never end stop after " + StreamUrl.MAX_REDIRECTS, tryResolve(base + "/loop", alternate).equals("ERR too many redirects"), tryResolve(base + "/loop", alternate));
            check("a redirect to plain http on a host the app may not reach that way is refused", tryResolve(base + "/away", local).startsWith("ERR not allowed: http://localhost"), tryResolve(base + "/away", local));
            check("plain http only for the hosts network_security_config allows (radiojar.com and its subdomains)", StreamUrl.cleartextAllowed("n05.radiojar.com") && StreamUrl.cleartextAllowed("radiojar.com")
                && !StreamUrl.cleartextAllowed("evilradiojar.com") && !StreamUrl.cleartextAllowed("backup.qurango.net") && !StreamUrl.cleartextAllowed(null), "");
            check("the app's own rule refuses plain http to the test server", tryResolve(base + "/n05", app).startsWith("ERR not allowed"), "");
            check("ftp and others are never followed", tryResolve("ftp://127.0.0.1/x", local).startsWith("ERR not allowed"), tryResolve("ftp://127.0.0.1/x", local));
            check("three tries in all, with 2 s then 4 s between them (8 s at most)", StreamUrl.again(1) && StreamUrl.again(2) && !StreamUrl.again(3)
                && StreamUrl.backoffMs(1) == 2000 && StreamUrl.backoffMs(2) == 4000 && StreamUrl.backoffMs(3) == 8000 && StreamUrl.backoffMs(9) == 8000, StreamUrl.backoffMs(1) + " " + StreamUrl.backoffMs(2));
            check("only a 5xx is passing", StreamUrl.passing(500) && StreamUrl.passing(503) && !StreamUrl.passing(404) && !StreamUrl.passing(200), "");
        } finally {
            srv.stop(0);
        }
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
