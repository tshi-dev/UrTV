package sa.urtv.screensaver.games.closest;

import java.util.ArrayList;
import java.util.List;

/**
 * «أقرب رقم» on paper (docs/GAMES-REMOTE.md), no Android: everybody writes a number on paper, the TV shows the right number, and the host picks the sides whose paper was closest:
 * the first place scores POINTS[0] (EXACT when the number was exact), the second POINTS[1], the third POINTS[2]; a side can hold one place; «ما أحد» stops the picking.
 * The items are ClosestGame.Item (the bank's own questions and numbers, unchanged).
 */
public final class PaperPlaces {
    /** the places and their points (one place to change them) */
    public static final int PLACES = 3; public static final int[] POINTS = {3, 2, 1}; public static final int EXACT = 5;
    public static final int MIN_SIDES = 2, MAX_SIDES = 8;

    public enum Phase { ASK, REVEAL, PLACE, END }
    public final int sides, total; public final List<ClosestGame.Item> items; public final int[] score, last;
    public Phase phase; public int index, place; public boolean exact;
    private final boolean[] used;

    public PaperPlaces(int sides, List<ClosestGame.Item> items) {
        if (sides < MIN_SIDES || sides > MAX_SIDES) throw new IllegalArgumentException("sides");
        this.sides = sides; this.items = items; total = items.size(); score = new int[sides]; last = new int[sides]; used = new boolean[sides];
        phase = total == 0 ? Phase.END : Phase.ASK;
    }
    public ClosestGame.Item item() { return phase == Phase.END ? null : items.get(index); }
    public void reveal() { if (phase == Phase.ASK) phase = Phase.REVEAL; }
    /** OK on the number: the places are picked */
    public void toPlace() { if (phase == Phase.REVEAL) { phase = Phase.PLACE; place = 0; exact = false; java.util.Arrays.fill(used, false); java.util.Arrays.fill(last, 0); } }
    /** the host's switch for the first place: the number was exact (5 instead of 3) */
    public void setExact(boolean v) { if (phase == Phase.PLACE && place == 0) exact = v; }
    public boolean taken(int side) { return side >= 0 && side < sides && used[side]; }
    public int pointsOfPlace() { return place == 0 && exact ? EXACT : POINTS[place]; }
    /** the side with the next place, or -1: nobody (the picking of this question stops). After the last place, or «ما أحد», the next question comes. */
    public boolean pick(int side) {
        if (phase != Phase.PLACE || side < -1 || side >= sides || (side >= 0 && used[side])) return false;
        if (side >= 0) { used[side] = true; int p = pointsOfPlace(); score[side] += p; last[side] = p; place++; }
        if (side < 0 || place >= PLACES || place >= sides) next();
        return true;
    }
    private void next() { index++; phase = index >= total ? Phase.END : Phase.ASK; }
    public void finish() { phase = Phase.END; }
    /** the names of the sides that took a place, in order, for the line under the number */
    public List<Integer> placed() { List<Integer> v = new ArrayList<>(); for (int s = 0; s < sides; s++) if (used[s]) v.add(s); return v; }
}
