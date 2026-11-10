package sa.urtv.screensaver;

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
    static Reply get(String url, Map<String, String> headers, int readMs, int deadlineMs, int maxBytes) throws IOException {
        long end = System.nanoTime() + deadlineMs * 1_000_000L;
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
                return exchange(io, host, port, tls, path, headers, end, maxBytes);
            } catch (IOException e) {
                last = e;
                try { s.close(); } catch (IOException ignored) { }
            }
        }
        throw last;
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

    private static Reply exchange(Socket io, String host, int port, boolean tls, String path, Map<String, String> headers, long end, int maxBytes) throws IOException {
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
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            if ("chunked".equalsIgnoreCase(h.get("transfer-encoding"))) {
                while (true) {
                    String sz = line(in, end);
                    if (sz == null) throw new IOException("chunk");
                    int semi = sz.indexOf(';');
                    int n;
                    try { n = Integer.parseInt((semi >= 0 ? sz.substring(0, semi) : sz).trim(), 16); } catch (NumberFormatException e) { throw new IOException("chunk"); }
                    if (n < 0) throw new IOException("chunk");
                    if (n == 0) break;
                    copy(in, body, n, end, maxBytes);
                    line(in, end);   // the line end after the chunk
                }
            } else {
                String len = h.get("content-length");
                long want = -1;
                if (len != null) { try { want = Long.parseLong(len.trim()); } catch (NumberFormatException e) { throw new IOException("length"); } }
                if (want > maxBytes) throw new IOException("too big");
                if (want >= 0) copy(in, body, want, end, maxBytes);
                else copy(in, body, Long.MAX_VALUE, end, maxBytes);   // to the end of the connection
            }
            return new Reply(code, body.toByteArray(), h);
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

    private static void copy(InputStream in, ByteArrayOutputStream to, long n, long end, int cap) throws IOException {
        byte[] buf = new byte[8192];
        long left = n;
        while (left > 0) {
            int r = in.read(buf, 0, (int) Math.min(buf.length, left));
            if (r < 0) { if (n == Long.MAX_VALUE) return; throw new IOException("short"); }
            to.write(buf, 0, r);
            if (to.size() > cap) throw new IOException("too big");
            if (System.nanoTime() > end) throw new IOException("deadline");
            left -= r;
        }
    }
}
