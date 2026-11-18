package sa.urtv.home;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One small GET for the app's own data files (no Android in it: tests/net_test.py runs it on a plain JVM). Why it exists: HttpURLConnection tries each address of a
 * host in turn with the WHOLE connect timeout, IPv6 first, so a TV whose IPv6 goes nowhere waited 4 x 12 s for raw.githubusercontent.com (the tester's 54 s for the leagues
 * list). Here: every address is tried IPv4 first, 4 s to connect to each, and the whole call has a deadline (the last address gets what is left of it); https is the
 * platform's own TLS over the socket we connected, the certificate checked against the host name as HttpsURLConnection does. A reply is read to a size cap (a body over
 * it is an error), plain or chunked. It follows no redirect (a 3xx comes back as the status) and sends nothing but what is given.
 * Both apps carry the same file (the launcher's is in sa.urtv.home).
 */
final class Net {
    private Net() {}

    static final int CONNECT_MS = 4000;

    static final class Reply {
        final int status; final byte[] body; final Map<String, String> headers;   // header names in lower case
        Reply(int status, byte[] body, Map<String, String> headers) { this.status = status; this.body = body; this.headers = headers; }
        String text() { return new String(body, StandardCharsets.UTF_8); }
    }

    /** IPv4 addresses first, then the others, each group in the order given */
    static List<InetAddress> order(InetAddress[] all) {
        List<InetAddress> v = new ArrayList<>();
        for (InetAddress a : all) if (a instanceof Inet4Address) v.add(a);
        for (InetAddress a : all) if (!(a instanceof Inet4Address)) v.add(a);
        return v;
    }

    /** GET `url` (http or https), `headers` extra request headers, at most `maxBytes` of body, the whole call within `deadlineMs`; an IOException when it cannot */
    static Reply get(String url, Map<String, String> headers, int readMs, int deadlineMs, int maxBytes) throws IOException { return get(url, headers, readMs, deadlineMs, maxBytes, null); }

    /** the same, but with a `sink` the body is written to as it arrives (a big file: never held whole in memory); the reply then has an empty body. On a failure the sink may hold a part. */
    static Reply get(String url, Map<String, String> headers, int readMs, int deadlineMs, int maxBytes, OutputStream sink) throws IOException {
        long end = System.nanoTime() + deadlineMs * 1_000_000L;
        NetLog.count(url);   // G3: the request log
        URL u = new URL(url);
        boolean tls = "https".equals(u.getProtocol());
        if (!tls && !"http".equals(u.getProtocol())) throw new IOException("scheme");
        String host = u.getHost();
        int port = u.getPort() > 0 ? u.getPort() : tls ? 443 : 80;
        String path = u.getFile().isEmpty() ? "/" : u.getFile();
        List<InetAddress> addrs = order(InetAddress.getAllByName(host));
        IOException last = new IOException("no address");
        for (InetAddress a : addrs) {
            long left = (end - System.nanoTime()) / 1_000_000L;
            if (left <= 0) break;
            Socket s = new Socket();
            try {
                s.connect(new InetSocketAddress(a, port), (int) Math.min(CONNECT_MS, left));
                Socket io = tls ? tls(s, host, port, (int) Math.min(readMs, Math.max(1, (end - System.nanoTime()) / 1_000_000L))) : s;
                io.setSoTimeout((int) Math.min(readMs, Math.max(1, (end - System.nanoTime()) / 1_000_000L)));
                return exchange(io, host, port, tls, path, headers, end, maxBytes, sink);
            } catch (IOException e) {
                last = e;
                try { s.close(); } catch (IOException ignored) { }
            }
        }
        throw last;
    }

    // ---- G3 (4.3.3): a GET with a copy on the disk (the apps' data files: the markets, the next match, the leagues list)

    /** the copies' folder holds at most this much; the oldest go first */
    static final long CACHE_CAP = 8L << 20;
    /** after a failed connection the next background call to that host waits this long (doubled at each failure, at most BACKOFF_MAX_MS); any answer ends it */
    static final long BACKOFF_MS = 15_000L, BACKOFF_MAX_MS = 5 * 60_000L;
    /** a call that waited for the same file being fetched takes its answer when it is at most this old (two screens asking at once: one request) */
    static final long MERGE_MS = 5_000L;

    private static final Map<String, Object> LOCKS = new HashMap<>();
    private static final Map<String, long[]> BACKOFF = new HashMap<>();   // host -> {failures, until (ms)}

    /**
     * GET `url` with the copy kept in `dir`: the request carries the copy's ETag (If-None-Match) and date (If-Modified-Since), and a 304 hands back the copy
     * as a 200 (header "x-urtv-cache: 304"), so an unchanged file costs no body. A copy younger than `freshMs` (at least MERGE_MS: a call that waited while the
     * same file was being fetched) is handed back without asking ("x-urtv-cache: fresh"). Calls for the same URL run one at a time, so two at once make one
     * request. `backOff` (background calls): after a failed connection to a host, calls to it fail at once ("back-off") for 15 s, doubled at each failure up
     * to 5 minutes; a call the person waits for passes false. A reply other than 200 or 304 is handed back as it is and keeps no copy.
     */
    static Reply cached(java.io.File dir, String url, int readMs, int deadlineMs, int maxBytes, long freshMs, boolean backOff) throws IOException {
        String key = key(url), host = new URL(url).getHost();
        Object lock;
        synchronized (LOCKS) { lock = LOCKS.get(key); if (lock == null) LOCKS.put(key, lock = new Object()); }
        synchronized (lock) {
            java.io.File body = new java.io.File(dir, key + ".body"), meta = new java.io.File(dir, key + ".meta");
            String[] m = readMeta(meta);   // {etag, last-modified, at}
            long now = System.currentTimeMillis(), at = m == null ? 0 : parseLong(m[2]);
            boolean have = m != null && body.isFile();
            if (have && now - at >= 0 && now - at < Math.max(freshMs, MERGE_MS)) {
                byte[] b = readFile(body, maxBytes);
                if (b != null) return new Reply(200, b, one("x-urtv-cache", "fresh"));
            }
            if (backOff) {
                synchronized (BACKOFF) { long[] f = BACKOFF.get(host); if (f != null && now < f[1]) throw new IOException("back-off"); }
            }
            Map<String, String> h = new java.util.LinkedHashMap<>();
            if (have && !m[0].isEmpty()) h.put("If-None-Match", m[0]);
            if (have && !m[1].isEmpty()) h.put("If-Modified-Since", m[1]);
            Reply r;
            try {
                r = get(url, h, readMs, deadlineMs, maxBytes);
            } catch (IOException e) {
                synchronized (BACKOFF) {
                    long[] f = BACKOFF.get(host);
                    long n = f == null ? 1 : f[0] + 1;
                    BACKOFF.put(host, new long[] {n, System.currentTimeMillis() + Math.min(BACKOFF_MAX_MS, BACKOFF_MS << Math.min(10, n - 1))});
                }
                throw e;
            }
            synchronized (BACKOFF) { BACKOFF.remove(host); }   // an answer: the network is there
            if (r.status == 304 && have) {
                byte[] b = readFile(body, maxBytes);
                if (b != null) {
                    writeMeta(meta, m[0], m[1], System.currentTimeMillis());
                    body.setLastModified(System.currentTimeMillis());   // used now: the last to be pruned
                    Map<String, String> rh = new HashMap<>(r.headers);
                    rh.put("x-urtv-cache", "304");
                    return new Reply(200, b, rh);
                }
            }
            if (r.status == 200) {
                String etag = clean(r.headers.get("etag")), lm = clean(r.headers.get("last-modified"));
                if (dir.isDirectory() || dir.mkdirs()) {
                    if (writeFile(body, r.body)) { writeMeta(meta, etag, lm, System.currentTimeMillis()); prune(dir, CACHE_CAP); }
                }
            }
            return r;
        }
    }

    /** a test: no host waits */
    static void forgetBackOff() { synchronized (BACKOFF) { BACKOFF.clear(); } }

    private static Map<String, String> one(String k, String v) { Map<String, String> m = new HashMap<>(); m.put(k, v); return m; }
    private static String clean(String v) { return v == null ? "" : v.replace("\r", "").replace("\n", "").trim(); }
    private static long parseLong(String s) { try { return Long.parseLong(s.trim()); } catch (RuntimeException e) { return 0; } }

    static String key(String url) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-1").digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder s = new StringBuilder();
            for (byte b : d) s.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return s.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return Integer.toHexString(url.hashCode());
        }
    }

    private static String[] readMeta(java.io.File f) {
        byte[] b = readFile(f, 4096);
        if (b == null) return null;
        String[] l = new String(b, StandardCharsets.UTF_8).split("\n", -1);
        return l.length >= 3 ? new String[] {l[0], l[1], l[2]} : null;
    }

    private static void writeMeta(java.io.File f, String etag, String lm, long at) { writeFile(f, (etag + "\n" + lm + "\n" + at + "\n").getBytes(StandardCharsets.UTF_8)); }

    private static byte[] readFile(java.io.File f, int max) {
        if (!f.isFile() || f.length() > max) return null;
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            ByteArrayOutputStream o = new ByteArrayOutputStream((int) f.length());
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) { o.write(buf, 0, n); if (o.size() > max) return null; }
            return o.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /** written whole or not at all (a temporary file, then renamed) */
    private static boolean writeFile(java.io.File f, byte[] b) {
        java.io.File tmp = new java.io.File(f.getPath() + ".tmp");
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(tmp)) {
            out.write(b);
        } catch (IOException e) {
            tmp.delete();
            return false;
        }
        if (tmp.renameTo(f)) return true;
        tmp.delete();
        return false;
    }

    /** the folder back under cap bytes: the copies used longest ago go first (a copy and its meta together) */
    static void prune(java.io.File dir, long cap) {
        java.io.File[] all = dir.listFiles((d, n) -> n.endsWith(".body"));
        if (all == null) return;
        long total = 0;
        for (java.io.File f : all) total += f.length();
        if (total <= cap) return;
        java.util.Arrays.sort(all, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (java.io.File f : all) {
            if (total <= cap) break;
            total -= f.length();
            String k = f.getName().substring(0, f.getName().length() - 5);
            f.delete();
            new java.io.File(dir, k + ".meta").delete();
        }
    }

    private static Socket tls(Socket plain, String host, int port, int timeoutMs) throws IOException {
        plain.setSoTimeout(timeoutMs);
        javax.net.ssl.SSLSocket ssl = (javax.net.ssl.SSLSocket) ((javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault()).createSocket(plain, host, port, true);
        try {   // the certificate is checked against the host name by the TLS layer itself (Android 7 and newer, and every JVM)
            javax.net.ssl.SSLParameters p = ssl.getSSLParameters();
            p.setEndpointIdentificationAlgorithm("HTTPS");
            ssl.setSSLParameters(p);
        } catch (RuntimeException | LinkageError ignored) { /* older: the check below */ }
        ssl.startHandshake();
        // on Android the platform's own verifier too (what HttpsURLConnection does: older versions rely on it alone); a plain JVM's default verifier refuses everything, so it is not asked there
        if (android() && !javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier().verify(host, ssl.getSession())) { ssl.close(); throw new javax.net.ssl.SSLPeerUnverifiedException("host"); }
        return ssl;
    }

    private static boolean android() { return String.valueOf(System.getProperty("java.vendor")).contains("Android") || String.valueOf(System.getProperty("java.vm.vendor")).contains("Android"); }

    private static Reply exchange(Socket io, String host, int port, boolean tls, String path, Map<String, String> headers, long end, int maxBytes, OutputStream sink) throws IOException {
        try (Socket c = io) {
            StringBuilder rq = new StringBuilder("GET ").append(path).append(" HTTP/1.1\r\nHost: ").append(host);
            if (port != (tls ? 443 : 80)) rq.append(':').append(port);
            rq.append("\r\nUser-Agent: urtv/2 (Android TV)\r\nAccept-Encoding: identity\r\nConnection: close\r\n");
            if (headers != null) for (Map.Entry<String, String> h : headers.entrySet()) rq.append(h.getKey()).append(": ").append(h.getValue()).append("\r\n");
            rq.append("\r\n");
            OutputStream out = c.getOutputStream();
            out.write(rq.toString().getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            InputStream in = new java.io.BufferedInputStream(c.getInputStream(), 8192);
            String status = line(in, end);
            String[] sp = status == null ? new String[0] : status.split(" ", 3);
            if (sp.length < 2 || !sp[0].startsWith("HTTP/1.")) throw new IOException("status");
            int code;
            try { code = Integer.parseInt(sp[1]); } catch (NumberFormatException e) { throw new IOException("status"); }
            Map<String, String> h = new HashMap<>();
            for (String l; (l = line(in, end)) != null && !l.isEmpty(); ) {
                int i = l.indexOf(':');
                if (i > 0) h.put(l.substring(0, i).trim().toLowerCase(Locale.ROOT), l.substring(i + 1).trim());
            }
            ByteArrayOutputStream mem = sink == null ? new ByteArrayOutputStream() : null;
            OutputStream body = sink == null ? mem : sink;
            long[] total = new long[1];
            if ("chunked".equalsIgnoreCase(h.get("transfer-encoding"))) {
                while (true) {
                    String sz = line(in, end);
                    if (sz == null) throw new IOException("chunk");
                    int semi = sz.indexOf(';');
                    int n;
                    try { n = Integer.parseInt((semi >= 0 ? sz.substring(0, semi) : sz).trim(), 16); } catch (NumberFormatException e) { throw new IOException("chunk"); }
                    if (n < 0) throw new IOException("chunk");
                    if (n == 0) break;
                    copy(in, body, n, end, maxBytes, total);
                    line(in, end);   // the line end after the chunk
                }
            } else {
                String len = h.get("content-length");
                long want = -1;
                if (len != null) { try { want = Long.parseLong(len.trim()); } catch (NumberFormatException e) { throw new IOException("length"); } }
                if (want > maxBytes) throw new IOException("too big");
                if (want >= 0) copy(in, body, want, end, maxBytes, total);
                else copy(in, body, Long.MAX_VALUE, end, maxBytes, total);   // to the end of the connection
            }
            return new Reply(code, mem == null ? new byte[0] : mem.toByteArray(), h);
        }
    }

    /** one line (up to 8 KB), without its line end; null at the end of the stream; the deadline is checked as it goes */
    private static String line(InputStream in, long end) throws IOException {
        StringBuilder b = new StringBuilder();
        int c;
        while ((c = in.read()) >= 0) {
            if (System.nanoTime() > end) throw new IOException("deadline");
            if (c == '\n') { int n = b.length(); if (n > 0 && b.charAt(n - 1) == '\r') b.setLength(n - 1); return b.toString(); }
            b.append((char) c);
            if (b.length() > 8192) throw new IOException("line");
        }
        return b.length() == 0 ? null : b.toString();
    }

    private static void copy(InputStream in, OutputStream to, long n, long end, int cap, long[] total) throws IOException {
        byte[] buf = new byte[8192];
        long left = n;
        while (left > 0) {
            int r = in.read(buf, 0, (int) Math.min(buf.length, left));
            if (r < 0) { if (n == Long.MAX_VALUE) return; throw new IOException("short"); }
            to.write(buf, 0, r);
            total[0] += r;
            if (total[0] > cap) throw new IOException("too big");
            if (System.nanoTime() > end) throw new IOException("deadline");
            left -= r;
        }
    }
}
