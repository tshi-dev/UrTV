package sa.urtv.screensaver.games.charades;

import java.util.ArrayList;
import java.util.List;

/** «بدون كلام / وصّف»: the rules of one game (games/charades/SPEC.md), with no Android and no screen in it: the screen (one remote, pass-and-play) and, later, a
 *  phone mode both drive this. Tested on a plain JVM (tests/PartyGamesTest.java, a fake clock).
 *
 *  A turn: PASS (the remote goes to the team's actor, the guessers cover their eyes) → PLAY: the word is shown; OK hides it and the clock starts. Then OK is «صح»
 *  (+1, the next word is shown at once, OK or WORD_MS hides it), skip() is «تخطّي» (free, SKIPS_MAX a turn), peek() shows the word again for PEEK_MS, and in
 *  «وصّف» with the setting on foul() is «مخالفة» (-1, never below 0). The clock ends the turn: TURN_END shows the word that was open. Every team gets `turns`
 *  turns; a tie for first is settled by sudden death (one word each for the tied teams, SUDDEN_MS, at most SUDDEN_MAX times; still tied: they share it). */
public final class CharadesGame {
    public interface Clock { long now(); }
    public enum Phase { PASS, PLAY, TURN_END, END }

    public static final int MIN_TEAMS = 2, MAX_TEAMS = 4, DEFAULT_TURNS = 3, SKIPS_MAX = 3, SUDDEN_MAX = 3;
    public static final int[] SECONDS = {45, 60, 90};
    public static final long WORD_MS = 4000, PEEK_MS = 3000, GUARD_MS = 600, SUDDEN_MS = 30000;

    public final boolean describe, fouls; public final int teams, seconds, turns;
    private final CharadesWords.Deck deck; private final Clock clock;

    public Phase phase = Phase.PASS;
    public final int[] score;
    public int team;                       // whose turn
    public int turn;                       // 0-based, of teams x turns (not counted in sudden death)
    public CharadesWords.Word word;        // the word in play (PLAY), or the one that was open when the turn ended (TURN_END; null when it was guessed)
    public int got, skips, fouled;         // this turn
    public boolean timeUp;                 // TURN_END: the clock ended it (ring the bell, show the word to everyone)
    public int sudden;                     // 0, or which sudden-death go this is (1 to SUDDEN_MAX)
    private List<Integer> tied = new ArrayList<>(); private int tiedAt;
    private boolean visible, started; private long hideAt, guardUntil, deadline, leftAtPause = -1;

    public CharadesGame(CharadesWords.Deck deck, boolean describe, int teams, int seconds, int turns, boolean fouls, Clock clock) {
        this.deck = deck; this.describe = describe; this.teams = Math.max(MIN_TEAMS, Math.min(MAX_TEAMS, teams)); this.seconds = Math.max(10, seconds);
        this.turns = Math.max(1, turns); this.fouls = fouls && describe; this.clock = clock;
        score = new int[this.teams];
        begin();
    }
    private void begin() { got = skips = fouled = 0; timeUp = false; started = visible = false; word = null; leftAtPause = -1; phase = Phase.PASS; }

    public int round() { return turn / teams; }
    public long turnMs() { return sudden > 0 ? SUDDEN_MS : seconds * 1000L; }
    public boolean deckReshuffled() { boolean r = deck.reshuffled; deck.reshuffled = false; return r; }
    public List<String> recent() { return deck.recent(); }

    // ---- PASS → PLAY
    /** OK on the pass screen: the actor sees the word (the clock waits) */
    public void ready() {
        if (phase != Phase.PASS || paused()) return;
        if (word == null) word = deck.draw();
        if (word == null) { phase = Phase.END; return; }
        visible = true; hideAt = Long.MAX_VALUE; phase = Phase.PLAY;
    }
    public boolean wordVisible() { return phase == Phase.PLAY && visible; }
    public boolean clockRunning() { return phase == Phase.PLAY && started && !paused(); }
    public long leftMs() {
        if (phase != Phase.PLAY || !started) return phase == Phase.TURN_END ? 0 : turnMs();
        return leftAtPause >= 0 ? leftAtPause : Math.max(0, deadline - clock.now());
    }
    /** the one key of the game: on an open word it hides it (the first time, that starts the clock); on a hidden word it is «صح» */
    public void ok() { if (wordVisible()) hide(); else correct(); }
    public void hide() {
        if (!wordVisible() || paused()) return;
        visible = false;
        guardUntil = clock.now() + GUARD_MS;   // the second press of a quick double OK on «أخفِ الكلمة» must not count a word
        if (!started) { started = true; deadline = clock.now() + turnMs(); }
    }
    /** «صح»: a point, and the next word is shown at once. Ignored for GUARD_MS after a new word came (a double press would throw it away). */
    public boolean correct() {
        if (phase != Phase.PLAY || !started || visible || paused() || clock.now() < guardUntil) return false;
        score[team]++; got++;
        if (sudden > 0) { word = null; phase = Phase.TURN_END; return true; }
        next(); return true;
    }
    public boolean canSkip() { return phase == Phase.PLAY && started && !paused() && skips < SKIPS_MAX && sudden == 0; }
    /** «تخطّي»: another word, no point lost, SKIPS_MAX a turn */
    public boolean skip() {
        if (!canSkip() || clock.now() < guardUntil) return false;
        skips++; next(); return true;
    }
    private void next() {
        word = deck.draw(); visible = true; hideAt = clock.now() + WORD_MS; guardUntil = clock.now() + GUARD_MS;
    }
    /** the actor forgot: the word again for PEEK_MS (free) */
    public void peek() { if (phase == Phase.PLAY && started && !visible && !paused()) { visible = true; hideAt = clock.now() + PEEK_MS; } }
    public boolean canFoul() { return fouls && phase == Phase.PLAY && started && !paused(); }
    /** «مخالفة» («وصّف» only, the setting on): the describer said a part of the word: -1, never below 0 */
    public boolean foul() {
        if (!canFoul()) return false;
        fouled++; if (score[team] > 0) score[team]--;
        return true;
    }

    // ---- TURN_END → the next turn, sudden death or END
    private void endTurn() { timeUp = true; visible = false; phase = Phase.TURN_END; }
    /** OK on the turn's result */
    public void nextTurn() {
        if (phase != Phase.TURN_END) return;
        if (sudden == 0) {
            if (++turn < teams * turns) { team = turn % teams; begin(); return; }
            tied = leaders();
        } else {
            if (++tiedAt < tied.size()) { team = tied.get(tiedAt); begin(); return; }
            List<Integer> still = new ArrayList<>();
            for (int t : leaders()) if (tied.contains(t)) still.add(t);
            tied = still;
        }
        if (tied.size() < 2 || sudden >= SUDDEN_MAX) { phase = Phase.END; return; }
        sudden++; tiedAt = 0; team = tied.get(0); begin();
    }
    /** «إنهاء الجولة» from the pause card: this turn ends now with what it has */
    public void dropTurn() { if (phase == Phase.PASS || phase == Phase.PLAY) { leftAtPause = -1; word = null; timeUp = false; visible = false; phase = Phase.TURN_END; } }
    /** the teams in sudden death now (empty outside it) */
    public List<Integer> suddenTeams() { return sudden > 0 ? new ArrayList<>(tied) : new ArrayList<Integer>(); }
    private List<Integer> leaders() {
        int best = Integer.MIN_VALUE; List<Integer> w = new ArrayList<>();
        for (int s : score) best = Math.max(best, s);
        for (int i = 0; i < teams; i++) if (score[i] == best) w.add(i);
        return w;
    }
    /** the teams with the most points (several only when sudden death did not settle it) */
    public List<Integer> winners() { return leaders(); }
    public List<Integer> ranking() {
        List<Integer> r = new ArrayList<>();
        for (int i = 0; i < teams; i++) r.add(i);
        java.util.Collections.sort(r, (a, b) -> score[b] != score[a] ? score[b] - score[a] : a - b);
        return r;
    }

    // ---- time
    public boolean paused() { return leftAtPause >= 0; }
    /** Back (the pause card), or the app leaves the screen: the word hides first, the clock stops. Before the clock started the turn goes back to its pass screen. */
    public void pause() {
        if (paused()) return;
        if (phase == Phase.PLAY && !started) { visible = false; phase = Phase.PASS; }
        visible = false;
        leftAtPause = phase == Phase.PLAY ? Math.max(0, deadline - clock.now()) : 0;
    }
    public void resume() {
        if (!paused()) return;
        if (phase == Phase.PLAY) deadline = clock.now() + leftAtPause;
        leftAtPause = -1;
    }
    /** call a few times a second: hides a word whose time is over, ends the turn at 0. True when something changed. */
    public boolean tick() {
        if (phase != Phase.PLAY || paused()) return false;
        if (started && clock.now() >= deadline) { endTurn(); return true; }
        if (visible && started && clock.now() >= hideAt) { visible = false; return true; }
        return false;
    }
}
