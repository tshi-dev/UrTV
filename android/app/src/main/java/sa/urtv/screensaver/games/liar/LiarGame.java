package sa.urtv.screensaver.games.liar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.author.AuthorGame;

/**
 * «واحد يكذب»: the rules (no Android, tests/NewGamesTest.java). Each round three players tell a short story on one topic; two stories are true and one is made up.
 * The others question them, then vote on who lied. A voter who finds the liar scores 2; the liar scores 1 for every voter fooled. The three are the players who
 * have told the fewest times (ties by chance), the liar is one of the three by chance. The topics are AuthorGame's prompt file (the same form). docs/GAMES-NEW.md.
 */
public final class LiarGame {
    public static final int MIN_PLAYERS = 4, MAX_PLAYERS = 10, FOUND = 2;
    public static final int[] ROUNDS = {3, 5, 6}, TELL_SECONDS = {60, 90, 120};

    public enum Phase { DEAL, TELL, VOTE, REVEAL, END }
    public final int players, total; public final List<AuthorGame.Prompt> topics; public final int[] score, gained, told;
    public Phase phase = Phase.DEAL; public int index;
    private int[] tellers = new int[3]; private int liar; private final int[] vote; private final Random rnd;

    public LiarGame(int players, List<AuthorGame.Prompt> topics, Random rnd) {
        if (players < MIN_PLAYERS || players > MAX_PLAYERS) throw new IllegalArgumentException("players");
        this.players = players; this.topics = topics; this.total = topics.size(); this.rnd = rnd;
        score = new int[players]; gained = new int[players]; told = new int[players]; vote = new int[players];
        if (total == 0) phase = Phase.END; else choose();
    }
    public AuthorGame.Prompt topic() { return phase == Phase.END ? null : topics.get(index); }
    public int[] tellers() { return tellers.clone(); }
    public boolean isTeller(int p) { for (int t : tellers) if (t == p) return true; return false; }
    /** whether `p` is the liar: for the phone of that player only (the TV never asks before the reveal) */
    public boolean liarIs(int p) { return tellers[liar] == p; }
    public int liar() { return phase == Phase.REVEAL ? tellers[liar] : -1; }

    private void choose() {
        List<Integer> order = new ArrayList<>();
        for (int p = 0; p < players; p++) order.add(p);
        Collections.shuffle(order, rnd);
        order.sort((a, b) -> Integer.compare(told[a], told[b]));   // the fewest turns first; the shuffle breaks the ties
        for (int i = 0; i < 3; i++) { tellers[i] = order.get(i); told[order.get(i)]++; }
        liar = rnd.nextInt(3);
        Arrays.fill(vote, -1); Arrays.fill(gained, 0);
        phase = Phase.DEAL;
    }

    /** the three have seen their roles: the telling starts */
    public void startTelling() { if (phase == Phase.DEAL) phase = Phase.TELL; }
    public void endTelling() { if (phase == Phase.TELL) phase = Phase.VOTE; }

    /** a vote: a player who did not tell says which of the three lied (once each) */
    public boolean vote(int p, int guess) {
        if (phase != Phase.VOTE || p < 0 || p >= players || isTeller(p) || !isTeller(guess) || vote[p] >= 0) return false;
        vote[p] = guess; return true;
    }
    public int votes() { int n = 0; for (int v : vote) if (v >= 0) n++; return n; }
    public boolean allVoted() { return votes() == players - 3; }
    public int voteOf(int p) { return vote[p]; }

    public void reveal() {
        if (phase != Phase.VOTE) return;
        int l = tellers[liar];
        Arrays.fill(gained, 0);
        for (int p = 0; p < players; p++) if (vote[p] >= 0) { if (vote[p] == l) gained[p] += FOUND; else gained[l]++; }
        for (int p = 0; p < players; p++) score[p] += gained[p];
        phase = Phase.REVEAL;
    }
    public void next() {
        if (phase != Phase.REVEAL) return;
        index++;
        if (index >= total) phase = Phase.END; else choose();
    }
    public void finish() { phase = Phase.END; }
    public List<String> asked() { List<String> v = new ArrayList<>(); for (int i = 0; i < Math.min(total, index + 1); i++) v.add(topics.get(i).id); return v; }
}
