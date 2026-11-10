package sa.urtv.screensaver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * «الأسرع»: the game-only server (no Android: tested on a plain JVM). It is not the remote-control pairing and has no route that does anything outside the game:
 * GET /&lt;code&gt; the phone page, GET /f/&lt;font&gt;.woff2, GET /api/state, POST /api/join, /api/answer, /api/leave. It answers only addresses of the local network
 * (private, link-local or loopback), takes bodies of 1 KB at most, closes every connection after one answer and keeps nothing on disk.
 */
final class QuizServer {
    /** where the page and the fonts come from (the app's assets; a test fakes it) */
    interface Assets { byte[] page(); byte[] font(String name); }

    private final QuizRoom room; private final Assets assets; private final boolean lanOnly;
    private ServerSocket socket; private ThreadPoolExecutor pool; private volatile boolean running;
    static final int BODY_MAX = 1024, HEAD_MAX = 8192, POOL = 8;
    /** a whole request must arrive within 3 s (slow-loris); one device holds at most 4 connections; 5 wrong room codes from one address lock that address for 60 s */
    static final long DEADLINE_MS = 3000, IP_LOCK_MS = 60000; static final int PER_IP = 4, IP_WRONG = 5;
    private final Map<String, Integer> open = new HashMap<>(), wrong = new HashMap<>(); private final Map<String, Long> lockedUntil = new HashMap<>();
    private static final java.util.regex.Pattern HOST = java.util.regex.Pattern.compile("(?i)((\\d{1,3}(\\.\\d{1,3}){3})|(\\[[0-9a-f:]+\\])|localhost)(:\\d{1,5})?");

    QuizServer(QuizRoom room, Assets assets, boolean lanOnly) { this.room = room; this.assets = assets; this.lanOnly = lanOnly; }

    /** starts on `bind` (the TV's own local address) and a free port; returns the port */
    int start(InetAddress bind) throws IOException {
        socket = new ServerSocket(0, 16, bind);
        pool = new ThreadPoolExecutor(POOL, POOL, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(16));
        pool.allowCoreThreadTimeOut(true);   // 8 threads at most, none kept while nobody asks
        running = true;
        Thread t = new Thread(this::loop, "quiz-server");
        t.setDaemon(true); t.start();
        return socket.getLocalPort();
    }

    void stop() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        if (pool != null) pool.shutdownNow();
    }

    private void loop() {
        while (running) {
            try {
                Socket s = socket.accept();
                try { pool.execute(() -> serve(s)); } catch (RejectedExecutionException e) { try { s.close(); } catch (IOException ignored) {} }
            } catch (IOException e) { if (!running) return; }
        }
    }

    /** private, link-local, loopback: the local network and nothing else */
    static boolean local(InetAddress a) {
        return a.isSiteLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress();
    }

    private synchronized boolean enter(String ip) {
        int n = open.getOrDefault(ip, 0);
        if (n >= PER_IP) return false;
        open.put(ip, n + 1);
        return true;
    }
    private synchronized void leave(String ip) { int n = open.getOrDefault(ip, 1) - 1; if (n <= 0) open.remove(ip); else open.put(ip, n); }
    private synchronized boolean banned(String ip) { Long t = lockedUntil.get(ip); if (t == null) return false; if (System.currentTimeMillis() >= t) { lockedUntil.remove(ip); return false; } return true; }
    /** a request carried a wrong room code: five of them lock that one address for 60 s (a stale page on a phone cannot lock the others out) */
    private synchronized void wrongCode(String ip) {
        int n = wrong.getOrDefault(ip, 0) + 1;
        if (n >= IP_WRONG) { lockedUntil.put(ip, System.currentTimeMillis() + IP_LOCK_MS); wrong.remove(ip); } else wrong.put(ip, n);
    }

    private void serve(Socket s) {
        String ip = s.getInetAddress().getHostAddress();
        boolean counted = false;
        try (Socket c = s) {
            if (lanOnly && !local(c.getInetAddress())) return;
            if (!(counted = enter(ip))) return;   // too many connections from this one device
            long deadline = System.nanoTime() + DEADLINE_MS * 1_000_000L;
            c.setSoTimeout((int) DEADLINE_MS);
            InputStream in = c.getInputStream();
            ByteArrayOutputStream head = new ByteArrayOutputStream();
            int state = 0, b;
            while (state < 4 && (b = in.read()) >= 0) {   // up to the blank line
                head.write(b);
                if (head.size() > HEAD_MAX) { reply(c, 431, "text/plain", "x".getBytes()); return; }
                if (System.nanoTime() > deadline) return;
                state = (b == '\r' && (state == 0 || state == 2)) ? state + 1 : (b == '\n' && (state == 1 || state == 3)) ? state + 1 : (b == '\r' ? 1 : 0);
            }
            if (state < 4) return;
            String[] lines = head.toString("ISO-8859-1").split("\r\n");
            String[] rl = lines.length > 0 ? lines[0].split(" ") : new String[0];
            if (rl.length < 2) { reply(c, 400, "text/plain", "x".getBytes()); return; }
            int len = 0; String host = null, origin = null, site = null;
            for (int i = 1; i < lines.length; i++) {
                String l = lines[i], low = l.toLowerCase(java.util.Locale.ROOT);
                if (low.startsWith("content-length:")) { try { len = Integer.parseInt(l.substring(15).trim()); } catch (NumberFormatException e) { len = -1; } }
                else if (low.startsWith("host:")) host = l.substring(5).trim();
                else if (low.startsWith("origin:")) origin = l.substring(7).trim();
                else if (low.startsWith("sec-fetch-site:")) site = low.substring(15).trim();
            }
            // DNS rebinding: the Host must be the TV's address itself (a number), never a name; CSRF: a page from another origin can write to this address but is refused
            if (host == null || !HOST.matcher(host).matches()) { reply(c, 403, "text/plain", "x".getBytes()); return; }
            if (origin != null && !origin.equalsIgnoreCase("http://" + host)) { reply(c, 403, "text/plain", "x".getBytes()); return; }
            if (site != null && (site.equals("cross-site") || site.equals("same-site"))) { reply(c, 403, "text/plain", "x".getBytes()); return; }
            if (len < 0 || len > BODY_MAX) { reply(c, 413, "text/plain", "x".getBytes()); return; }
            if (banned(ip)) { json(c, "{\"ok\":false,\"e\":\"locked\"}"); return; }
            byte[] body = new byte[len];
            for (int off = 0; off < len; ) {
                int n = in.read(body, off, len - off);
                if (n < 0 || System.nanoTime() > deadline) { return; }
                off += n;
            }
            route(c, rl[0], rl[1], new String(body, StandardCharsets.UTF_8), ip);
        } catch (IOException ignored) {
            // a phone that went away, or one too slow: nothing to do
        } finally {
            if (counted) leave(ip);
        }
    }

    private void route(Socket c, String method, String target, String body, String ip) throws IOException {
        String path = target, query = "";
        int q = target.indexOf('?');
        if (q >= 0) { path = target.substring(0, q); query = target.substring(q + 1); }
        boolean get = method.equals("GET"), post = method.equals("POST");
        if (!get && !post) { reply(c, 405, "text/plain", "x".getBytes()); return; }
        if (get && path.equals("/" + room.code)) { reply(c, 200, "text/html; charset=utf-8", assets.page()); return; }
        if (get && path.startsWith("/f/") && path.endsWith(".woff2")) {
            String n = path.substring(3, path.length() - 6);
            byte[] f = n.matches("[A-Za-z0-9\\-]{1,40}") ? assets.font(n) : null;
            if (f == null) reply(c, 404, "text/plain", "x".getBytes()); else reply(c, 200, "font/woff2", f);
            return;
        }
        if (get && path.equals("/api/state")) {
            Map<String, String> p = form(query);
            if (!room.code.equals(p.get("c"))) { wrongCode(ip); json(c, "{\"ok\":false,\"e\":\"code\"}"); return; }
            json(c, room.stateFor(p.get("t")));
            return;
        }
        if (post && path.equals("/api/join")) {
            Map<String, String> p = form(body);
            if (!room.code.equals(p.get("c"))) wrongCode(ip);
            QuizRoom.Result r = room.join(p.get("c"), p.get("n"), num(p.get("k"), -1), p.get("t"));
            json(c, r.ok ? "{\"ok\":true,\"t\":" + QuizRoom.esc(r.player.token) + ",\"n\":" + QuizRoom.esc(r.player.name) + ",\"k\":" + r.player.color + "}" : "{\"ok\":false,\"e\":\"" + r.error + "\"}");
            return;
        }
        if (post && path.equals("/api/answer")) {
            Map<String, String> p = form(body);
            if (!room.code.equals(p.get("c"))) { wrongCode(ip); json(c, "{\"ok\":false,\"e\":\"code\"}"); return; }
            String e = room.answer(p.get("t"), num(p.get("q"), -1), num(p.get("a"), -1), p.get("x"));
            json(c, e == null ? "{\"ok\":true}" : "{\"ok\":false,\"e\":\"" + e + "\"}");
            return;
        }
        if (post && path.equals("/api/leave")) {
            Map<String, String> p = form(body);
            if (room.code.equals(p.get("c"))) room.leave(p.get("t")); else wrongCode(ip);
            json(c, "{\"ok\":true}");
            return;
        }
        if (get && path.matches("/[0-9]{4}")) { wrongCode(ip); reply(c, 404, "text/html; charset=utf-8", WRONG_CODE.getBytes(StandardCharsets.UTF_8)); return; }   // an old or wrong room code
        reply(c, 404, "text/plain", "x".getBytes());
    }

    /** what a phone with an old or wrong code sees: the message and nothing else */
    static final String WRONG_CODE = "<!doctype html><meta charset=utf-8><meta name=viewport content=\"width=device-width,initial-scale=1\"><body style=\"font:24px sans-serif;margin:24px;background:#EFE8DA;color:#16140F\"><p dir=rtl>الرمز غير صحيح</p><p dir=ltr>Wrong code</p>";

    private static int num(String s, int d) { try { return Integer.parseInt(s.trim()); } catch (RuntimeException e) { return d; } }

    static Map<String, String> form(String s) {
        Map<String, String> m = new HashMap<>();
        if (s == null || s.isEmpty()) return m;
        for (String kv : s.split("&")) {
            int i = kv.indexOf('=');
            try {
                if (i > 0) m.put(URLDecoder.decode(kv.substring(0, i), "UTF-8"), URLDecoder.decode(kv.substring(i + 1), "UTF-8"));
            } catch (Exception ignored) { /* a malformed pair is skipped */ }
        }
        return m;
    }

    private void json(Socket c, String j) throws IOException { reply(c, 200, "application/json; charset=utf-8", j.getBytes(StandardCharsets.UTF_8)); }

    private void reply(Socket c, int code, String type, byte[] body) throws IOException {
        String reason = code == 200 ? "OK" : code == 404 ? "Not Found" : code == 400 ? "Bad Request" : code == 405 ? "Method Not Allowed" : code == 413 ? "Payload Too Large" : code == 403 ? "Forbidden" : "Error";
        String h = "HTTP/1.1 " + code + " " + reason + "\r\nContent-Type: " + type + "\r\nContent-Length: " + body.length
            + "\r\nCache-Control: no-store\r\nConnection: close\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\nReferrer-Policy: no-referrer\r\n"
            + (type.startsWith("text/html") ? "Content-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; font-src 'self'; connect-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'; form-action 'none'\r\n" : "")
            + "\r\n";
        OutputStream o = c.getOutputStream();
        o.write(h.getBytes(StandardCharsets.ISO_8859_1));
        o.write(body);
        o.flush();
    }
}
