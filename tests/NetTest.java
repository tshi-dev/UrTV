package sa.urtv.screensaver;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** For tests/net_test.py: Net (the small GET) on a plain JVM against a scripted server on the loopback. */
public final class NetTest {
    static int failed = 0;
    static void check(String w, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + w); if (!ok) failed++; }

    /** a one-connection-at-a-time server: each connection gets `script` after its request headers are read */
    static ServerSocket serve(final String name, final ScriptRunner script) throws IOException {
        final ServerSocket ss = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
        Thread t = new Thread(() -> {
            while (!ss.isClosed()) {
                try (Socket c = ss.accept()) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.ISO_8859_1));
                    StringBuilder rq = new StringBuilder(); String l;
                    while ((l = r.readLine()) != null && !l.isEmpty()) rq.append(l).append('\n');
                    script.run(c.getOutputStream(), rq.toString());
                } catch (Exception ignored) { }
            }
        }, name);
        t.setDaemon(true); t.start();
        return ss;
    }
    interface ScriptRunner { void run(OutputStream o, String request) throws Exception; }
    static void write(OutputStream o, String s) throws IOException { o.write(s.getBytes(StandardCharsets.ISO_8859_1)); o.flush(); }
    static String url(ServerSocket s) { return "http://127.0.0.1:" + s.getLocalPort() + "/a/b.json?x=1"; }

    public static void main(String[] a) throws Exception {
        InetAddress v4 = InetAddress.getByAddress(new byte[]{1, 2, 3, 4}), v4b = InetAddress.getByAddress(new byte[]{5, 6, 7, 8}), v6 = InetAddress.getByAddress(new byte[]{0x20, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1});
        List<InetAddress> o = Net.order(new InetAddress[]{v6, v4, v4b});
        check("order: IPv4 addresses first (in their order), then the others", o.get(0) == v4 && o.get(1) == v4b && o.get(2) == v6);

        ServerSocket s = serve("len", (out, rq) -> {
            write(out, "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 11\r\nX-Seen: " + (rq.contains("If-None-Match: abc") ? "yes" : "no") + "\r\n\r\nhello world");
        });
        Map<String, String> h = new HashMap<>(); h.put("If-None-Match", "abc");
        Net.Reply r = Net.get(url(s), h, 3000, 5000, 1000);
        check("a reply with Content-Length: status, body, headers (lower case), the extra request header and the Host sent", r.status == 200 && r.text().equals("hello world") && "yes".equals(r.headers.get("x-seen")) && r.headers.get("content-type") != null);
        s.close();

        s = serve("req", (out, rq) -> write(out, "HTTP/1.1 200 OK\r\nContent-Length: " + rq.length() + "\r\n\r\n" + rq));
        String echoed = Net.get(url(s), null, 3000, 5000, 4000).text();
        check("the request is a plain GET of the path and query, Host with the port, identity encoding, Connection: close", echoed.startsWith("GET /a/b.json?x=1 HTTP/1.1") && echoed.contains("Host: 127.0.0.1:" + s.getLocalPort()) && echoed.contains("Accept-Encoding: identity") && echoed.contains("Connection: close"));
        s.close();

        s = serve("chunked", (out, rq) -> write(out, "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n5\r\nhello\r\n6;ext=1\r\n world\r\n0\r\n\r\n"));
        check("a chunked reply is joined", Net.get(url(s), null, 3000, 5000, 1000).text().equals("hello world"));
        s.close();

        s = serve("eof", (out, rq) -> write(out, "HTTP/1.0 200 OK\r\n\r\nuntil the end"));
        check("a reply with no length runs to the end of the connection", Net.get(url(s), null, 3000, 5000, 1000).text().equals("until the end"));
        s.close();

        s = serve("404", (out, rq) -> write(out, "HTTP/1.1 404 Not Found\r\nContent-Length: 2\r\n\r\nno"));
        Net.Reply nf = Net.get(url(s), null, 3000, 5000, 1000);
        check("a 404 comes back as its status (the caller decides)", nf.status == 404 && nf.text().equals("no"));
        s.close();

        s = serve("302", (out, rq) -> write(out, "HTTP/1.1 302 Found\r\nLocation: http://example.invalid/x\r\nContent-Length: 0\r\n\r\n"));
        Net.Reply rd = Net.get(url(s), null, 3000, 5000, 1000);
        check("a redirect is not followed: its status and Location come back", rd.status == 302 && rd.headers.get("location") != null);
        s.close();

        s = serve("big", (out, rq) -> write(out, "HTTP/1.1 200 OK\r\nContent-Length: 5000\r\n\r\n" + "x".repeat(5000)));
        boolean big = false; try { Net.get(url(s), null, 3000, 5000, 1000); } catch (IOException e) { big = true; }
        check("a body over the cap (by its Content-Length) is an error, before it is read", big);
        s.close();
        s = serve("bigchunk", (out, rq) -> write(out, "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n" + Integer.toHexString(4000) + "\r\n" + "x".repeat(4000) + "\r\n0\r\n\r\n"));
        big = false; try { Net.get(url(s), null, 3000, 5000, 1000); } catch (IOException e) { big = true; }
        check("a chunked body over the cap is an error", big);
        s.close();
        s = serve("endless", (out, rq) -> { write(out, "HTTP/1.1 200 OK\r\n\r\n"); for (int i = 0; i < 200; i++) write(out, "x".repeat(1000)); });
        big = false; try { Net.get(url(s), null, 3000, 5000, 5000); } catch (IOException e) { big = true; }
        check("a reply with no length that never ends is stopped at the cap", big);
        s.close();

        s = serve("stall", (out, rq) -> { write(out, "HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\nabc"); Thread.sleep(20000); });
        long t0 = System.nanoTime(); boolean timed = false;
        try { Net.get(url(s), null, 15000, 2000, 1000); } catch (IOException e) { timed = true; }
        long took = (System.nanoTime() - t0) / 1_000_000;
        check("a server that stalls is given up on at the deadline (2 s here), not at the read timeout", timed && took < 4500);
        s.close();

        s = serve("junk", (out, rq) -> write(out, "not http at all\r\n\r\n"));
        boolean junk = false; try { Net.get(url(s), null, 3000, 5000, 1000); } catch (IOException e) { junk = true; }
        check("an answer that is not HTTP is an error", junk);
        s.close();

        // an address that refuses (nothing listens) is given up on at once and the call fails fast, within the deadline
        ServerSocket none = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()); int dead = none.getLocalPort(); none.close();
        t0 = System.nanoTime(); boolean refused = false;
        try { Net.get("http://127.0.0.1:" + dead + "/", null, 3000, 5000, 1000); } catch (IOException e) { refused = true; }
        check("a refused connection fails at once", refused && (System.nanoTime() - t0) / 1_000_000 < 2000);
        boolean scheme = false; try { Net.get("ftp://127.0.0.1/x", null, 1000, 1000, 10); } catch (IOException e) { scheme = true; }
        check("only http and https", scheme);
        System.exit(failed == 0 ? 0 : 1);
    }
}
