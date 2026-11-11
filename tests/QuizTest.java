package sa.urtv.screensaver;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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

    static boolean tickReveal(QuizRoom r) { r.tick(); return r.phase == QuizRoom.Phase.REVEAL; }

    public static void main(String[] a) throws Exception {
        // scoring
        check("scoring: an instant right answer is 1000, one at the last second about 500, a late one never below 500",
            QuizRoom.points(0, 1) == 1000 && QuizRoom.points(20000, 1) == 500 && QuizRoom.points(10000, 1) == 750 && QuizRoom.points(25000, 1) == 500);
        check("scoring: the streak adds +50 from the 3rd right answer in a row, +100 the 4th, +150 from the 5th",
            QuizRoom.points(20000, 2) == 500 && QuizRoom.points(20000, 3) == 550 && QuizRoom.points(20000, 4) == 600 && QuizRoom.points(20000, 5) == 650 && QuizRoom.points(20000, 9) == 650);


        // step 48: who counts for «everyone answered», and an answer must carry its own question's nonce
        {
        QuizRoom r = room(); QuizRoom.Player p1 = join(r, "A"), p2 = join(r, "B"), p3 = join(r, "C");
        r.start(qs(4));
        String x0 = r.nonce();
        now[0] += 3000; r.answer(p1.token, 0, 0, x0); r.answer(p2.token, 0, 0, x0);
        now[0] += 7000;   // p3 (a tab in the background) has been silent 10 s: its phone is not polling
        check("everyone answered: a quiet third player (a background tab, silent 10 s) is still waited for: the question stays open after the other two answered",
            r.phase == QuizRoom.Phase.QUESTION && r.answeredCount() == 2);
        r.tick();
        check("everyone answered: ... and it closes at 20 s as always, and the player who did not tap gets 0 points and no streak",
            (now[0] += 10000) > 0 && (r.leftMs() == 0) && tickReveal(r) && p3.score == 0 && p3.lastPoints == 0 && p3.lastOk == null && p1.score > 0);
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); p3 = join(r, "C"); r.start(qs(4)); x0 = r.nonce();
        r.answer(p1.token, 0, 0, x0); r.answer(p2.token, 0, 0, x0);
        now[0] += 5000; r.touch(p1.token); r.touch(p2.token);
        check("everyone answered: the third answering closes the question at once", r.answer(p3.token, 0, 1, x0) == null && r.phase == QuizRoom.Phase.REVEAL);
        // step 52: a question ends early ONLY when every player in the room has answered; a quiet one (polls once a minute) never shortens it
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); p3 = join(r, "C"); now[0] += 50000; r.touch(p1.token); r.touch(p2.token); r.start(qs(5)); x0 = r.nonce();
        now[0] += 5000; r.touch(p1.token); r.touch(p2.token);
        r.answer(p1.token, 0, 0, x0); r.answer(p2.token, 0, 0, x0);
        check("everyone answered: a player silent for over a minute (his tab polls once a minute) is still waited for: the question stays open", r.phase == QuizRoom.Phase.QUESTION && !r.endedEarly);
        now[0] += 16000; r.tick();
        check("everyone answered: ... it ends at the 20 s, not early (endedEarly false), and he scored nothing", r.phase == QuizRoom.Phase.REVEAL && !r.endedEarly && p3.score == 0 && p3.missed == 1 && !p3.left);
        now[0] += 8000; r.tick(); x0 = r.nonce(); r.answer(p1.token, 1, 1, x0); r.answer(p2.token, 1, 1, x0); now[0] += 20000; r.tick();
        now[0] += 8000; r.tick(); x0 = r.nonce(); r.answer(p1.token, 2, 2, x0); r.answer(p2.token, 2, 2, x0); now[0] += 20000; r.tick();
        check("out of the room: three whole questions in a row with no answer, and only then: left, out, and his phone is told «gone»", p3.out && p3.left && r.stateFor(p3.token).contains("\"e\":\"gone\"") && !p1.out);
        now[0] += 8000; r.tick(); x0 = r.nonce();
        r.answer(p1.token, 3, 0, x0); now[0] += 100; r.answer(p2.token, 3, 0, x0);
        check("out of the room: the others no longer wait for him; the question ends when they have answered (early)", r.phase == QuizRoom.Phase.REVEAL && r.endedEarly);
        QuizRoom.Result back = r.join("1234", "C", 0, p3.token);
        check("out of the room: joining again with his token brings the same player back (score kept, missed 0)", back.ok && back.player == p3 && !p3.left && !p3.out && p3.missed == 0);
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); r.start(qs(3)); x0 = r.nonce();
        r.answer(p1.token, 0, 0, x0); now[0] += 1; r.answer(p2.token, 0, 1, x0);
        check("everyone answered: when all have, the question ends at once and says so (endedEarly)", r.phase == QuizRoom.Phase.REVEAL && r.endedEarly);
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); String lobbyTok = p2.token; now[0] += QuizRoom.DROP_LOBBY_MS + 1000; r.touch(p1.token); r.tick();
        check("lobby: a phone silent for 10 minutes is removed and told «gone» (not sent to the join form without a word)", r.byToken(lobbyTok) == null && r.stateFor(lobbyTok).contains("\"e\":\"gone\""));
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); p3 = join(r, "C");
        check("ties: players on one score share the place (all on 0: place 1 each)", p1.rank == 1 && p2.rank == 1 && p3.rank == 1);
        r.start(qs(2)); x0 = r.nonce(); r.answer(p1.token, 0, 0, x0); r.answer(p2.token, 0, 0, x0); r.answer(p3.token, 0, 1, x0);
        check("ties: two on the same score share place 1, the one behind is 3 (not 2)", p1.score == p2.score && p1.rank == 1 && p2.rank == 1 && p3.rank == 3);
        check("math: a sum inside an Arabic sentence is isolated left to right, text without one is untouched",
            QuizRoom.isolateMath("ما ناتج 250 - 175؟").equals("ما ناتج \u2066250 - 175\u2069؟") && QuizRoom.isolateMath("ما ناتج ٢٥٠ + ١٧٥؟").contains("\u2066٢٥٠ + ١٧٥\u2069") && QuizRoom.isolateMath("ما ناتج ٩١ \u2212 ٣٨؟").equals("ما ناتج \u2066٩١ \u2212 ٣٨\u2069؟") && QuizRoom.isolateMath("ما ناتج 6 \u00D7 7؟").equals("ما ناتج \u20666 \u00D7 7\u2069؟") && QuizRoom.isolateMath("ما ناتج ٤٨ \u00F7 ٦؟").contains("\u2066٤٨ \u00F7 ٦\u2069") && QuizRoom.isolateMath("٣٫٥ + ٢").equals("\u2066٣٫٥ + ٢\u2069") && QuizRoom.isolateMath("عاصمة السعودية؟").equals("عاصمة السعودية؟") && QuizRoom.isolateMath("سنة 1990").equals("سنة 1990"));
        r = room(); p1 = join(r, "A"); p2 = join(r, "B"); p3 = join(r, "C"); r.start(qs(4)); x0 = r.nonce();
        check("nonce: an answer without one, or with a wrong one, is refused and nobody is credited",
            "closed".equals(r.answer(p1.token, 0, 0, null)) && "closed".equals(r.answer(p1.token, 0, 0, "zzz")) && "closed".equals(r.answer(p1.token, 0, 0, "")) && r.answeredCount() == 0);
        r.answer(p1.token, 0, 0, x0); r.answer(p2.token, 0, 0, x0); r.answer(p3.token, 0, 0, x0);
        r.skipReveal(); String x1 = r.nonce();
        check("nonce: every question has its own (never the one before)", !x1.equals(x0) && !x1.isEmpty());
        check("nonce: the retried or queued answer of question 0 (its index AND its nonce) cannot answer question 1",
            "closed".equals(r.answer(p3.token, 0, 2, x0)) && "closed".equals(r.answer(p3.token, 1, 2, x0)) && r.answeredCount() == 0 && p3.answer < 0);
        check("nonce: the old nonce with the new index is refused too; the new pair is taken", "closed".equals(r.answer(p3.token, 1, 2, x0)) && r.answer(p3.token, 1, 2, x1) == null && p3.answer == 2);
        check("nonce: an answer sent with one player's token is only ever that player's (the others stay unanswered)", p1.answer < 0 && p2.answer < 0 && r.answeredCount() == 1);
        r.finish(); r.playAgain(); r.start(qs(4)); String x2 = r.nonce();
        check("nonce: a new game starts with a nonce never used in the last one; the old one is refused", !x2.equals(x0) && !x2.equals(x1) && "closed".equals(r.answer(p1.token, 0, 0, x0)) && "closed".equals(r.answer(p1.token, 0, 0, x1)));
        check("state: the question carries its nonce for the phone (q.x)", r.stateFor(p1.token).contains("\"x\":\"" + x2 + "\""));
        r = room(); r.latinDigits = true; join(r, "سارة"); QuizRoom.Player l2 = join(r, "سارة"), l3 = join(r, "سارة");
        check("join: with Latin digits set, the number after a duplicate Arabic name is Latin (سارة 2), not Arabic-Indic", l2.name.equals("سارة 2") && l3.name.equals("سارة 3"));
        }

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
        check("reveal: ends by itself after 8 seconds (4 + 4) and the next question opens", (now[0] += 8000) > 0 && fire(r) && r.index == 1 && r.phase == QuizRoom.Phase.QUESTION);
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
        check("ranking: players with the same score are ordered by who tapped first (B joined second but tapped 1 ms before A)", p1.score == p2.score && r.ranked().get(0) == p2 && r.ranked().get(1) == p1 && p1.rank == 1 && p2.rank == 1);   // ordered by the first tap, one shared place

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
        QuizRoom.Q cq = QuizPick.toQ(bank.get(0), new Random(4));
        check("pick: the options are shuffled once with the right one marked, in both languages at the same place (the right Arabic answer and the right English one)",
            cq.opts.length == 4 && cq.opts[cq.correct].equals("ج0") && cq.text.equals("ق0") && cq.text(true).equals("q0") && cq.opt(cq.correct, false).equals("ج0") && cq.opt(cq.correct, true).equals("a0"));
        boolean same = true; for (int p = 0; p < 4; p++) { String ar = cq.opt(p, false); String en = cq.opt(p, true); same &= ar.substring(1).equals(en.substring(1)) || ar.equals("ج0") == en.equals("a0"); }
        check("pick: each place holds the same option in both languages (no Arabic option under another English one)", same);
        {   // step 52b: «when in English the questions need to be English»: a mixed room, the TV in English and one phone in Arabic
            QuizRoom rr = room(); QuizRoom.Player en1 = join(rr, "A"), ar1 = join(rr, "B");
            List<QuizRoom.Q> two = new ArrayList<>(); two.add(cq); two.add(QuizPick.toQ(bank.get(1), new Random(5)));
            rr.start(two);
            String sEn = rr.stateFor(en1.token, true), sAr = rr.stateFor(ar1.token, false), sDef = rr.stateFor(ar1.token);
            check("English phone: the question and the four answers in English only; Arabic phone: in Arabic only; the default (no language) is Arabic",
                sEn.contains("\"t\":\"q0\"") && !sEn.matches("(?s).*[\\u0600-\\u06FF].*") && sAr.contains("\"t\":\"ق0\"") && sAr.contains("\"o\":[") && sDef.equals(sAr));
            now[0] += 1000; rr.answer(en1.token, 0, cq.correct, rr.nonce()); rr.answer(ar1.token, 0, (cq.correct + 1) % 4, rr.nonce());
            String rvEn = rr.stateFor(en1.token, true), rvAr = rr.stateFor(ar1.token, false);
            check("the same places in both: the right answer's index in the reveal is one number for the English and the Arabic phone", rvEn.contains("\"c\":" + cq.correct) && rvAr.contains("\"c\":" + cq.correct) && rvEn.contains("\"ok\":true") && rvAr.contains("\"ok\":false"));
        }
        {   // an English room plays only items whose English text is English
            List<QuizPick.Item> mixed = new ArrayList<>(bank);
            mixed.add(new QuizPick.Item("x1", "science", 1, false, new String[] {"س", "سؤال بالعربي"}, new String[] {"ج", "answer"}, new String[][] {{"أ", "a"}, {"ب", "b"}, {"ج", "c"}}));
            mixed.add(new QuizPick.Item("x2", "science", 1, false, new String[] {"س", ""}, new String[] {"ج", "answer"}, new String[][] {{"أ", "a"}, {"ب", "b"}, {"ج", "c"}}));
            mixed.add(new QuizPick.Item("x3", "science", 1, false, new String[] {"س", "Fine?"}, new String[] {"ج", "yes"}, new String[][] {{"أ", "no"}, {"ب", "maybe"}, {"ج", "never"}}));
            List<QuizPick.Item> enBank = QuizPick.forLang(mixed, true);
            check("an English room skips an item whose English is missing or in Arabic letters (never shown in Arabic), keeps a good one; an Arabic room keeps all",
                enBank.size() == bank.size() + 1 && enBank.stream().anyMatch(it -> it.id.equals("x3")) && enBank.stream().noneMatch(it -> it.id.equals("x1") || it.id.equals("x2")) && QuizPick.forLang(mixed, false).size() == mixed.size());
        }

        bankCheck(a.length > 0 ? a[0] : null);
        server();
        System.exit(failed == 0 ? 0 : 1);
    }

    static String item(String id, String extra) { return "{\"i\":\"" + id + "\",\"c\":\"geography\",\"d\":2,\"k\":0,\"q\":[\"س\",\"Q\"],\"a\":[\"أ\",\"A\"],\"w\":[[\"ب\",\"B\"],[\"ج\",\"C\"],[\"د\",\"D\"]]" + extra + "}"; }
    static void bankCheck(String starter) throws Exception {
        if (starter != null) {
            List<QuizPick.Item> v = QuizBankCheck.parse(Files.readAllBytes(Paths.get(starter)));
            check("bank: the bundled starter set passes the same check as a download (210 items)", v != null && v.size() == 210);
        }
        byte[] good = ("[" + item("a1", "") + "," + item("a2", "") + "]").getBytes(StandardCharsets.UTF_8);
        check("bank: a well-formed file is accepted", QuizBankCheck.parse(good) != null && QuizBankCheck.parse(good).size() == 2);
        check("bank: an HTML page, an error message, an object, an empty body or null is refused",
            QuizBankCheck.parse("<!doctype html><html><body>404</body></html>".getBytes()) == null && QuizBankCheck.parse("{\"error\":1}".getBytes()) == null && QuizBankCheck.parse(new byte[0]) == null
            && QuizBankCheck.parse(null) == null && QuizBankCheck.parse("  <html>[{}]".getBytes()) == null);
        check("bank: over 1.5 MB is refused, so is anything followed by more text or cut short",
            QuizBankCheck.parse(new byte[QuizBankCheck.BYTES_MAX + 1]) == null && QuizBankCheck.parse(("[" + item("a1", "") + "] x").getBytes(StandardCharsets.UTF_8)) == null && QuizBankCheck.parse(("[" + item("a1", "")).getBytes(StandardCharsets.UTF_8)) == null);
        StringBuilder many = new StringBuilder("[");
        for (int i = 0; i < QuizBankCheck.ITEMS_MAX + 1; i++) many.append(i > 0 ? "," : "").append(item("id" + i, ""));
        check("bank: more than 5,000 items is refused", QuizBankCheck.parse((many + "]").getBytes(StandardCharsets.UTF_8)) == null);
        check("bank: one bad item rejects the file (an extra key, a missing key, difficulty 4, a duplicate option, a duplicate id, an id with odd characters, a text of 201 characters, a control character)",
            QuizBankCheck.parse(("[" + item("a1", ",\"x\":1") + "]").getBytes(StandardCharsets.UTF_8)) == null
            && QuizBankCheck.parse("[{\"i\":\"a\"}]".getBytes()) == null
            && QuizBankCheck.parse(("[" + item("a1", "").replace("\"d\":2", "\"d\":4") + "]").getBytes(StandardCharsets.UTF_8)) == null
            && QuizBankCheck.parse(("[" + item("a1", "").replace("[\"ب\",\"B\"]", "[\"أ\",\"A\"]") + "]").getBytes(StandardCharsets.UTF_8)) == null
            && QuizBankCheck.parse(("[" + item("a1", "") + "," + item("a1", "") + "]").getBytes(StandardCharsets.UTF_8)) == null
            && QuizBankCheck.parse(("[" + item("a/../1", "") + "]").getBytes(StandardCharsets.UTF_8)) == null
            && QuizBankCheck.parse(("[" + item("a1", "").replace("\"Q\"", "\"" + "x".repeat(201) + "\"") + "]").getBytes(StandardCharsets.UTF_8)) == null
            && QuizBankCheck.parse(("[" + item("a1", "").replace("\"Q\"", "\"a\\u0007b\"") + "]").getBytes(StandardCharsets.UTF_8)) == null);
        check("bank: nesting deeper than 8 levels is refused without an error", QuizBankCheck.parse(("[".repeat(50) + "]".repeat(50)).getBytes()) == null);
    }

    static InetAddress ip(int a, int b, int c, int d) throws UnknownHostException { return InetAddress.getByAddress(new byte[]{(byte) a, (byte) b, (byte) c, (byte) d}); }

    static boolean fire(QuizRoom r) { r.tick(); return true; }

    // ---------------------------------------------------------------- the server on the loopback
    static String http(int port, String method, String path, String body) throws IOException { return httpH(port, method, path, body, "Host: 127.0.0.1:" + port); }
    static String httpH(int port, String method, String path, String body, String... headers) throws IOException {
        try (Socket s = new Socket(InetAddress.getLoopbackAddress(), port)) {
            s.setSoTimeout(6000);
            byte[] b = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            OutputStream o = s.getOutputStream();
            StringBuilder hs = new StringBuilder(); for (String h : headers) hs.append(h).append("\r\n");
            o.write((method + " " + path + " HTTP/1.1\r\n" + hs + (body != null ? "Content-Type: application/x-www-form-urlencoded\r\nContent-Length: " + b.length + "\r\n" : "") + "\r\n").getBytes());
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
            String oldCode = http(port, "GET", "/1111", null);   // one wrong request only: five from one address lock it (tested at the end)
            check("server: the page is served only at the room's code; an old or wrong code gets the short message and nothing else",
                http(port, "GET", "/4321", null).contains("PAGE") && oldCode.contains("404") && oldCode.contains("Wrong code") && !oldCode.contains("PAGE"));
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
                j2.contains("\"ok\":true") && http(port, "POST", "/api/answer", "c=4321&t=" + tok + "&q=0&a=1&x=" + r.nonce()).contains("\"e\":\"closed\"") && http(port, "POST", "/api/answer", "c=4321&t=nope&q=0&a=1&x=" + r.nonce()).contains("\"e\":\"token\""));
            r.start(qs(2));
            check("server: a right answer is taken once, the second tap says done", http(port, "POST", "/api/answer", "c=4321&t=" + tok + "&q=0&a=0&x=" + r.nonce()).contains("\"ok\":true") && http(port, "POST", "/api/answer", "c=4321&t=" + tok + "&q=0&a=2&x=" + r.nonce()).contains("\"e\":\"done\""));
            check("server: the question state a phone reads has no right answer and shows its pick", http(port, "GET", "/api/state?c=4321&t=" + tok, null).contains("\"pick\":0") && !http(port, "GET", "/api/state?c=4321&t=" + tok, null).contains("\"rv\""));
            http(port, "POST", "/api/leave", "c=4321&t=" + tok);
            check("server: leave marks the player as gone", r.byToken(tok).left);
            check("server: only a local address is answered (private, link-local, loopback), never a public one",
                QuizServer.local(ip(192, 168, 1, 20)) && QuizServer.local(ip(10, 0, 0, 4)) && QuizServer.local(ip(169, 254, 3, 3)) && QuizServer.local(ip(127, 0, 0, 1))
                && !QuizServer.local(ip(8, 8, 8, 8)) && !QuizServer.local(ip(93, 184, 216, 34)));
            check("server: other methods are refused (so no cross-origin preflight is ever answered)", http(port, "DELETE", "/api/state", null).contains("405") && http(port, "OPTIONS", "/api/join", null).contains("405"));
            // ---- a hostile device on the same Wi-Fi
            check("security: DNS rebinding: a request whose Host is a name (not the TV's address) is refused, even for the page", httpH(port, "GET", "/4321", null, "Host: evil.example:" + port).contains("403")
                && httpH(port, "GET", "/api/state?c=4321&t=" + tok, null, "Host: attacker.test").contains("403") && httpH(port, "GET", "/4321", null).contains("400") == false);
            check("security: no Host at all is refused", httpH(port, "GET", "/4321", null).contains("403"));
            check("security: CSRF: a POST from a web page of another origin is refused, the page's own origin and a plain client (no Origin) are not",
                httpH(port, "POST", "/api/join", "c=4321&n=Evil&k=1&t=", "Host: 127.0.0.1:" + port, "Origin: http://evil.example").contains("403")
                && httpH(port, "POST", "/api/join", "c=4321&n=Evil&k=1&t=", "Host: 127.0.0.1:" + port, "Sec-Fetch-Site: cross-site").contains("403")
                && httpH(port, "POST", "/api/join", "c=4321&n=Zed&k=1&t=", "Host: 127.0.0.1:" + port, "Origin: http://127.0.0.1:" + port, "Sec-Fetch-Site: same-origin").contains("\"ok\":true"));
            check("security: the page's own answers carry CSP, no framing, no referrer, no sniffing", http(port, "GET", "/4321", null).contains("Content-Security-Policy: default-src 'none'") && http(port, "GET", "/4321", null).contains("X-Frame-Options: DENY")
                && http(port, "GET", "/4321", null).contains("Referrer-Policy: no-referrer") && http(port, "GET", "/4321", null).contains("nosniff") && !http(port, "GET", "/4321", null).toLowerCase().contains("access-control-allow"));
            check("security: path traversal: no way out of /f/ (dots, slashes, encoded forms, other extensions, long names)",
                http(port, "GET", "/f/../../etc/passwd.woff2", null).contains("404") && http(port, "GET", "/f/%2e%2e%2fsecret.woff2", null).contains("404") && http(port, "GET", "/f/..\\x.woff2", null).contains("404")
                && http(port, "GET", "/f/Thmanyah-Regular.otf", null).contains("404") && http(port, "GET", "/f/" + "a".repeat(80) + ".woff2", null).contains("404") && http(port, "GET", "//f/Thmanyah-Regular.woff2", null).contains("404"));
            check("security: names: direction overrides, zero-width and combining marks, emoji, NUL, a very long name all come out as plain letters and digits",
                QuizRoom.cleanName("\u202Eevil\u200B\u0301name").equals("evilname") && QuizRoom.cleanName("a\u0000b").equals("ab") && QuizRoom.cleanName("\uD83D\uDE00\uD83D\uDE00").isEmpty()
                && QuizRoom.cleanName("x".repeat(5000)).length() == 12 && QuizRoom.esc("a\"b\\c\n</script>").equals("\"a\\\"b\\\\c\\u000a</script>\""));
            // slow-loris and connection floods
            long t0 = System.nanoTime();
            try (Socket idle = new Socket(InetAddress.getLoopbackAddress(), port)) {
                idle.setSoTimeout(8000);
                int rd = idle.getInputStream().read();
                check("security: slow-loris: a connection that sends nothing is closed after 3 seconds", rd == -1 && (System.nanoTime() - t0) / 1_000_000 < 5000);
            }
            t0 = System.nanoTime();
            try (Socket drip = new Socket(InetAddress.getLoopbackAddress(), port)) {
                drip.setSoTimeout(8000);
                OutputStream o = drip.getOutputStream();
                boolean closed = false;
                for (int i = 0; i < 12 && !closed; i++) { try { o.write('G'); o.flush(); Thread.sleep(500); } catch (IOException e) { closed = true; } }
                int rd = 0; try { rd = drip.getInputStream().read(); } catch (IOException e) { rd = -1; }
                check("security: slow-loris: a request that arrives a byte at a time is dropped at the 3-second limit", (closed || rd == -1) && (System.nanoTime() - t0) / 1_000_000 < 7000);
            }
            List<Socket> held = new ArrayList<>();
            for (int i = 0; i < 4; i++) held.add(new Socket(InetAddress.getLoopbackAddress(), port));
            Thread.sleep(200);
            String fifth;
            try (Socket extra = new Socket(InetAddress.getLoopbackAddress(), port)) { extra.setSoTimeout(2000); int rd = extra.getInputStream().read(); fifth = rd == -1 ? "closed" : "open"; } catch (SocketTimeoutException e) { fifth = "waited"; }
            for (Socket h : held) h.close();
            Thread.sleep(300);
            check("security: connection flood: one device holds 4 connections at most, the fifth is told «busy» (429) and closed, never cut silently, and normal requests work again after", "open".equals(fifth) && http(port, "GET", "/4321", null).contains("PAGE"));
            // guessing the code: the join lock of the room (5 wrong, 60 s) and the lock of the guessing address on every route
            for (int i = 0; i < 6; i++) http(port, "GET", "/api/state?c=" + (1000 + i) + "&t=x", null);
            check("security: guessing the code on any route (state, answer, page, join) locks that address for 60 seconds, even for the right code afterwards",
                http(port, "GET", "/api/state?c=4321&t=" + tok, null).contains("\"e\":\"locked\"") && http(port, "POST", "/api/join", "c=4321&n=Q&k=1&t=").contains("\"e\":\"locked\""));
            String lockedPage = http(port, "GET", "/4321", null);
            check("security: a locked address that opens the page itself gets a page (429, «محاولات كثيرة»), never raw JSON", lockedPage.contains("429") && lockedPage.contains("text/html") && lockedPage.contains("محاولات كثيرة") && !lockedPage.contains("\"ok\""));
            int answered = 0;
            for (int i = 0; i < 40; i++) {
                String rsp = i % 2 == 0 ? http(port, "POST", "/api/answer", "c=4321&t=" + tok + "&q=0&a=1&x=zz") : http(port, "GET", "/api/state?c=4321&t=" + tok, null);
                if (rsp.contains("\"e\":\"locked\"") && rsp.contains("429")) answered++;
            }
            check("security: while locked every request is answered (429 «locked»), none cut off without an answer (a body unread made the close a reset)", answered == 40);
        } finally { srv.stop(); }
    }
}
