package sa.urtv.home.phone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * A small HTTP/1.1 server on a plain ServerSocket: one request per connection, bounded everywhere (a line, the headers, the body, the time a
 * client may take, the connections at once). It only parses; what a request means is the Handler's business (PhoneApi). The socket comes from
 * a Binder, and the Binder is only asked when the owner turns the feature on: this file and DefaultBinder hold the only `new ServerSocket`.
 */
public final class PhoneServer {
    public interface Binder {
        /** Opens a server socket on exactly this address and port. */
        ServerSocket bind(InetAddress address, int port) throws IOException;
    }

    /** The real thing: one socket, on the given address only (the caller never passes a wildcard). */
    public static final class DefaultBinder implements Binder {
        @Override public ServerSocket bind(InetAddress address, int port) throws IOException {
            return new ServerSocket(port, 8, address);
        }
    }

    public interface Handler {
        Response handle(Request r);
    }

    public static final class Request {
        public final String method, path, body, client;
        public final Map<String, String> headers;   // names in lower case
        /** 4.3: the raw body of POST /api/photo (a JPEG; body is then ""), else null */
        public final byte[] raw;
        Request(String method, String path, Map<String, String> headers, String body, String client) { this(method, path, headers, body, client, null); }
        Request(String method, String path, Map<String, String> headers, String body, String client, byte[] raw) {
            this.method = method; this.path = path; this.headers = headers; this.body = body; this.client = client; this.raw = raw;
        }
        public String header(String name) { return headers.get(name.toLowerCase(Locale.ROOT)); }
    }

    public static final class Response {
        public final int status;
        public final Map<String, String> headers = new LinkedHashMap<>();
        public final byte[] body;
        public Response(int status, String type, byte[] body) {
            this.status = status;
            this.body = body;
            if (type != null) headers.put("Content-Type", type);
        }
    }

    public static final int MAX_LINE = 4096, MAX_HEADERS = 40, MAX_HEADER_BYTES = 8192, MAX_BODY = 4096;
    /** 4.3: «صوري» from the phone, one JPEG the phone resized (1920 px): only on POST /api/photo, only as image/jpeg */
    public static final String PHOTO_PATH = "/api/photo";
    public static final int MAX_PHOTO_BODY = 1_000_000;
    static final int READ_TIMEOUT_MS = 3000;
    static final long TOTAL_MS = 8000;

    private final ServerSocket socket;
    private final Handler handler;
    private final Predicate<InetAddress> clientOk;
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 6, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(8));
    private volatile boolean closed;

    public PhoneServer(ServerSocket socket, Handler handler, Predicate<InetAddress> clientOk) {
        this.socket = socket;
        this.handler = handler;
        this.clientOk = clientOk;
    }

    public int port() { return socket.getLocalPort(); }

    public void start() {
        Thread t = new Thread(new Runnable() { @Override public void run() { accept(); } }, "phone-accept");
        t.setDaemon(true);
        t.start();
    }

    public void close() {
        closed = true;
        try { socket.close(); } catch (IOException ignored) { }
        pool.shutdown();
    }

    private void accept() {
        while (!closed) {
            final Socket s;
            try {
                s = socket.accept();
            } catch (IOException e) {
                return;   // closed
            }
            if (!clientOk.test(s.getInetAddress())) { quiet(s); continue; }   // not a private address: no answer at all
            try {
                pool.execute(new Runnable() { @Override public void run() { serve(s); } });
            } catch (RejectedExecutionException e) {
                quiet(s);   // too many at once
            }
        }
    }

    private static void quiet(Socket s) {
        try { s.close(); } catch (IOException ignored) { }
    }

    private void serve(Socket s) {
        try {
            s.setSoTimeout(READ_TIMEOUT_MS);
            Response r;
            try {
                Request req = read(s);
                r = req == null ? null : handler.handle(req);
            } catch (BadRequest e) {
                r = new Response(e.status, "text/plain; charset=utf-8", new byte[0]);
            } catch (RuntimeException e) {
                r = new Response(500, "text/plain; charset=utf-8", new byte[0]);
            }
            if (r != null) write(s.getOutputStream(), r);
        } catch (IOException ignored) {
            // a client that went away or was too slow
        } finally {
            quiet(s);
        }
    }

    private static final class BadRequest extends Exception {
        final int status;
        BadRequest(int status) { super("bad request"); this.status = status; }
    }

    private static String line(InputStream in, long deadline) throws IOException, BadRequest {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        while (true) {
            if (System.nanoTime() > deadline) throw new SocketTimeoutException("slow");
            int c = in.read();
            if (c < 0) return b.size() == 0 ? null : fail(400);
            if (c == '\n') break;
            if (c != '\r') b.write(c);
            if (b.size() > MAX_LINE) fail(431);
        }
        return new String(b.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    private static String fail(int status) throws BadRequest { throw new BadRequest(status); }

    private Request read(Socket s) throws IOException, BadRequest {
        InputStream in = new java.io.BufferedInputStream(s.getInputStream(), 1024);
        long deadline = System.nanoTime() + TOTAL_MS * 1_000_000L;
        String first = line(in, deadline);
        if (first == null) return null;
        String[] p = first.split(" ", -1);
        if (p.length != 3 || !p[2].startsWith("HTTP/1.")) fail(400);
        if (!p[0].equals("GET") && !p[0].equals("POST")) fail(405);
        String target = p[1];
        if (!target.startsWith("/") || target.length() > 200 || target.contains("?") || target.contains("\\")) fail(404);
        Map<String, String> h = new LinkedHashMap<>();
        int bytes = 0;
        while (true) {
            String l = line(in, deadline);
            if (l == null) fail(400);
            if (l.isEmpty()) break;
            bytes += l.length();
            if (h.size() >= MAX_HEADERS || bytes > MAX_HEADER_BYTES) fail(431);
            int colon = l.indexOf(':');
            if (colon <= 0 || l.charAt(0) == ' ' || l.charAt(0) == '\t') fail(400);
            String name = l.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = l.substring(colon + 1).trim();
            if (h.containsKey(name)) fail(400);   // a repeated Host, Content-Length, Origin... is how requests get smuggled
            h.put(name, value);
        }
        if (h.containsKey("transfer-encoding")) fail(400);
        String body = "";
        if (p[0].equals("POST")) {
            String cl = h.get("content-length");
            boolean photo = target.equals(PHOTO_PATH) && "image/jpeg".equals(String.valueOf(h.get("content-type")).toLowerCase(Locale.ROOT).trim());
            if (cl == null || !cl.matches(photo ? "[0-9]{1,7}" : "[0-9]{1,5}")) fail(411);
            int n = Integer.parseInt(cl);
            if (n > (photo ? MAX_PHOTO_BODY : MAX_BODY)) fail(413);
            byte[] buf = new byte[n];
            int got = 0;
            while (got < n) {
                if (System.nanoTime() > deadline) throw new SocketTimeoutException("slow");
                int r = in.read(buf, got, n - got);
                if (r < 0) fail(400);
                got += r;
            }
            if (photo) return new Request(p[0], target, h, "", s.getInetAddress().getHostAddress(), buf);
            body = new String(buf, StandardCharsets.UTF_8);
        } else if (h.containsKey("content-length") && !"0".equals(h.get("content-length"))) {
            fail(400);
        }
        return new Request(p[0], target, h, body, s.getInetAddress().getHostAddress());
    }

    private static String reason(int status) {
        switch (status) {
            case 200: return "OK";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 411: return "Length Required";
            case 413: return "Payload Too Large";
            case 415: return "Unsupported Media Type";
            case 429: return "Too Many Requests";
            case 431: return "Request Header Fields Too Large";
            default: return "Error";
        }
    }

    private static void write(OutputStream out, Response r) throws IOException {
        StringBuilder h = new StringBuilder("HTTP/1.1 " + r.status + " " + reason(r.status) + "\r\n");
        for (Map.Entry<String, String> e : r.headers.entrySet()) h.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
        h.append("Content-Length: ").append(r.body.length).append("\r\nConnection: close\r\n\r\n");
        out.write(h.toString().getBytes(StandardCharsets.ISO_8859_1));
        out.write(r.body);
        out.flush();
    }
}
