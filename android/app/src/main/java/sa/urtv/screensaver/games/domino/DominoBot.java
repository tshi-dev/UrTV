package sa.urtv.screensaver.games.domino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * The computer players of «دومينو», three levels. Each decides from a DominoGame.View only: its own hand, the table, the counts and what
 * everybody saw (who drew or passed on which ends). It never sees another hand or the boneyard. The same view and the same Random give the same move.
 *   EASY    any tile that fits, by chance.
 *   MEDIUM  doubles first, then the heaviest tile.
 *   HARD    doubles early and heavy tiles out, but keeps numbers to answer both ends with, and closes the ends on the numbers a
 *           player was seen to lack: more so when it is ahead.
 */
public final class DominoBot {
    public static final int EASY = 1, MEDIUM = 2, HARD = 3;

    public static final class Action {
        public final int kind; public final DominoGame.Move move;
        Action(int kind, DominoGame.Move move) { this.kind = kind; this.move = move; }
    }

    private DominoBot() {}

    public static Action choose(DominoGame.View v, int level, Random rnd) {
        if (v.legal.isEmpty()) return new Action(v.boneyard > 0 ? DominoGame.DRAW : DominoGame.PASS, null);
        List<DominoGame.Move> legal = new ArrayList<>(v.legal);
        Collections.sort(legal, new Comparator<DominoGame.Move>() {   // a fixed order, so the hand's order never changes the choice
            @Override public int compare(DominoGame.Move a, DominoGame.Move b) { return a.hashCode() - b.hashCode(); }
        });
        if (level <= EASY) return new Action(DominoGame.PLAY, legal.get(rnd.nextInt(legal.size())));
        if (v.hand.size() == 1) return new Action(DominoGame.PLAY, legal.get(0));
        DominoGame.Move best = null; int top = Integer.MIN_VALUE;
        boolean[][] lacks = level >= HARD ? lacks(v) : null;
        for (DominoGame.Move m : legal) {
            int s = level >= HARD ? hard(v, m, lacks) : m.tile.pips() + (m.tile.isDouble() ? 20 : 0);
            if (s > top) { top = s; best = m; }
        }
        return new Action(DominoGame.PLAY, best);
    }

    /**
     * lacks[p][n]: player p surely holds no tile with n. He drew or passed while n was an open end. A drawn tile is unknown: if he played right after
     * drawing, it was that tile and the marks stand; if he drew again or passed, he keeps it, so only the two ends he just failed on stay sure.
     */
    static boolean[][] lacks(DominoGame.View v) {
        boolean[][] x = new boolean[v.players][7];
        boolean[] unknown = new boolean[v.players];
        for (DominoGame.Event e : v.events) {
            if (e.kind == DominoGame.PLAY) { unknown[e.player] = false; continue; }
            if (unknown[e.player]) x[e.player] = new boolean[7];
            x[e.player][e.left] = true; x[e.player][e.right] = true;
            unknown[e.player] = e.kind == DominoGame.DRAW;
        }
        return x;
    }

    private static int hard(DominoGame.View v, DominoGame.Move m, boolean[][] lacks) {
        Tile t = m.tile;
        int l = v.left, r = v.right;
        if (l < 0) { l = t.low; r = t.high; } else if (m.end == DominoGame.LEFT) l = t.other(l); else r = t.other(r);
        int s = t.pips() + (t.isDouble() ? 8 : 0);                       // heavy tiles and doubles out early
        boolean[] kept = new boolean[7]; int answers = 0;
        for (Tile h : v.hand) {
            if (h.equals(t)) continue;
            kept[h.low] = true; kept[h.high] = true;
            if (h.has(l) || h.has(r)) answers++;
        }
        for (boolean k : kept) if (k) s += 1;                            // variety
        s += 3 * Math.min(answers, 3);                                   // still something to play next turn
        if (answers == 0) s -= 12;
        int[] seen = new int[7];                                         // how many tiles of each number are on the table or in my hand
        for (Tile x : v.table) { seen[x.low]++; if (!x.isDouble()) seen[x.high]++; }
        for (Tile x : v.hand) { seen[x.low]++; if (!x.isDouble()) seen[x.high]++; }
        int fewest = Integer.MAX_VALUE, lead = Integer.MAX_VALUE;
        for (int p = 0; p < v.players; p++) if (p != v.me) { fewest = Math.min(fewest, v.counts[p]); lead = Math.min(lead, v.scores[v.me] - v.scores[p]); }
        boolean ahead = v.hand.size() - 1 <= fewest || lead > 0;
        int w = ahead ? 6 : 3;
        for (int k = 1; k < v.players; k++) {                            // the next player counts most
            int p = (v.me + k) % v.players;
            int both = (lacks[p][l] ? 1 : 0) + (lacks[p][r] ? 1 : 0);
            s += (both == 2 ? w * 2 : both * w / 2) / k;
        }
        if (answers > 0) s += (ahead ? 2 : 1) * (seen[l] + seen[r]) / 2; // ends on numbers that are mostly out: the others hold few of them
        return s;
    }
}
