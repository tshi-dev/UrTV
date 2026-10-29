package sa.urtv.home.phone;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What each request means. The order of the checks is the security design: the client's address (PhoneServer), the rate, the Host header,
 * then per endpoint: Origin and Content-Type on every POST, the session (cookie AND the X-UrTV-Session header, equal), the JSON and its
 * allow-listed keys (Schema), the PIN for protected changes. Answers are small JSON objects with fixed words; nothing from the request
 * is ever sent back, and no secret is (the session value is sent once, to the phone that was allowed, when it asks for the answer).
 */
final class PhoneApi implements PhoneServer.Handler {
    static final String COOKIE = "urtv", HEADER = "x-urtv-session";
    private static final String JSON = "application/json; charset=utf-8";
    /** The page's fonts (Thmanyah, as on the TV's screens), served from the APK's assets/fonts; nothing else from there is ever served. */
    static final List<String> FONTS = java.util.Arrays.asList("Thmanyah-Regular.woff2", "Thmanyah-Medium.woff2");

    private final PhoneControl control;
    private final String ip;
    private final int port;
    private final Guard.Rate general = new Guard.Rate(100, 5_000), pairing = new Guard.Rate(12, 60_000);

    PhoneApi(PhoneControl control, String ip, int port) {
        this.control = control;
        this.ip = ip;
        this.port = port;
    }

    @Override public PhoneServer.Response handle(PhoneServer.Request r) {
        long now = control.now();
        if (!general.allow(r.client, now)) return json(429, err("slow_down"));
        if (!Guard.hostOk(r.header("host"), ip, port)) return json(403, err("bad_host"));   // DNS rebinding, or a stray name for this address
        boolean post = r.method.equals("POST");
        if (!post && !r.method.equals("GET")) return json(405, err("method"));

        if (r.path.equals("/p") || r.path.equals("/")) return post ? json(405, err("method")) : page();
        // the fonts are no secret, and CSS asks for them without the X-UrTV-Session header: no session, like the page itself
        if (r.path.startsWith("/fonts/")) return post ? json(405, err("method")) : font(r.path.substring("/fonts/".length()));

        if (post) {
            if (!Guard.originOk(r.header("origin"), ip, port) || !Guard.fetchSiteOk(r.header("sec-fetch-site"))) return json(403, err("bad_origin"));
            String type = r.header("content-type");
            if (type == null || !type.toLowerCase().replace(" ", "").matches("application/json(;charset=utf-8)?")) return json(415, err("json_only"));
        }

        switch (r.path) {
            case "/api/pair": return post ? pair(r) : json(405, err("method"));
            case "/api/pair/status": return post ? pairStatus(r) : json(405, err("method"));
            default: break;
        }

        // everything below needs a phone the owner allowed
        String secret = session(r);
        if (secret == null) return json(401, err("no_session"));

        if (r.path.equals("/api/state")) return post ? json(405, err("method")) : state(secret);
        if (!post) {
            switch (r.path) {
                case "/api/apps": return ok(control.actions().apps());
                case "/api/family": return ok(control.actions().family());
                case "/api/backdrop": return backdrop();
                default: break;
            }
            if (r.path.startsWith("/api/icon/")) return icon(r.path.substring("/api/icon/".length()));
            return json(404, err("not_found"));
        }

        String endpoint;
        switch (r.path) {
            case "/api/key": endpoint = "key"; break;
            case "/api/text": endpoint = "text"; break;
            case "/api/set": endpoint = "set"; break;
            case "/api/quran": endpoint = "quran"; break;
            case "/api/cities": endpoint = "cities"; break;
            case "/api/update": endpoint = "update"; break;
            case "/api/launch": endpoint = "launch"; break;
            case "/api/adhan_stop": endpoint = "adhanstop"; break;
            case "/api/do": endpoint = "do"; break;
            case "/api/logout": endpoint = "logout"; break;
            default: return json(404, err("not_found"));
        }
        Schema.Clean c = body(endpoint, r);
        if (!c.ok()) return json(400, err("bad_request"));
        Map<String, Object> v = c.values;

        // cancelling the sleep timer is a change like the others: a child's phone must not undo a parent's bedtime timer (the screensaver, which only opens a screen, is not)
        boolean protectedChange = endpoint.equals("set") || (endpoint.equals("update") && "install".equals(v.get("op")))
            || (endpoint.equals("do") && "sleep_cancel".equals(v.get("op")));
        PinGate gate = control.pinGate();
        if (protectedChange && gate.required()) {
            Object given = v.get("pin");
            if (given == null) return json(403, err("pin_required"));
            if (!gate.verify((String) given)) {
                if (control.pinFailed(secret)) return json(401, err("no_session"));
                return json(403, err("pin_wrong"));
            }
        }
        v.remove("pin");

        Actions a = control.actions();
        switch (endpoint) {
            case "key": return result(a.key((String) v.get("key")));
            case "text": return result(a.text((String) v.get("text"), (String) v.get("mode"), Boolean.TRUE.equals(v.get("search"))));
            case "set": return result(a.set(v));
            case "launch": return result(a.launch((String) v.get("target")));
            case "adhanstop": return result(a.adhanStop());
            case "do": return result(a.doOp((String) v.get("op")));
            case "quran": return result(a.quran(v));
            case "cities": {
                Map<String, Object> out = ok();
                out.put("cities", a.cities((String) v.get("q")));
                return json(200, out);
            }
            case "update": {
                Map<String, Object> out = ok();
                out.putAll(a.update((String) v.get("op")));
                return json(200, out);
            }
            default: {   // logout
                control.logout(secret);
                PhoneServer.Response resp = json(200, ok());
                resp.headers.put("Set-Cookie", COOKIE + "=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0");
                return resp;
            }
        }
    }

    // ---- pieces

    private PhoneServer.Response pair(PhoneServer.Request r) {
        if (!pairing.allow(r.client, control.now())) return json(429, err("slow_down"));
        Schema.Clean c = body("pair", r);
        if (!c.ok()) return json(400, err("bad_request"));
        boolean isCode = c.values.containsKey("code");
        Object[] res = control.pair((String) c.values.get(isCode ? "code" : "token"), isCode, r.client);
        switch ((PhoneControl.Pair) res[0]) {
            case OK: {
                control.changed();
                Map<String, Object> out = ok();
                out.put("id", res[1]);
                return json(200, out);
            }
            case BUSY: return json(429, err("busy"));
            case LOCKED: return json(403, err("locked"));
            default: return json(403, err("wrong"));
        }
    }

    private PhoneServer.Response pairStatus(PhoneServer.Request r) {
        Schema.Clean c = body("pairstatus", r);
        if (!c.ok()) return json(400, err("bad_request"));
        String[] st = control.pairStatus((String) c.values.get("id"), r.client);
        Map<String, Object> out = ok();
        out.put("status", st[0]);
        PhoneServer.Response resp;
        if (st[1] != null) {
            out.put("session", st[1]);   // for the X-UrTV-Session header; the cookie below is the other half
            resp = json(200, out);
            resp.headers.put("Set-Cookie", COOKIE + "=" + st[1] + "; HttpOnly; SameSite=Strict; Path=/; Max-Age=" + (Sessions.MAX_MS / 1000));
        } else {
            resp = json(200, out);
        }
        return resp;
    }

    private PhoneServer.Response state(String secret) {
        Map<String, Object> out = ok();
        out.putAll(control.actions().state());
        out.put("pin_required", control.pinGate().required());
        out.put("seconds_left", control.secondsLeft(secret));
        out.put("phones", (long) control.phones().size());
        return json(200, out);
    }

    /** One favourite's icon, a PNG the browser keeps for a day; 404 for a package that is not one of them (nothing else is ever revealed). */
    private PhoneServer.Response icon(String pkg) {
        if (!Schema.packageName(pkg)) return json(404, err("not_found"));
        byte[] png = control.actions().icon(pkg);
        if (png == null) return json(404, err("not_found"));
        PhoneServer.Response resp = new PhoneServer.Response(200, "image/png", png);
        headers(resp);
        resp.headers.put("Cache-Control", "private, max-age=86400");
        return resp;
    }

    /** The glass look's small blurred backdrop of the home's photo, for the page's background; 404 while there is none. */
    private PhoneServer.Response backdrop() {
        byte[] png = control.actions().backdrop();
        if (png == null) return json(404, err("not_found"));
        PhoneServer.Response resp = new PhoneServer.Response(200, "image/png", png);
        headers(resp);
        resp.headers.put("Cache-Control", "private, max-age=600");
        return resp;
    }

    /** One of the page's fonts, by its exact allow-listed name (no path, no other asset); 404 for any other name, or when the APK has none. */
    private PhoneServer.Response font(String name) {
        if (!FONTS.contains(name)) return json(404, err("not_found"));
        byte[] woff2 = control.page().font(name);
        if (woff2 == null) return json(404, err("not_found"));
        PhoneServer.Response resp = new PhoneServer.Response(200, "font/woff2", woff2);
        headers(resp);
        resp.headers.put("Cache-Control", "public, max-age=31536000");
        return resp;
    }

    private static PhoneServer.Response ok(Map<String, Object> body) {
        Map<String, Object> out = ok();
        out.putAll(body);
        return json(200, out);
    }

    /** The session value, if the cookie and the header both carry it and it is a live session; else null. */
    private String session(PhoneServer.Request r) {
        String header = r.header(HEADER);
        String cookie = cookie(r.header("cookie"));
        if (header == null || cookie == null || !header.matches("[0-9a-f]{64}") || !Pairing.same(header, cookie)) return null;
        return control.sessionOk(header, r.client) ? header : null;   // only from the address the phone was allowed from
    }

    static String cookie(String all) {
        if (all == null) return null;
        for (String part : all.split(";")) {
            String p = part.trim();
            if (p.startsWith(COOKIE + "=")) return p.substring(COOKIE.length() + 1);
        }
        return null;
    }

    private Schema.Clean body(String endpoint, PhoneServer.Request r) {
        try {
            return Schema.check(endpoint, Json.parseObject(r.body));
        } catch (Json.Bad e) {
            return Schema.check("none", new LinkedHashMap<String, Object>());
        }
    }

    private PhoneServer.Response page() {
        byte[] nonceBytes = new byte[16];
        new java.security.SecureRandom().nextBytes(nonceBytes);
        StringBuilder n = new StringBuilder();
        for (byte b : nonceBytes) n.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
        String html = control.page().html().replace("__NONCE__", n.toString());
        PhoneServer.Response resp = new PhoneServer.Response(200, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
        resp.headers.put("Content-Security-Policy", "default-src 'none'; script-src 'nonce-" + n + "'; style-src 'unsafe-inline'; "
            + "connect-src 'self'; img-src 'self' data:; font-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'");
        headers(resp);
        return resp;
    }

    private static void headers(PhoneServer.Response r) {
        r.headers.put("Cache-Control", "no-store");
        r.headers.put("X-Content-Type-Options", "nosniff");
        r.headers.put("X-Frame-Options", "DENY");
        r.headers.put("Referrer-Policy", "no-referrer");
        r.headers.put("Cross-Origin-Resource-Policy", "same-origin");
    }

    private static Map<String, Object> ok() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        return m;
    }

    private static Map<String, Object> err(String code) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", false);
        m.put("error", code);
        return m;
    }

    private static PhoneServer.Response result(Actions.Result res) {
        Map<String, Object> m = res.ok ? ok() : err(res.code);
        if (res.ok) m.put("result", res.code);
        return json(200, m);
    }

    static PhoneServer.Response json(int status, Map<String, Object> body) {
        PhoneServer.Response r = new PhoneServer.Response(status, JSON, Json.write(body).getBytes(StandardCharsets.UTF_8));
        headers(r);
        return r;
    }

    /** For tests: the endpoints that need a session. */
    static List<String> sessionPaths() {
        List<String> l = new ArrayList<>();
        for (String p : new String[] {"/api/state", "/api/key", "/api/text", "/api/set", "/api/quran", "/api/cities", "/api/update", "/api/launch", "/api/adhan_stop", "/api/do", "/api/apps", "/api/family", "/api/icon/com.example.app", "/api/backdrop", "/api/logout"}) l.add(p);
        return l;
    }
}
