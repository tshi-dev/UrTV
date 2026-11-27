package sa.urtv.screensaver.games.remote;

import java.util.ArrayList;
import java.util.List;

/**
 * «نادوا الجواب» (docs/GAMES-REMOTE.md): the remote-only way to play the four phone races (اقرأ الإيموجي، ألغاز المنطق، كمّل الآية، سباق الحساب), no Android.
 * The TV shows the question (and its options, when it has them); everybody calls the answer aloud; OK shows the right answer; the host then says WHO called it
 * first (the same pick as «خلية الحروف»), or nobody. The side picked gets POINTS. The items come from the games' own banks (the screen builds them); nothing is made up here.
 */
public final class CallAloudRound {
    /** the points of the side that called it first: one place to change them */
    public static final int POINTS = 3;
    public static final int MIN_SIDES = 2, MAX_SIDES = 8;

    /** one question: `meta` the small line, `prompt` the big words, `options` (empty for a sum) and the index of the right one, `answer` the text of the right answer, `note` a line under the answer (a surah and verse) or "" */
    public static final class Item {
        public final String meta, prompt, answer, note; public final List<String> options; public final int right;
        public Item(String meta, String prompt, List<String> options, int right, String answer, String note) {
            this.meta = meta; this.prompt = prompt; this.options = options == null ? new ArrayList<String>() : options; this.right = right; this.answer = answer; this.note = note == null ? "" : note;
        }
    }

    public enum Phase { ASK, REVEAL, PICK, END }
    public final int sides, total; public final List<Item> items; public final int[] score;
    public Phase phase; public int index; public int winner = -1;   // winner: the side picked for the last question, -1 for nobody

    public CallAloudRound(int sides, List<Item> items) {
        if (sides < MIN_SIDES || sides > MAX_SIDES) throw new IllegalArgumentException("sides");
        this.sides = sides; this.items = items; total = items.size(); score = new int[sides];
        phase = total == 0 ? Phase.END : Phase.ASK;
    }
    public Item item() { return phase == Phase.END ? null : items.get(index); }

    /** OK on the question: the right answer shows */
    public void reveal() { if (phase == Phase.ASK) phase = Phase.REVEAL; }
    /** OK on the answer: who called it first? */
    public void toPick() { if (phase == Phase.REVEAL) { phase = Phase.PICK; winner = -1; } }
    /** back from the pick to the answer */
    public void backToReveal() { if (phase == Phase.PICK) phase = Phase.REVEAL; }
    /** the side that called it first (0 to sides - 1), or -1 for nobody: the points go to it and the next question comes (or the end) */
    public boolean pick(int side) {
        if (phase != Phase.PICK || side < -1 || side >= sides) return false;
        winner = side;
        if (side >= 0) score[side] += POINTS;
        index++;
        phase = index >= total ? Phase.END : Phase.ASK;
        return true;
    }
    public void finish() { phase = Phase.END; }
    /** the ids of what was asked (for «nothing repeats on this TV») are kept by the screen from its items */
}
