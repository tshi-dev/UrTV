package sa.urtv.screensaver.games.draw;

import java.util.Arrays;

import sa.urtv.screensaver.games.guess.GuessGame;

/**
 * «ارسم وخمّن»: the rules (no Android, tests/NewGamesTest.java). One player draws a word on their phone, the TV shows the strokes, the others type guesses on their
 * phones. The first right guess scores 3, the second 2, the rest 1; the drawer scores 1 for each right guess. Each player draws `turns` times; the words never repeat
 * (the caller deals them). Guesses are compared in the forgiving spelling of GuessGame.norm. docs/GAMES-NEW.md.
 */
public final class DrawGame {
    public static final int MIN_PLAYERS = 3, MAX_PLAYERS = 10, GUESS_MAX = 40;
    public static final int[] TURNS = {1, 2}, SECONDS = {60, 90, 120};
    public enum Phase { DRAW, REVEAL, END }

    public final int players, turns; public final int[] score, gained; public Phase phase = Phase.DRAW; public int round, correct;
    public String word = "", alt = "";
    private final boolean[] right; private final int[] order;

    public DrawGame(int players, int turns) {
        if (players < MIN_PLAYERS || players > MAX_PLAYERS || turns < 1) throw new IllegalArgumentException("players");
        this.players = players; this.turns = turns; score = new int[players]; gained = new int[players]; right = new boolean[players]; order = new int[players];
    }
    public int rounds() { return players * turns; }
    public int drawer() { return round % players; }
    /** the next word (the Arabic one, and the English one as a second spelling that also counts) */
    public void setWord(String ar, String en) { word = ar == null ? "" : ar; alt = en == null ? "" : en; }

    /** a guess: true when it is the word (the drawer cannot guess; a player who is right cannot score twice) */
    public boolean guess(int p, String text) {
        if (phase != Phase.DRAW || p < 0 || p >= players || p == drawer() || right[p] || text == null || text.length() > GUESS_MAX || word.isEmpty()) return false;
        String n = GuessGame.norm(text);
        if (n.isEmpty() || !(n.equals(GuessGame.norm(word)) || (!alt.isEmpty() && n.equals(GuessGame.norm(alt))))) return false;
        right[p] = true; order[p] = ++correct; return true;
    }
    public boolean isRight(int p) { return right[p]; }
    public boolean allRight() { return correct == players - 1; }

    /** closes the round and scores it */
    public void endRound() {
        if (phase != Phase.DRAW) return;
        Arrays.fill(gained, 0);
        for (int p = 0; p < players; p++) if (right[p]) gained[p] = order[p] == 1 ? 3 : order[p] == 2 ? 2 : 1;
        gained[drawer()] = correct;
        for (int p = 0; p < players; p++) score[p] += gained[p];
        phase = Phase.REVEAL;
    }
    public void next() {
        if (phase != Phase.REVEAL) return;
        round++; correct = 0; Arrays.fill(right, false); Arrays.fill(order, 0); Arrays.fill(gained, 0); word = ""; alt = "";
        phase = round >= rounds() ? Phase.END : Phase.DRAW;
    }
    public void finish() { phase = Phase.END; }
}
