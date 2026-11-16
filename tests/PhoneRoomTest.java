package sa.urtv.screensaver;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** For tests/phone_room_test.py: the party games' phone room (GamePhoneRoom) and the game-only server on a plain JVM. Prints PASS/FAIL lines. */
public final class PhoneRoomTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static long[] now = {1_000_000L};
    static GamePhoneRoom room() { now[0] = 1_000_000L; return new GamePhoneRoom("4321", () -> now[0], new Random(7)); }
    static QuizRoom.Player join(GamePhoneRoom r, String n) { return r.join("4321", n, -1, null).player; }

    /** `serve <page.html>`: the real phone page behind the real server on the loopback, driven by lines on stdin (tests/phone_page.test.mjs):
     *  `start`, `secret <seat> <text>`, `vote <seat>`, `act <seat> <text>`, `wait <seat> <text>`, `reply <seat>` (prints the first answer), `take <seat>`, `count`. */
    static void serve(String pagePath) throws Exception {
        final byte[] page = Files.readAllBytes(Paths.get(pagePath));
        GamePhoneRoom r = new GamePhoneRoom("4321", System::currentTimeMillis, new Random(3));
        QuizServer s = new QuizServer(r, new QuizServer.Assets() { public byte[] page() { return page; } public byte[] font(String n) { return null; } }, true);
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));
        System.out.println("PORT " + s.start(InetAddress.getLoopbackAddress())); System.out.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        for (String l; (l = in.readLine()) != null; ) {
            String[] w = l.split(" ", 3);
            int seat = w.length > 1 && w[1].matches("\\d+") ? Integer.parseInt(w[1]) : 0; String txt = w.length > 2 ? w[2] : "";
            switch (w[0]) {
                case "start": r.start(); break;
                case "secret": r.card(seat, GamePhoneRoom.SECRET, r.name(seat), txt, "note", null, null, -1); break;
                case "vote": r.card(seat, GamePhoneRoom.VOTE, "", "who?", "", r.names().toArray(new String[0]), null, seat); break;
                case "act": r.card(seat, GamePhoneRoom.ACT, "act", txt, "", null, new int[]{0, 1, 2}, -1); break;
                case "wait": r.hold(seat, txt); break;
                case "pick": r.card(seat, GamePhoneRoom.PICK, "", "tiles", "note", new String[]{"3|5", "1|1", "6|2"}, new int[]{0, 2}, -1); break;
                case "text": r.card(seat, GamePhoneRoom.TEXT, "", "who?", "", txt.isEmpty() ? new String[]{"one"} : txt.split(","), null, -1); break;
                case "typed": { String[] t = r.typed(seat); System.out.println("TYPED " + (t == null ? "none" : String.join("|", t))); break; }
                case "reply": System.out.println("REPLY " + r.reply(seat)); break;
                case "take": System.out.println("TAKE " + r.take(seat)); break;
                case "count": System.out.println("COUNT " + r.count()); break;
                default: break;
            }
            System.out.flush();
        }
        s.stop();
    }

    public static void main(String[] a) throws Exception {
        if (a.length == 2 && a[0].equals("serve")) { serve(a[1]); return; }
        GamePhoneRoom r = room();
        QuizRoom.Player p0 = join(r, "سارة"), p1 = join(r, "سارة"), p2 = join(r, "Omar");
        check("join: three phones take seats in order, a repeated name gets a number", r.count() == 3 && !p0.name.equals(p1.name) && r.name(2).equals("Omar"));
        check("join: the same phone coming back (its token) keeps its seat", r.join("4321", "x", -1, p1.token).player == p1 && r.count() == 3);
        check("join: a wrong code is refused and five lock joining for a minute",
            "code".equals(r.join("0000", "x", -1, null).error) && r.join("1", "x", -1, null).error != null && r.join("2", "x", -1, null).error != null && r.join("3", "x", -1, null).error != null
            && r.join("4", "x", -1, null).error != null && "locked".equals(r.join("4321", "Zed", -1, null).error));
        now[0] += GamePhoneRoom.LOCK_MS + 1;
        check("join: after the lock the right code works again", r.join("4321", "Zed", -1, null).ok);
        check("join: a name that is a link is refused, an empty one too", "name".equals(r.join("4321", "http://x.y", -1, null).error) && "name".equals(r.join("4321", "  ", -1, null).error));
        GamePhoneRoom full = room();
        for (int i = 0; i < GamePhoneRoom.MAX_PLAYERS; i++) { now[0] += 4000; full.join("4321", "p" + i, -1, null); }
        now[0] += 60000;
        check("join: 12 phones at most", "full".equals(full.join("4321", "late", -1, null).error) && full.count() == 12);

        r = room(); p0 = join(r, "A"); p1 = join(r, "B"); p2 = join(r, "C");
        String lobby = r.stateFor(p0.token, false);
        check("state: in the lobby the phone sees its name and the number of players, no card text", lobby.contains("\"ph\":\"lobby\"") && lobby.contains("\"pl\":3") && lobby.contains("\"text\":\"\""));
        check("state: an unknown token is told so, a left one is told gone", r.stateFor("zzz", false).contains("\"e\":\"token\""));
        r.start();
        check("start: nobody new joins a game in play, a known phone can return", "closed".equals(r.join("4321", "Late", -1, null).error) && r.join("4321", "", -1, p1.token).ok);
        int c0 = r.card(0, GamePhoneRoom.SECRET, "A", "كلمة", "الموضوع", null, null, -1);
        r.card(1, GamePhoneRoom.SECRET, "B", "أنت برا السالفة", "", null, null, -1);
        check("secret: each phone sees only its own card", r.stateFor(p0.token, false).contains("كلمة") && !r.stateFor(p1.token, false).contains("كلمة") && r.stateFor(p1.token, false).contains("برا"));
        check("secret: «شفتها» (0) is taken once; a stale card id is refused", "stale".equals(r.answer(p0.token, c0 - 1, 0, null)) && r.answer(p0.token, c0, 0, null) == null && r.replied(0) && "card".equals(r.answer(p0.token, c0, 0, null)));
        r.hold(2, "انتظر");
        check("wait: a waiting card takes no answer", "card".equals(r.answer(p2.token, r.card(2, GamePhoneRoom.WAIT, "", "x", "", null, null, -1), 0, null)));

        int tid = r.card(1, GamePhoneRoom.TEXT, "", "word?", "", new String[]{"a", "b"}, null, -1);
        check("text: nothing typed is none; a post is cleaned (symbols out, spaces single), kept per field, taken once",
            r.typed(1) == null && r.answer(p1.token, tid, 0, "كلمة!!  طويلة\n") == null && Arrays.equals(r.typed(1), new String[]{"كلمة طويلة", ""}) && "card".equals(r.answer(p1.token, tid, 0, "x")));
        now[0] += 1500;
        int tid2 = r.card(1, GamePhoneRoom.TEXT, "", "word?", "", new String[]{"a"}, null, -1);
        check("text: an empty post, a link-long post and the wrong card are refused; a new card forgets the old post", "card".equals(r.answer(p1.token, tid2, 0, "  \n ")) && "card".equals(r.answer(p1.token, tid2, 0, "y".repeat(700))) && "stale".equals(r.answer(p1.token, tid, 0, "x")) && r.typed(1) == null);
        now[0] += 1500;
        for (int i = 0; i < 3; i++) r.card(i, GamePhoneRoom.ACT, "", "buzz", "", null, new int[]{GamePhoneRoom.ACT_BUZZ}, -1);
        check("buzzer: nobody is first until a phone presses; the earliest press is first, a later one does not take it over",
            r.firstReplied() == -1 && r.answer(p2.token, r.stateId(2), 3, null) == null && r.firstReplied() == 2 && advance(5) && r.answer(p0.token, r.stateId(0), 3, null) == null && r.firstReplied() == 2);
        now[0] += 1500;
        int pk = r.card(2, GamePhoneRoom.PICK, "", "tiles", "", new String[]{"3|5", "1|1", "6|2"}, new int[]{0, 2}, -1);
        check("pick: only a number the card offers is taken, once; the TV reads it", "card".equals(r.answer(p2.token, pk, 1, null)) && "card".equals(r.answer(p2.token, pk, 5, null)) && r.answer(p2.token, pk, 2, null) == null && r.reply(2) == 2 && "card".equals(r.answer(p2.token, pk, 0, null)));
        now[0] += 1500;
        String[] names = {"A", "B", "C"};
        int[] ids = new int[3];
        for (int i = 0; i < 3; i++) ids[i] = r.card(i, GamePhoneRoom.VOTE, "", "مين؟", "", names, null, i);
        now[0] += 1500;
        check("vote: you cannot vote for yourself, a number out of range is refused", "card".equals(r.answer(p0.token, ids[0], 0, null)) && "card".equals(r.answer(p0.token, ids[0], 9, null)) && "card".equals(r.answer(p0.token, ids[0], -1, null)));
        check("vote: one vote counts, a second one on the same card is refused, «ما أدري» is the number after the last name",
            r.answer(p0.token, ids[0], 1, null) == null && "card".equals(r.answer(p0.token, ids[0], 2, null)) && r.reply(0) == 1 && r.answer(p1.token, ids[1], 3, null) == null && r.reply(1) == 3 && r.repliedCount() == 2);
        now[0] += 1500;
        for (int i = 0; i < 6; i++) r.answer(p2.token, ids[2], 0, null);
        check("rate: no more than 5 answers a second from one phone", "rate".equals(r.answer(p2.token, ids[2], 0, null)));

        now[0] += 1500;
        int act = r.card(0, GamePhoneRoom.ACT, "", "وردة", "", null, new int[]{GamePhoneRoom.ACT_OK, GamePhoneRoom.ACT_SKIP}, -1);
        check("act: صح and تخطّي are taken in order, مخالفة (not offered) is refused, take() empties the inbox",
            r.answer(p0.token, act, 0, null) == null && r.answer(p0.token, act, 1, null) == null && "card".equals(r.answer(p0.token, act, 2, null)) && r.take(0).equals(Arrays.asList(0, 1)) && r.take(0).isEmpty());
        r.card(0, GamePhoneRoom.ACT, "", "قلم", "", null, new int[]{0}, -1);
        check("act: a new card drops what the old one had not sent, and the old card id is stale", r.take(0).isEmpty() && "stale".equals(r.answer(p0.token, act, 0, null)));
        check("json: a name with quotes and slashes stays valid JSON text", GamePhoneRoom.class != null && QuizRoom.esc("a\"b\\c").equals("\"a\\\"b\\\\c\""));
        r.stateFor(p0.token, false);
        check("connected: a phone that polled lately is connected, one silent for 6 s is not", r.connected(0) && advance(7000) && !r.connected(0));
        r.leave(p1.token); r.close();
        check("close: a closed room answers closed to every poll", r.stateFor(p0.token, false).contains("closed") && "closed".equals(r.join("4321", "x", -1, null).error));

        // the game-only server answers for a GamePhoneRoom as for the quiz
        GamePhoneRoom sr = room();
        QuizServer s = new QuizServer(sr, new QuizServer.Assets() {
            public byte[] page() { return "<html>game</html>".getBytes(StandardCharsets.UTF_8); }
            public byte[] font(String n) { return null; }
        }, true);
        int port = s.start(InetAddress.getLoopbackAddress());
        try {
            String host = "127.0.0.1:" + port;
            check("server: the page is on /<code> and not on another code", http(host, "GET", "/4321", "").contains("<html>game") && http(host, "GET", "/0000", "").contains("404"));
            String j = http(host, "POST", "/api/join", "c=4321&n=%D8%B3%D8%A7%D8%B1%D8%A9&k=1");
            String tok = j.replaceAll("(?s).*\"t\":\"([a-z0-9]+)\".*", "$1");
            check("server: join over HTTP gives a token and the seat", j.contains("\"ok\":true") && tok.length() == 16 && sr.count() == 1);
            sr.start();
            int id = sr.card(0, GamePhoneRoom.SECRET, "", "سر", "", null, null, -1);
            check("server: the phone reads its card and answers it", http(host, "GET", "/api/state?c=4321&t=" + tok, "").contains("سر")
                && http(host, "POST", "/api/answer", "c=4321&t=" + tok + "&q=" + id + "&a=0").contains("\"ok\":true") && sr.reply(0) == 0);
        } finally { s.stop(); }
        System.out.println(failed == 0 ? "DONE" : "FAILED " + failed);
        if (failed > 0) System.exit(1);
    }
    static boolean advance(long ms) { now[0] += ms; return true; }

    static String http(String host, String method, String path, String body) throws IOException {
        try (Socket c = new Socket()) {
            c.connect(new InetSocketAddress("127.0.0.1", Integer.parseInt(host.substring(host.indexOf(':') + 1))), 2000); c.setSoTimeout(3000);
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            String h = method + " " + path + " HTTP/1.1\r\nHost: " + host + "\r\nContent-Length: " + b.length + "\r\nConnection: close\r\n\r\n";
            OutputStream o = c.getOutputStream(); o.write(h.getBytes(StandardCharsets.ISO_8859_1)); o.write(b); o.flush();
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buf = new byte[4096]; int n;
            while ((n = c.getInputStream().read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }
}
