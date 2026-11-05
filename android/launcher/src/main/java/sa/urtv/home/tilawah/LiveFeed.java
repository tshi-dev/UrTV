package sa.urtv.home.tilawah;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A live station read by us, not by the platform's player (tester step 32: radiojar's Cairo station gave nothing to MediaPlayer.setDataSource(url) from either the
 * resolved http address or the https original). We make the one GET ourselves, redirects included (an https -> http step only to a host the app allows cleartext for),
 * so a single-use token is spent by the request that reads the audio, and keep the last KEEP bytes in a ring that the player reads through a MediaDataSource
 * (LiveSource). Plain Java: tests/live_feed_test.py runs it against a local server.
 */
final class LiveFeed {
    static final int KEEP = 1 << 20, MAX_REDIRECTS = 5;

    private final byte[] ring = new byte[KEEP];
    private long end;                 // bytes received so far (the ring holds the last KEEP of them)
    private boolean done;
    private IOException failure;
    private volatile boolean closed;
    private HttpURLConnection conn;
    private final String address;
    private final int timeoutMs;
    private final StreamUrl.HttpHosts http;

    LiveFeed(String address, int timeoutMs, StreamUrl.HttpHosts http) {
        this.address = address;
        this.timeoutMs = timeoutMs;
        this.http = http;
    }

    /** Opens the stream (follows the redirects) and starts reading it on a thread of its own; an IOException when it does not answer 2xx. */
    void start() throws IOException {
        String now = address;
        InputStream in = null;
        for (int hop = 0; hop <= MAX_REDIRECTS && in == null; hop++) {
            URL u = new URL(now);
            String scheme = u.getProtocol();
            if (!scheme.equals("https") && !(scheme.equals("http") && http.allowed(u.getHost()))) throw new IOException("not allowed: " + scheme + "://" + u.getHost());
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(timeoutMs);
            c.setReadTimeout(timeoutMs);
            c.setRequestProperty("Icy-MetaData", "0");
            c.setRequestProperty("Accept-Encoding", "identity");
            int status = c.getResponseCode();
            if (status >= 300 && status < 400) {
                String to = c.getHeaderField("Location");
                c.disconnect();
                if (to == null || to.isEmpty()) throw new IOException("redirect without a Location");
                now = new URL(u, to).toString();
                continue;
            }
            if (status < 200 || status >= 300) { c.disconnect(); throw new IOException("refused " + status); }
            conn = c;
            in = c.getInputStream();
        }
        if (in == null) throw new IOException("too many redirects");
        final InputStream src = in;
        Thread t = new Thread(() -> pump(src), "live-feed");
        t.setDaemon(true);
        t.start();
    }

    private void pump(InputStream in) {
        byte[] chunk = new byte[16 * 1024];
        try {
            int n;
            while (!closed && (n = in.read(chunk)) >= 0) {
                synchronized (this) {
                    for (int i = 0; i < n; i++) ring[(int) ((end + i) % KEEP)] = chunk[i];
                    end += n;
                    notifyAll();
                }
            }
        } catch (IOException e) {
            synchronized (this) { if (!closed) failure = e; }
        } finally {
            synchronized (this) { done = true; notifyAll(); }
            try { in.close(); } catch (IOException ignored) { }
        }
    }

    /**
     * Reads up to size bytes at position (a position still in the ring: the player's probe may read the first bytes again). Waits for data that has not come yet, at most
     * waitMs; -1 at the end of a stream that ended, or for a position that left the ring; an IOException when the stream failed or nothing came in time.
     */
    synchronized int readAt(long position, byte[] buf, int off, int size, long waitMs) throws IOException {
        long until = System.currentTimeMillis() + waitMs;
        while (position >= end) {
            if (failure != null) throw failure;
            if (done || closed) return -1;
            long left = until - System.currentTimeMillis();
            if (left <= 0) throw new IOException("no data in " + waitMs + " ms");
            try { wait(left); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("interrupted"); }
        }
        if (position < end - KEEP) return -1;
        int n = (int) Math.min(size, end - position);
        for (int i = 0; i < n; i++) buf[off + i] = ring[(int) ((position + i) % KEEP)];
        return n;
    }

    synchronized void close() {
        closed = true;
        notifyAll();
        if (conn != null) conn.disconnect();
    }
}
