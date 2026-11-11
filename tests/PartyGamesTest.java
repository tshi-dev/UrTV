package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import sa.urtv.screensaver.games.charades.CharadesGame;
import sa.urtv.screensaver.games.charades.CharadesWords;
import sa.urtv.screensaver.games.oddone.OddOneGame;
import sa.urtv.screensaver.games.oddone.OddOneWords;

/** For tests/party_games_test.py: the rules of «واحد مو معنا» and «بدون كلام / وصّف» on a plain JVM (a fake clock, the bundled word files). Prints PASS/FAIL lines. */
public final class PartyGamesTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static long[] now = {1_000_000L};
    static List<OddOneWords.Topic> topics; static List<CharadesWords.Word> words;

    static OddOneGame odd(int players, int rounds, boolean secret, long seed) { return new OddOneGame(topics, players, rounds, 1, secret, null, null, () -> now[0], new Random(seed)); }
    /** the whole deal: every player looks and hides */
    static void deal(OddOneGame g) { while (g.phase == OddOneGame.Phase.DEAL) { g.show(); g.hide(); } }
    static void talk(OddOneGame g) { while (g.phase == OddOneGame.Phase.TALK) g.nextSpeaker(); }
    static int other(OddOneGame g) { return (g.odd + 1) % g.players; }
    /** a round to its points by hands: `who` is accused, the odd one's guess is `right` */
    static void round(OddOneGame g, int who, boolean right) { deal(g); talk(g); g.accuse(who); g.afterReveal(); if (g.phase == OddOneGame.Phase.GUESS) { g.showWord(); g.guess(right); } }
    static CharadesGame cha(boolean describe, int teams, int seconds, int turns, boolean fouls, long seed) {
        return new CharadesGame(new CharadesWords.Deck(words, describe, false, null, new Random(seed)), describe, teams, seconds, turns, fouls, () -> now[0]);
    }
    /** a whole turn with `n` right words, then the clock runs out */
    static void turn(CharadesGame g, int n) {
        g.ready(); g.ok();
        for (int i = 0; i < n; i++) { now[0] += 1000; g.correct(); g.hide(); }
        now[0] += g.turnMs(); g.tick();
    }

    public static void main(String[] a) throws Exception {
        String oddJson = new String(Files.readAllBytes(Paths.get(a[0], "odd-one.json")), StandardCharsets.UTF_8), chaJson = new String(Files.readAllBytes(Paths.get(a[0], "charades.json")), StandardCharsets.UTF_8);
        topics = OddOneWords.parse(MiniJson.parse(oddJson)); words = CharadesWords.parse(MiniJson.parse(chaJson));
        check("the bundled starters are read (the form of docs/GAMES-DATA.md): 10 topics or more of 20 words or more, 150 charades words or more", topics != null && topics.size() >= 10 && words != null && words.size() >= 150
            && topics.stream().allMatch(t -> t.words.size() >= 20));
        if (topics == null || words == null) { System.exit(1); }
        String W0 = "\"" + topics.get(0).words.get(0).text[0] + "\"", W1 = "\"" + topics.get(0).words.get(1).text[0] + "\"", C0 = "\"" + words.get(0).text[0] + "\"", C1 = "\"" + words.get(1).text[0] + "\"";
        check("a file with a broken part is refused whole: a difficulty of 4, a duplicate word in a topic, a «·» in a word, a duplicate charades word, a category that is not in the list, another version, an HTML page",
            OddOneWords.parse(MiniJson.parse(oddJson.replaceFirst("\",1]", "\",4]"))) == null && OddOneWords.parse(MiniJson.parse(oddJson.replaceFirst(W1, W0))) == null
            && OddOneWords.parse(MiniJson.parse(oddJson.replaceFirst(W1, "خ·ف"))) == null && CharadesWords.parse(MiniJson.parse(chaJson.replaceFirst(C1, C0))) == null && CharadesWords.parse(MiniJson.parse(chaJson.replaceFirst(",0,1,", ",99,1,"))) == null
            && CharadesWords.parse(MiniJson.parse(chaJson.replaceFirst("\"v\":1", "\"v\":2"))) == null && OddOneWords.parse(MiniJson.parse(oddJson.replaceFirst("\"v\":1", "\"v\":2"))) == null
            && OddOneWords.parse(MiniJson.parse("<html>404</html>")) == null && CharadesWords.parse(MiniJson.parse("<html>404</html>")) == null && CharadesWords.parse(MiniJson.parse("{\"v\":1,\"c\":[\"a\"],\"w\":[]}")) == null);

        // ================= «واحد مو معنا»
        {
        OddOneGame g = odd(5, 3, false, 7);
        check("odd-one deal: the remote goes to player 1 first with nothing shown; show() opens the secret, hide() passes to player 2", g.phase == OddOneGame.Phase.DEAL && g.dealPlayer() == 0 && !g.secretShown());
        g.show(); boolean s1 = g.secretShown(); g.hide();
        check("odd-one deal: ... and after hide the next pass screen has no secret open", s1 && !g.secretShown() && g.dealPlayer() == 1);
        int nulls = 0; for (int i = 0; i < 5; i++) if (g.secretFor(i) == null) nulls++; else if (g.secretFor(i) != g.word) nulls = 99;
        check("odd-one deal: everyone gets the same word but exactly one player, who gets the topic only", nulls == 1 && g.secretFor(g.odd) == null && g.topic != null && g.word.topic.equals(g.topic.id));
        g.show(); now[0] += OddOneGame.SECRET_MS - 1; boolean still = !g.tick() && g.secretShown(); now[0] += 1;
        check("odd-one deal: a secret closes by itself after 5 s and the remote moves on", still && g.tick() && !g.secretShown() && g.dealPlayer() == 2);
        g.dealBack();
        check("odd-one deal: «رجّعها» gives the last player his pass screen again (he hid it too soon), not while a secret is open", g.dealPlayer() == 1 && !g.secretShown());
        g.show(); g.dealBack(); boolean no = g.dealPlayer() == 1 && g.secretShown();
        g.cover();
        check("odd-one deal: the app leaving the screen hides an open secret and the same player gets the pass screen again", no && !g.secretShown() && g.dealPlayer() == 1);
        g.show(); g.pause(); boolean hidden = !g.secretShown() && g.paused(); g.show(); boolean stillHidden = !g.secretShown(); g.resume();
        check("odd-one deal: Back hides the secret first, and nothing opens while paused", hidden && stillHidden && !g.paused() && g.dealPlayer() == 1);
        deal(g);
        int[] o = g.order(); boolean circle = true; for (int i = 1; i < 5; i++) circle &= o[i] == (o[0] + i) % 5;
        check("odd-one talk: after the last player the talk starts; the order is the seats in a circle from a random first speaker", g.phase == OddOneGame.Phase.TALK && circle && g.speaker() == o[0] && g.talkTotal() == 5);
        g.dealBack(); boolean back = g.phase == OddOneGame.Phase.DEAL && g.dealPlayer() == 4; g.show(); g.hide();
        check("odd-one talk: before anyone spoke, «رجّعها» still gives the last player his word again", back && g.phase == OddOneGame.Phase.TALK);
        now[0] += 12000; long left = g.talkLeftMs(); g.pause(); now[0] += 60000; g.resume();
        check("odd-one talk: 30 s a speaker, and the pause card stops the timer", left == 18000 && g.talkLeftMs() == 18000);
        now[0] += 40000; boolean zero = g.talkLeftMs() == 0 && !g.tick() && g.phase == OddOneGame.Phase.TALK && g.talkDone() == 0;
        g.nextSpeaker();
        check("odd-one talk: the timer is a guide only (at 0 nothing moves); OK goes to the next speaker with a fresh 30 s", zero && g.talkDone() == 1 && g.talkLeftMs() == 30000 && g.speaker() == g.order()[1]);
        talk(g);
        check("odd-one talk: after one lap, the vote", g.phase == OddOneGame.Phase.VOTE);
        OddOneGame g2 = new OddOneGame(topics, 4, 3, 2, false, null, null, () -> now[0], new Random(3)); deal(g2);
        for (int i = 0; i < 4; i++) g2.nextSpeaker();
        boolean lap2 = g2.phase == OddOneGame.Phase.TALK && g2.lap() == 1 && g2.speaker() == g2.order()[0]; talk(g2);
        check("odd-one talk: two laps when set", lap2 && g2.talkTotal() == 8 && g2.phase == OddOneGame.Phase.VOTE);
        // by hands
        g.accuse(g.odd); boolean rev = g.phase == OddOneGame.Phase.REVEAL && g.caught(); g.afterReveal();
        boolean guess = g.phase == OddOneGame.Phase.GUESS && !g.wordShown; g.showWord(); g.guess(false);
        boolean pts = true; for (int i = 0; i < 5; i++) pts &= g.gained[i] == (i == g.odd ? 0 : 1) && g.score[i] == g.gained[i];
        check("odd-one by hands: the odd one accused → his one guess (the word opens for the room first); wrong → 1 point to every other player", rev && guess && pts && g.outcome == OddOneGame.Outcome.CAUGHT && g.phase == OddOneGame.Phase.SCORE);
        String w1 = g.word.id, t1 = g.topic.id; g.nextRound();
        check("odd-one rounds: the next round deals a new word of another topic", g.phase == OddOneGame.Phase.DEAL && g.round == 1 && !g.word.id.equals(w1) && !g.topic.id.equals(t1) && g.dealPlayer() == 0);
        int[] before = g.score.clone(); int oddNow = g.odd; round(g, g.odd, true);
        boolean only = true; for (int i = 0; i < 5; i++) only &= g.score[i] - before[i] == (i == oddNow ? 1 : 0);
        check("odd-one scoring: caught but he guessed the word → 1 point to the odd one only", only && g.outcome == OddOneGame.Outcome.CAUGHT_GUESSED);
        g.nextRound(); before = g.score.clone(); oddNow = g.odd; deal(g); talk(g); g.accuse(other(g)); g.afterReveal();
        boolean two = g.phase == OddOneGame.Phase.SCORE; for (int i = 0; i < 5; i++) two &= g.score[i] - before[i] == (i == oddNow ? 2 : 0);
        check("odd-one scoring: the room accused someone else → no guess, 2 points to the odd one", two && g.outcome == OddOneGame.Outcome.ESCAPED && g.lastRound());
        g.nextRound(); int best = Arrays.stream(g.score).max().getAsInt();
        check("odd-one end: after the last round the game ends; the winners are everyone with the most points, the ranking is by points", g.phase == OddOneGame.Phase.END && g.winners().stream().allMatch(i -> g.score[i] == best)
            && g.score[g.ranking().get(0)] == best && g.score[g.ranking().get(4)] == Arrays.stream(g.score).min().getAsInt());
        OddOneGame h = odd(4, 3, false, 11); deal(h); talk(h); int ho = h.odd; h.accuse(OddOneGame.NOBODY); h.afterReveal();
        check("odd-one by hands: «ما اتفقنا» (nobody accused) counts as not caught: 2 points to the odd one", h.phase == OddOneGame.Phase.SCORE && h.score[ho] == 2 && h.outcome == OddOneGame.Outcome.ESCAPED);
        h.nextRound(); h.show(); h.hide(); h.dropRound();
        check("odd-one: «إنهاء الجولة» drops the round with no points and deals the next", h.round == 2 && h.phase == OddOneGame.Phase.DEAL && Arrays.stream(h.score).sum() == 2);
        }
        {   // secret vote
        OddOneGame g = odd(5, 3, true, 5); deal(g); talk(g);
        check("odd-one secret vote: player 1 picks first, from everyone but himself", g.voter() == 0 && g.candidates().equals(Arrays.asList(1, 2, 3, 4)) && !g.vote(0) && g.voter() == 0 && !g.vote(9));
        g.vote(3); g.vote(3); g.vote(3); g.vote(1); boolean open = g.phase == OddOneGame.Phase.VOTE; g.vote(1);
        check("odd-one secret vote: the count comes only after the last vote; the most votes is accused", open && g.phase == OddOneGame.Phase.REVEAL && g.accused == 3 && Arrays.equals(g.tally(), new int[] {0, 2, 0, 3, 0}));
        g = odd(4, 3, true, 5); deal(g); talk(g);
        g.vote(1); g.vote(0); g.vote(0); g.vote(1);
        check("odd-one secret vote: a tie → a revote between the tied players only (a tied player cannot pick himself)", g.phase == OddOneGame.Phase.VOTE && g.revote && g.voter() == 0 && g.candidates().equals(Arrays.asList(1))
            && !g.vote(2));
        g.vote(1); g.vote(0); g.vote(0); g.vote(0);
        check("odd-one secret vote: the revote settles it", g.phase == OddOneGame.Phase.REVEAL && g.accused == 0);
        g = odd(3, 3, true, 5); deal(g); talk(g); int o3 = g.odd;
        g.vote(1); g.vote(2); g.vote(0); boolean re = g.revote && g.phase == OddOneGame.Phase.VOTE; g.vote(1); g.vote(2); g.vote(0); g.afterReveal();
        check("odd-one secret vote, 3 players: a three-way tie is voted again once; a second tie accuses nobody and the odd one wins the round (2 points)",
            re && g.accused == OddOneGame.NOBODY && g.phase == OddOneGame.Phase.SCORE && g.score[o3] == 2);
        }
        {   // the picking
        int[] counts = new int[6]; Set<Integer> firsts = new HashSet<>();
        for (int s = 0; s < 600; s++) { OddOneGame g = odd(6, 3, false, s); counts[g.odd]++; deal(g); firsts.add(g.speaker()); }
        check("odd-one: the odd player is any seat with about the same chance (600 games of 6: each 60 to 140 times), and so is the first speaker", Arrays.stream(counts).allMatch(c -> c >= 60 && c <= 140) && firsts.size() == 6);
        OddOneGame g = odd(4, 40, false, 2); Set<String> seenW = new HashSet<>(), seenT = new HashSet<>(); boolean rep = false, topicRep = false;
        for (int r = 0; r < 40; r++) { rep |= !seenW.add(g.word.id); if (r < topics.size()) topicRep |= !seenT.add(g.topic.id); g.dropRound(); }
        check("odd-one words: never a word twice in a game, and «عشوائي» goes through every topic before one comes again", !rep && !topicRep);
        String tid = topics.get(2).id; int n = topics.get(2).words.size();
        g = new OddOneGame(topics, 4, n + 3, 1, false, tid, null, () -> now[0], new Random(4)); boolean inTopic = true; Set<String> w = new HashSet<>();
        for (int r = 0; r < n; r++) { inTopic &= g.topic.id.equals(tid) && w.add(g.word.id); g.dropRound(); }
        check("odd-one words: a chosen topic gives its own words, each once; when it runs out the next word comes from another topic", inTopic && !g.topic.id.equals(tid));
        List<String> recent = new ArrayList<>(); boolean fresh = true;
        for (int game = 0; game < 30; game++) { OddOneGame x = new OddOneGame(topics, 4, 3, 1, false, null, recent, () -> now[0], new Random(game)); List<String> played = new ArrayList<>();
            for (int r = 0; r < 3; r++) { fresh &= !recent.contains(x.word.id); played.add(x.word.id); x.dropRound(); } recent = x.recent(); fresh &= recent.containsAll(played); }
        check("odd-one words: across games a word of the last 120 played on this TV does not come back (30 games in a row, 90 words, none twice)", fresh && recent.size() == 90);
        List<String> all = new ArrayList<>(); for (OddOneWords.Word x : topics.get(0).words) all.add(x.id);
        OddOneWords.Word oldest = OddOneWords.pick(topics.subList(0, 1), topics.get(0).id, new HashSet<>(), new HashSet<>(), all, new Random(1));
        check("odd-one words: when every word left is a recent one, the oldest comes first; the list keeps the newest 120", oldest.id.equals(all.get(0))
            && OddOneWords.remember(Arrays.asList("a", "b", "c"), "a", 2).equals(Arrays.asList("c", "a")));
        check("odd-one: players are kept to 3 to 10", odd(1, 3, false, 1).players == 3 && odd(12, 3, false, 1).players == 10 && odd(10, 3, false, 1).score.length == 10);
        }

        // ================= «بدون كلام / وصّف»
        {
        CharadesGame g = cha(false, 2, 60, 3, false, 1);
        check("charades: the first screen is the pass screen: no word yet, the first team, the clock full and still", g.phase == CharadesGame.Phase.PASS && g.word == null && g.team == 0 && g.leftMs() == 60000 && !g.clockRunning());
        g.ready(); now[0] += 20000; boolean waits = g.wordVisible() && !g.tick() && g.leftMs() == 60000 && !g.correct() && !g.skip();
        check("charades: OK shows the actor the word; the clock waits for as long as he reads (no point, no skip yet)", waits && g.word != null);
        g.ok(); now[0] += 5000;
        check("charades: OK again hides it and the clock starts", !g.wordVisible() && g.clockRunning() && g.leftMs() == 55000);
        CharadesWords.Word first = g.word; g.ok();
        check("charades: OK on a hidden word is «صح»: +1 and the next word is shown at once, the clock still running", g.score[0] == 1 && g.got == 1 && g.word != first && g.wordVisible() && g.clockRunning());
        boolean guard = !g.correct(); g.ok(); now[0] += 100; boolean guard2 = !g.correct() && g.score[0] == 1; now[0] += CharadesGame.GUARD_MS;
        check("charades: a double press does not throw the new word away (the next «صح» counts only 0.6 s after a new word)", guard && guard2 && g.correct() && g.score[0] == 2);
        now[0] += CharadesGame.WORD_MS - 1; boolean vis = g.wordVisible() && !g.tick(); now[0] += 1;
        check("charades: a new word hides by itself after 4 s", vis && g.tick() && !g.wordVisible());
        g.peek(); boolean p = g.wordVisible(); now[0] += CharadesGame.PEEK_MS; g.tick(); int sc = g.score[0];
        check("charades: a peek shows the word again for 3 s and costs nothing", p && !g.wordVisible() && g.score[0] == sc);
        CharadesWords.Word b = g.word; boolean s1 = g.skip() && g.word != b; g.hide(); now[0] += 700; boolean s2 = g.skip(); g.hide(); now[0] += 700; boolean s3 = g.skip(); g.hide(); now[0] += 700;
        check("charades: «تخطّي» gives another word for free, three times a turn at most", s1 && s2 && s3 && !g.canSkip() && !g.skip() && g.skips == 3 && g.score[0] == sc && !g.foul());
        long left = g.leftMs(); g.peek(); g.pause(); boolean hid = !g.wordVisible() && g.paused(); now[0] += 500000; boolean frozen = !g.tick() && g.leftMs() == left && !g.correct(); g.resume();
        check("charades: Back hides the word first and stops the clock; nothing counts while paused", hid && frozen && g.leftMs() == left && g.clockRunning());
        CharadesWords.Word open = g.word; now[0] += left; boolean ended = g.tick();
        check("charades: at 0 the turn ends and the word that was open is shown to everyone", ended && g.phase == CharadesGame.Phase.TURN_END && g.timeUp && g.word == open && g.leftMs() == 0 && !g.correct());
        g.nextTurn();
        check("charades: the turn passes to the next team, with its own pass screen and a full clock", g.phase == CharadesGame.Phase.PASS && g.team == 1 && g.turn == 1 && g.got == 0 && g.skips == 0 && g.leftMs() == 60000);
        g.ready(); g.pause(); boolean backToPass = g.phase == CharadesGame.Phase.PASS && !g.wordVisible(); CharadesWords.Word kept = g.word; g.ready(); boolean blocked = g.phase == CharadesGame.Phase.PASS; g.resume(); g.ready();
        check("charades: Back before the clock started returns to the pass screen; the same word comes again", backToPass && blocked && g.word == kept && g.wordVisible());
        g.ok(); now[0] += 60000; g.tick(); g.nextTurn();
        turn(g, 1); g.nextTurn(); turn(g, 3); g.nextTurn(); turn(g, 0); g.nextTurn(); boolean last = g.turn == 5 && g.round() == 2; turn(g, 1); g.nextTurn();
        check("charades: every team gets the same number of turns (3 each), then the team with the most points wins", last && g.phase == CharadesGame.Phase.END && g.score[0] == 3 && g.score[1] == 4 && g.winners().equals(Arrays.asList(1)) && g.ranking().equals(Arrays.asList(1, 0)));
        }
        {   // «وصّف» and the foul
        CharadesGame g = cha(true, 2, 45, 1, true, 2); g.ready(); boolean early = !g.foul(); g.ok();
        boolean zero = g.foul() && g.score[0] == 0 && g.fouled == 1; now[0] += 700; g.correct(); g.hide(); now[0] += 700; g.correct(); g.hide();
        check("«وصّف»: «مخالفة» takes a point (never below 0) once the clock runs", early && zero && g.foul() && g.score[0] == 1 && g.fouled == 2 && g.turnMs() == 45000);
        check("«مخالفة» exists only in «وصّف» with the setting on", !cha(false, 2, 60, 1, true, 2).fouls && !cha(true, 2, 60, 1, false, 2).fouls);
        }
        {   // sudden death
        CharadesGame g = cha(false, 3, 60, 1, false, 3);
        turn(g, 2); g.nextTurn(); turn(g, 2); g.nextTurn(); turn(g, 1); g.nextTurn();
        check("charades tie: the two teams tied for first go to sudden death (the third does not), one word each, 30 s", g.phase == CharadesGame.Phase.PASS && g.sudden == 1 && g.suddenTeams().equals(Arrays.asList(0, 1)) && g.team == 0 && g.turnMs() == 30000);
        g.ready(); g.ok(); now[0] += 3000; boolean noSkip = !g.canSkip(); g.correct();
        check("charades tie: the word guessed ends that team's go at once (no skip in sudden death)", noSkip && g.phase == CharadesGame.Phase.TURN_END && !g.timeUp && g.score[0] == 3);
        g.nextTurn(); boolean second = g.team == 1 && g.phase == CharadesGame.Phase.PASS; g.ready(); g.ok(); now[0] += 30000; g.tick(); g.nextTurn();
        check("charades tie: the team that got its word wins", second && g.phase == CharadesGame.Phase.END && g.winners().equals(Arrays.asList(0)));
        g = cha(false, 2, 60, 1, false, 4); turn(g, 1); g.nextTurn(); turn(g, 1); g.nextTurn(); int goes = 0;
        while (g.phase != CharadesGame.Phase.END && goes < 20) { goes++; g.ready(); g.ok(); now[0] += 30000; g.tick(); g.nextTurn(); }
        check("charades tie: still tied after three sudden-death goes each → they share the win", goes == 6 && g.winners().equals(Arrays.asList(0, 1)) && g.sudden == 3);
        g = cha(false, 2, 60, 2, false, 5); g.ready(); g.ok(); now[0] += 1000; g.correct(); g.dropTurn();
        check("charades: «إنهاء الجولة» ends the turn now and keeps its points", g.phase == CharadesGame.Phase.TURN_END && g.score[0] == 1 && !g.timeUp && g.word == null);
        }
        {   // the deck
        int act = 0, desc = 0, kids = 0; for (CharadesWords.Word w : words) { if (CharadesWords.matches(w, false, false)) act++; if (CharadesWords.matches(w, true, false)) desc++; if (CharadesWords.matches(w, false, true)) kids++; }
        CharadesWords.Deck d = new CharadesWords.Deck(words, false, true, null, new Random(1)); Set<String> seen = new HashSet<>(); boolean ok = d.size() == kids;
        for (int i = 0; i < kids; i++) { CharadesWords.Word w = d.draw(); ok &= w.kids && w.diff == 1 && w.act && seen.add(w.id) && !d.reshuffled; }
        check("charades deck: «أطفال» takes only kids words of difficulty 1 that can be acted; «وصّف» has more words than «بدون كلام»; no word twice", ok && kids >= 40 && desc > act && act >= 100);
        CharadesWords.Word lastW = null, again = d.draw();
        check("charades deck: used up → shuffled again, and it says so", d.reshuffled && again != null && d.left() == kids - 1);
        d = new CharadesWords.Deck(words.subList(0, 40), true, false, null, new Random(9)); boolean twice = false;
        for (int i = 0; i < 400; i++) { CharadesWords.Word w = d.draw(); twice |= w == lastW; lastW = w; }
        check("charades deck: never the same word twice in a row, even across a reshuffle", !twice);
        List<String> recent = new ArrayList<>(); boolean fresh = true;
        for (int game = 0; game < 4; game++) { CharadesWords.Deck x = new CharadesWords.Deck(words, false, false, recent, new Random(game)); for (int i = 0; i < 30; i++) fresh &= !recent.contains(x.draw().id); recent = x.recent(); }
        check("charades deck: across games the last 200 words played on this TV wait until the rest is used (4 games of 30 words, none twice)", fresh && recent.size() == 120);
        CharadesGame g = new CharadesGame(new CharadesWords.Deck(new ArrayList<CharadesWords.Word>(), false, false, null, new Random(1)), false, 2, 60, 1, false, () -> now[0]); g.ready();
        check("charades: no words at all → the game ends instead of showing an empty card; teams are kept to 2 to 4", g.phase == CharadesGame.Phase.END && cha(false, 9, 60, 1, false, 1).teams == 4 && cha(false, 1, 60, 1, false, 1).teams == 2);
        }
        {
        OddOneGame g = odd(4, 3, false, 9); deal(g); String w = g.word.id; int o = g.odd; g.redeal();
        boolean again = g.phase == OddOneGame.Phase.DEAL && g.dealPlayer() == 0 && g.word.id.equals(w) && g.odd == o; deal(g); g.nextSpeaker(); g.redeal();
        check("odd-one: before the first speaker the words can be dealt again (the same word, the same odd one); not once the talk began", again && g.phase == OddOneGame.Phase.TALK);
        check("charades: a published file needs 200 words, the bundled starter 100", CharadesWords.parse(MiniJson.parse(chaJson), 200) == null && CharadesWords.parse(MiniJson.parse(chaJson), 100) != null);
        }
        System.exit(failed == 0 ? 0 : 1);
    }
}
