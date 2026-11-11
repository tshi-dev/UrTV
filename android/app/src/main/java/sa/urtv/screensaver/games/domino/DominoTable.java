package sa.urtv.screensaver.games.domino;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * One remote at the table of «دومينو»: whose turn it is on the screen, what is in focus and when a hand may be seen. Plain Java (no Android):
 * the screen sends the keys (move, ok, back) and draws what the answer says changed, nothing more.
 *   HANDOVER   two people or more: «دور <name>», no hand on the screen; OK shows his hand.
 *   TURN       his hand: focus only on the tiles that fit; with none, on the one button (draw while there are tiles to draw, then pass).
 *   CHOOSE_END the tile fits both ends, which differ: left or right, OK lays it, Back returns to the hand.
 *   BOT        a computer player is to move: botStep() makes one move (the screen calls it after its short animation), OK skips to the next person.
 *   ROUND_END, GAME_END   the scores; OK deals the next round.
 * With one person at the table his hand stays on the screen through everything. With more, a hand shows only in its owner's TURN or CHOOSE_END.
 */
public final class DominoTable {
    public enum State { HANDOVER, TURN, CHOOSE_END, BOT, ROUND_END, GAME_END }
    /** What a key changed, to draw only that: nothing, the focus, the hand strip, the chain, the whole state, the scores. */
    public static final int NOTHING = 0, FOCUS = 1, HAND = 2, CHAIN = 4, STATE = 8, SCORE = 16;
    /** The focus is on the draw / pass button, not on a tile. */
    public static final int BUTTON = -1;

    public final DominoGame game;
    private final int[] levels;      // per seat: 0 a person, 1 to 3 a computer player's level
    private final Random botRnd;
    private final int people;
    private State state;
    private int focus = BUTTON;      // index in the hand of the player whose turn it is
    private int end = DominoGame.RIGHT;
    private Tile drawn;              // the tile the person just drew, to show it arriving

    public DominoTable(int[] levels, int target, Random deal, Random bots) {
        this.levels = levels.clone(); botRnd = bots;
        int n = 0; for (int l : levels) { if (l < 0 || l > 3) throw new IllegalArgumentException("level"); if (l == 0) n++; }
        people = n;
        game = new DominoGame(levels.length, target, deal);
        enter();
    }

    public State state() { return state; }
    public int people() { return people; }
    public boolean person(int p) { return levels[p] == 0; }
    public int level(int p) { return levels[p]; }
    public int focus() { return focus; }
    public int end() { return end; }
    public Tile drawn() { return drawn; }
    /** Whose hand is on the screen now: -1 none. */
    public int shown() {
        if (people == 0) return -1;
        if (people == 1) { for (int p = 0; p < levels.length; p++) if (levels[p] == 0) return p; }
        return state == State.TURN || state == State.CHOOSE_END ? game.turn() : -1;
    }
    /** May the tile at this place in the shown hand take the focus (it fits an end, and it is its owner's turn)? */
    public boolean fits(int index) {
        if (state != State.TURN && state != State.CHOOSE_END) return false;
        List<Tile> h = game.hand(game.turn());
        if (index < 0 || index >= h.size()) return false;
        for (DominoGame.Move m : game.legal(game.turn())) if (m.tile.equals(h.get(index))) return true;
        return false;
    }
    /** The button under the hand in TURN: DRAW, PASS, or -1 when a tile fits (no button). */
    public int button() {
        if (state != State.TURN) return -1;
        int p = game.turn();
        return game.mustDraw(p) ? DominoGame.DRAW : game.mustPass(p) ? DominoGame.PASS : -1;
    }

    /** Left (-1) or right (+1) in the hand's order: to the next tile that fits; in CHOOSE_END the end. */
    public int move(int d) {
        if (d == 0) return NOTHING;
        if (state == State.CHOOSE_END) { int e = d < 0 ? DominoGame.LEFT : DominoGame.RIGHT; if (e == end) return NOTHING; end = e; return FOCUS; }
        if (state != State.TURN || focus == BUTTON) return NOTHING;
        int n = game.count(game.turn());
        for (int i = focus + (d < 0 ? -1 : 1); i >= 0 && i < n; i += d < 0 ? -1 : 1) if (fits(i)) { focus = i; return FOCUS; }
        return NOTHING;
    }

    public int ok() {
        int p = game.turn();
        switch (state) {
            case HANDOVER: state = State.TURN; aim(); return STATE | HAND;
            case TURN: {
                if (focus == BUTTON) {
                    if (game.mustDraw(p)) {
                        drawn = game.draw(p);
                        if (game.phase() != DominoGame.Phase.PLAY) { enter(); return STATE | HAND | SCORE; }
                        aim(); return HAND;
                    }
                    if (game.pass(p)) return after(HAND);
                    return NOTHING;
                }
                Tile t = game.hand(p).get(focus);
                boolean l = false, r = false;
                for (DominoGame.Move m : game.legal(p)) if (m.tile.equals(t)) { if (m.end == DominoGame.LEFT) l = true; else r = true; }
                int shorter = game.anchor() <= game.chain().size() - 1 - game.anchor() ? DominoGame.LEFT : DominoGame.RIGHT;
                if (l && r && game.left() != game.right()) { end = shorter; state = State.CHOOSE_END; return STATE; }
                return game.play(p, t, l && r ? shorter : l ? DominoGame.LEFT : DominoGame.RIGHT) ? after(HAND | CHAIN) : NOTHING;
            }
            case CHOOSE_END: return game.play(p, game.hand(p).get(focus), end) ? after(HAND | CHAIN) : NOTHING;
            case BOT: { int c = NOTHING; while (state == State.BOT) c |= botStep(); return c; }
            case ROUND_END: game.nextRound(); enter(); return STATE | HAND | CHAIN | SCORE;
            default: return NOTHING;
        }
    }

    /** Back: true when the table used it (leaving the choice of the end); false: the screen opens its pause card. */
    public boolean back() {
        if (state != State.CHOOSE_END) return false;
        state = State.TURN; return true;
    }

    /** The app left the screen, or the pause card opened: with two people or more a hand that was showing is hidden behind the hand-over again. */
    public int hide() {
        if (people < 2 || (state != State.TURN && state != State.CHOOSE_END)) return NOTHING;
        state = State.HANDOVER; return STATE | HAND;
    }

    /** One move of the computer player whose turn it is (a play, one drawn tile, or a pass). */
    public int botStep() {
        if (state != State.BOT) return NOTHING;
        int p = game.turn();
        DominoBot.Action a = DominoBot.choose(game.view(p), levels[p], botRnd);
        if (a.kind == DominoGame.PLAY) { game.play(p, a.move.tile, a.move.end); return after(CHAIN); }
        if (a.kind == DominoGame.DRAW) { game.draw(p); if (game.phase() != DominoGame.Phase.PLAY) { enter(); return STATE | SCORE; } return HAND; }
        game.pass(p); return after(NOTHING);
    }

    private int after(int changed) {
        State was = state;
        enter();
        return changed | STATE | (state == State.ROUND_END || state == State.GAME_END ? SCORE : 0) | (was != state || people > 1 ? HAND : 0);
    }

    /** The state for whoever is to move now. */
    private void enter() {
        drawn = null;
        if (game.phase() == DominoGame.Phase.GAME_OVER) { state = State.GAME_END; return; }
        if (game.phase() == DominoGame.Phase.ROUND_OVER) { state = State.ROUND_END; return; }
        if (levels[game.turn()] > 0) { state = State.BOT; return; }
        state = people > 1 ? State.HANDOVER : State.TURN;
        aim();
    }

    /** Focus on the first tile that fits (the drawn one when it does), else on the button. */
    private void aim() {
        int p = game.turn(); List<Tile> h = game.hand(p);
        focus = BUTTON;
        List<Integer> can = new ArrayList<>();
        for (int i = 0; i < h.size(); i++) for (DominoGame.Move m : game.legal(p)) if (m.tile.equals(h.get(i))) { can.add(i); break; }
        if (can.isEmpty()) return;
        focus = drawn != null && can.contains(h.size() - 1) ? h.size() - 1 : can.get(0);
    }
}
