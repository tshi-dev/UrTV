package sa.urtv.screensaver.games.night;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * «ليل القرية»: the rules (no Android, tests/NewGamesTest.java). Secret roles: villagers, wolves (1 to 3), a guard and a seer. Night: the wolves choose someone to send
 * away, the guard protects one player, the seer asks whether one player is a wolf. Day: the village learns who was sent away (nobody if the guard guessed it), talks,
 * then votes on who leaves; the most votes leaves (a tie, or no vote, nobody). The village wins when no wolf is left; the wolves win when they are as many as
 * the others. A player who leaves does not play on but their role is told. docs/GAMES-NEW.md.
 */
public final class NightGame {
    public static final int MIN_PLAYERS = 6, MAX_PLAYERS = 10;
    public static final int VILLAGER = 0, WOLF = 1, GUARD = 2, SEER = 3;
    public static final int VILLAGE_WINS = 1, WOLVES_WIN = 2;

    public enum Phase { NIGHT, DAY, VOTE, VOTED, END }
    public final int players; public final int[] role; public final boolean[] alive;
    public Phase phase = Phase.NIGHT; public int night = 1, winner, outLast = -1; public boolean savedLast; public int voteOut = -1;
    private final int[] wolfPick, vote; private int guardPick = -1, seerPick = -1, lastGuarded = -1; private final Random rnd;

    public static int wolvesFor(int n) { return n >= 9 ? 3 : n >= 7 ? 2 : 1; }

    public NightGame(int players, Random rnd) {
        if (players < MIN_PLAYERS || players > MAX_PLAYERS) throw new IllegalArgumentException("players");
        this.players = players; this.rnd = rnd; role = new int[players]; alive = new boolean[players]; wolfPick = new int[players]; vote = new int[players];
        List<Integer> order = new ArrayList<>(); for (int i = 0; i < players; i++) order.add(i);
        Collections.shuffle(order, rnd);
        int k = 0, w = wolvesFor(players);
        for (int i = 0; i < w; i++) role[order.get(k++)] = WOLF;
        role[order.get(k++)] = GUARD; role[order.get(k++)] = SEER;
        Arrays.fill(alive, true); Arrays.fill(wolfPick, -1); Arrays.fill(vote, -1);
    }
    public int aliveCount() { int n = 0; for (boolean a : alive) if (a) n++; return n; }
    public int wolvesAlive() { int n = 0; for (int i = 0; i < players; i++) if (alive[i] && role[i] == WOLF) n++; return n; }
    public boolean needsPick(int p) { return phase == Phase.NIGHT && alive[p] && (role[p] == WOLF || role[p] == GUARD || role[p] == SEER); }

    // ---------------------------------------------------------------- the night

    /** a wolf's choice: a player who is alive and not a wolf */
    public boolean wolf(int p, int target) { if (phase != Phase.NIGHT || !ok(p, WOLF) || !alive(target) || role[target] == WOLF) return false; wolfPick[p] = target; return true; }
    /** the guard's choice: anyone alive, but not the same player two nights in a row */
    public boolean guard(int p, int target) { if (phase != Phase.NIGHT || !ok(p, GUARD) || !alive(target) || target == lastGuarded) return false; guardPick = target; return true; }
    /** the seer asks: anyone alive but themselves; the answer is returned (true = a wolf), or null when it is refused */
    public Boolean seer(int p, int target) { if (phase != Phase.NIGHT || !ok(p, SEER) || !alive(target) || target == p) return null; seerPick = target; return role[target] == WOLF; }
    private boolean ok(int p, int r) { return p >= 0 && p < players && alive[p] && role[p] == r; }
    private boolean alive(int t) { return t >= 0 && t < players && alive[t]; }
    public boolean nightDone() {
        for (int i = 0; i < players; i++) if (alive[i] && role[i] == WOLF && wolfPick[i] < 0) return false;
        return (guardAlive() ? guardPick >= 0 : true) && (seerAlive() ? seerPick >= 0 : true);
    }
    private boolean guardAlive() { for (int i = 0; i < players; i++) if (alive[i] && role[i] == GUARD) return true; return false; }
    private boolean seerAlive() { for (int i = 0; i < players; i++) if (alive[i] && role[i] == SEER) return true; return false; }

    /** the night ends: the most chosen (a tie by chance) is sent away unless the guard chose the same player */
    public void endNight() {
        if (phase != Phase.NIGHT) return;
        int[] c = new int[players]; int best = 0;
        for (int i = 0; i < players; i++) if (wolfPick[i] >= 0 && alive[i]) { c[wolfPick[i]]++; best = Math.max(best, c[wolfPick[i]]); }
        List<Integer> top = new ArrayList<>(); if (best > 0) for (int i = 0; i < players; i++) if (c[i] == best) top.add(i);
        int victim = top.isEmpty() ? -1 : top.get(rnd.nextInt(top.size()));
        savedLast = victim >= 0 && victim == guardPick; outLast = victim >= 0 && !savedLast ? victim : -1;
        if (outLast >= 0) alive[outLast] = false;
        lastGuarded = guardPick; guardPick = -1; seerPick = -1; Arrays.fill(wolfPick, -1);
        phase = check() ? Phase.END : Phase.DAY;
    }

    // ---------------------------------------------------------------- the day

    public void startVote() { if (phase == Phase.DAY) { phase = Phase.VOTE; Arrays.fill(vote, -1); } }
    /** a vote: an alive player names another alive player (never themselves), once */
    public boolean vote(int p, int target) { if (phase != Phase.VOTE || p < 0 || p >= players || !alive[p] || vote[p] >= 0 || !alive(target) || target == p) return false; vote[p] = target; return true; }
    public int votes() { int n = 0; for (int i = 0; i < players; i++) if (alive[i] && vote[i] >= 0) n++; return n; }
    public boolean allVoted() { return votes() == aliveCount(); }
    public int voteOf(int p) { return vote[p]; }

    /** the votes are counted: a clear most-voted player leaves; a tie or no vote leaves nobody */
    public void endVote() {
        if (phase != Phase.VOTE) return;
        int[] c = new int[players]; int best = 0, who = -1; boolean tie = false;
        for (int i = 0; i < players; i++) if (alive[i] && vote[i] >= 0) c[vote[i]]++;
        for (int i = 0; i < players; i++) { if (c[i] > best) { best = c[i]; who = i; tie = false; } else if (c[i] == best && best > 0) tie = true; }
        voteOut = best > 0 && !tie ? who : -1;
        if (voteOut >= 0) alive[voteOut] = false;
        phase = check() ? Phase.END : Phase.VOTED;
    }
    public void nextNight() { if (phase == Phase.VOTED) { night++; phase = Phase.NIGHT; Arrays.fill(wolfPick, -1); guardPick = -1; seerPick = -1; } }
    public void finish() { phase = Phase.END; }

    private boolean check() {
        int w = wolvesAlive(), others = aliveCount() - w;
        if (w == 0) { winner = VILLAGE_WINS; return true; }
        if (w >= others) { winner = WOLVES_WIN; return true; }
        return false;
    }
}
