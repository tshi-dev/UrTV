package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * «خمس خانات»: the rules of one game (no Android, tests/GamesKitTest.java; games/khanat/SPEC.md, the remote-only flow). A round: a letter that was not
 * used in this game, the writing time (anyone may stop it early: «خلّصنا»), then the boxes one at a time: for each box the host marks every team's answer
 * as none, valid, or valid but the same as another team's. A valid answer nobody else gave is 10, one another team gave too is 5; the team that stopped the
 * round gets 5 more, once, when all its boxes were valid. After the last round a tie for first plays ONE more round with a single box drawn at random
 * (no bonus in it); a tie after that stands. The phone mode will drive the same engine later.
 */
final class KhanatEngine {
    static final int UNIQUE = 10, SHARED = 5, FIRST_BONUS = 5;
    /** a team's answer in a box, as the group judged it */
    static final int NONE = 0, VALID = 1, SAME = 2;

    enum Phase { LETTER, WRITING, WHO, REVEAL, SCORE, END }

    final GameScore score; final int rounds; private final String[] allBoxes; private final List<String> pool; private final Random rnd;
    Phase phase = Phase.LETTER; int round = -1, box, finisher = -1; String letter = ""; String[] boxes; int[][] marks; boolean tiebreak, bonusGiven;

    /** `letters`: the letters this game may draw (each at most once); `boxes`: the keys of the boxes of every round */
    KhanatEngine(int teams, int rounds, String[] boxes, List<String> letters, Random rnd) {
        this.score = new GameScore(Math.max(2, teams)); this.rounds = Math.max(1, rounds); this.allBoxes = boxes.clone(); this.rnd = rnd;
        this.pool = new ArrayList<>(letters); Collections.shuffle(this.pool, rnd);
        startRound();
    }

    private void startRound() {
        round++;
        boxes = tiebreak ? new String[]{allBoxes[rnd.nextInt(allBoxes.length)]} : allBoxes;
        letter = pool.isEmpty() ? letter : pool.remove(0);   // never the same letter twice in a game (the pool is larger than any game)
        marks = new int[boxes.length][score.n];
        box = 0; finisher = -1; bonusGiven = false;
        score.newRound();
        phase = Phase.LETTER;
    }

    /** the letter has been shown: the writing time starts */
    void write() { if (phase == Phase.LETTER) phase = Phase.WRITING; }

    /** the writing time is over: by the clock (nobody finished first), or by OK («خلّصنا»: the next screen asks which team) */
    void stop(boolean early) { if (phase == Phase.WRITING) phase = early ? Phase.WHO : Phase.REVEAL; }

    /** which team said «خلّصنا» (-1: nobody, a slip of the remote) */
    void finishedFirst(int team) { if (phase != Phase.WHO) return; finisher = team >= 0 && team < score.n ? team : -1; phase = Phase.REVEAL; }

    /** OK on a team's chip: none, valid, the same as another team's, none again */
    void cycle(int team) { if (phase == Phase.REVEAL && team >= 0 && team < score.n) marks[box][team] = (marks[box][team] + 1) % 3; }
    void set(int team, int state) { if (phase == Phase.REVEAL && team >= 0 && team < score.n && state >= NONE && state <= SAME) marks[box][team] = state; }

    static int points(int state) { return state == VALID ? UNIQUE : state == SAME ? SHARED : 0; }

    /** the next box; after the last one the round is scored and the totals are shown */
    void nextBox() {
        if (phase != Phase.REVEAL) return;
        if (box + 1 < boxes.length) { box++; return; }
        for (int b = 0; b < boxes.length; b++) for (int t = 0; t < score.n; t++) score.add(t, points(marks[b][t]));
        if (!tiebreak && finisher >= 0) {   // a team that stopped early with a wrong box keeps the points of its valid boxes, without the bonus
            boolean all = true;
            for (int b = 0; b < boxes.length; b++) if (marks[b][finisher] == NONE) all = false;
            if (all) { score.add(finisher, FIRST_BONUS); bonusGiven = true; }
        }
        phase = Phase.SCORE;
    }

    /** from the totals: the next round, the tie-break, or the end */
    void next() {
        if (phase != Phase.SCORE) return;
        if (!tiebreak && round + 1 < rounds) { startRound(); return; }
        if (!tiebreak && score.tie()) { tiebreak = true; startRound(); return; }
        phase = Phase.END;
    }

    /** «إنهاء الجولة» on the pause card: the game ends with the totals as they are */
    void finish() { phase = Phase.END; }

    boolean lastRound() { return tiebreak || round + 1 >= rounds; }
}
