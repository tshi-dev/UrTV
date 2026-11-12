package sa.urtv.screensaver.games.oddone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** «واحد مو معنا»: the rules of one game (games/odd-one/SPEC.md), with no Android and no screen in it: the screen (one remote, pass-and-play) and, later, a
 *  phone mode both drive this. Tested on a plain JVM (tests/PartyGamesTest.java, a fake clock).
 *
 *  A round: DEAL (the remote goes round: each player sees «topic: word», the odd one the topic only) → TALK (one sentence each, a 30 s guide timer, one or two
 *  laps) → VOTE → REVEAL (who the room chose) → GUESS (only when the odd one was caught: one spoken guess, the host says right or wrong) → SCORE → the next round or END.
 *
 *  The vote has two ways. By hands (the default: the fastest in a living room): everyone points on the count, the host enters who got the most (accuse), or
 *  «ما اتفقنا» (accuse(NOBODY)) when the room cannot settle it. Secret: the remote goes round and each picks a suspect (vote); a tie is voted again between the
 *  tied players only, once; a second tie means nobody is accused.
 *  Points: the odd one caught and wrong about the word: 1 to every other player. Caught but right: 1 to the odd one only. Not caught: 2 to the odd one. */
public final class OddOneGame {
    public interface Clock { long now(); }
    public enum Phase { DEAL, TALK, VOTE, REVEAL, GUESS, SCORE, END }
    public enum Outcome { CAUGHT, CAUGHT_GUESSED, ESCAPED }

    public static final int MIN_PLAYERS = 3, MAX_PLAYERS = 10, DEFAULT_PLAYERS = 4, NOBODY = -1;
    public static final int[] ROUNDS = {3, 5};
    public static final long SECRET_MS = 5000, TALK_MS = 30000;

    public final int players, rounds, laps; public final boolean secretVote;
    private final String topicId; private final List<OddOneWords.Topic> topics; private final Clock clock; private final Random rnd;
    private final Set<String> usedWords = new HashSet<>(), usedTopics = new HashSet<>();
    private List<String> recent;

    public Phase phase; public int round;                 // round: 0-based
    public OddOneWords.Word word; public OddOneWords.Topic topic; public int odd;
    public final int[] score, gained;                     // gained: this round's points, set in SCORE
    public Outcome outcome; public int accused = NOBODY;
    // deal
    private int dealAt; private boolean shown; private long shownAt;
    // talk
    private int[] order; private int talkAt; private long talkStart;
    // vote
    private int voterAt; private int[] ballots; private List<Integer> candidates; public boolean revote;
    public boolean wordShown;                             // GUESS: the host opened the word for the room
    private long pausedAt = -1;

    public OddOneGame(List<OddOneWords.Topic> topics, int players, int rounds, int laps, boolean secretVote, String topicId, List<String> recent, Clock clock, Random rnd) {
        this.topics = topics; this.players = Math.max(MIN_PLAYERS, Math.min(MAX_PLAYERS, players)); this.rounds = Math.max(1, rounds); this.laps = laps == 2 ? 2 : 1;
        this.secretVote = secretVote; this.topicId = topicId != null && OddOneWords.topic(topics, topicId) != null ? topicId : null;
        this.recent = recent == null ? new ArrayList<String>() : new ArrayList<>(recent); this.clock = clock; this.rnd = rnd;
        score = new int[this.players]; gained = new int[this.players];
        round = -1; deal();
    }

    /** the last words played on this TV, this game's included: the screen keeps it for the next game */
    public List<String> recent() { return new ArrayList<>(recent); }

    private void deal() {
        round++;
        word = OddOneWords.pick(topics, topicId, usedWords, usedTopics, recent, rnd);
        if (word == null) { usedWords.clear(); word = OddOneWords.pick(topics, topicId, usedWords, usedTopics, recent, rnd); }   // every word of the file played in one game: start over
        topic = OddOneWords.topic(topics, word.topic);
        usedWords.add(word.id); usedTopics.add(topic.id);
        if (usedTopics.size() >= topics.size()) usedTopics.clear();
        recent = OddOneWords.remember(recent, word.id, OddOneWords.RECENT_MAX);
        odd = rnd.nextInt(players);
        accused = NOBODY; outcome = null; revote = false; wordShown = false;
        for (int i = 0; i < players; i++) gained[i] = 0;
        dealAt = 0; shown = false; phase = Phase.DEAL;
    }

    // ---- DEAL: pass screen (no secret) → OK → the secret, SECRET_MS or until OK → the next player's pass screen
    /** whose turn it is to look (DEAL) */
    public int dealPlayer() { return dealAt; }
    public boolean secretShown() { return phase == Phase.DEAL && shown; }
    public boolean isOdd(int player) { return player == odd; }
    /** what `player` may see: the word, or null for the odd one (the topic only). A phone mode asks this per player. */
    public OddOneWords.Word secretFor(int player) { return player == odd ? null : word; }
    public void show() { if (phase == Phase.DEAL && !shown && pausedAt < 0) { shown = true; shownAt = clock.now(); } }
    /** OK on the secret, or its time is over: hidden, and the remote goes to the next player (after the last: the talk) */
    public void hide() {
        if (phase != Phase.DEAL || !shown) return;
        shown = false;
        if (++dealAt >= players) startTalk();
    }
    /** the app left the screen or the TV slept with a secret open: hidden, and the same player gets the pass screen again */
    public void cover() { if (phase == Phase.DEAL) shown = false; }
    /** «رجّعها»: the last player hid the word too soon: his pass screen again (from the next player's pass screen, or from the start of the talk) */
    public boolean canDealBack() { return (phase == Phase.DEAL && !shown && dealAt > 0) || (phase == Phase.TALK && talkAt == 0); }
    public void dealBack() {
        if (!canDealBack()) return;
        if (phase == Phase.TALK) { phase = Phase.DEAL; dealAt = players - 1; } else dealAt--;
    }
    /** «نعيد التوزيع»: someone missed his word and nobody has spoken yet: the remote goes round again with the same word and the same odd one */
    public boolean canRedeal() { return phase == Phase.TALK && talkAt == 0; }
    public void redeal() { if (canRedeal()) { phase = Phase.DEAL; dealAt = 0; shown = false; } }
    public long secretLeftMs() { return secretShown() ? Math.max(0, SECRET_MS - (now() - shownAt)) : 0; }

    // ---- TALK
    private void startTalk() {
        int first = rnd.nextInt(players);
        order = new int[players];
        for (int i = 0; i < players; i++) order[i] = (first + i) % players;
        talkAt = 0; talkStart = clock.now(); phase = Phase.TALK;
    }
    /** the order of one lap */
    public int[] order() { return order.clone(); }
    public int speaker() { return order[talkAt % players]; }
    public int lap() { return talkAt / players; }
    /** how many have spoken, of players x laps */
    public int talkDone() { return talkAt; }
    public int talkTotal() { return players * laps; }
    /** the guide timer: at 0 nothing happens by itself (the screen beeps softly once) */
    public long talkLeftMs() { return phase == Phase.TALK ? Math.max(0, TALK_MS - (now() - talkStart)) : 0; }
    /** OK in the talk: the next speaker; after the last one, the vote */
    public void nextSpeaker() {
        if (phase != Phase.TALK) return;
        talkStart = clock.now();
        if (++talkAt >= talkTotal()) startVote(null);
    }

    // ---- VOTE
    private void startVote(List<Integer> tied) {
        phase = Phase.VOTE; voterAt = 0; ballots = new int[players];
        candidates = new ArrayList<>();
        if (tied != null) candidates.addAll(tied); else for (int i = 0; i < players; i++) candidates.add(i);
    }
    /** by hands: the host enters who the room pointed at, or NOBODY («ما اتفقنا») */
    public void accuse(int player) {
        if (phase != Phase.VOTE || player < NOBODY || player >= players) return;
        accused = player; phase = Phase.REVEAL;
    }
    /** secret vote: whose turn it is to pick */
    public int voter() { return voterAt; }
    /** secret vote: who the current voter may pick (never himself; in a revote the tied players only) */
    public List<Integer> candidates() {
        List<Integer> c = new ArrayList<>(candidates);
        c.remove(Integer.valueOf(voterAt));
        return c;
    }
    /** secret vote: the current voter's pick; false when it is not allowed. After the last voter: the count. */
    public boolean vote(int suspect) {
        if (phase != Phase.VOTE || !candidates().contains(suspect)) return false;
        ballots[voterAt] = suspect;
        if (++voterAt < players) return true;
        int[] n = tally(); int best = 0;
        for (int v : n) best = Math.max(best, v);
        List<Integer> top = new ArrayList<>();
        for (int i = 0; i < players; i++) if (n[i] == best) top.add(i);
        if (top.size() == 1) { accused = top.get(0); phase = Phase.REVEAL; }
        else if (!revote) { revote = true; startVote(top); }
        else { accused = NOBODY; phase = Phase.REVEAL; }
        return true;
    }
    /** the votes each player got in the count just closed (secret vote) */
    public int[] tally() {
        int[] n = new int[players];
        for (int i = 0; i < Math.min(voterAt, players); i++) n[ballots[i]]++;
        return n;
    }

    // ---- REVEAL → GUESS → SCORE
    public boolean caught() { return accused == odd; }
    /** OK on the reveal: the odd one was caught → his one guess; not caught → the points */
    public void afterReveal() {
        if (phase != Phase.REVEAL) return;
        if (caught()) phase = Phase.GUESS; else settle(Outcome.ESCAPED);
    }
    /** GUESS: OK opens the word for the room, then the host says whether the spoken guess was right */
    public void showWord() { if (phase == Phase.GUESS) wordShown = true; }
    public void guess(boolean right) { if (phase == Phase.GUESS) settle(right ? Outcome.CAUGHT_GUESSED : Outcome.CAUGHT); }
    private void settle(Outcome o) {
        outcome = o;
        for (int i = 0; i < players; i++) gained[i] = o == Outcome.CAUGHT ? (i == odd ? 0 : 1) : i == odd ? (o == Outcome.ESCAPED ? 2 : 1) : 0;
        for (int i = 0; i < players; i++) score[i] += gained[i];
        wordShown = true; phase = Phase.SCORE;
    }
    public boolean lastRound() { return round + 1 >= rounds; }
    /** OK on the points: the next round, or the end */
    public void nextRound() { if (phase == Phase.SCORE) { if (lastRound()) phase = Phase.END; else deal(); } }
    /** «إنهاء الجولة» from the pause card: this round is dropped with no points */
    /** «إنهاء الجولة» on the pause card ends the GAME, as in every other game: the result screen */
    public void finish() { phase = Phase.END; }
    public void dropRound() { if (phase != Phase.END) { if (lastRound()) phase = Phase.END; else deal(); } }

    /** the players with the most points (several on a tie) */
    public List<Integer> winners() {
        int best = 0; List<Integer> w = new ArrayList<>();
        for (int s : score) best = Math.max(best, s);
        for (int i = 0; i < players; i++) if (score[i] == best) w.add(i);
        return w;
    }
    /** the players by points, the best first (seat order inside a tie) */
    public List<Integer> ranking() {
        List<Integer> r = new ArrayList<>();
        for (int i = 0; i < players; i++) r.add(i);
        java.util.Collections.sort(r, (a, b) -> score[b] != score[a] ? score[b] - score[a] : a - b);
        return r;
    }

    // ---- time
    private long now() { return pausedAt >= 0 ? pausedAt : clock.now(); }
    public boolean paused() { return pausedAt >= 0; }
    /** Back (the pause card), or the app leaves the screen: an open secret hides first, the timers stop */
    public void pause() { cover(); if (pausedAt < 0) pausedAt = clock.now(); }
    public void resume() {
        if (pausedAt < 0) return;
        long d = clock.now() - pausedAt; pausedAt = -1;
        talkStart += d;
    }
    /** call a few times a second: closes a secret whose time is over. True when something changed. */
    public boolean tick() {
        if (secretShown() && pausedAt < 0 && clock.now() - shownAt >= SECRET_MS) { hide(); return true; }
        return false;
    }
}
