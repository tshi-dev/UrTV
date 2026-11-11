package sa.urtv.screensaver.games.domino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * «دومينو»: the draw game with the double-six set, 2 to 4 players each for himself, rounds to a target score (docs/DOMINO.md has every rule
 * and every choice). Plain Java, no Android, no clock: the only chance is the Random given in, so a seed gives the same game again.
 * Nothing illegal is ever accepted: play, draw and pass answer false (or null) and change nothing.
 */
public final class DominoGame {
    public enum Phase { PLAY, ROUND_OVER, GAME_OVER }
    public static final int LEFT = 0, RIGHT = 1;
    public static final int PLAY = 0, DRAW = 1, PASS = 2;
    public static final int[] TARGETS = {50, 100, 150};

    /** A tile and the end of the chain it goes to. */
    public static final class Move {
        public final Tile tile; public final int end;
        public Move(Tile tile, int end) { this.tile = tile; this.end = end; }
        @Override public boolean equals(Object o) { return o instanceof Move && ((Move) o).tile.equals(tile) && ((Move) o).end == end; }
        @Override public int hashCode() { return tile.id() * 2 + end; }
        @Override public String toString() { return tile + (end == LEFT ? "<" : ">"); }
    }

    /** A tile on the table as it lies: «first» faces the left end of the chain, «second» the right end. */
    public static final class Placed {
        public final Tile tile; public final int first, second;
        Placed(Tile tile, int first, int second) { this.tile = tile; this.first = first; this.second = second; }
    }

    /** What everybody at the table saw: who, what (PLAY, DRAW, PASS), the tile and end of a play (a drawn tile is never here), the open ends before it (-1: empty table). */
    public static final class Event {
        public final int player, kind, end, left, right; public final Tile tile;
        Event(int player, int kind, Tile tile, int end, int left, int right) { this.player = player; this.kind = kind; this.tile = tile; this.end = end; this.left = left; this.right = right; }
    }

    /** How a round ended: the winner (-1: a blocked round with the lowest count shared, nobody scores), blocked or not, the points, each hand's pips. */
    public static final class RoundResult {
        public final int winner, points; public final boolean blocked; public final int[] pips;
        RoundResult(int winner, boolean blocked, int points, int[] pips) { this.winner = winner; this.blocked = blocked; this.points = points; this.pips = pips; }
    }

    /** All a player may know: his own hand and the table. The computer players get this and nothing else. */
    public static final class View {
        public final int me, players, left, right, boneyard, target;
        public final List<Tile> hand; public final List<Tile> table; public final List<Move> legal; public final List<Event> events;
        public final int[] counts, scores;
        View(int me, int players, int left, int right, int boneyard, int target, List<Tile> hand, List<Tile> table, List<Move> legal, List<Event> events, int[] counts, int[] scores) {
            this.me = me; this.players = players; this.left = left; this.right = right; this.boneyard = boneyard; this.target = target;
            this.hand = hand; this.table = table; this.legal = legal; this.events = events; this.counts = counts; this.scores = scores;
        }
    }

    public final int players, target;
    private final Random rnd;
    private final List<List<Tile>> hands = new ArrayList<>();
    private final List<Tile> boneyard = new ArrayList<>();
    private final List<Placed> chain = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private final int[] scores;
    private Phase phase = Phase.ROUND_OVER;
    private int turn, round, starter, gameWinner = -1;
    private Tile opening;          // the tile the first move of this round must be (null: any)
    private Tile aside;            // three players: the double blank, out of the round
    private RoundResult last;

    public DominoGame(int players, int target, Random rnd) {
        if (players < 2 || players > 4) throw new IllegalArgumentException("2 to 4 players");
        if (target < 1) throw new IllegalArgumentException("target");
        this.players = players; this.target = target; this.rnd = rnd; scores = new int[players];
        for (int i = 0; i < players; i++) hands.add(new ArrayList<Tile>());
        deal();
    }

    /** Tiles each player is dealt: 7, or 9 with three players (the double blank set aside, no boneyard). */
    public static int handSize(int players) { return players == 3 ? 9 : 7; }

    /** Fisher-Yates with the given Random: the same seed, the same order, on any JVM. */
    public static void shuffle(List<Tile> v, Random rnd) {
        for (int i = v.size() - 1; i > 0; i--) Collections.swap(v, i, rnd.nextInt(i + 1));
    }

    /** The next round, after ROUND_OVER. */
    public boolean nextRound() {
        if (phase != Phase.ROUND_OVER) return false;
        deal(); return true;
    }

    private void deal() {
        List<Tile> all = Tile.set();
        aside = null;
        if (players == 3) { aside = all.remove(0); }
        shuffle(all, rnd);
        chain.clear(); events.clear(); boneyard.clear();
        int n = handSize(players), k = 0;
        for (List<Tile> h : hands) { h.clear(); for (int i = 0; i < n; i++) h.add(all.get(k++)); }
        while (k < all.size()) boneyard.add(all.get(k++));
        round++;
        if (last != null && last.winner >= 0) { starter = last.winner; opening = null; }   // the winner opens the next round with any tile
        else {                                                                              // first round, or after a drawn one: the highest double opens
            Tile best = null; int who = 0;
            for (int p = 0; p < players; p++) for (Tile t : hands.get(p)) if (opens(t, best)) { best = t; who = p; }
            starter = who; opening = best;
        }
        turn = starter; phase = Phase.PLAY;
    }

    /** Opening order: any double beats any other tile, a higher double a lower one; with no double dealt, the heaviest tile (then the higher half). */
    private static boolean opens(Tile t, Tile best) {
        if (best == null) return true;
        if (t.isDouble() != best.isDouble()) return t.isDouble();
        if (t.pips() != best.pips()) return t.pips() > best.pips();
        return t.high > best.high;
    }

    // ---- what is on the table
    public Phase phase() { return phase; }
    public int turn() { return turn; }
    public int round() { return round; }
    public int starter() { return starter; }
    public Tile opening() { return chain.isEmpty() ? opening : null; }
    public Tile aside() { return aside; }
    public int left() { return chain.isEmpty() ? -1 : chain.get(0).first; }
    public int right() { return chain.isEmpty() ? -1 : chain.get(chain.size() - 1).second; }
    public List<Placed> chain() { return Collections.unmodifiableList(chain); }
    public List<Event> events() { return Collections.unmodifiableList(events); }
    public int boneyard() { return boneyard.size(); }
    public int count(int p) { return hands.get(p).size(); }
    public int score(int p) { return scores[p]; }
    public RoundResult lastRound() { return last; }
    public int winner() { return gameWinner; }
    /** A player's own hand (read only): for his own eyes, behind the hand-over screen. */
    public List<Tile> hand(int p) { return Collections.unmodifiableList(hands.get(p)); }
    public static int pips(List<Tile> v) { int s = 0; for (Tile t : v) s += t.pips(); return s; }

    // ---- the moves
    public List<Move> legal(int p) { return legal(hands.get(p)); }

    private List<Move> legal(List<Tile> hand) {
        List<Move> v = new ArrayList<>();
        if (phase != Phase.PLAY) return v;
        if (chain.isEmpty()) {
            for (Tile t : hand) if (opening == null || t.equals(opening)) v.add(new Move(t, RIGHT));
            return v;
        }
        int l = left(), r = right();
        for (Tile t : hand) {
            if (t.has(l)) v.add(new Move(t, LEFT));
            if (t.has(r)) v.add(new Move(t, RIGHT));
        }
        return v;
    }

    public boolean canPlay(int p) { return !legal(p).isEmpty(); }
    /** No tile fits and the boneyard has tiles: the player must draw. */
    public boolean mustDraw(int p) { return phase == Phase.PLAY && p == turn && !canPlay(p) && !boneyard.isEmpty(); }
    /** No tile fits and nothing to draw: the player must pass. */
    public boolean mustPass(int p) { return phase == Phase.PLAY && p == turn && !canPlay(p) && boneyard.isEmpty(); }

    public boolean play(int p, Tile t, int end) {
        if (phase != Phase.PLAY || p != turn || t == null || (end != LEFT && end != RIGHT)) return false;
        List<Tile> hand = hands.get(p);
        if (!hand.contains(t) || !legal(hand).contains(new Move(t, end))) return false;
        int l = left(), r = right();
        hand.remove(t);
        if (chain.isEmpty()) chain.add(new Placed(t, t.low, t.high));
        else if (end == LEFT) chain.add(0, new Placed(t, t.other(l), l));
        else chain.add(new Placed(t, r, t.other(r)));
        events.add(new Event(p, PLAY, t, end, l, r));
        if (hand.isEmpty()) { finish(p, false); return true; }
        turn = (turn + 1) % players;
        if (stuck()) finishBlocked();
        return true;
    }

    /** One tile from the boneyard into the hand of the player whose turn it is and who has nothing to play; null when that is not allowed. The turn stays his. */
    public Tile draw(int p) {
        if (!mustDraw(p)) return null;
        Tile t = boneyard.remove(boneyard.size() - 1);
        hands.get(p).add(t);
        events.add(new Event(p, DRAW, null, -1, left(), right()));
        if (stuck()) finishBlocked();
        return t;
    }

    public boolean pass(int p) {
        if (!mustPass(p)) return false;
        events.add(new Event(p, PASS, null, -1, left(), right()));
        turn = (turn + 1) % players;
        return true;
    }

    /** Blocked: nothing to draw and no hand holds a tile that fits. */
    private boolean stuck() {
        if (!boneyard.isEmpty()) return false;
        for (List<Tile> h : hands) if (!legal(h).isEmpty()) return false;
        return true;
    }

    private void finishBlocked() {
        int best = Integer.MAX_VALUE, who = -1; boolean shared = false;
        for (int p = 0; p < players; p++) {
            int s = pips(hands.get(p));
            if (s < best) { best = s; who = p; shared = false; } else if (s == best) shared = true;
        }
        finish(shared ? -1 : who, true);
    }

    private void finish(int w, boolean blocked) {
        int[] pp = new int[players]; int points = 0;
        for (int p = 0; p < players; p++) { pp[p] = pips(hands.get(p)); if (p != w) points += pp[p]; }
        if (w < 0) points = 0;
        last = new RoundResult(w, blocked, points, pp);
        if (w >= 0) scores[w] += points;
        if (w >= 0 && scores[w] >= target) { phase = Phase.GAME_OVER; gameWinner = w; } else phase = Phase.ROUND_OVER;
    }

    /** What player p may know, as copies: his hand, the table, the counts. */
    public View view(int p) {
        List<Tile> table = new ArrayList<>();
        for (Placed x : chain) table.add(x.tile);
        int[] counts = new int[players];
        for (int i = 0; i < players; i++) counts[i] = hands.get(i).size();
        return new View(p, players, left(), right(), boneyard.size(), target, Collections.unmodifiableList(new ArrayList<>(hands.get(p))), Collections.unmodifiableList(table),
            Collections.unmodifiableList(p == turn ? legal(p) : new ArrayList<Move>()), Collections.unmodifiableList(new ArrayList<>(events)), counts, scores.clone());
    }

    // for the tests (same package): the hidden tiles, to count them and to shuffle what a player cannot see
    List<Tile> rawHand(int p) { return hands.get(p); }
    List<Tile> rawBoneyard() { return boneyard; }
}
