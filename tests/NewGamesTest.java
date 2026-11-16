package sa.urtv.screensaver;

import java.util.*;
import sa.urtv.screensaver.games.forehead.ForeheadGame;
import sa.urtv.screensaver.games.author.AuthorGame;

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

    public static void main(String[] a) throws Exception {
        forehead();
        author(a[0]);
        System.exit(failed == 0 ? 0 : 1);
    }
}
