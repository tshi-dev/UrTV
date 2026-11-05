package sa.urtv.home.tilawah;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/** For tests/live_feed_test.py: LiveFeed against a local server (a redirect to a one-use address, a refusal, a stream that ends, a ring that moves on). */
public final class LiveFeedRun {
    static int failed;
    static void check(String name, boolean ok, Object got) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + got));
        if (!ok) failed++;
    }

    public static void main(String[] a) throws Exception {
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = srv.getAddress().getPort();
        String base = "http://127.0.0.1:" + port;
        final AtomicInteger tokenGets = new AtomicInteger();
        final byte[] audio = new byte[300_000];
        for (int i = 0; i < audio.length; i++) audio[i] = (byte) (i * 31 + 7);
        srv.createContext("/live", x -> { x.getResponseHeaders().add("Location", base + "/n11/stream?rj-tok=1"); x.sendResponseHeaders(302, -1); x.close(); });
        srv.createContext("/n11", x -> {
            if (tokenGets.incrementAndGet() > 1) { x.sendResponseHeaders(404, -1); x.close(); return; }   // the token is spent by the first request
            x.getResponseHeaders().add("Content-Type", "audio/mpeg");
            x.sendResponseHeaders(200, audio.length);
            x.getResponseBody().write(audio);
            x.close();
        });
        srv.createContext("/gone", x -> { x.sendResponseHeaders(404, -1); x.close(); });
        srv.start();
        StreamUrl.HttpHosts local = h -> h.equals("127.0.0.1");
        try {
            LiveFeed f = new LiveFeed(base + "/live", 3000, local);
            f.start();
            byte[] buf = new byte[4096];
            int n = f.readAt(0, buf, 0, 4096, 3000);
            check("the redirect is followed and the audio read: the first bytes are the stream's", n > 0 && buf[0] == audio[0] && buf[1] == audio[1], n);
            check("the one-use address was requested once, by the feed", tokenGets.get() == 1, tokenGets.get());
            byte[] all = new byte[audio.length];
            int got = 0, r;
            while (got < all.length && (r = f.readAt(got, all, got, all.length - got, 3000)) > 0) got += r;
            check("every byte comes back in order", got == audio.length && java.util.Arrays.equals(all, audio), got);
            check("the end of a stream that ended is -1", f.readAt(audio.length, buf, 0, 10, 3000) == -1, "");
            check("the first bytes can be read again (the player's probe)", f.readAt(0, buf, 0, 4, 3000) == 4 && buf[0] == audio[0], "");
            f.close();
            // the proxy: an http stream on 127.0.0.1 of what the feed read, from its first byte
            tokenGets.set(0);
            LiveFeed pf = new LiveFeed(base + "/live", 3000, local);
            pf.start();
            LiveProxy px = LiveProxy.start(pf);
            java.net.URL pu = new java.net.URL(px.url());
            check("the proxy listens on this device only", "127.0.0.1".equals(pu.getHost()), pu.getHost());
            java.net.HttpURLConnection hc = (java.net.HttpURLConnection) pu.openConnection();
            hc.setReadTimeout(5000);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            try (java.io.InputStream is = hc.getInputStream()) { byte[] tmp = new byte[8192]; int k; while (bo.size() < audio.length && (k = is.read(tmp)) > 0) bo.write(tmp, 0, k); }
            check("the proxy answers audio/mpeg with every byte of the stream in order", "audio/mpeg".equals(hc.getContentType()) && java.util.Arrays.equals(java.util.Arrays.copyOf(bo.toByteArray(), audio.length), audio), bo.size());
            check("a second connection gets the stream from its start too (the player may sniff, then play)", tokenGets.get() == 1, tokenGets.get());
            px.close();
            LiveFeed g = new LiveFeed(base + "/gone", 3000, local);
            String err = "";
            try { g.start(); } catch (Exception e) { err = e.getMessage(); }
            check("a 404 is an error at start", err.equals("refused 404"), err);
            LiveFeed h = new LiveFeed(base + "/live", 3000, x -> false);
            err = "";
            try { h.start(); } catch (Exception e) { err = e.getMessage(); }
            check("an http step to a host that may not be reached over http is refused", err.startsWith("not allowed"), err);
        } finally {
            srv.stop(0);
        }
        System.out.println(failed == 0 ? "ALL PASS" : failed + " FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }
}
