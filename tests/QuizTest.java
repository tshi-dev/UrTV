package sa.urtv.screensaver;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** For tests/quiz_test.py: the quiz's rules, room, picking and server on a plain JVM (fake clock; the server on the loopback). Prints PASS/FAIL lines. */
public final class QuizTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static long[] now = {1_000_000L};

    static QuizRoom.Q q(int correct) { return new QuizRoom.Q("Q?", new String[]{"a", "b", "c", "d"}, correct); }
    static QuizRoom room() { now[0] = 1_000_000L; return new QuizRoom("1234", () -> now[0], new Random(1)); }
    static QuizRoom.Player join(QuizRoom r, String name) { QuizRoom.Result x = r.join("1234", name, -1, null); return x.player; }
    static List<QuizRoom.Q> qs(int n) { List<QuizRoom.Q> v = new ArrayList<>(); for (int i = 0; i < n; i++) v.add(q(i % 4)); return v; }

    public static void main(String[] a) throws Exception {
        // scoring
        check("scoring: an instant right answer is 1000, one at the last second about 500, a late one never below 500",
            QuizRoom.points(0, 1) == 1000 && QuizRoom.points(20000, 1) == 500 && QuizRoom.points(10000, 1) == 750 && QuizRoom.points(25000, 1) == 500);
        check("scoring: the streak adds +50 from the 3rd right answer in a row, +100 the 4th, +150 from the 5th",
            QuizRoom.points(20000, 2) == 500 && QuizRoom.points(20000, 3) == 550 && QuizRoom.points(20000, 4) == 600 && QuizRoom.points(20000, 5) == 650 && QuizRoom.points(20000, 9) == 650);

        // join rules
        QuizRoom r = room();
        QuizRoom.Result wrong = r.join("9999", "x", 0, null);
        check("join: a wrong room code is refused", !wrong.ok && "code".equals(wrong.error));
        for (int i = 0; i < 4; i++) r.join("9999", "x", 0, null);
        check("join: five wrong codes lock joining for 60 seconds, even for the right code, and it opens again after", "locked".equals(r.join("1234", "ok", 0, null).error)
            && (now[0] += 61000) > 0 && r.join("1234", "ok", 0, null).ok);
        r = room();
        check("join: a name is letters, digits and single spaces, 12 characters at most; empty or link-like is refused",
            QuizRoom.cleanName("  Sara   Ali!!<b> ").equals("Sara Alib") && QuizRoom.cleanName("abcdefghijklmnopqrstuvwxyz").length() == 12
            && QuizRoom.cleanName("!!!").isEmpty() && QuizRoom.cleanName("http example").isEmpty() && "name".equals(r.join("1234", "<>", 0, null).error));
        QuizRoom.Player s1 = join(r, "سارة"), s2 = join(r, "سارة"), s3 = join(r, "Sam"), s4 = join(r, "Sam");
        check("join: a duplicate name gets a number (Arabic digit after an Arabic name, Latin after a Latin one)", s1.name.equals("سارة") && s2.name.equals("سارة ٢") && s3.name.equals("Sam") && s4.name.equals("Sam 2"));
        check("join: a colour outside 0 to 7 is replaced by one of the 8, a good one kept", r.join("1234", "C", 5, null).player.color == 5 && r.join("1234", "D", 99, null).player.color < 8);
        r = room();
        for (int i = 0; i < 12; i++) join(r, "p" + i);
        check("join: at most 12 players", "full".equals(r.join("1234", "late", 0, null).error));
        r = room();
        int ok = 0; for (int i = 0; i < 25; i++) if (r.join("1234", "n" + i, 0, null).ok) ok++;
        check("join: at most 20 join attempts a minute for the whole room (and 12 players)", ok == 12 && "rate".equals(r.join("1234", "z", 0, null).error));

        // the game: start, answers, validation
        r = room();
        QuizRoom.Player p1 = join(r, "A"), p2 = join(r, "B");
        check("start: needs 2 connected players", !room().canStart() && r.canStart());
        check("start: the first question opens, the clock starts", r.start(qs(3)) && r.phase == QuizRoom.Phase.QUESTION && r.index == 0 && r.leftMs() == 20000);
        String st = r.stateFor(p1.token);
        check("state: the question is sent without the right answer before the reveal", st.contains("\"ph\":\"question\"") && !st.contains("\"rv\"") && !st.contains("correct") && st.contains("\"pick\":-1"));
        check("answer: an index outside 0 to 3, the wrong question number, an unknown token are refused",
            "bad".equals(r.answer(p1.token, 0, 4)) && "closed".equals(r.answer(p1.token, 5, 0)) && "token".equals(r.answer("nope", 0, 0)));
        now[0] += 2000;
        check("answer: the first tap counts, the second is refused (lock on first tap)", r.answer(p1.token, 0, 0) == null && "done".equals(r.answer(p1.token, 0, 1)));
        check("answer: the time is measured on the TV from the question's start", p1.answerMs == 2000);
        now[0] += 1000;
        check("answer: when everyone connected has answered the question closes at once", r.phase == QuizRoom.Phase.QUESTION && r.answer(p2.token, 0, 1) == null && r.phase == QuizRoom.Phase.REVEAL);
        check("reveal: the right answer scores 500 + 500 x left/20 s (2 s used: 950), the wrong one 0", p1.score == 950 && p2.score == 0 && p1.rank == 1 && p2.rank == 2);
        String rv = r.stateFor(p1.token);
        check("reveal: the phone gets right or wrong, the points, its rank and the votes", rv.contains("\"rv\":{\"c\":0,\"ok\":true,\"p\":950") && rv.contains("\"cnt\":[1,1,0,0]"));
        check("reveal: ends by itself after 6 seconds and the next question opens", (now[0] += 6000) > 0 && fire(r) && r.index == 1 && r.phase == QuizRoom.Phase.QUESTION);
        now[0] += 20001; fire(r);
        check("timeout: a question closes at 20 seconds; no answer is 0 points and breaks the streak", r.phase == QuizRoom.Phase.REVEAL && p1.lastOk == null && p1.streak == 0 && p1.score == 950);

        // streak, ties, late join, reconnect
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); r.start(qs(5));
        for (int i = 0; i < 5; i++) {
            r.answer(p1.token, i, i % 4); r.answer(p2.token, i, (i + 1) % 4);
            r.skipReveal();
        }
        check("end: after the last reveal the podium (END), the order is by score, and the streak adds 50, 100, 150 from the 3rd right answer", r.phase == QuizRoom.Phase.END && r.ranked().get(0) == p1 && p1.score == 1000 + 1000 + 1050 + 1100 + 1150);
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); r.start(qs(3));
        QuizRoom.Player late = join(r, "Late");
        check("late join: allowed between questions, waits for the next question, starts at 0, cannot answer the open one", late != null && late.waiting && late.score == 0 && "closed".equals(r.answer(late.token, 0, 0)));
        r.answer(p1.token, 0, 0); r.answer(p2.token, 0, 0);
        r.skipReveal();
        check("late join: the next question includes the new player", !late.waiting && r.answer(late.token, 1, 1) == null);
        now[0] += 30000;
        QuizRoom.Result back = r.join("1234", "other name", 3, p1.token);
        check("reconnect: the same token gets the same name, colour and score back", back.ok && back.player == p1 && p1.name.equals("A"));
        r.leave(p2.token);
        check("leave: during a game the player stays in the list, marked as left, with the points", r.players().contains(p2) && p2.left && r.activeCount() == 2);
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); r.leave(p2.token);
        r.kick(p1.token);
        check("leave: from the lobby the player is gone at once; the host can remove one with kick", !r.players().contains(p2) && r.players().isEmpty());
        r = room(); p1 = join(r, "A"); now[0] += QuizRoom.DROP_LOBBY_MS + 1000; r.tick();
        check("lobby: a phone silent for 2 minutes is dropped", r.players().isEmpty());

        Random cr = new Random(9); Set<String> codes = new HashSet<>();
        for (int i = 0; i < 200; i++) codes.add(QuizRoom.freshCode(cr, 5_000_000L + i));
        check("code: 4 digits, random for each game, never the same code twice within the hour", codes.size() == 200 && codes.stream().allMatch(c -> c.matches("[0-9]{4}")));

        // pause, end, again, close
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); r.start(qs(2));
        now[0] += 5000; r.pause();
        long left = r.leftMs(); now[0] += 60000; r.tick();
        check("pause: the question clock stops while the card is open and the answer is refused", r.phase == QuizRoom.Phase.PAUSED && r.leftMs() == left && left == 15000 && "closed".equals(r.answer(p1.token, 0, 0)));
        r.resume();
        check("pause: resuming gives back the time that was left", r.phase == QuizRoom.Phase.QUESTION && r.leftMs() == 15000);
        r.finish();
        check("finish: «إنهاء اللعبة» shows the podium so far", r.phase == QuizRoom.Phase.END);
        r.playAgain();
        check("play again: the same players, scores 0, back to the lobby", r.phase == QuizRoom.Phase.LOBBY && r.players().size() == 2 && p1.score == 0);
        String code0 = r.code; r.close();
        check("close: everything dropped, the phones are told", r.players().isEmpty() && r.stateFor("x").contains("closed") && "closed".equals(r.join(code0, "z", 0, null).error));
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); r.start(qs(1)); r.answer(p1.token, 0, 0); r.answer(p2.token, 0, 0); r.skipReveal();
        now[0] += QuizRoom.CODE_LIFE_AFTER_END_MS + 1;
        check("code: valid for 10 minutes after the podium, then closed", "closed".equals(r.join("1234", "z", 0, p1.token).error));

        // rate limit on answers
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); r.start(qs(1));
        String e = null; for (int i = 0; i < 8; i++) e = r.answer(p1.token, 0, i % 4);
        check("rate: more than 5 messages a second from one player are refused", "rate".equals(e));

        // ties are broken by who tapped first
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); r.start(qs(1));
        now[0] += 1000; r.answer(p2.token, 0, 0); now[0] += 1; r.answer(p1.token, 0, 0);
        check("ranking: players with the same score are ordered by who tapped first (B joined second but tapped 1 ms before A)", p1.score == p2.score && p2.rank == 1 && p1.rank == 2);

        // picking
        List<QuizPick.Item> bank = new ArrayList<>();
        for (int i = 0; i < 120; i++) bank.add(new QuizPick.Item("id" + i, i % 3 == 0 ? "science" : "geography", 1 + i % 3, i % 4 == 0, new String[]{"ق" + i, "q" + i}, new String[]{"ج" + i, "a" + i}, new String[][]{{"x1", "w1"}, {"x2", "w2"}, {"x3", "w3"}}));
        check("pick: a category is offered only with 40 or more items for the level and filter; mix with any",
            QuizPick.offered(bank, "science", 0, false, 10) && !QuizPick.offered(bank, "science", 1, true, 10) && QuizPick.offered(bank, QuizPick.MIX, 2, false, 10) && !QuizPick.offered(bank, "animals", 0, false, 10));
        Set<String> recent = new HashSet<>(); for (int i = 0; i < 100; i++) recent.add("id" + i);
        List<QuizPick.Item> got = QuizPick.pick(bank, QuizPick.MIX, 0, false, 15, recent, new Random(2));
        Set<String> ids = new HashSet<>(); for (QuizPick.Item it : got) ids.add(it.id);
        check("pick: the asked number, never one twice, the last 100 asked avoided when others remain (20 are left)", got.size() == 15 && ids.size() == 15 && ids.stream().noneMatch(recent::contains));
        got = QuizPick.pick(bank, "science", 1, false, 15, new HashSet<>(Arrays.asList("id0", "id3")), new Random(3));
        check("pick: only the chosen category, level and kids filter; the recent ones come back only when too few others remain", got.size() == 15 && got.stream().allMatch(it -> it.cat.equals("science") && it.diff == 1));
        QuizRoom.Q cq = QuizPick.toQ(bank.get(0), false, new Random(4));
        check("pick: the options are shuffled with the right one marked, in the screen's language", cq.opts.length == 4 && cq.opts[cq.correct].equals("ج0") && cq.text.equals("ق0") && QuizPick.toQ(bank.get(0), true, new Random(4)).opts[0 + QuizPick.toQ(bank.get(0), true, new Random(4)).correct].equals("a0"));

        server();
        System.exit(failed == 0 ? 0 : 1);
    }

    static boolean fire(QuizRoom r) { r.tick(); return true; }

    // ---------------------------------------------------------------- the server on the loopback
    static String http(int port, String method, String path, String body) throws IOException {
        try (Socket s = new Socket(InetAddress.getLoopbackAddress(), port)) {
            s.setSoTimeout(4000);
            byte[] b = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            OutputStream o = s.getOutputStream();
            o.write((method + " " + path + " HTTP/1.1\r\nHost: x\r\n" + (body != null ? "Content-Type: application/x-www-form-urlencoded\r\nContent-Length: " + b.length + "\r\n" : "") + "\r\n").getBytes());
            o.write(b); o.flush();
            ByteArrayOutputStream all = new ByteArrayOutputStream(); byte[] buf = new byte[4096]; int n;
            while ((n = s.getInputStream().read(buf)) > 0) all.write(buf, 0, n);
            return all.toString("UTF-8");
        }
    }

    static void server() throws Exception {
        QuizRoom r = new QuizRoom("4321", System::currentTimeMillis, new Random(5));
        QuizServer srv = new QuizServer(r, new QuizServer.Assets() {
            public byte[] page() { return "<html>PAGE</html>".getBytes(); }
            public byte[] font(String n) { return n.equals("Thmanyah-Regular") ? new byte[]{1, 2, 3} : null; }
        }, true);
        int port = srv.start(InetAddress.getLoopbackAddress());
        try {
            check("server: the page is served only at the room's code; an old or wrong code gets the short message and nothing else",
                http(port, "GET", "/4321", null).contains("PAGE") && http(port, "GET", "/1111", null).contains("404") && http(port, "GET", "/1111", null).contains("Wrong code") && !http(port, "GET", "/1111", null).contains("PAGE"));
            check("server: fonts only by a plain name that exists; nothing else is served (no file paths, no other routes)",
                http(port, "GET", "/f/Thmanyah-Regular.woff2", null).contains("200") && http(port, "GET", "/f/..%2Fx.woff2", null).contains("404") && http(port, "GET", "/etc/passwd", null).contains("404") && http(port, "GET", "/remote", null).contains("404"));
            String j = http(port, "POST", "/api/join", "c=4321&n=%D8%B3%D8%A7%D8%B1%D8%A9&k=3&t=");
            String tok = j.replaceAll("(?s).*\"t\":\"([a-z0-9]+)\".*", "$1");
            check("server: join returns a token, the final name and the colour", j.contains("\"ok\":true") && tok.length() == 16 && j.contains("\"k\":3") && j.contains("سارة"));
            check("server: join with a wrong code is refused", http(port, "POST", "/api/join", "c=0000&n=x&k=1&t=").contains("\"e\":\"code\""));
            String s1 = http(port, "GET", "/api/state?c=4321&t=" + tok, null);
            check("server: the state of a player in the lobby, and «token» for a stranger", s1.contains("\"ph\":\"lobby\"") && http(port, "GET", "/api/state?c=4321&t=zzz", null).contains("\"e\":\"token\"") && http(port, "GET", "/api/state?c=1&t=" + tok, null).contains("\"e\":\"code\""));
            StringBuilder big = new StringBuilder("c=4321&n="); for (int i = 0; i < 2000; i++) big.append('a');
            check("server: a body over 1 KB is refused", http(port, "POST", "/api/join", big.toString()).contains("413"));
            String j2 = http(port, "POST", "/api/join", "c=4321&n=Ben&k=1&t=");
            check("server: a second phone joins; answers are validated by the TV (closed before the game, bad index, bad token)",
                j2.contains("\"ok\":true") && http(port, "POST", "/api/answer", "c=4321&t=" + tok + "&q=0&a=1").contains("\"e\":\"closed\"") && http(port, "POST", "/api/answer", "c=4321&t=nope&q=0&a=1").contains("\"e\":\"token\""));
            r.start(qs(2));
            check("server: a right answer is taken once, the second tap says done", http(port, "POST", "/api/answer", "c=4321&t=" + tok + "&q=0&a=0").contains("\"ok\":true") && http(port, "POST", "/api/answer", "c=4321&t=" + tok + "&q=0&a=2").contains("\"e\":\"done\""));
            check("server: the question state a phone reads has no right answer and shows its pick", http(port, "GET", "/api/state?c=4321&t=" + tok, null).contains("\"pick\":0") && !http(port, "GET", "/api/state?c=4321&t=" + tok, null).contains("\"rv\""));
            http(port, "POST", "/api/leave", "c=4321&t=" + tok);
            check("server: leave marks the player as gone", r.byToken(tok).left);
            check("server: only a local address is answered (private, link-local, loopback), never a public one",
                QuizServer.local(InetAddress.getByName("192.168.1.20")) && QuizServer.local(InetAddress.getByName("10.0.0.4")) && QuizServer.local(InetAddress.getByName("169.254.3.3")) && QuizServer.local(InetAddress.getByName("127.0.0.1"))
                && !QuizServer.local(InetAddress.getByName("8.8.8.8")) && !QuizServer.local(InetAddress.getByName("93.184.216.34")));
            check("server: other methods are refused", http(port, "DELETE", "/api/state", null).contains("405"));
        } finally { srv.stop(); }
    }
}
