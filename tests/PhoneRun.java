package sa.urtv.home.phone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** For tests/phone_test.py: the phone control's logic (sa.urtv.home.phone) on a plain JVM. Prints PASS/FAIL lines. */
public final class PhoneRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    static byte[] ip(int a, int b, int c, int d) { return new byte[] {(byte) a, (byte) b, (byte) c, (byte) d}; }

    static InetAddress addr(int a, int b, int c, int d) throws IOException { return InetAddress.getByAddress(ip(a, b, c, d)); }

    static final byte[] LOOP = ip(127, 0, 0, 1);

    // ---------------------------------------------------------------- stand-ins

    static final class CountingBinder implements PhoneServer.Binder {
        final AtomicInteger binds = new AtomicInteger();
        final List<ServerSocket> sockets = new ArrayList<>();
        final List<InetAddress> asked = new ArrayList<>();
        @Override public ServerSocket bind(InetAddress a, int port) throws IOException {
            binds.incrementAndGet();
            asked.add(a);
            ServerSocket s = new ServerSocket(port, 8, a);
            sockets.add(s);
            return s;
        }
    }

    static final class FakeActions implements Actions {
        final List<String> calls = new ArrayList<>();
        Result next = Result.ok("done");
        @Override public Result key(String k) { calls.add("key:" + k); return next; }
        @Override public Result text(String t, String mode, boolean search) { calls.add("text:" + t + ":" + mode + ":" + search); return Result.ok("typed"); }
        @Override public Map<String, Object> state() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("city", "الرياض");
            m.put("foreground", "own");
            return m;
        }
        @Override public Result set(Map<String, Object> v) { calls.add("set:" + v.keySet()); if (v.containsKey("home_cards")) calls.add("cards:" + v.get("home_cards")); return Result.ok("saved"); }
        @Override public Result quran(Map<String, Object> v) { calls.add("quran:" + v.get("op")); return Result.ok("sent"); }
        @Override public List<String> cities(String q) { calls.add("cities"); return new ArrayList<String>(Arrays.asList("الرياض")); }
        @Override public Map<String, Object> apps() {
            calls.add("apps");
            Map<String, Object> m = new LinkedHashMap<>();
            List<Object> apps = new ArrayList<>(), tools = new ArrayList<>();
            Map<String, Object> yt = new LinkedHashMap<>();
            yt.put("pkg", "com.example.video"); yt.put("name", "فيديو"); yt.put("locked", false);
            apps.add(yt);
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", "tasks"); t.put("name", "المهام"); t.put("locked", true);
            tools.add(t);
            m.put("apps", apps);
            m.put("tools", tools);
            return m;
        }
        @Override public byte[] backdrop() { calls.add("backdrop"); return null; }
        @Override public byte[] icon(String pkg) { calls.add("icon:" + pkg); return pkg.equals("com.example.video") ? new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10} : null; }
        @Override public Result launch(String target) { calls.add("launch:" + target); return target.equals("@tasks") ? Result.ok("pin_on_tv") : Result.ok("opened"); }
        @Override public Result adhanStop() { calls.add("adhan_stop"); return Result.ok("done"); }
        @Override public Result doOp(String op) { calls.add("do:" + op); return Result.ok("done"); }
        @Override public Map<String, Object> family() {
            calls.add("family");
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pin_set", true); m.put("kids", false);
            m.put("locked", new ArrayList<Object>(Arrays.asList("@wifi")));
            return m;
        }
        @Override public Map<String, Object> update(String op) { calls.add("update:" + op); Map<String, Object> m = new LinkedHashMap<>(); m.put("status", "idle"); return m; }
    }

    static final class Env {
        final long[] now = {1_000_000L};
        final FakeActions actions = new FakeActions();
        final CountingBinder binder = new CountingBinder();
        PhoneControl ctl;
        int port;
        String origin;
    }

    static Env env(Predicate<InetAddress> clientOk) {
        final Env e = new Env();
        e.ctl = new PhoneControl(e.binder, new PhoneControl.Clock() { @Override public long now() { return e.now[0]; } }, e.actions,
            new PhoneControl.Page() { @Override public String html() { return "<html><script nonce=\"__NONCE__\">1</script></html>"; } }, clientOk, new SecureRandom());
        return e;
    }

    static Env started() {
        Env e = env(new Predicate<InetAddress>() { @Override public boolean test(InetAddress a) { return true; } });
        PhoneControl.Start r = e.ctl.start(LOOP);
        if (r != PhoneControl.Start.OK) throw new IllegalStateException("start " + r);
        e.port = e.ctl.port();
        e.origin = "http://127.0.0.1:" + e.port;
        return e;
    }

    // ---------------------------------------------------------------- a raw HTTP client (so Host, Origin and the rest can be anything)

    static final class Reply {
        int status;
        String head = "", body = "";
        boolean empty;
        String header(String name) {
            for (String l : head.split("\r\n")) if (l.toLowerCase().startsWith(name.toLowerCase() + ":")) return l.substring(name.length() + 1).trim();
            return null;
        }
        Map<String, Object> json() {
            try { return Json.parseObject(body); } catch (Json.Bad e) { return new LinkedHashMap<String, Object>(); }
        }
    }

    /** When set, requests leave from this local address (another 127.x address stands in for another phone on the network). */
    static String source;

    static Reply raw(int port, byte[] request) throws IOException {
        Socket s = new Socket();
        if (source != null) s.bind(new java.net.InetSocketAddress(InetAddress.getByName(source), 0));
        s.connect(new java.net.InetSocketAddress(InetAddress.getByAddress(LOOP), port));
        s.setSoTimeout(6000);
        s.getOutputStream().write(request);
        s.getOutputStream().flush();
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        InputStream in = s.getInputStream();
        byte[] buf = new byte[4096];
        try {
            int n;
            while ((n = in.read(buf)) > 0) all.write(buf, 0, n);
        } catch (IOException e) {
            // a reset after the answer
        }
        s.close();
        Reply r = new Reply();
        String text = new String(all.toByteArray(), StandardCharsets.UTF_8);
        r.empty = text.isEmpty();
        int cut = text.indexOf("\r\n\r\n");
        if (cut < 0) return r;
        r.head = text.substring(0, cut);
        r.body = text.substring(cut + 4);
        r.status = Integer.parseInt(r.head.split(" ")[1]);
        return r;
    }

    /** A request with the headers a browser's page would send; null removes a default. */
    static Reply send(Env e, String method, String path, String body, String... extra) throws IOException {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Host", "127.0.0.1:" + e.port);
        if (method.equals("POST")) {
            h.put("Origin", e.origin);
            h.put("Content-Type", "application/json");
        }
        for (int i = 0; i + 1 < extra.length; i += 2) {
            if (extra[i + 1] == null) h.remove(extra[i]); else h.put(extra[i], extra[i + 1]);
        }
        byte[] b = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        StringBuilder q = new StringBuilder(method + " " + path + " HTTP/1.1\r\n");
        for (Map.Entry<String, String> x : h.entrySet()) q.append(x.getKey()).append(": ").append(x.getValue()).append("\r\n");
        if (method.equals("POST")) q.append("Content-Length: ").append(b.length).append("\r\n");
        q.append("\r\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(q.toString().getBytes(StandardCharsets.ISO_8859_1));
        out.write(b);
        return raw(e.port, out.toByteArray());
    }

    static String sessionOf(Reply r) { return (String) r.json().get("session"); }

    /** Pairs a phone the way the page does and returns its session value. */
    static String pair(Env e) throws IOException {
        Pairing.Offer o = e.ctl.offer();
        Reply p = send(e, "POST", "/api/pair", "{\"token\":\"" + o.token + "\"}");
        String id = (String) p.json().get("id");
        e.ctl.approve();
        Reply st = send(e, "POST", "/api/pair/status", "{\"id\":\"" + id + "\"}");
        return sessionOf(st);
    }

    static Reply authed(Env e, String method, String path, String body, String session) throws IOException {
        return send(e, method, path, body, "Cookie", "urtv=" + session, "X-UrTV-Session", session);
    }

    // ---------------------------------------------------------------- tests

    public static void main(String[] args) throws Exception {
        pairing();
        pairingLocales();
        sessions();
        guards();
        json();
        schema();
        updateRules();
        qr();
        off();
        http();
        controller();
        remoteV2();
        System.out.println(failed == 0 ? "ALL PASS" : failed + " FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }

    /** The phone remote's newer endpoints: apps, icons, launch, adhan stop, do, family, the new settings. Each needs the session, validates, and stays inside the limits. */
    static void remoteV2() throws Exception {
        Env e = started();
        String sess = pair(e);
        String[][] get = {{"/api/apps"}, {"/api/family"}, {"/api/icon/com.example.video"}};
        String[][] post = {{"/api/launch", "{\"target\":\"com.example.video\"}"}, {"/api/adhan_stop", "{}"}, {"/api/do", "{\"op\":\"screensaver\"}"}};
        boolean none = true;
        for (String[] g : get) none &= send(e, "GET", g[0], null).status == 401;
        for (String[] p : post) none &= send(e, "POST", p[0], p[1]).status == 401;
        check("v2: none of the new endpoints answers without a session", none && e.actions.calls.isEmpty(), e.actions.calls.toString());
        boolean noSecond = true;
        for (String[] p : post) noSecond &= send(e, "POST", p[0], p[1], "Cookie", "urtv=" + sess).status == 401 && send(e, "POST", p[0], p[1], "X-UrTV-Session", sess).status == 401;
        check("v2: the cookie alone or the header alone is not enough for them either", noSecond && e.actions.calls.isEmpty(), "");
        boolean origin = true;
        for (String[] p : post) origin &= authed(e, "POST", p[0], p[1], sess).status == 200
            && send(e, "POST", p[0], p[1], "Cookie", "urtv=" + sess, "X-UrTV-Session", sess, "Origin", "http://evil.example").status == 403
            && send(e, "POST", p[0], p[1], "Cookie", "urtv=" + sess, "X-UrTV-Session", sess, "Content-Type", "text/plain").status == 415;
        check("v2: a POST from another origin or not as JSON is refused, a proper one works", origin, e.actions.calls.toString());
        e.actions.calls.clear();

        e.now[0] += 6_000;   // the per-address rate is 100 requests in 5 seconds: a fresh window
        Reply apps = authed(e, "GET", "/api/apps", null, sess);
        check("v2: /api/apps lists the favourites and the pills with their lock", apps.status == 200 && apps.json().get("ok") == Boolean.TRUE
            && ((List<?>) apps.json().get("apps")).size() == 1 && ((List<?>) apps.json().get("tools")).size() == 1 && apps.body.contains("\"locked\":true"), apps.body);
        Reply fam = authed(e, "GET", "/api/family", null, sess);
        check("v2: /api/family is the read-only summary (no PIN in it)", fam.status == 200 && fam.body.contains("\"pin_set\":true") && !fam.body.toLowerCase().contains("\"pin\":"), fam.body);
        check("v2: there is no way to change the family lock from the phone (no such endpoint, a POST to /api/family is refused)",
            authed(e, "POST", "/api/family", "{\"kids\":true}", sess).status == 404 && authed(e, "POST", "/api/kids", "{\"on\":true}", sess).status == 404
            && authed(e, "POST", "/api/pin", "{\"pin\":\"1234\"}", sess).status == 404 && authed(e, "POST", "/api/reset", "{}", sess).status == 404
            && authed(e, "POST", "/api/uninstall", "{\"pkg\":\"com.example.video\"}", sess).status == 404 && authed(e, "POST", "/api/adb", "{}", sess).status == 404, "");
        Reply icon = authed(e, "GET", "/api/icon/com.example.video", null, sess);
        check("v2: an icon is a PNG the browser may keep for a day, for the session only", icon.status == 200 && "image/png".equals(icon.header("Content-Type"))
            && "private, max-age=86400".equals(icon.header("Cache-Control")) && "nosniff".equals(icon.header("X-Content-Type-Options")) && icon.body.contains("PNG"), icon.head);
        e.actions.calls.clear();
        Reply unknown = authed(e, "GET", "/api/icon/com.example.other", null, sess);
        boolean badNames = true;
        for (String bad : new String[] {"a%20b", "..%2f..%2fx", "com.x%3fy", "q", "a..b", "com.example.video.png/../x", "-a.b"}) {
            Reply r = authed(e, "GET", "/api/icon/" + bad, null, sess);
            badNames &= r.status == 404 && !e.actions.calls.toString().contains(bad);
        }
        check("v2: a package that is not a favourite is 404 and nothing else about the TV is revealed; a name that is no package name never reaches the TV code",
            unknown.status == 404 && "not_found".equals(unknown.json().get("error")) && badNames && e.actions.calls.equals(Arrays.asList("icon:com.example.other")), e.actions.calls.toString());
        check("v2: a query string on any address is refused by the server itself", authed(e, "GET", "/api/icon/com.example.video?x=1", null, sess).status == 404, "");
        e.actions.calls.clear();

        e.now[0] += 6_000;
        Reply l1 = authed(e, "POST", "/api/launch", "{\"target\":\"com.example.video\"}", sess);
        Reply l2 = authed(e, "POST", "/api/launch", "{\"target\":\"@tasks\"}", sess);
        check("v2: launch takes a package or one of our pills and answers what the TV said (opened, or pin_on_tv: the PIN is asked on the TV)",
            l1.status == 200 && "opened".equals(l1.json().get("result")) && l2.status == 200 && "pin_on_tv".equals(l2.json().get("result"))
            && e.actions.calls.equals(Arrays.asList("launch:com.example.video", "launch:@tasks")), e.actions.calls.toString() + l1.body + l2.body);
        e.actions.calls.clear();
        boolean badLaunch = true;
        for (String body : new String[] {"{}", "{\"target\":\"rm -rf /\"}", "{\"target\":\"@wifi\"}", "{\"target\":\"@urtv\"}", "{\"target\":\"@parental\"}", "{\"target\":\"x\"}", "{\"target\":5}",
                "{\"target\":\"com.example.video\",\"pin\":\"1234\"}", "{\"target\":\"com.example.video\",\"x\":1}", "{\"target\":\"intent:#Intent;end\"}", "{\"target\":\"com.example.video; reboot\"}"}) {
            badLaunch &= authed(e, "POST", "/api/launch", body, sess).status == 400;
        }
        check("v2: launch refuses anything else (another pill, a word, a command, a PIN: the phone never sends the PIN for a launch) and nothing reaches the TV code", badLaunch && e.actions.calls.isEmpty(), e.actions.calls.toString());

        check("v2: adhan_stop and do work with their exact bodies", authed(e, "POST", "/api/adhan_stop", "{}", sess).status == 200
            && authed(e, "POST", "/api/do", "{\"op\":\"screensaver\"}", sess).status == 200 && authed(e, "POST", "/api/do", "{\"op\":\"sleep_cancel\"}", sess).status == 200
            && e.actions.calls.equals(Arrays.asList("adhan_stop", "do:screensaver", "do:sleep_cancel")), e.actions.calls.toString());
        e.actions.calls.clear();
        boolean badDo = authed(e, "POST", "/api/adhan_stop", "{\"x\":1}", sess).status == 400;
        for (String op : new String[] {"reboot", "reset", "shell", "uninstall", "kids", "unlock", ""}) badDo &= authed(e, "POST", "/api/do", "{\"op\":\"" + op + "\"}", sess).status == 400;
        badDo &= authed(e, "POST", "/api/do", "{}", sess).status == 400 && authed(e, "POST", "/api/do", "{\"op\":\"screensaver\",\"x\":1}", sess).status == 400;
        check("v2: do takes screensaver and sleep_cancel only; adhan_stop no extra keys", badDo && e.actions.calls.isEmpty(), e.actions.calls.toString());

        e.now[0] += 6_000;
        boolean keysOk = true;
        for (String k : new String[] {"up", "down", "left", "right", "ok", "back", "home", "vol_up", "vol_down", "mute", "play_pause"}) keysOk &= authed(e, "POST", "/api/key", "{\"key\":\"" + k + "\"}", sess).status == 200;
        boolean keysBad = true;
        for (String k : new String[] {"power", "menu", "settings", "app_switch", "sleep", "wakeup", "channel_up", "tv_input", "0", "assist", "search", "UP", "up ", "reboot"}) keysBad &= authed(e, "POST", "/api/key", "{\"key\":\"" + k + "\"}", sess).status == 400;
        check("v2: the keys are exactly the allow-list (DPAD, OK, BACK, HOME, VOLUME, MUTE, PLAY_PAUSE), nothing else", keysOk && keysBad, "");
        e.actions.calls.clear();

        e.now[0] += 6_000;
        boolean setBad = true;
        for (String body : new String[] {"{\"sleep_minutes\":45}", "{\"sleep_minutes\":-5}", "{\"sleep_minutes\":\"30\"}", "{\"saver_idle\":7}", "{\"saver_idle\":0}", "{\"saver_idle\":\"10\"}", "{\"sleep_minutes\":true}"}) setBad &= authed(e, "POST", "/api/set", body, sess).status == 400;
        check("v2: set refuses a sleep or idle value that is not one of the lists", setBad && e.actions.calls.isEmpty(), e.actions.calls.toString());
        e.now[0] += 6_000;
        boolean setOk = true;
        for (String body : new String[] {"{\"sleep_minutes\":0}", "{\"sleep_minutes\":15}", "{\"sleep_minutes\":90}", "{\"saver_idle\":1}", "{\"saver_idle\":30}"}) setOk &= authed(e, "POST", "/api/set", body, sess).status == 200;
        check("v2: set takes the sleep minutes (0 cancels) and the idle minutes of the lists", setOk && e.actions.calls.size() == 5, e.actions.calls.toString());
        e.actions.calls.clear();
        // the owner's TV test (fix7/8): a card, then «بدون» (an empty list) and «لا شيء» (none): each reaches the actions as it was sent
        e.now[0] += 6_000;
        boolean cardsOk = authed(e, "POST", "/api/set", "{\"home_cards\":[\"nextprayer\"]}", sess).status == 200;
        e.now[0] += 6_000;
        cardsOk &= authed(e, "POST", "/api/set", "{\"home_cards\":[]}", sess).status == 200;
        e.now[0] += 6_000;
        cardsOk &= authed(e, "POST", "/api/set", "{\"home_cards\":[\"none\"]}", sess).status == 200;
        check("v2: set takes a card, then an empty list, then none: all three reach the actions", cardsOk
            && e.actions.calls.equals(Arrays.asList("set:[home_cards]", "cards:[nextprayer]", "set:[home_cards]", "cards:[]", "set:[home_cards]", "cards:[none]")), e.actions.calls.toString());
        e.actions.calls.clear();
        e.now[0] += 6_000;
        check("v2: text takes a mode (append or replace) and search (a boolean), passed on as asked",
            authed(e, "POST", "/api/text", "{\"text\":\"ماء\",\"mode\":\"append\"}", sess).status == 200
            && authed(e, "POST", "/api/text", "{\"text\":\"ماء\",\"mode\":\"replace\",\"search\":true}", sess).status == 200
            && e.actions.calls.equals(Arrays.asList("text:ماء:append:false", "text:ماء:replace:true")), e.actions.calls.toString());
        e.actions.calls.clear();
        boolean textBad = true;
        for (String body : new String[] {"{\"text\":\"a\",\"mode\":\"insert\"}", "{\"text\":\"a\",\"mode\":5}", "{\"text\":\"a\",\"search\":\"yes\"}", "{\"text\":\"a\",\"search\":1}",
                "{\"text\":\"a\",\"pkg\":\"com.example.video\"}", "{\"text\":\"a\\u202eb\"}", "{\"text\":\"a\\u0000b\"}", "{\"text\":\"\"}", "{\"text\":\"   \"}", "{\"mode\":\"append\"}"}) {
            textBad &= authed(e, "POST", "/api/text", body, sess).status == 400;
        }
        check("v2: text refuses another mode or search value, an extra key (no way to name an app), direction overrides, controls and empty text", textBad && e.actions.calls.isEmpty(), e.actions.calls.toString());
        check("v2: /api/state counts the connected phones", authed(e, "GET", "/api/state", null, sess).json().get("phones") instanceof Long, "");
        e.ctl.stop("owner");

        // the PIN protects the new settings, and is not asked for the launches (the TV asks)
        e = started();
        e.ctl.setPinGate(new PinGate() {
            @Override public boolean required() { return true; }
            @Override public boolean verify(String pin) { return "1234".equals(pin); }
        });
        sess = pair(e);
        e.actions.calls.clear();
        Reply sm = authed(e, "POST", "/api/set", "{\"sleep_minutes\":30}", sess);
        Reply si = authed(e, "POST", "/api/set", "{\"saver_idle\":5}", sess);
        check("v2: with a PIN set, the sleep timer and the idle time ask for it like every setting, and nothing was done without it",
            sm.status == 403 && "pin_required".equals(sm.json().get("error")) && si.status == 403 && e.actions.calls.isEmpty(), sm.body + si.body);
        e.actions.calls.clear();
        Reply sc = authed(e, "POST", "/api/do", "{\"op\":\"sleep_cancel\"}", sess);
        Reply scBad = authed(e, "POST", "/api/do", "{\"op\":\"sleep_cancel\",\"pin\":\"9999\"}", sess);
        check("v2: with a PIN set, cancelling the sleep timer asks for it (a child's phone cannot undo a bedtime timer) and nothing was done without it",
            sc.status == 403 && "pin_required".equals(sc.json().get("error")) && scBad.status == 403 && "pin_wrong".equals(scBad.json().get("error")) && e.actions.calls.isEmpty(), sc.body + scBad.body);
        check("v2: the right PIN lets the cancel through and the PIN goes no further", authed(e, "POST", "/api/do", "{\"op\":\"sleep_cancel\",\"pin\":\"1234\"}", sess).status == 200 && e.actions.calls.equals(Arrays.asList("do:sleep_cancel")), e.actions.calls.toString());
        e.actions.calls.clear();
        check("v2: the screensaver stays open to the phone (it only opens a screen; a locked saver asks on the TV)", authed(e, "POST", "/api/do", "{\"op\":\"screensaver\",\"pin\":\"12345\"}", sess).status == 400
            && authed(e, "POST", "/api/do", "{\"op\":\"screensaver\"}", sess).status == 200, "");
        check("v2: with the right PIN they go through", authed(e, "POST", "/api/set", "{\"sleep_minutes\":30,\"pin\":\"1234\"}", sess).status == 200 && e.actions.calls.contains("set:[sleep_minutes]"), e.actions.calls.toString());
        check("v2: launching, stopping the adhan, the screensaver and reading the family card never ask the phone for the PIN (a locked app asks on the TV)",
            authed(e, "POST", "/api/launch", "{\"target\":\"@tasks\"}", sess).status == 200 && authed(e, "POST", "/api/adhan_stop", "{}", sess).status == 200
            && authed(e, "POST", "/api/do", "{\"op\":\"screensaver\"}", sess).status == 200 && authed(e, "GET", "/api/family", null, sess).status == 200
            && authed(e, "GET", "/api/apps", null, sess).status == 200, "");
        e.ctl.stop("owner");

        // Schema, directly
        check("v2: Schema.target takes packages and our five pills only", Schema.target("com.example.video") && Schema.target("@tasks") && Schema.target("@saver") && Schema.target("@inputs")
            && Schema.target("@settings") && Schema.target("@listen") && !Schema.target("@wifi") && !Schema.target("@urtv") && !Schema.target("@parental") && !Schema.target("a") && !Schema.target(null)
            && !Schema.target("com.example.") && !Schema.target("com..x") && !Schema.target(new StringBuilder("x").toString()), "");
        check("v2: Schema.packageName is bounded", !Schema.packageName("a." + "b".repeat(200)) && Schema.packageName("sa.urtv.home"), "");
        boolean unique = true;
        for (String t : Schema.TOOLS) unique &= t.startsWith("@") && t.length() > 2;
        check("v2: the sleep and idle lists are the app's (SleepTimer.MINUTES plus 0; Prefs.IDLE_MINUTES)", unique && Arrays.toString(Schema.SLEEP_MINUTES).equals("[0, 15, 30, 60, 90]") && Arrays.toString(Schema.SAVER_IDLE).equals("[1, 2, 5, 10, 15, 30]"), "");
    }

    /** The code the TV shows and the one the phone sends are both ASCII digits, whatever language the TV is set to (ar-SA and fa-IR format %d with their own digits). */
    static void pairingLocales() {
        java.util.Locale was = java.util.Locale.getDefault();
        try {
            for (String tag : new String[] {"ar-SA", "fa-IR", "hi-IN", "en-US"}) {
                java.util.Locale.setDefault(java.util.Locale.forLanguageTag(tag));
                for (final int n : new int[] {7, 42, 123456, 999999, 0}) {
                    SecureRandom fixed = new SecureRandom() {
                        @Override public int nextInt(int bound) { return n; }
                    };
                    Pairing p = new Pairing(fixed);
                    Pairing.Offer o = p.offer(1000);
                    String want = String.format(java.util.Locale.ROOT, "%06d", n);
                    check("pairing code " + want + " under " + tag + ": six ASCII digits", o.code.equals(want) && o.code.matches("[0-9]{6}") && o.token.matches("[0-9a-f]{32}"), o.code);
                    check("pairing code " + want + " under " + tag + ": the phone's ASCII code is accepted", p.redeem(want, true, 1001) == Pairing.Result.OK && p.wrongTries() == 0, "");
                }
            }
        } finally {
            java.util.Locale.setDefault(was);
        }
    }

    static void pairing() {
        SecureRandom rnd = new SecureRandom();
        Pairing p = new Pairing(rnd);
        long t = 5000;
        Pairing.Offer o = p.offer(t);
        check("token is 128 bits (32 hex digits), code is 6 digits", o.token.matches("[0-9a-f]{32}") && o.code.matches("[0-9]{6}"), o.token + " " + o.code);
        check("a token works once", p.redeem(o.token, false, t + 1) == Pairing.Result.OK && p.redeem(o.token, false, t + 2) == Pairing.Result.WRONG, "");
        o = p.offer(t);
        check("using the token spends the code too", p.redeem(o.token, false, t + 1) == Pairing.Result.OK && p.redeem(o.code, true, t + 2) == Pairing.Result.WRONG, "");
        o = p.offer(t);
        check("the code works once", p.redeem(o.code, true, t + 1) == Pairing.Result.OK && p.redeem(o.code, true, t + 2) == Pairing.Result.WRONG, "");
        o = p.offer(t);
        check("a token works up to the last millisecond before two minutes", p.redeem(o.token, false, t + Pairing.TTL_MS - 1) == Pairing.Result.OK, "");
        o = p.offer(t);
        check("a token is dead at two minutes", p.redeem(o.token, false, t + Pairing.TTL_MS) != Pairing.Result.OK, "");
        p = new Pairing(rnd);   // (the tries above were counted: start clean)
        Pairing.Offer a = p.offer(t), b = p.offer(t);
        check("a new offer ends the old one", !a.token.equals(b.token) && p.redeem(a.token, false, t + 1) == Pairing.Result.WRONG && p.redeem(b.token, false, t + 1) == Pairing.Result.OK, "");
        check("a spent or expired offer is not shown", p.current(t + 10) == null, "");

        Pairing q = new Pairing(rnd);
        Pairing.Offer g = q.offer(t);
        Pairing.Result last = null;
        int wrong = 0;
        for (int i = 0; i < Pairing.MAX_WRONG; i++) { last = q.redeem("00000000000000000000000000000000", false, t + 1); wrong++; }
        check("five wrong tries lock it", last == Pairing.Result.LOCKED && q.locked() && wrong == 5, "" + last);
        check("locked: even the right token is refused, and no new offer is made", q.redeem(g.token, false, t + 2) == Pairing.Result.LOCKED && q.offer(t + 3) == null, "");
        Pairing r = new Pairing(rnd);
        Pairing.Offer h = r.offer(t);
        for (int i = 0; i < Pairing.MAX_WRONG - 1; i++) r.redeem("x", true, t + 1);
        check("four wrong tries do not lock it", !r.locked() && r.redeem(h.token, false, t + 2) == Pairing.Result.OK, "");
        Pairing s = new Pairing(rnd);
        Pairing.Offer late = s.offer(t);
        s.redeem(late.token, false, t + Pairing.TTL_MS + 1);
        check("a token that came late counts as a wrong try", s.wrongTries() == 1, "" + s.wrongTries());
        check("Pairing.same is false for null and for a different length", !Pairing.same(null, "a") && !Pairing.same("a", null) && !Pairing.same("a", "ab") && Pairing.same("ab", "ab"), "");
        Set<String> seen = new HashSet<>();
        Pairing u = new Pairing(rnd);
        for (int i = 0; i < 200; i++) seen.add(u.offer(t).token);
        check("tokens do not repeat", seen.size() == 200, "" + seen.size());
    }

    static void sessions() throws Exception {
        Sessions s = new Sessions(new SecureRandom());
        long t = 10_000;
        String a = s.issue("p", t);
        check("a session value is 256 bits (64 hex digits)", a != null && a.matches("[0-9a-f]{64}"), a);
        check("a wrong value is no session", !s.check("0".repeat(64), t + 1) && !s.check(null, t + 1), "");
        check("use keeps it alive: idle for 29 minutes is fine, again 29 minutes later too",
            s.check(a, t + 29 * 60_000L) && s.check(a, t + 58 * 60_000L), "");
        check("30 minutes idle ends it", !s.check(a, t + 58 * 60_000L + Sessions.IDLE_MS), "");
        String b = s.issue("p", t);
        boolean alive = true;
        for (long at = t; at < t + Sessions.MAX_MS - 1; at += 20 * 60_000L) alive &= s.check(b, at);
        check("a busy session lives until four hours", alive && s.check(b, t + Sessions.MAX_MS - 1), "");
        check("four hours at most, however busy it was", !s.check(b, t + Sessions.MAX_MS), "");
        String c = s.issue("p", t + Sessions.MAX_MS);
        String id = s.idOf(c, t + Sessions.MAX_MS);
        check("the owner can cancel one", s.revoke(id) && !s.check(c, t + Sessions.MAX_MS + 1) && !s.revoke(id), "");
        for (int i = 0; i < Sessions.MAX_SESSIONS; i++) s.issue("p", t + Sessions.MAX_MS + 5);
        check("no more than five phones at once", s.issue("p", t + Sessions.MAX_MS + 6) == null && s.count(t + Sessions.MAX_MS + 6) == Sessions.MAX_SESSIONS, "");
        boolean leaks = false;
        String secret = s.issue("p", t + 2 * Sessions.MAX_MS);
        s.clear();
        secret = s.issue("p", t + 2 * Sessions.MAX_MS);
        for (Sessions.Info i : s.list(t + 2 * Sessions.MAX_MS)) {
            for (java.lang.reflect.Field f : Sessions.Info.class.getDeclaredFields()) {
                f.setAccessible(true);
                if (String.valueOf(f.get(i)).contains(secret)) leaks = true;
            }
        }
        check("what the TV lists about a session never holds its value", !leaks, "");
    }

    static void guards() throws Exception {
        check("Host must be exactly address:port", Guard.hostOk("h:50000", "h", 50000) && !Guard.hostOk("h", "h", 50000) && !Guard.hostOk("h:50001", "h", 50000)
            && !Guard.hostOk("evil.example:50000", "h", 50000) && !Guard.hostOk(null, "h", 50000) && !Guard.hostOk("h:50000.evil.example", "h", 50000), "");
        check("Origin must be exactly this server", Guard.originOk("http://h:50000", "h", 50000) && !Guard.originOk(null, "h", 50000) && !Guard.originOk("null", "h", 50000)
            && !Guard.originOk("https://h:50000", "h", 50000) && !Guard.originOk("http://evil.example", "h", 50000) && !Guard.originOk("http://h:50000.evil.example", "h", 50000), "");
        check("a browser that says cross-site is refused; none or same-origin pass", !Guard.fetchSiteOk("cross-site") && !Guard.fetchSiteOk("same-site") && Guard.fetchSiteOk(null)
            && Guard.fetchSiteOk("same-origin") && Guard.fetchSiteOk("none"), "");
        int[][] yes = {{10, 0, 0, 1}, {10, 255, 255, 255}, {172, 16, 0, 1}, {172, 31, 255, 255}, {192, 168, 0, 1}, {192, 168, 100, 20}};
        int[][] no = {{172, 15, 255, 255}, {172, 32, 0, 1}, {192, 167, 0, 1}, {192, 169, 0, 1}, {11, 0, 0, 1}, {9, 255, 255, 255}, {8, 8, 8, 8}, {127, 0, 0, 1},
            {169, 254, 1, 1}, {100, 64, 0, 1}, {0, 0, 0, 0}, {255, 255, 255, 255}, {224, 0, 0, 1}};
        boolean okYes = true, okNo = true;
        for (int[] a : yes) okYes &= Guard.privateClient(addr(a[0], a[1], a[2], a[3]));
        for (int[] a : no) okNo &= !Guard.privateClient(addr(a[0], a[1], a[2], a[3]));
        check("private-range clients are accepted (10/8, 172.16/12, 192.168/16)", okYes, "");
        check("every other address is refused (public, loopback, link-local, shared, multicast, wildcard)", okNo, "");
        InetAddress v6 = InetAddress.getByAddress(new byte[] {(byte) 0xfd, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1});
        check("an IPv6 client is refused", !Guard.privateClient(v6), "");
        List<Guard.Candidate> c = new ArrayList<>();
        c.add(new Guard.Candidate("eth0", ip(192, 168, 1, 5), true));
        c.add(new Guard.Candidate("wlan0", ip(8, 8, 4, 4), true));
        check("only the Wi-Fi interface counts, and only with a private address", Guard.wifiAddress(c) == null, "");
        c.add(new Guard.Candidate("wlan0", ip(192, 168, 1, 77), false));
        check("a Wi-Fi interface that is down is not used", Guard.wifiAddress(c) == null, "");
        c.add(new Guard.Candidate("wlan0", ip(10, 1, 2, 3), true));
        byte[] w = Guard.wifiAddress(c);
        check("the Wi-Fi address is the private IPv4 of the up wlan interface", w != null && w[0] == 10 && w.length == 4, "");
        check("no wildcard address is ever chosen", Guard.wifiAddress(Arrays.asList(new Guard.Candidate("wlan0", ip(0, 0, 0, 0), true))) == null, "");
        Guard.Rate rate = new Guard.Rate(3, 1000);
        check("rate limit: three per second per client", rate.allow("a", 0) && rate.allow("a", 1) && rate.allow("a", 2) && !rate.allow("a", 3) && rate.allow("b", 3) && rate.allow("a", 1001), "");
    }

    static void json() {
        String[] bad = {"", "{", "{\"a\":1}x", "{\"a\":1,\"a\":2}", "{\"a\":1.5}", "{\"a\":1e3}", "{\"a\":1234567890}", "{\"a\":01}", "{\"a\":\"\u0001\"}",
            "{\"a\":[[[[[1]]]]]}", "{'a':1}", "{\"a\":NaN}", "{\"a\":\"\\x\"}", "{\"a\":\"\\u12\"}", "[1]", "\"x\"", "{\"a\":}", "{\"a\" 1}", "{,}"};
        boolean allBad = true;
        String which = "";
        for (String b : bad) {
            try { Json.parseObject(b); allBad = false; which = b; } catch (Json.Bad e) { /* wanted */ }
        }
        check("the JSON reader refuses what it should", allBad, which);
        try {
            Map<String, Object> m = Json.parseObject("{\"s\":\"\\u0627\\u0644\\n\",\"n\":-12,\"b\":true,\"z\":null,\"l\":[\"a\",1],\"o\":{\"k\":false}}");
            check("it reads strings with escapes, whole numbers, booleans, null, lists and objects",
                "ال\n".equals(m.get("s")) && Long.valueOf(-12).equals(m.get("n")) && Boolean.TRUE.equals(m.get("b")) && m.containsKey("z") && ((List<?>) m.get("l")).size() == 2, m.toString());
        } catch (Json.Bad e) {
            check("it reads valid JSON", false, e.getMessage());
        }
        check("quoting escapes markup and quotes", Json.quote("<a href=\"x\">&</a>").equals("\"\\u003ca href=\\\"x\\\"\\u003e\\u0026\\u003c/a\\u003e\""), Json.quote("<a href=\"x\">&</a>"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("n", 5L);
        m.put("l", Arrays.asList("a", "ب"));
        check("writing makes the JSON the page reads", Json.write(m).equals("{\"ok\":true,\"n\":5,\"l\":[\"a\",\"ب\"]}"), Json.write(m));
    }

    static boolean ok(String endpoint, String json) {
        try { return Schema.check(endpoint, Json.parseObject(json)).ok(); } catch (Json.Bad e) { return false; }
    }

    static void schema() {
        String tok = "0123456789abcdef0123456789abcdef";
        Object[][] cases = {
            {"pair", "{\"token\":\"" + tok + "\"}", true}, {"pair", "{\"code\":\"123456\"}", true}, {"pair", "{\"code\":\"12345\"}", false}, {"pair", "{\"code\":123456}", false},
            {"pair", "{\"token\":\"" + tok + "\",\"code\":\"123456\"}", false}, {"pair", "{}", false}, {"pair", "{\"token\":\"" + tok.toUpperCase() + "\"}", false},
            {"pair", "{\"token\":\"" + tok + "\",\"x\":1}", false}, {"pairstatus", "{\"id\":\"" + tok + "\"}", true}, {"pairstatus", "{\"id\":\"../../x\"}", false},
            {"key", "{\"key\":\"up\"}", true}, {"key", "{\"key\":\"play_pause\"}", true}, {"key", "{\"key\":\"up;reboot\"}", false}, {"key", "{\"key\":\"power\"}", false},
            {"key", "{\"key\":\"up\",\"extra\":1}", false}, {"key", "{\"key\":5}", false}, {"key", "{}", false},
            {"text", "{\"text\":\"مرحبا بالعالم\"}", true}, {"text", "{\"text\":\"\"}", false}, {"text", "{\"text\":\"   \"}", false}, {"text", "{\"text\":\"a\\nb\"}", false},
            {"text", "{\"text\":\"a\\u202eb\"}", false}, {"text", "{\"text\":\"" + "x".repeat(121) + "\"}", false}, {"text", "{\"text\":\"" + "x".repeat(120) + "\"}", true},
            {"text", "{\"text\":\"a\\u0000b\"}", false}, {"cities", "{\"q\":\"مكة\"}", true}, {"cities", "{\"q\":\"<script>\"}", false}, {"cities", "{\"q\":\"a'; DROP TABLE x;--\"}", false},
            {"cities", "{\"q\":\"../../etc\"}", false}, {"cities", "{\"q\":\"" + "x".repeat(31) + "\"}", false},
            {"set", "{\"city\":\"الرياض\"}", true}, {"set", "{\"city\":\"ليلى (الأفلاج)\"}", true}, {"set", "{\"city\":\"a|b;c\"}", false}, {"set", "{\"city\":\"x\",\"lat\":\"1\"}", false},
            {"set", "{\"home_cards\":[\"prayers\",\"hourly\",\"verse\"]}", true}, {"set", "{\"home_cards\":[]}", true}, {"set", "{\"home_cards\":[\"a\",\"b\",\"c\",\"d\"]}", false},
            {"set", "{\"home_cards\":[\"prayers\",\"prayers\"]}", false}, {"set", "{\"home_cards\":[\"x y\"]}", false}, {"set", "{\"home_cards\":\"prayers\"}", false},
            {"set", "{\"adhan_on\":true}", true}, {"set", "{\"adhan_on\":\"true\"}", false}, {"set", "{\"adhan_voice\":21}", true}, {"set", "{\"adhan_voice\":22}", false},
            {"set", "{\"adhan_voice\":0}", false}, {"set", "{\"adhan_fajr_voice\":1}", true}, {"set", "{\"adhan_volume\":10}", true}, {"set", "{\"adhan_volume\":9}", false},
            {"set", "{\"adhan_volume\":101}", false}, {"set", "{\"adhan_volume\":50.5}", false}, {"set", "{\"adhan_short\":false,\"adhan_stopkey\":true}", true},
            {"set", "{\"pin\":\"1234\"}", false}, {"set", "{\"adhan_on\":true,\"pin\":\"1234\"}", true}, {"set", "{\"adhan_on\":true,\"pin\":\"12\"}", false}, {"set", "{\"adhan_on\":true,\"pin\":\"12345\"}", false}, {"set", "{\"adhan_on\":true,\"pin\":\"12345678\"}", false},
            {"set", "{\"adhan_on\":true,\"pin\":1234}", false}, {"set", "{\"reset\":true}", false}, {"set", "{\"uninstall\":\"x\"}", false}, {"set", "{}", false},
            {"quran", "{\"op\":\"play\",\"reciter\":\"abdulbasit_murattal\",\"surah\":18}", true}, {"quran", "{\"op\":\"play\",\"surah\":18}", false},
            {"quran", "{\"op\":\"play\",\"reciter\":\"x y\",\"surah\":1}", false}, {"quran", "{\"op\":\"play\",\"reciter\":\"r\",\"surah\":115}", false},
            {"quran", "{\"op\":\"play\",\"reciter\":\"r\",\"surah\":0}", false}, {"quran", "{\"op\":\"play\",\"reciter\":\"r\",\"surah\":\"5\"}", false},
            {"quran", "{\"op\":\"pause\"}", true}, {"quran", "{\"op\":\"next\",\"surah\":3}", false}, {"quran", "{\"op\":\"sleep\",\"minutes\":30}", true},
            {"quran", "{\"op\":\"sleep\",\"minutes\":0}", true}, {"quran", "{\"op\":\"sleep\",\"minutes\":181}", false}, {"quran", "{\"op\":\"sleep\"}", false},
            {"quran", "{\"op\":\"delete\"}", false}, {"update", "{\"op\":\"check\"}", true}, {"update", "{\"op\":\"install\",\"pin\":\"1234\"}", true}, {"update", "{\"op\":\"uninstall\"}", false},
            {"logout", "{}", true}, {"logout", "{\"a\":1}", false}, {"reset", "{}", false}, {"adb", "{}", false},
        };
        List<String> wrong = new ArrayList<>();
        for (Object[] c : cases) if (ok((String) c[0], (String) c[1]) != (Boolean) c[2]) wrong.add(c[0] + " " + c[1]);
        check("the allow-list: " + cases.length + " bodies decided as they should be (keys, types, ranges, injection-shaped strings)", wrong.isEmpty(), wrong.toString());
        check("Schema.ADHAN_VOICES is 21", Schema.ADHAN_VOICES == 21, "");
    }

    static void updateRules() {
        Set<String> mine = new HashSet<>(Arrays.asList("a".repeat(64))), same = new HashSet<>(Arrays.asList("a".repeat(64))), other = new HashSet<>(Arrays.asList("b".repeat(64)));
        check("same app, newer, same signer: allowed", UpdateRules.verify("p", "p", 5, 6, mine, same) == null, "");
        check("a different signer is refused", "other_key".equals(UpdateRules.verify("p", "p", 5, 6, mine, other)), "");
        Set<String> two = new HashSet<>(Arrays.asList("a".repeat(64), "b".repeat(64)));
        check("an extra signer is refused too (the sets must be equal)", "other_key".equals(UpdateRules.verify("p", "p", 5, 6, mine, two)), "");
        check("another package is refused", "other_app".equals(UpdateRules.verify("p", "q", 5, 6, mine, same)), "");
        check("the same or an older version is refused", "not_newer".equals(UpdateRules.verify("p", "p", 5, 5, mine, same)) && "not_newer".equals(UpdateRules.verify("p", "p", 5, 4, mine, same)), "");
        check("no certificate on either side is refused", "unsigned".equals(UpdateRules.verify("p", "p", 5, 6, mine, new HashSet<String>())) && "unsigned".equals(UpdateRules.verify("p", "p", 5, 6, new HashSet<String>(), same))
            && "unsigned".equals(UpdateRules.verify("p", "p", 5, 6, null, same)), "");
        check("a digest that is not 64 hex digits is refused", "unsigned".equals(UpdateRules.verify("p", "p", 5, 6, new HashSet<>(Arrays.asList("zz")), new HashSet<>(Arrays.asList("zz")))), "");
        check("version.txt is read as a build number", UpdateRules.buildOf("3.51\n") == 51 && UpdateRules.buildOf("3.5.1") == -1 && UpdateRules.buildOf("<html>") == -1 && UpdateRules.buildOf(null) == -1 && UpdateRules.buildOf("") == -1, "");
        check("the APKs come from the apk-latest release over https, the screensaver first and the home screen last",
            UpdateRules.BASE.equals("https://github.com/tshi-dev/UrTV/releases/download/apk-latest/") && UpdateRules.APKS[0][0].equals("sa.urtv.screensaver") && UpdateRules.APKS[1][0].equals("sa.urtv.home"), "");
        check("the update channel: nothing, latest and any unknown word read the latest; only next reads apk-next",
            UpdateRules.baseFor(null).equals(UpdateRules.BASE) && UpdateRules.baseFor("latest").equals(UpdateRules.BASE) && UpdateRules.baseFor("nope").equals(UpdateRules.BASE)
            && UpdateRules.baseFor("next").equals("https://github.com/tshi-dev/UrTV/releases/download/apk-next/"), "");
        check("a download is only ever a file of our two releases",
            UpdateRules.isReleaseUrl(UpdateRules.BASE + "urtv-home.apk") && UpdateRules.isReleaseUrl(UpdateRules.NEXT_BASE + "urtv-home.apk")
            && UpdateRules.isReleaseUrl(UpdateRules.NEXT_BASE + "version.txt") && UpdateRules.isReleaseUrl(UpdateRules.BASE + "urtv-screensaver.apk")
            && !UpdateRules.isReleaseUrl(UpdateRules.BASE + "other.apk") && !UpdateRules.isReleaseUrl(UpdateRules.NEXT_BASE + "../apk-latest/urtv-home.apk.evil") && !UpdateRules.isReleaseUrl(UpdateRules.BASE)
            && !UpdateRules.isReleaseUrl("https://github.com/tshi-dev/UrTV/releases/download/v3.1/urtv-home.apk") && !UpdateRules.isReleaseUrl("https://example.org/") && !UpdateRules.isReleaseUrl(null), "");
    }

    static void qr() {
        int[] cap = {14, 26, 42, 62, 84, 106, 122, 152, 180, 213};
        boolean fits = true;
        String bad = "";
        for (int v = 1; v <= 10; v++) {
            QrCode atCap = QrCode.encode("a".repeat(cap[v - 1]));
            if (atCap.version != v) { fits = false; bad += " cap" + v + "->" + atCap.version; }
            if (v < 10 && QrCode.encode("a".repeat(cap[v - 1] + 1)).version != v + 1) { fits = false; bad += " over" + v; }
            if (atCap.size != 4 * v + 17) { fits = false; bad += " size" + v; }
        }
        check("each version is chosen at its byte capacity (level M), 1 to 10", fits, bad);
        boolean tooLong = false;
        try { QrCode.encode("a".repeat(214)); } catch (IllegalArgumentException e) { tooLong = true; }
        check("a text that does not fit is refused", tooLong, "");
        String url = "http://" + "192" + "." + "168" + "." + "100" + "." + "100" + ":65535/p#0123456789abcdef0123456789abcdef";
        QrCode q = QrCode.encode(url);
        check("the pairing address (63 characters) is a version 5 code, 37 modules wide", q.version == 5 && q.size == 37, q.version + " " + q.size);
        boolean finders = true;
        int[][] corners = {{0, 0}, {q.size - 7, 0}, {0, q.size - 7}};
        for (int[] c : corners) {
            for (int dy = 0; dy < 7; dy++) for (int dx = 0; dx < 7; dx++) {
                boolean want = dx == 0 || dx == 6 || dy == 0 || dy == 6 || (dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4);
                finders &= q.dark(c[0] + dx, c[1] + dy) == want;
            }
        }
        check("the three finder patterns are in place", finders, "");
        boolean timing = true;
        for (int i = 8; i < q.size - 8; i++) timing &= q.dark(i, 6) == (i % 2 == 0) && q.dark(6, i) == (i % 2 == 0);
        check("the timing lines alternate, and the dark module is set", timing && q.dark(8, q.size - 8), "");
        int f1 = 0, f2 = 0;
        for (int i = 0; i <= 5; i++) f1 |= (q.dark(8, i) ? 1 : 0) << i;
        f1 |= (q.dark(8, 7) ? 1 : 0) << 6; f1 |= (q.dark(8, 8) ? 1 : 0) << 7; f1 |= (q.dark(7, 8) ? 1 : 0) << 8;
        for (int i = 9; i < 15; i++) f1 |= (q.dark(14 - i, 8) ? 1 : 0) << i;
        for (int i = 0; i < 8; i++) f2 |= (q.dark(q.size - 1 - i, 8) ? 1 : 0) << i;
        for (int i = 8; i < 15; i++) f2 |= (q.dark(8, q.size - 15 + i) ? 1 : 0) << i;
        check("both copies of the format information agree and name the mask used", f1 == f2 && f1 == QrCode.formatValue(q.mask), f1 + " " + f2 + " " + QrCode.formatValue(q.mask));
        check("the same text always gives the same code", Arrays.equals(bits(QrCode.encode(url)), bits(q)), "");
        // Reed-Solomon: the data followed by its check bytes leaves nothing over
        byte[] data = new byte[40];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i * 37 + 5);
        byte[] gen = QrCode.generator(18);
        byte[] ec = QrCode.remainder(data, gen);
        byte[] whole = new byte[data.length + ec.length];
        System.arraycopy(data, 0, whole, 0, data.length);
        System.arraycopy(ec, 0, whole, data.length, ec.length);
        boolean zero = true;
        for (byte b : QrCode.remainder(whole, gen)) zero &= b == 0;
        check("Reed-Solomon: data plus check bytes divides by the generator with no remainder", zero, "");
        check("GF(256) multiplication: 2*128 = 29 (the field's polynomial 0x11D)", QrCode.mul(2, 128) == 29 && QrCode.mul(0, 77) == 0 && QrCode.mul(1, 77) == 77, "" + QrCode.mul(2, 128));
    }

    static boolean[] bits(QrCode q) {
        boolean[] b = new boolean[q.size * q.size];
        for (int y = 0; y < q.size; y++) for (int x = 0; x < q.size; x++) b[y * q.size + x] = q.dark(x, y);
        return b;
    }

    static int liveThreads() {
        int n = 0;
        for (Thread t : Thread.getAllStackTraces().keySet()) if (t.isAlive() && t.getName().startsWith("phone-")) n++;
        return n;
    }

    static void off() throws Exception {
        Env e = env(new Predicate<InetAddress>() { @Override public boolean test(InetAddress a) { return Guard.privateClient(a); } });
        e.ctl.setPinGate(null);
        e.ctl.setListener(null);
        boolean quiet = e.ctl.offer() == null && e.ctl.pending() == null && e.ctl.phones().isEmpty() && e.ctl.address() == null && !e.ctl.isOn();
        e.ctl.tick();
        e.ctl.approve();
        e.ctl.deny();
        e.ctl.revoke("x");
        e.ctl.withdrawOffer();
        e.ctl.stop("owner");
        check("feature off: the controller answers and nothing opens a ServerSocket", quiet && e.binder.binds.get() == 0, "binds " + e.binder.binds.get());
        check("feature off: no thread of ours exists", liveThreads() == 0, "" + liveThreads());
        check("turning it on with no Wi-Fi address opens nothing", e.ctl.start(null) == PhoneControl.Start.NO_WIFI && e.binder.binds.get() == 0 && !e.ctl.isOn(), "");
        check("a bad address length opens nothing", e.ctl.start(new byte[] {1, 2}) == PhoneControl.Start.NO_WIFI && e.binder.binds.get() == 0, "");
        e.ctl.start(LOOP);
        check("on: exactly one socket, bound to the address given (never the wildcard)", e.binder.binds.get() == 1 && Arrays.equals(e.binder.asked.get(0).getAddress(), LOOP) && !e.binder.asked.get(0).isAnyLocalAddress(), "");
        check("on: the port is a random high one", e.ctl.port() >= 49152 && e.ctl.port() <= 65535, "" + e.ctl.port());
        check("on: starting again opens nothing more", e.ctl.start(LOOP) == PhoneControl.Start.ALREADY_ON && e.binder.binds.get() == 1, "");
        e.ctl.stop("owner");
        check("off again: the socket is closed", e.binder.sockets.get(0).isClosed() && !e.ctl.isOn() && e.ctl.address() == null, "");
        boolean gone = false;
        for (int i = 0; i < 40 && !gone; i++) { gone = liveThreads() == 0; if (!gone) Thread.sleep(50); }
        check("off again: our threads are gone", gone, "" + liveThreads());
        check("off again: nothing is left of the pairing or the phones", e.ctl.offer() == null && e.ctl.phones().isEmpty(), "");
    }

    static void http() throws Exception {
        // the page, Host and Origin
        Env e = started();
        Reply page = send(e, "GET", "/p", null);
        String csp = page.header("Content-Security-Policy");
        Reply page2 = send(e, "GET", "/p", null);
        String nonce = csp == null ? "" : csp.replaceAll(".*'nonce-([0-9a-f]+)'.*", "$1");
        check("the page is served with a CSP that names its script's nonce, new each time",
            page.status == 200 && page.body.contains("nonce=\"" + nonce + "\"") && nonce.length() == 32 && !csp.contains("unsafe-eval") && !csp.contains("script-src 'unsafe-inline'")
            && !nonce.equals(page2.header("Content-Security-Policy").replaceAll(".*'nonce-([0-9a-f]+)'.*", "$1")), String.valueOf(csp));
        check("the page is never cached and never framed", "no-store".equals(page.header("Cache-Control")) && "DENY".equals(page.header("X-Frame-Options")) && "nosniff".equals(page.header("X-Content-Type-Options")) && csp.contains("frame-ancestors 'none'"), page.head);
        check("a wrong Host is refused (DNS rebinding)", send(e, "GET", "/p", null, "Host", "evil.example:" + e.port).status == 403
            && send(e, "GET", "/p", null, "Host", "127.0.0.1").status == 403 && send(e, "GET", "/p", null, "Host", "localhost:" + e.port).status == 403
            && send(e, "GET", "/p", null, "Host", null).status == 403, "");
        check("a POST without Origin, or from another site, is refused",
            send(e, "POST", "/api/pair", "{\"code\":\"123456\"}", "Origin", null).status == 403
            && send(e, "POST", "/api/pair", "{\"code\":\"123456\"}", "Origin", "http://evil.example").status == 403
            && send(e, "POST", "/api/pair", "{\"code\":\"123456\"}", "Origin", "null").status == 403
            && send(e, "POST", "/api/pair", "{\"code\":\"123456\"}", "Sec-Fetch-Site", "cross-site").status == 403, "");
        check("a POST that is not JSON is refused", send(e, "POST", "/api/pair", "code=123456", "Content-Type", "application/x-www-form-urlencoded").status == 415
            && send(e, "POST", "/api/pair", "{\"code\":\"123456\"}", "Content-Type", "text/plain").status == 415, "");
        check("only GET and POST; no query strings", raw(e.port, ("PUT /p HTTP/1.1\r\nHost: 127.0.0.1:" + e.port + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1)).status == 405
            && send(e, "GET", "/p?x=1", null).status == 404, "");
        check("a request with a repeated Host header is refused (smuggling)", raw(e.port, ("GET /p HTTP/1.1\r\nHost: 127.0.0.1:" + e.port + "\r\nHost: evil.example\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1)).status == 400, "");
        check("chunked bodies are refused", raw(e.port, ("POST /api/pair HTTP/1.1\r\nHost: 127.0.0.1:" + e.port + "\r\nTransfer-Encoding: chunked\r\n\r\n0\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1)).status == 400, "");
        check("a body over 4096 bytes is refused", send(e, "POST", "/api/pair", "{\"code\":\"" + "1".repeat(5000) + "\"}").status == 413, "");
        check("nothing answers without a session", send(e, "GET", "/api/state", null).status == 401 && send(e, "POST", "/api/key", "{\"key\":\"up\"}").status == 401, "");
        e.ctl.stop("owner");

        // pairing, sessions, CSRF
        e = started();
        Pairing.Offer o = e.ctl.offer();
        Reply bad = send(e, "POST", "/api/pair", "{\"token\":\"" + "0".repeat(32) + "\"}");
        check("a wrong token is refused", bad.status == 403 && "wrong".equals(bad.json().get("error")), bad.body);
        Reply good = send(e, "POST", "/api/pair", "{\"token\":\"" + o.token + "\"}");
        String id = (String) good.json().get("id");
        check("a right token asks the owner, and the TV sees the request", good.status == 200 && id != null && e.ctl.pending() != null && e.ctl.pending().waiting(), good.body);
        check("the token cannot be used a second time", send(e, "POST", "/api/pair", "{\"token\":\"" + o.token + "\"}").status != 200, "");
        Reply busy = send(e, "POST", "/api/pair", "{\"code\":\"" + e.ctl.offer().code + "\"}");
        check("while one phone waits for the owner, another is told to wait and its code is not spent", busy.status == 429 && "busy".equals(busy.json().get("error")), busy.body);
        Reply wait = send(e, "POST", "/api/pair/status", "{\"id\":\"" + id + "\"}");
        check("no session before the owner presses سماح", "waiting".equals(wait.json().get("status")) && wait.json().get("session") == null && wait.header("Set-Cookie") == null, wait.body);
        check("another id gets nothing", "gone".equals(send(e, "POST", "/api/pair/status", "{\"id\":\"" + "1".repeat(32) + "\"}").json().get("status")), "");
        e.ctl.approve();
        Reply ok = send(e, "POST", "/api/pair/status", "{\"id\":\"" + id + "\"}");
        String sess = sessionOf(ok);
        String cookie = ok.header("Set-Cookie");
        check("allowed: a 256-bit session in an HttpOnly, SameSite=Strict cookie, and in the body for the header",
            sess != null && sess.matches("[0-9a-f]{64}") && cookie != null && cookie.contains("urtv=" + sess) && cookie.contains("HttpOnly") && cookie.contains("SameSite=Strict") && cookie.contains("Max-Age="), String.valueOf(cookie));
        check("the answer is read once", "gone".equals(send(e, "POST", "/api/pair/status", "{\"id\":\"" + id + "\"}").json().get("status")), "");
        check("cookie and header together open the API", authed(e, "GET", "/api/state", null, sess).status == 200, "");
        check("the cookie alone is not enough (CSRF)", send(e, "GET", "/api/state", null, "Cookie", "urtv=" + sess).status == 401
            && send(e, "POST", "/api/key", "{\"key\":\"up\"}", "Cookie", "urtv=" + sess).status == 401, "");
        check("the header alone is not enough", send(e, "GET", "/api/state", null, "X-UrTV-Session", sess).status == 401, "");
        check("a header that differs from the cookie is refused", send(e, "GET", "/api/state", null, "Cookie", "urtv=" + sess, "X-UrTV-Session", "0".repeat(64)).status == 401
            && send(e, "GET", "/api/state", null, "Cookie", "urtv=" + "0".repeat(64), "X-UrTV-Session", "0".repeat(64)).status == 401, "");
        Reply st = authed(e, "GET", "/api/state", null, sess);
        check("the state never carries a secret (no session, token, code, address)", !st.body.contains(sess) && !st.body.contains(o.token) && !st.body.contains(o.code) && !st.body.toLowerCase().contains("secret") && !st.body.contains("127.0.0.1"), st.body);
        check("a foreign Origin is refused even with a good session", authed(e, "POST", "/api/key", "{\"key\":\"up\"}", sess).status == 200
            && send(e, "POST", "/api/key", "{\"key\":\"up\"}", "Cookie", "urtv=" + sess, "X-UrTV-Session", sess, "Origin", "http://evil.example").status == 403
            && send(e, "POST", "/api/key", "{\"key\":\"up\"}", "Cookie", "urtv=" + sess, "X-UrTV-Session", sess, "Origin", null).status == 403, "");

        // what reaches the TV, and what does not
        e.actions.calls.clear();
        Reply k = authed(e, "POST", "/api/key", "{\"key\":\"ok\"}", sess);
        check("a key reaches the TV", k.status == 200 && e.actions.calls.contains("key:ok"), e.actions.calls.toString());
        e.actions.calls.clear();
        int status = 0;
        String[] refused = {"{\"key\":\"power\"}", "{\"key\":\"up\",\"x\":1}", "not json", "{\"key\":\"up\",\"key\":\"down\"}", "{\"key\":[\"up\"]}", "[]"};
        for (String r : refused) status |= authed(e, "POST", "/api/key", r, sess).status == 400 ? 0 : 1;
        check("a key that is not on the list, extra fields, repeated keys and bad JSON never reach the TV", status == 0 && e.actions.calls.isEmpty(), e.actions.calls.toString());
        authed(e, "POST", "/api/text", "{\"text\":\"السلام عليكم\"}", sess);
        check("Arabic text arrives whole (replace is the default, no search)", e.actions.calls.contains("text:السلام عليكم:null:false"), e.actions.calls.toString());
        check("text with a line break or an override character is refused", authed(e, "POST", "/api/text", "{\"text\":\"a\\nb\"}", sess).status == 400 && authed(e, "POST", "/api/text", "{\"text\":\"\\u202e\"}", sess).status == 400, "");
        e.actions.next = Actions.Result.fail("other_app");
        Reply fail = authed(e, "POST", "/api/key", "{\"key\":\"up\"}", sess);
        check("a key Android doesn't allow is answered with a fixed word, not silence", fail.status == 200 && Boolean.FALSE.equals(fail.json().get("ok")) && "other_app".equals(fail.json().get("error")), fail.body);
        e.actions.next = Actions.Result.ok("done");
        check("the endpoints that are not in the list don't exist (reset, uninstall, adb, shell)", authed(e, "POST", "/api/reset", "{}", sess).status == 404 && authed(e, "POST", "/api/uninstall", "{}", sess).status == 404
            && authed(e, "POST", "/api/adb", "{}", sess).status == 404 && authed(e, "POST", "/api/shell", "{\"cmd\":\"id\"}", sess).status == 404, "");
        authed(e, "POST", "/api/logout", "{}", sess);
        boolean stoppedByLogout = false;
        for (int i = 0; i < 60 && !stoppedByLogout; i++) { stoppedByLogout = !e.ctl.isOn(); if (!stoppedByLogout) Thread.sleep(50); }
        check("logging out ends the session, and as it was the last one the server stops", stoppedByLogout && e.ctl.phones().isEmpty() && "ended".equals(e.ctl.lastStop()), e.ctl.lastStop());
        check("and nothing answers any more", refusedConnection(e.port), "");

        // a session value is good only from the address the phone was allowed from
        e = started();
        sess = pair(e);
        check("a session works from the address it was allowed from", authed(e, "GET", "/api/state", null, sess).status == 200, "");
        source = "127" + "." + "0" + "." + "0" + "." + "2";   // another address of this computer stands in for another phone
        try {
            Reply other = authed(e, "GET", "/api/state", null, sess);
            check("the same session value from a second address is refused (no_session)", other.status == 401 && "no_session".equals(other.json().get("error")), other.head);
            Reply otherPost = authed(e, "POST", "/api/key", "{\"key\":\"up\"}", sess);
            check("and so is an action from it", otherPost.status == 401 && e.actions.calls.isEmpty(), otherPost.head + e.actions.calls);
        } finally {
            source = null;
        }
        check("the allowed address still has its session, the refusals did not end it", authed(e, "GET", "/api/state", null, sess).status == 200 && e.ctl.phones().size() == 1, "");
        e.ctl.stop("owner");

        // the family PIN: the same store as the TV's keypad, so one lockout for both
        {
            final java.util.Map<String, String> kv = new java.util.HashMap<>();
            final sa.urtv.home.PinStore store = new sa.urtv.home.PinStore(new sa.urtv.home.PinStore.Backing() {
                @Override public String get(String k) { return kv.get(k); }
                @Override public void put(String k, String v) { kv.put(k, v); }
                @Override public void remove(String k) { kv.remove(k); }
            });
            e = started();
            final Env env = e;
            e.ctl.setPinGate(new PinStoreGate(store, new PinStoreGate.Clock() { @Override public long now() { return env.now[0]; } }));
            sess = pair(e);
            e.actions.calls.clear();
            check("no family PIN set: a setting from the phone goes through without one (nothing changes for those who never set a PIN)",
                authed(e, "POST", "/api/set", "{\"adhan_on\":true}", sess).status == 200 && e.actions.calls.contains("set:[adhan_on]"), e.actions.calls.toString());
            e.actions.calls.clear();
            store.set("4321");
            Reply none = authed(e, "POST", "/api/set", "{\"adhan_on\":true}", sess);
            check("a family PIN set: /api/set without the PIN is 403 pin_required and nothing was done", none.status == 403 && "pin_required".equals(none.json().get("error")) && e.actions.calls.isEmpty(), none.body);
            Reply inst = authed(e, "POST", "/api/update", "{\"op\":\"install\"}", sess);
            check("so is an update install", inst.status == 403 && "pin_required".equals(inst.json().get("error")) && !e.actions.calls.contains("update:install"), inst.body);
            check("the Quran and the keys stay open", authed(e, "POST", "/api/key", "{\"key\":\"up\"}", sess).status == 200, "");
            e.actions.calls.clear();
            Reply wrong = authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"0000\"}", sess);
            check("a wrong PIN is refused, and it counts in the TV's own store (shared tries)", wrong.status == 403 && "pin_wrong".equals(wrong.json().get("error")) && store.fails() == 1 && e.actions.calls.isEmpty(), wrong.body + store.fails());
            Reply right = authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"4321\"}", sess);
            check("the right PIN lets it through (and the tries start over)", right.status == 200 && e.actions.calls.contains("set:[adhan_on]") && store.fails() == 0, right.body);
            e.ctl.stop("owner");

            // five wrong tries typed on the TV lock the phone out too, until the wait is over
            e = started();
            final Env env2 = e;
            e.ctl.setPinGate(new PinStoreGate(store, new PinStoreGate.Clock() { @Override public long now() { return env2.now[0]; } }));
            sess = pair(e);
            e.actions.calls.clear();
            for (int i = 0; i < sa.urtv.home.PinStore.TRIES; i++) store.verify("0000", e.now[0]);   // typed on the TV's keypad
            check("the TV's keypad is locked after five wrong tries", store.waitLeft(e.now[0]) > 0, "");
            Reply locked = authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"4321\"}", sess);
            check("the right PIN from the phone is turned away while the TV's lock lasts (403, nothing done)", locked.status == 403 && e.actions.calls.isEmpty(), locked.body);
            e.now[0] += 61_000L;
            Reply later = authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"4321\"}", sess);
            check("after the wait the right PIN works again", later.status == 200 && e.actions.calls.contains("set:[adhan_on]"), later.body);
            e.ctl.stop("owner");

            // five wrong tries from phones lock the keypad on the TV, and a new session has no fresh tries
            store.clear();
            store.set("4321");
            e = started();
            final Env env3 = e;
            e.ctl.setPinGate(new PinStoreGate(store, new PinStoreGate.Clock() { @Override public long now() { return env3.now[0]; } }));
            sess = pair(e);
            String keep = pair(e);   // a second phone, so the server stays on when the first one's session ends
            int lastStatus = 0;
            for (int i = 0; i < 5; i++) lastStatus = authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"0000\"}", sess).status;
            check("five wrong PINs from a phone end its session", lastStatus == 401, "" + lastStatus);
            check("and they locked the TV's own keypad too (the same store)", store.waitLeft(e.now[0]) > 0, "");
            String second = pair(e);   // a new session of a new phone
            e.actions.calls.clear();
            Reply fresh = authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"4321\"}", second);
            check("a new phone session gets no fresh tries: the right PIN is still refused while the lock lasts", fresh.status == 403 && e.actions.calls.isEmpty(), fresh.body);
            e.ctl.stop("owner");
        }

        // the PIN hook
        e = started();
        final String[] gatePin = {"1234"};
        e.ctl.setPinGate(new PinGate() {
            @Override public boolean required() { return true; }
            @Override public boolean verify(String pin) { return gatePin[0].equals(pin); }
        });
        sess = pair(e);
        e.actions.calls.clear();
        Reply np = authed(e, "POST", "/api/set", "{\"adhan_on\":true}", sess);
        check("with a PIN set, a setting without it is refused", np.status == 403 && "pin_required".equals(np.json().get("error")) && e.actions.calls.isEmpty(), np.body);
        Reply wp = authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"9999\"}", sess);
        check("a wrong PIN is refused", wp.status == 403 && "pin_wrong".equals(wp.json().get("error")) && e.actions.calls.isEmpty(), wp.body);
        check("the update install is protected too, the check is not", authed(e, "POST", "/api/update", "{\"op\":\"install\"}", sess).status == 403 && authed(e, "POST", "/api/update", "{\"op\":\"check\"}", sess).status == 200, "");
        check("the Quran and the remote keys are not protected", authed(e, "POST", "/api/quran", "{\"op\":\"pause\"}", sess).status == 200 && authed(e, "POST", "/api/key", "{\"key\":\"up\"}", sess).status == 200, "");
        Reply rp = authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"1234\"}", sess);
        check("the right PIN lets it through, and the PIN itself goes no further", rp.status == 200 && e.actions.calls.contains("set:[adhan_on]"), e.actions.calls.toString());
        authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"0000\"}", sess);
        authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"0000\"}", sess);
        authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"0000\"}", sess);
        int last = authed(e, "POST", "/api/set", "{\"adhan_on\":true,\"pin\":\"0000\"}", sess).status;
        check("five wrong PINs end that phone's session (it was the only one, so the server stops too)", last == 401 && refusedConnection(e.port) && e.ctl.phones().isEmpty(), "" + last);
        e.ctl.stop("owner");

        // the PIN hook is off by default
        e = started();
        sess = pair(e);
        check("without a PIN set nothing is asked", authed(e, "POST", "/api/set", "{\"adhan_on\":true}", sess).status == 200, "");
        e.ctl.stop("owner");

        // rate limit
        e = started();
        int tooMany = 0;
        for (int i = 0; i < 110; i++) if (send(e, "GET", "/p", null).status == 429) tooMany++;
        check("a client asking too often is slowed down (429)", tooMany > 0, "" + tooMany);
        e.ctl.stop("owner");

        // five wrong codes stop the server
        e = started();
        e.ctl.offer();
        int lastStatus = 0;
        for (int i = 0; i < 5; i++) lastStatus = send(e, "POST", "/api/pair", "{\"code\":\"000000\"}").status;
        boolean stopped = false;
        for (int i = 0; i < 60 && !stopped; i++) { stopped = !e.ctl.isOn(); if (!stopped) Thread.sleep(50); }
        check("five wrong codes stop the server", lastStatus == 403 && stopped && "locked".equals(e.ctl.lastStop()) && refusedConnection(e.port), lastStatus + " " + e.ctl.lastStop());

        // a client outside the private ranges gets no answer at all
        Env p = env(new Predicate<InetAddress>() { @Override public boolean test(InetAddress a) { return Guard.privateClient(a); } });
        p.ctl.start(LOOP);
        p.port = p.ctl.port();
        p.origin = "http://127.0.0.1:" + p.port;
        Reply none = send(p, "GET", "/p", null);
        check("a client that is not on a private network gets no answer (loopback stands in for a public address)", none.empty, none.head);
        p.ctl.stop("owner");
    }

    static boolean refusedConnection(int port) {
        for (int i = 0; i < 40; i++) {
            try {
                Socket s = new Socket(InetAddress.getByAddress(LOOP), port);
                s.close();
                Thread.sleep(50);
            } catch (IOException e) {
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }
        return false;
    }

    static void controller() throws Exception {
        Env e = started();
        e.ctl.tick();
        check("a server nobody paired with stops after fifteen minutes", e.ctl.isOn(), "");
        e.now[0] += PhoneControl.START_WINDOW_MS;
        e.ctl.tick();
        check("... and says why", !e.ctl.isOn() && "unused".equals(e.ctl.lastStop()), e.ctl.lastStop());

        e = started();
        String s1 = pair(e);
        e.now[0] += Sessions.IDLE_MS;
        e.ctl.tick();
        check("a session idle for 30 minutes ends, and then so does the server", !e.ctl.isOn() && "ended".equals(e.ctl.lastStop()), e.ctl.lastStop());

        e = started();
        pair(e);
        String second = pair(e);
        e.now[0] += Sessions.IDLE_MS - 1000;
        send(e, "GET", "/api/state", null, "Cookie", "urtv=" + second, "X-UrTV-Session", second);
        e.now[0] += 2000;
        e.ctl.tick();
        check("one phone leaving doesn't stop the server while another is in use", e.ctl.isOn() && e.ctl.phones().size() == 1, e.ctl.phones().size() + "");
        e.ctl.revoke(e.ctl.phones().get(0).id);
        check("the owner cancelling the last phone stops it", !e.ctl.isOn(), "");

        e = started();
        Pairing.Offer o = e.ctl.offer();
        send(e, "POST", "/api/pair", "{\"token\":\"" + o.token + "\"}");
        e.now[0] += PhoneControl.PENDING_MS;
        e.ctl.tick();
        check("a request the owner doesn't answer in a minute is dropped", e.ctl.pending() == null, "");
        e.ctl.stop("owner");

        e = started();
        o = e.ctl.offer();
        e.now[0] += Pairing.TTL_MS;
        Pairing.Offer n = e.ctl.offer();
        check("the code on the TV is renewed when it runs out", n != null && !n.token.equals(o.token) && e.ctl.pairUrl(n).endsWith("/p#" + n.token) && e.ctl.pairUrl(n).startsWith("http://127.0.0.1:" + e.ctl.port()), "");
        e.ctl.withdrawOffer();
        e.ctl.stop("owner");
        check("nothing persists: a new start has no old session or code", e.ctl.start(LOOP) == PhoneControl.Start.OK && e.ctl.phones().isEmpty(), "");
        e.ctl.stop("owner");
    }
}
