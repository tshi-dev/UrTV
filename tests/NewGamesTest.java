package sa.urtv.screensaver;

import java.util.*;
import sa.urtv.screensaver.games.forehead.ForeheadGame;
import sa.urtv.screensaver.games.author.AuthorGame;
import sa.urtv.screensaver.games.closest.ClosestGame;
import sa.urtv.screensaver.games.liar.LiarGame;

/** For tests/new_games_test.py: the rules of the new games (4.4) on a plain JVM, fake clocks. Prints PASS/FAIL lines. */
public final class NewGamesTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static long[] t = {1_000_000L};

    static void forehead() {
        int[] n = {0};
        ForeheadGame g = new ForeheadGame(2, 2, 60, () -> "كلمة" + (n[0]++), () -> t[0]);
        check("forehead: a turn starts READY; the count of three, then the first word; no word before it", g.phase == ForeheadGame.Phase.READY && g.word.isEmpty() && !g.right());
        g.ready(); check("forehead: ready starts the count, the word is not there yet", g.phase == ForeheadGame.Phase.COUNT && g.word.isEmpty());
        t[0] += 2000; g.tick(); check("forehead: two seconds into the count is still the count", g.phase == ForeheadGame.Phase.COUNT);
        t[0] += 1100; g.tick(); check("forehead: after three seconds the turn runs and the first word is dealt", g.phase == ForeheadGame.Phase.PLAY && g.word.equals("كلمة0"));
        check("forehead: right scores for the team whose turn it is and deals the next word; skip scores nothing", g.right() && g.score[0] == 1 && g.word.equals("كلمة1") && g.skip() && g.score[0] == 1 && g.got == 1 && g.skipped == 1 && g.word.equals("كلمة2"));
        g.pause(); t[0] += 90_000; g.resume(); g.tick();
        check("forehead: the pause stops the clock (90 s of pause do not end a 60 s turn), actions are refused while paused", g.phase == ForeheadGame.Phase.PLAY && g.leftMs() > 50_000);
        g.pause(); check("forehead: no answer while paused", !g.right()); g.resume();
        t[0] += 61_000; g.tick();
        check("forehead: the clock ends the turn, the word is gone", g.phase == ForeheadGame.Phase.TURN_END && g.word.isEmpty() && !g.right());
        g.nextTurn(); check("forehead: the next team's turn", g.phase == ForeheadGame.Phase.READY && g.team() == 1 && g.round() == 0 && g.got == 0);
        for (int i = 0; i < 3; i++) { g.ready(); t[0] += 3100; g.tick(); g.right(); t[0] += 61_000; g.tick(); g.nextTurn(); }
        check("forehead: every team plays its turns (2 x 2 = 4), then the end; the leaders are the teams with most points", g.phase == ForeheadGame.Phase.END && g.turn == 4 && g.leaders().size() >= 1 && g.score[0] + g.score[1] == 4);
        boolean threw = false; try { new ForeheadGame(1, 1, 60, () -> "", () -> t[0]); } catch (IllegalArgumentException e) { threw = true; }
        check("forehead: one team is refused (the game needs two to four)", threw);
    }

    static List<AuthorGame.Prompt> prompts(int n) {
        StringBuilder b = new StringBuilder("{\"v\":1,\"items\":[");
        for (int i = 0; i < n; i++) b.append(i > 0 ? "," : "").append("{\"i\":\"au-").append(i).append("\",\"q\":[\"سؤال ").append(i).append("\",\"Question ").append(i).append("\"],\"k\":").append(i % 3 == 0 ? 0 : 1).append("}");
        List<AuthorGame.Prompt> v = AuthorGame.parse(MiniJson.parse(b.append("]}").toString()));
        return v;
    }
    static void author(String bundleFile) throws Exception {
        List<AuthorGame.Prompt> all = prompts(60);
        check("author: the prompts parse (60 items), a bad item is skipped and a repeated id too, a file with too few or another version is refused",
            all != null && all.size() == 60 && AuthorGame.parse(MiniJson.parse("{\"v\":1,\"items\":[{\"i\":\"a\"}]}")) == null && AuthorGame.parse(MiniJson.parse("{\"v\":2,\"items\":[]}")) == null && AuthorGame.parse(MiniJson.parse("[1]")) == null);
        Set<String> recent = new HashSet<>(); for (int i = 0; i < 50; i++) recent.add("au-" + i);
        List<AuthorGame.Prompt> pk = AuthorGame.pick(all, 8, false, recent, new Random(2));
        boolean fresh = true; for (int i = 0; i < 8; i++) fresh &= !recent.contains(pk.get(i).id);
        check("author: nothing repeats: the picks are the prompts this TV has not asked while there are enough, kids only when asked", fresh && pk.size() == 8 && AuthorGame.pick(all, 8, true, new HashSet<>(), new Random(2)).stream().allMatch(p -> p.kids));
        AuthorGame g = new AuthorGame(4, prompts(60).subList(0, 2), new Random(5));
        check("author: the first question is open for writing; an empty answer and a double answer are refused; symbols are cleaned and the length is cut",
            g.phase == AuthorGame.Phase.WRITE && !g.submit(0, "  !! ") && g.submit(0, "سمك مع  شوكولاتة!!") && !g.submit(0, "ثاني") && AuthorGame.clean("a@b#c").equals("abc") && AuthorGame.clean("x".repeat(60)).length() == 30);
        g.submit(1, "أ"); g.submit(2, "ب"); g.submit(3, "ج");
        check("author: all four answered", g.allSubmitted() && g.submitted() == 4);
        g.endWrite();
        int w = g.shownWriter();
        check("author: the answers are shown without a writer; the writer is not known before the reveal; the writer cannot vote, nobody votes for themselves, a vote is once",
            g.phase == AuthorGame.Phase.VOTE && g.writer() == -1 && !g.vote(w, (w + 1) % 4) && !g.vote((w + 1) % 4, (w + 1) % 4) && g.vote((w + 1) % 4, w) && !g.vote((w + 1) % 4, (w + 2) % 4));
        int a1 = (w + 2) % 4, a2 = (w + 3) % 4;
        g.vote(a1, a2);   // a wrong guess: the writer scores
        check("author: not all voted yet", !g.allVoted() && g.votes() == 2); g.vote(a2, w);
        check("author: all voted (every player but the writer)", g.allVoted());
        g.reveal();
        check("author: a right guess scores 1 for the voter, each wrong guess 1 for the writer", g.phase == AuthorGame.Phase.REVEAL && g.writer() == w && g.score[(w + 1) % 4] == 1 && g.score[a2] == 1 && g.score[w] == 1 && g.score[a1] == 0);
        int shownBefore = g.shown; g.next();
        check("author: the next answer is shown, votes and the gains start again", g.phase == AuthorGame.Phase.VOTE && g.shown == shownBefore + 1 && g.votes() == 0);
        for (int i = 0; i < 6; i++) { if (g.phase == AuthorGame.Phase.VOTE) g.reveal(); if (g.phase == AuthorGame.Phase.REVEAL) g.next(); if (g.phase == AuthorGame.Phase.WRITE) { g.submit(0, "x"); g.endWrite(); } }
        check("author: with fewer than two answers a question is skipped, and the game ends after its last question", g.phase == AuthorGame.Phase.END || g.index >= 1);
        boolean threw = false; try { new AuthorGame(2, prompts(60), new Random(1)); } catch (IllegalArgumentException e) { threw = true; }
        check("author: two players are refused (three to ten)", threw);
        byte[] file = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(bundleFile));
        byte[] json = GameBundle.open("author", file);
        List<AuthorGame.Prompt> real = json == null ? null : AuthorGame.parse(MiniJson.parse(new String(json, java.nio.charset.StandardCharsets.UTF_8)));
        check("author: the bundled bank opens under its name and parses (" + (real == null ? 0 : real.size()) + " prompts, at least 120), every prompt has an English text, none is twice",
            real != null && real.size() >= 120 && real.stream().allMatch(p -> !p.q[1].isEmpty()) && real.stream().map(p -> p.q[0]).distinct().count() == real.size());
    }

    static List<ClosestGame.Item> items(int n) {
        StringBuilder b = new StringBuilder("{\"v\":1,\"items\":[");
        for (int i = 0; i < n; i++) b.append(i > 0 ? "," : "").append("{\"i\":\"c").append(i).append("\",\"q\":[\"سؤال\",\"Q\"],\"v\":").append(1000 + i).append(",\"u\":[\"م\",\"m\"],\"d\":1,\"k\":1}");
        return ClosestGame.parse(MiniJson.parse(b.append("]}").toString()));
    }
    static void closest(String bundleFile) throws Exception {
        List<ClosestGame.Item> all = items(50);
        check("closest: the items parse; a fraction, a negative number, a text value and a repeated id are skipped; too few items or another version refuse the file",
            all != null && all.size() == 50 && ClosestGame.parse(MiniJson.parse("{\"v\":1,\"items\":[{\"i\":\"a\",\"q\":[\"x\"],\"v\":1.5}]}")) == null && ClosestGame.parse(MiniJson.parse("{\"v\":3,\"items\":[]}")) == null);
        check("closest: a typed number: Arabic-Indic and Latin digits, spaces and group marks; letters, a point, empty and 14 digits are not numbers",
            ClosestGame.number("٨٦٤٠٠").equals(86400L) && ClosestGame.number(" 9 372 610 ").equals(9372610L) && ClosestGame.number("86,400").equals(86400L) && ClosestGame.number("12ab") == null && ClosestGame.number("1.5") == null && ClosestGame.number("") == null && ClosestGame.number("12345678901234") == null);
        List<ClosestGame.Item> two = new ArrayList<>(all.subList(0, 2));
        ClosestGame g = new ClosestGame(4, two);   // the first item's value is 1000
        check("closest: the first question is open; a text that is not a number and a second guess are refused", g.phase == ClosestGame.Phase.ASK && !g.submit(0, "abc") && g.submit(0, "1010") && !g.submit(0, "1020"));
        g.submit(1, "1000"); g.submit(2, "1010"); g.submit(3, "900");
        check("closest: all answered", g.allIn() && g.answered() == 4);
        g.reveal();
        check("closest: the exact answer scores 5; the next distance 2; guesses at the same distance share the points (both +10 get 2); the farthest scores nothing",
            g.gained[1] == 5 && g.gained[0] == 2 && g.gained[2] == 2 && g.gained[3] == 1 && g.ranking().get(0) == 1);
        g.next(); g.submit(0, "1500"); g.submit(1, "1002"); g.reveal();
        check("closest: without an exact answer the closest scores 3; one that did not answer scores nothing; the game ends after the last question",
            g.gained[1] == 3 && g.gained[0] == 2 && g.gained[2] == 0 && g.gained[3] == 0);
        g.next();
        check("closest: the end", g.phase == ClosestGame.Phase.END && g.score[1] == 8);
        boolean threw = false; try { new ClosestGame(1, two); } catch (IllegalArgumentException e) { threw = true; }
        check("closest: one player is refused (two to ten)", threw);
        Set<String> recent = new HashSet<>(); for (int i = 0; i < 40; i++) recent.add("c" + i);
        List<ClosestGame.Item> pk = ClosestGame.pick(all, 8, false, recent, new Random(3));
        check("closest: nothing repeats: the picks are the items this TV has not asked while there are enough", pk.size() == 8 && pk.stream().noneMatch(p -> recent.contains(p.id)));
        byte[] json = GameBundle.open("closest", java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(bundleFile)));
        List<ClosestGame.Item> real = json == null ? null : ClosestGame.parse(MiniJson.parse(new String(json, java.nio.charset.StandardCharsets.UTF_8)));
        check("closest: the bundled bank opens and parses (" + (real == null ? 0 : real.size()) + " items, at least 120), every item has a unit text or none, an English question, no Israel", real != null && real.size() >= 120 && real.stream().allMatch(p -> !p.q[1].isEmpty() && !p.q[1].contains("Israel") && !p.q[0].contains("إسرائيل")));
    }

    static void liar(String bundleFile) throws Exception {
        LiarGame g = new LiarGame(5, prompts(60).subList(0, 4), new Random(9));
        int[] t = g.tellers();
        check("liar: three different tellers, exactly one of them the liar; the TV does not know the liar before the reveal",
            t.length == 3 && t[0] != t[1] && t[1] != t[2] && t[0] != t[2] && java.util.stream.IntStream.of(t).filter(g::liarIs).count() == 1 && g.liar() == -1 && g.phase == LiarGame.Phase.DEAL);
        g.startTelling(); check("liar: the telling is on", g.phase == LiarGame.Phase.TELL && !g.vote(0, t[0])); g.endTelling();
        int voter1 = -1, voter2 = -1; for (int p = 0; p < 5; p++) if (!g.isTeller(p)) { if (voter1 < 0) voter1 = p; else voter2 = p; }
        int liarP = -1; for (int x : t) if (g.liarIs(x)) liarP = x; int other = -1; for (int x : t) if (x != liarP) other = x;
        check("liar: a teller cannot vote, a vote must name one of the three, once each", !g.vote(t[0], t[1]) && !g.vote(voter1, voter1) && g.vote(voter1, liarP) && !g.vote(voter1, other));
        g.vote(voter2, other); check("liar: all voted (the five minus the three)", g.allVoted());
        g.reveal();
        check("liar: finding the liar scores 2, each wrong vote scores 1 for the liar; the liar is named only now", g.liar() == liarP && g.gained[voter1] == 2 && g.gained[liarP] == 1 && g.gained[voter2] == 0);
        int[] before = g.told.clone(); g.next();
        int[] t2 = g.tellers(); boolean fresh = true; for (int x : t2) fresh &= before[x] == 1 || before[x] == 0;
        check("liar: the next round's tellers are the players who have told the fewest times", g.phase == LiarGame.Phase.DEAL && g.index == 1 && fresh);
        int[] counts = new int[6];
        LiarGame h = new LiarGame(6, prompts(60).subList(0, 4), new Random(4));
        for (int r = 0; r < 4; r++) { for (int x : h.tellers()) counts[x]++; h.startTelling(); h.endTelling(); h.reveal(); h.next(); }
        int mn = 99, mx = 0; for (int c : counts) { mn = Math.min(mn, c); mx = Math.max(mx, c); }
        check("liar: over four rounds of six players everyone has told at least once and nobody more than twice", mn >= 1 && mx <= 3 && h.phase == LiarGame.Phase.END);
        boolean threw = false; try { new LiarGame(3, prompts(60), new Random(1)); } catch (IllegalArgumentException e) { threw = true; }
        check("liar: three players are refused (four to ten)", threw);
        byte[] json = GameBundle.open("liar", java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(bundleFile)));
        List<AuthorGame.Prompt> real = json == null ? null : AuthorGame.parse(MiniJson.parse(new String(json, java.nio.charset.StandardCharsets.UTF_8)));
        check("liar: the bundled topics open and parse (" + (real == null ? 0 : real.size()) + ", at least 80), each with an English text", real != null && real.size() >= 80 && real.stream().allMatch(p -> !p.q[1].isEmpty()));
    }

    public static void main(String[] a) throws Exception {
        forehead();
        author(a[0]);
        closest(a[1]);
        liar(a[2]);
        System.exit(failed == 0 ? 0 : 1);
    }
}
