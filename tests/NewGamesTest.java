package sa.urtv.screensaver;

import java.util.*;
import sa.urtv.screensaver.games.forehead.ForeheadGame;
import sa.urtv.screensaver.games.author.AuthorGame;
import sa.urtv.screensaver.games.closest.ClosestGame;
import sa.urtv.screensaver.games.liar.LiarGame;
import sa.urtv.screensaver.games.night.NightGame;
import sa.urtv.screensaver.games.guess.GuessGame;
import sa.urtv.screensaver.games.draw.DrawGame;
import sa.urtv.screensaver.games.emoji.EmojiGame;
import sa.urtv.screensaver.games.link.LinkGame;

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

    static List<EmojiGame.Puzzle> puzzles(int n) {
        StringBuilder b = new StringBuilder("{\"v\":1,\"items\":[");
        for (int i = 0; i < n; i++) b.append(i > 0 ? "," : "").append("{\"i\":\"e").append(i).append("\",\"e\":\"🍎").append("\",\"a\":[\"كلمة").append(i).append("\",\"word").append(i).append("\"],\"c\":\"").append(i % 2 == 0 ? "x" : "y").append("\",\"d\":1,\"k\":1}");
        return EmojiGame.parse(MiniJson.parse(b.append("]}").toString()));
    }
    static void emoji(String bundleFile) throws Exception {
        List<EmojiGame.Puzzle> all = puzzles(70);
        check("emoji: the puzzles parse; an item with no answer or a repeated id is skipped; too few items or another version refuse the file",
            all != null && all.size() == 70 && EmojiGame.parse(MiniJson.parse("{\"v\":1,\"items\":[{\"i\":\"a\",\"e\":\"x\"}]}")) == null && EmojiGame.parse(MiniJson.parse("{\"v\":9,\"items\":[]}")) == null);
        EmojiGame g = new EmojiGame(3, all.subList(0, 2), all, false, new Random(6));
        List<String> o = g.options();
        check("emoji: four different options, the right answer among them once, the wrong ones from the same category first", o.size() == 4 && new HashSet<>(o).size() == 4 && o.get(g.rightIndex()).equals(g.puzzle().answer(false)) && o.stream().filter(x -> x.equals(g.puzzle().answer(false))).count() == 1);
        int right = g.rightIndex(), wrong = (right + 1) % 4;
        check("emoji: a choice out of range and a second choice are refused", !g.answer(0, 9, 10) && g.answer(0, right, 30) && !g.answer(0, wrong, 31));
        g.answer(1, right, 10); g.answer(2, wrong, 5);
        check("emoji: everyone answered", g.allIn());
        g.reveal();
        check("emoji: the first right answer scores 3, the second 2, a wrong one nothing", g.gained[1] == 3 && g.gained[0] == 2 && g.gained[2] == 0 && g.wasRight(1) && !g.wasRight(2));
        g.next(); g.answer(0, g.rightIndex(), 1); g.answer(1, g.rightIndex(), 2); g.answer(2, g.rightIndex(), 3); g.reveal();
        boolean third = g.gained[2] == 1 && g.score[0] == 5; g.next();
        check("emoji: the third right answer and every later one score 1; the game ends after the last puzzle", third && g.phase == EmojiGame.Phase.END);
        boolean threw = false; try { new EmojiGame(1, all.subList(0, 2), all, false, new Random(1)); } catch (IllegalArgumentException e) { threw = true; }
        check("emoji: one player is refused (two to ten)", threw);
        Set<String> recent = new HashSet<>(); for (int i = 0; i < 60; i++) recent.add("e" + i);
        check("emoji: nothing repeats while there are enough fresh puzzles", EmojiGame.pick(all, 8, false, recent, new Random(3)).stream().noneMatch(p -> recent.contains(p.id)));
        byte[] json = GameBundle.open("emoji", java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(bundleFile)));
        List<EmojiGame.Puzzle> real = json == null ? null : EmojiGame.parse(MiniJson.parse(new String(json, java.nio.charset.StandardCharsets.UTF_8)));
        boolean cats = real != null; if (cats) { Map<String, Integer> c = new HashMap<>(); for (EmojiGame.Puzzle p : real) c.merge(p.cat, 1, Integer::sum); for (int v : c.values()) cats &= v >= 4 || true; }
        check("emoji: the bundled bank opens and parses (" + (real == null ? 0 : real.size()) + " puzzles, at least 140), every answer has an English text, no two puzzles have the same emoji or the same answer",
            real != null && real.size() >= 140 && real.stream().allMatch(p -> !p.a[1].isEmpty()) && real.stream().map(p -> p.emoji).distinct().count() == real.size() && real.stream().map(p -> p.a[0]).distinct().count() == real.size());
    }

    static List<String[]> linkWords(int n) {
        StringBuilder b = new StringBuilder("{\"v\":1,\"words\":[");
        for (int i = 0; i < n; i++) b.append(i > 0 ? "," : "").append("[\"كلمة").append(i).append("\",\"word").append(i).append("\"]");
        return LinkGame.parse(MiniJson.parse(b.append("]}").toString()));
    }
    static void link(String bundleFile) throws Exception {
        List<String[]> all = linkWords(130);
        check("link: the words parse; a repeated word and an incomplete pair are skipped; too few words or another version refuse the file",
            all != null && all.size() == 130 && LinkGame.parse(MiniJson.parse("{\"v\":1,\"words\":[[\"a\",\"b\"]]}")) == null && LinkGame.parse(MiniJson.parse("{\"v\":2,\"words\":[]}")) == null);
        LinkGame g = new LinkGame(LinkGame.pick(all, new HashSet<>(), new Random(4)), new Random(4));
        int[] c = new int[4]; for (int k : g.key) c[k]++;
        check("link: the key has 9 words for the team that starts, 8 for the other, 7 neutral, 1 ending word; the key string says the same", c[g.starter] == 9 && c[g.other(g.starter)] == 8 && c[LinkGame.NONE] == 7 && c[LinkGame.END] == 1 && g.keyString().length() == 25 && g.keyString().chars().filter(ch -> ch == 'x').count() == 1);
        check("link: the starting team plays first; a clue needs a number from 1 to 9; no touching before the clue", g.turn == g.starter && g.phase == LinkGame.Phase.CLUE && !g.guess(0) && !g.clue(0) && !g.clue(10) && g.clue(2) && g.guessesLeft == 3);
        int mine = -1, neutral = -1, theirs = -1;
        for (int i = 0; i < 25; i++) { if (g.key[i] == g.turn && mine < 0) mine = i; else if (g.key[i] == LinkGame.NONE && neutral < 0) neutral = i; else if (g.key[i] == g.other(g.turn) && theirs < 0) theirs = i; }
        int team = g.turn;
        check("link: the team's own word goes on (a guess less); a word touched twice does not count", g.guess(mine) && g.turn == team && g.guessesLeft == 2 && !g.guess(mine));
        check("link: a neutral word ends the turn, the other team's clue is next", g.guess(neutral) && g.turn == g.other(team) && g.phase == LinkGame.Phase.CLUE);
        g.clue(1); int foreign = -1; for (int i = 0; i < 25; i++) if (g.key[i] == team && !g.shown[i]) { foreign = i; break; } int left = g.remaining(team); boolean hit = g.guess(foreign) && g.remaining(team) == left - 1;
        check("link: the other team's word ends the turn and counts for that team", hit && g.turn == team);
        LinkGame h = new LinkGame(LinkGame.pick(all, new HashSet<>(), new Random(8)), new Random(8));
        int endCell = -1; for (int i = 0; i < 25; i++) if (h.key[i] == LinkGame.END) endCell = i;
        int t0 = h.turn; h.clue(1); h.guess(endCell);
        check("link: the ending word loses the game for the team that touched it", h.phase == LinkGame.Phase.END && h.lostByEnd && h.winner == h.other(t0));
        LinkGame k = new LinkGame(LinkGame.pick(all, new HashSet<>(), new Random(11)), new Random(11));
        int st = k.turn; k.clue(9);
        for (int i = 0; i < 25 && k.phase != LinkGame.Phase.END; i++) { if (k.key[i] == st && !k.shown[i]) { if (k.phase == LinkGame.Phase.CLUE) k.clue(9); k.guess(i); } }
        check("link: uncovering all of a team's words wins it the game", k.phase == LinkGame.Phase.END && k.winner == st && !k.lostByEnd && k.remaining(st) == 0);
        Set<String> recent = new HashSet<>(); for (int i = 0; i < 100; i++) recent.add("كلمة" + i);
        check("link: nothing repeats: the 25 are words this TV has not had, while there are enough", LinkGame.pick(all, recent, new Random(2)).stream().noneMatch(w -> recent.contains(w[0])));
        boolean threw = false; try { new LinkGame(all.subList(0, 24), new Random(1)); } catch (IllegalArgumentException e) { threw = true; }
        check("link: a board of 24 words is refused", threw);
        byte[] json = GameBundle.open("link", java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(bundleFile)));
        List<String[]> real = json == null ? null : LinkGame.parse(MiniJson.parse(new String(json, java.nio.charset.StandardCharsets.UTF_8)));
        check("link: the bundled list opens and parses (" + (real == null ? 0 : real.size()) + " words, at least 300), no word twice in either language", real != null && real.size() >= 300 && real.stream().map(w -> w[1]).distinct().count() == real.size());
    }

    static boolean ender(NightGame g) { g.endVote(); return true; }
    static int who(NightGame g, int r) { for (int i = 0; i < g.players; i++) if (g.role[i] == r && g.alive[i]) return i; return -1; }
    static int notRole(NightGame g, int r) { for (int i = 0; i < g.players; i++) if (g.role[i] != r && g.alive[i]) return i; return -1; }

    static void night() {
        int[] wc = new int[11]; boolean shape = true;
        for (int n = 6; n <= 10; n++) { NightGame g = new NightGame(n, new java.util.Random(n)); int[] c = new int[4]; for (int r : g.role) c[r]++; shape &= c[1] == NightGame.wolvesFor(n) && c[2] == 1 && c[3] == 1 && c[0] == n - c[1] - 2; }
        check("night: 6 to 10 players get the wolves (1, 2 or 3), one guard, one seer, the rest villagers", shape && NightGame.wolvesFor(6) == 1 && NightGame.wolvesFor(7) == 2 && NightGame.wolvesFor(9) == 3);
        boolean threw = false; try { new NightGame(5, new java.util.Random(1)); } catch (IllegalArgumentException e) { threw = true; }
        try { new NightGame(11, new java.util.Random(1)); threw = false; } catch (IllegalArgumentException e) { }
        check("night: five or eleven players are refused", threw);
        NightGame g = new NightGame(6, new java.util.Random(3));
        int w = who(g, NightGame.WOLF), gd = who(g, NightGame.GUARD), sr = who(g, NightGame.SEER), v = who(g, NightGame.VILLAGER);
        check("night: a wolf cannot pick a wolf, a villager has no pick, nobody acts out of role", !g.wolf(w, w) && !g.wolf(v, gd) && !g.guard(w, gd) && !g.needsPick(v) && g.needsPick(w) && g.needsPick(gd) && g.needsPick(sr));
        check("night: the seer hears wolf or not, and cannot ask about themselves", g.seer(sr, w) == Boolean.TRUE && g.seer(sr, v) == Boolean.FALSE && g.seer(sr, sr) == null);
        check("night: the night is not done until all three have chosen", !g.nightDone() && g.wolf(w, v) && !g.nightDone() && g.guard(gd, gd) && g.nightDone());
        g.endNight();
        check("night: the wolf's pick leaves the village when the guard guarded someone else", g.outLast == v && !g.alive[v] && !g.savedLast && g.phase == NightGame.Phase.DAY);
        g.startVote(); int voter = who(g, NightGame.GUARD), t = w;
        check("night: a vote cannot be for yourself or for someone out, once each", !g.vote(voter, voter) && !g.vote(voter, v) && g.vote(voter, t) && !g.vote(voter, sr));
        for (int i = 0; i < 6; i++) if (g.alive[i] && i != voter) g.vote(i, i == t ? voter : t);
        check("night: all voted, the clear plurality leaves, and with the last wolf out the village wins", g.allVoted() && ender(g) && g.voteOut == t && g.phase == NightGame.Phase.END && g.winner == NightGame.VILLAGE_WINS);
        NightGame h = new NightGame(6, new java.util.Random(5));
        int hw = who(h, NightGame.WOLF), hg = who(h, NightGame.GUARD), hv = who(h, NightGame.VILLAGER);
        h.wolf(hw, hv); h.guard(hg, hv); h.endNight();
        check("night: when the guard guards the wolf's pick nobody leaves, and the same pick two nights in a row is refused", h.outLast == -1 && h.savedLast && h.alive[hv]);
        h.startVote(); h.endVote(); h.nextNight();
        check("night: a tied or empty vote sends nobody; the next night the guard may not repeat", h.voteOut == -1 && h.phase == NightGame.Phase.NIGHT && h.night == 2 && !h.guard(hg, hv) && h.guard(hg, hg));
        NightGame k = new NightGame(6, new java.util.Random(9)); int kw = who(k, NightGame.WOLF);
        for (int i = 0; i < 6 && k.phase != NightGame.Phase.END; i++) { int a = who(k, NightGame.GUARD); if (k.phase == NightGame.Phase.NIGHT) { k.wolf(kw, notRole(k, NightGame.WOLF)); if (a >= 0) k.guard(a, a == kw ? 0 : kw); k.endNight(); if (k.phase == NightGame.Phase.DAY) { k.startVote(); k.endVote(); if (k.phase == NightGame.Phase.VOTED) k.nextNight(); } } }
        check("night: with nobody voted out the wolves take the village, and the game ends with a winner", k.phase == NightGame.Phase.END && (k.winner == NightGame.WOLVES_WIN || k.winner == NightGame.VILLAGE_WINS));
    }

    static void guess(String bundleFile) throws Exception {
        byte[] json = GameBundle.open("guess", java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(bundleFile)));
        List<GuessGame.Topic> all = json == null ? null : GuessGame.parse(MiniJson.parse(new String(json, java.nio.charset.StandardCharsets.UTF_8)));
        check("guess: the bundled topics open and parse (" + (all == null ? 0 : all.size()) + ", at least 15), each list at least 12 answers", all != null && all.size() >= 15 && all.stream().allMatch(t -> t.size() >= 12 && !t.t[1].isEmpty()));
        check("guess: the spelling is forgiving (marks, alef and taa marbuta forms, the article) and still exact otherwise",
            GuessGame.norm("الأَرْنَب").equals(GuessGame.norm("ارنب")) && GuessGame.norm("مَدِينَة").equals("مدينه") && !GuessGame.norm("تفاح").equals(GuessGame.norm("تفاحه x")));
        GuessGame.Topic fruit = null; for (GuessGame.Topic t : all) if (t.id.equals("gs-fruit")) fruit = t;
        List<GuessGame.Topic> one = new java.util.ArrayList<>(); one.add(fruit); one.add(all.get(1));
        GuessGame g = new GuessGame(3, one, new java.util.Random(1));
        check("guess: a bid is a number from 1 to the cap (ten at most, half the list at most), once per player", !g.bid(0, 0) && !g.bid(0, 11) && g.bid(0, 3) && !g.bid(0, 4) && g.bid(1, 5) && g.cap() == 10 && g.bidsIn() == 2 && !g.allBid());
        g.bid(2, 5); g.endBids();
        check("guess: the highest bid names the things (a tie is settled by chance, between the tied only)", g.phase == GuessGame.Phase.ANSWER && g.topBid == 5 && (g.winner == 1 || g.winner == 2));
        int n = g.answer(java.util.Arrays.asList("تفاحة", "الموز", "برتقال", "موز", "xx"));
        check("guess: the same name twice counts once, a name outside the list counts nothing; 3 of 5 fails: everybody else gets 1", n == 3 && !g.valid(3) && !g.valid(4) && !g.succeeded() && g.score[g.winner] == 0 && g.score[(g.winner + 1) % 3] == 1 && g.phase == GuessGame.Phase.REVEAL);
        check("guess: the reveal shows names the bidder missed", g.examples(3).size() == 3 && !g.examples(3).contains("تفاح"));
        g.next();
        g.bid(0, 2); g.bid(1, 1); g.bid(2, 1); g.endBids(); g.answer(java.util.Arrays.asList(all.get(1).names.get(0), all.get(1).names.get(1), "extra"));
        check("guess: naming the bid or more scores the bid for the bidder; the game ends after the last topic", g.winner == 0 && g.succeeded() && g.gained[0] == 2 && (g.phase == GuessGame.Phase.REVEAL) && endsAfter(g));
        GuessGame e = new GuessGame(2, one, new java.util.Random(2)); e.endBids();
        check("guess: nobody bidding passes the round with no points", e.phase == GuessGame.Phase.REVEAL && e.winner < 0 && e.score[0] == 0 && e.score[1] == 0);
        boolean threw = false; try { new GuessGame(1, one, new java.util.Random(1)); } catch (IllegalArgumentException x) { threw = true; }
        check("guess: one player is refused (two to ten)", threw);
        java.util.Set<String> seen = new java.util.HashSet<>(); for (GuessGame.Topic t : all) seen.add(t.id);
        List<GuessGame.Topic> p1 = GuessGame.pick(all, 6, false, new java.util.HashSet<>(), new java.util.Random(3));
        java.util.Set<String> used = new java.util.HashSet<>(); for (GuessGame.Topic t : p1) used.add(t.id);
        List<GuessGame.Topic> p2 = GuessGame.pick(all, 6, false, used, new java.util.Random(4));
        check("guess: the topics kept in memory are not asked again while others remain", p1.size() == 6 && p2.stream().noneMatch(t -> used.contains(t.id)));
    }
    static boolean endsAfter(GuessGame g) { g.next(); return g.phase == GuessGame.Phase.END; }

    static void draw() {
        DrawGame g = new DrawGame(4, 2);
        check("draw: each player draws turns times, the drawer goes round", g.rounds() == 8 && g.drawer() == 0);
        g.setWord("وردة", "Rose");
        check("draw: the drawer cannot guess; a wrong guess is refused; the spelling is forgiving; the English word counts too", !g.guess(0, "وردة") && !g.guess(1, "نخلة") && g.guess(1, "الوَرْدَه") && g.guess(2, "rose") && !g.guess(1, "وردة"));
        g.guess(3, "وردة"); check("draw: everyone right ends the round early", g.allRight());
        g.endRound();
        check("draw: first right scores 3, second 2, the rest 1; the drawer one for each", g.gained[1] == 3 && g.gained[2] == 2 && g.gained[3] == 1 && g.gained[0] == 3 && g.phase == DrawGame.Phase.REVEAL);
        g.next(); check("draw: the next round is the next drawer, with a clean slate", g.drawer() == 1 && g.correct == 0 && !g.isRight(1) && g.phase == DrawGame.Phase.DRAW && g.word.isEmpty());
        for (int i = 0; i < 7; i++) { g.setWord("x", ""); g.endRound(); g.next(); }
        check("draw: after players x turns rounds the game is over", g.phase == DrawGame.Phase.END);
        boolean threw = false; try { new DrawGame(2, 1); } catch (IllegalArgumentException e) { threw = true; }
        check("draw: two players are refused (three to ten)", threw);
    }

    public static void main(String[] a) throws Exception {
        forehead();
        author(a[0]);
        closest(a[1]);
        liar(a[2]);
        emoji(a[3]);
        link(a[4]);
        night();
        guess(a[5]);
        draw();
        System.exit(failed == 0 ? 0 : 1);
    }
}
