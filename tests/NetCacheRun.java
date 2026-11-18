package sa.urtv.screensaver;

import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** G3: Net.cached on a plain JVM against a local server (tests/net_cache_test.py): ETag and date revalidation, merged calls, the back-off, the cap. */
public final class NetCacheRun {
    static int failed;
    static void check(String what, boolean ok, Object got) { System.out.println((ok ? "PASS " : "FAIL ") + what + (ok ? "" : "  -> " + got)); if (!ok) failed++; }

    public static void main(String[] a) throws Exception {
        final AtomicInteger asked = new AtomicInteger(), full = new AtomicInteger();
        final String[] lastInm = {null}, lastIms = {null};
        final String[] body = {"{\"v\":1}"};
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/etag", x -> {
            asked.incrementAndGet();
            lastInm[0] = x.getRequestHeaders().getFirst("If-None-Match");
            String tag = "\"" + body[0].hashCode() + "\"";
            if (tag.equals(lastInm[0])) { x.sendResponseHeaders(304, -1); x.close(); return; }
            full.incrementAndGet();
            byte[] b = body[0].getBytes(StandardCharsets.UTF_8);
            x.getResponseHeaders().add("ETag", tag);
            x.sendResponseHeaders(200, b.length); x.getResponseBody().write(b); x.close();
        });
        s.createContext("/date", x -> {
            asked.incrementAndGet();
            lastIms[0] = x.getRequestHeaders().getFirst("If-Modified-Since");
            if ("Mon, 16 Nov 2026 05:00:00 GMT".equals(lastIms[0])) { x.sendResponseHeaders(304, -1); x.close(); return; }
            byte[] b = "dated".getBytes(StandardCharsets.UTF_8);
            x.getResponseHeaders().add("Last-Modified", "Mon, 16 Nov 2026 05:00:00 GMT");
            x.sendResponseHeaders(200, b.length); x.getResponseBody().write(b); x.close();
        });
        s.createContext("/slow", x -> {
            asked.incrementAndGet();
            try { Thread.sleep(400); } catch (InterruptedException ignored) { }
            byte[] b = "slow".getBytes(StandardCharsets.UTF_8);
            x.sendResponseHeaders(200, b.length); x.getResponseBody().write(b); x.close();
        });
        s.createContext("/gone", x -> { asked.incrementAndGet(); x.sendResponseHeaders(404, -1); x.close(); });
        s.start();
        String base = "http://127.0.0.1:" + s.getAddress().getPort();
        File dir = java.nio.file.Files.createTempDirectory("netcache").toFile();
        try {
            Net.Reply r1 = Net.cached(dir, base + "/etag", 3000, 5000, 1 << 20, 0, true);
            check("the first call: the whole file, kept", r1.status == 200 && r1.text().equals(body[0]) && lastInm[0] == null && full.get() == 1, r1.status + " " + lastInm[0]);
            Thread.sleep(Net.MERGE_MS + 200);
            Net.Reply r2 = Net.cached(dir, base + "/etag", 3000, 5000, 1 << 20, 0, true);
            check("the next call sends the copy's ETag; the 304 hands back the copy as a 200, no body sent", r2.status == 200 && r2.text().equals(body[0]) && lastInm[0] != null && full.get() == 1 && "304".equals(r2.headers.get("x-urtv-cache")), r2.headers + " " + full.get());
            body[0] = "{\"v\":2}";
            Thread.sleep(Net.MERGE_MS + 200);
            Net.Reply r3 = Net.cached(dir, base + "/etag", 3000, 5000, 1 << 20, 0, true);
            check("a changed file comes whole and replaces the copy", r3.status == 200 && r3.text().equals("{\"v\":2}") && full.get() == 2, r3.text());
            int before = asked.get();
            Net.Reply r4 = Net.cached(dir, base + "/etag", 3000, 5000, 1 << 20, 60_000, true);
            check("a copy younger than freshMs: no request", asked.get() == before && "fresh".equals(r4.headers.get("x-urtv-cache")) && r4.text().equals("{\"v\":2}"), asked.get() - before);

            Net.cached(dir, base + "/date", 3000, 5000, 1 << 20, 0, true);
            Thread.sleep(Net.MERGE_MS + 200);
            Net.Reply d2 = Net.cached(dir, base + "/date", 3000, 5000, 1 << 20, 0, true);
            check("no ETag: the copy's date goes as If-Modified-Since and the 304 hands back the copy", "Mon, 16 Nov 2026 05:00:00 GMT".equals(lastIms[0]) && d2.text().equals("dated") && "304".equals(d2.headers.get("x-urtv-cache")), lastIms[0]);

            before = asked.get();
            Thread[] t = new Thread[4];
            final String[] got = new String[4];
            for (int i = 0; i < t.length; i++) { final int k = i; t[i] = new Thread(() -> { try { got[k] = Net.cached(dir, base + "/slow", 3000, 5000, 1 << 20, 0, true).text(); } catch (Exception e) { got[k] = e.toString(); } }); t[i].start(); }
            for (Thread x : t) x.join();
            check("four calls at once for the same file: one request, the same answer for all", asked.get() - before == 1 && java.util.Arrays.stream(got).allMatch("slow"::equals), (asked.get() - before) + " " + java.util.Arrays.toString(got));

            before = asked.get();
            Net.Reply g = Net.cached(dir, base + "/gone", 3000, 5000, 1 << 20, 60_000, true);
            Net.Reply g2 = Net.cached(dir, base + "/gone", 3000, 5000, 1 << 20, 60_000, true);
            check("a 404 is handed back as it is and keeps no copy (asked again next time)", g.status == 404 && g2.status == 404 && asked.get() - before == 2, g.status + " " + (asked.get() - before));
        } finally {
            s.stop(0);
        }

        // the back-off: a host that refuses
        java.net.ServerSocket ss = new java.net.ServerSocket(0);
        int dead = ss.getLocalPort();
        ss.close();
        Net.forgetBackOff();
        String off = "http://127.0.0.1:" + dead + "/x";
        String e1 = "", e2 = "", e3 = "";
        try { Net.cached(dir, off, 1000, 2000, 1024, 0, true); } catch (java.io.IOException e) { e1 = String.valueOf(e.getMessage()); }
        try { Net.cached(dir, off, 1000, 2000, 1024, 0, true); } catch (java.io.IOException e) { e2 = String.valueOf(e.getMessage()); }
        try { Net.cached(dir, off, 1000, 2000, 1024, 0, false); } catch (java.io.IOException e) { e3 = String.valueOf(e.getMessage()); }
        check("offline: the first call fails, the next background call fails at once with «back-off»", !e1.equals("back-off") && e2.equals("back-off"), e1 + " | " + e2);
        check("a call the person waits for is tried all the same", !e3.equals("back-off") && !e3.isEmpty(), e3);
        Net.forgetBackOff();

        // the cap
        File capDir = java.nio.file.Files.createTempDirectory("netcap").toFile();
        for (int i = 0; i < 5; i++) {
            File f = new File(capDir, "k" + i + ".body");
            java.nio.file.Files.write(f.toPath(), new byte[1000]);
            java.nio.file.Files.write(new File(capDir, "k" + i + ".meta").toPath(), "\n\n0\n".getBytes(StandardCharsets.UTF_8));
            f.setLastModified(1_000_000L + i * 1000L);
        }
        Net.prune(capDir, 3000);
        check("over the cap: the copies used longest ago go first, each with its meta", !new File(capDir, "k0.body").exists() && !new File(capDir, "k1.meta").exists() && new File(capDir, "k2.body").exists() && new File(capDir, "k4.body").exists(), java.util.Arrays.toString(capDir.list()));
        System.exit(failed == 0 ? 0 : 1);
    }
}
