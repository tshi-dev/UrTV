package sa.urtv.screensaver.games.forehead;

/**
 * «على جبيني»: the rules of one game (no Android, no network: tests/NewGamesTest.java). A team plays a turn: one of its players holds a phone to the forehead, the
 * word is on the phone (never on the TV) and the team describes it without saying it; tilt down or a button is «صح» (a point, the next word), tilt up or a button
 * is «تخطّي» (the next word, no point). The turn ends with the clock. Every team plays `turnsEach` turns, the most points wins, equal points share the win.
 * The words come from a `Words` (the activity gives the charades deck, the test a list). docs/GAMES-NEW.md.
 */
public final class ForeheadGame {
    public interface Words { String next(); }
    public interface Clock { long now(); }

    public enum Phase { READY, COUNT, PLAY, TURN_END, END }

    public static final int[] SECONDS = {45, 60, 90}, TURNS = {2, 3, 4};
    public static final int MIN_TEAMS = 2, MAX_TEAMS = 4;
    public static final long COUNT_MS = 3000;

    public final int teams, turnsEach; public final long turnMs; public final int[] score;
    public Phase phase = Phase.READY; public int turn, got, skipped; public String word = "";
    private final Words words; private final Clock clock; private long countAt, deadline, pausedAt = -1, leftAtPause;

    public ForeheadGame(int teams, int turnsEach, int seconds, Words words, Clock clock) {
        if (teams < MIN_TEAMS || teams > MAX_TEAMS) throw new IllegalArgumentException("teams");
        this.teams = teams; this.turnsEach = Math.max(1, turnsEach); this.turnMs = Math.max(10, seconds) * 1000L; this.words = words; this.clock = clock; this.score = new int[teams];
    }

    public int team() { return turn % teams; }
    public int round() { return turn / teams; }
    public int turns() { return teams * turnsEach; }
    public boolean paused() { return pausedAt >= 0; }

    /** the player is ready (the phone is on the forehead): the count of three begins */
    public void ready() { if (phase == Phase.READY && !paused()) { phase = Phase.COUNT; countAt = clock.now(); } }

    /** the clock: the count ends and the first word comes; the turn ends */
    public void tick() {
        if (paused()) return;
        long now = clock.now();
        if (phase == Phase.COUNT && now - countAt >= COUNT_MS) { phase = Phase.PLAY; deadline = now + turnMs; word = words.next(); }
        else if (phase == Phase.PLAY && now >= deadline) { phase = Phase.TURN_END; word = ""; }
    }
    public long leftMs() { return phase == Phase.PLAY ? Math.max(0, paused() ? leftAtPause : deadline - clock.now()) : phase == Phase.COUNT ? turnMs : 0; }

    /** «صح»: a point and the next word; false when no turn is running */
    public boolean right() { if (phase != Phase.PLAY || paused()) return false; score[team()]++; got++; word = words.next(); return true; }
    /** «تخطّي»: the next word, no point */
    public boolean skip() { if (phase != Phase.PLAY || paused()) return false; skipped++; word = words.next(); return true; }

    /** after the turn's result: the next team's turn, or the end */
    public void nextTurn() {
        if (phase != Phase.TURN_END) return;
        turn++; got = 0; skipped = 0;
        phase = turn >= turns() ? Phase.END : Phase.READY;
    }
    /** «إنهاء الجولة» on the pause card */
    public void finish() { phase = Phase.END; word = ""; paused(); pausedAt = -1; }

    public void pause() { if (!paused() && (phase == Phase.PLAY || phase == Phase.COUNT)) { pausedAt = clock.now(); leftAtPause = Math.max(0, deadline - pausedAt); } }
    public void resume() {
        if (!paused()) return;
        long gap = clock.now() - pausedAt; pausedAt = -1;
        if (phase == Phase.PLAY) deadline = clock.now() + leftAtPause; else countAt += gap;
    }

    /** the teams with the most points (all of them when nobody scored): the winners */
    public java.util.List<Integer> leaders() {
        int best = 0; for (int s : score) best = Math.max(best, s);
        java.util.List<Integer> v = new java.util.ArrayList<>();
        for (int i = 0; i < teams; i++) if (score[i] == best) v.add(i);
        return v;
    }
}
