package sa.urtv.screensaver.games.maths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import sa.urtv.screensaver.games.closest.ClosestGame;

/**
 * «سباق الحساب» (no Android, tests/NewGamesTest.java): quick mental arithmetic, made by code (always right, endless, no bank). Everyone types the answer on their phone
 * (a number pad); the first right answer scores 3, the second 2, every other right answer 1; a wrong answer costs nothing. Three levels. A question is the same in both
 * languages: the expression in digits, with a short instruction in Arabic and in English that the TV shows together. docs/GAMES-NEW.md.
 */
public final class MathGame {
    public static final int MIN_PLAYERS = 2, MAX_PLAYERS = 8;
    public static final int[] QUESTIONS = {10, 15, 20}, SECONDS = {15, 25, 40};
    public enum Phase { ASK, REVEAL, END }

    /** `expr` is in plain digits and signs (the TV shows it in the person's digits); the two lines tell what to do */
    public static final class Question {
        public final String expr, ar, en; public final long answer;
        Question(String expr, String ar, String en, long answer) { this.expr = expr; this.ar = ar; this.en = en; this.answer = answer; }
    }

    /** n different questions of a level (1 easy, 2 medium, 3 hard); the answers are whole and never negative */
    public static List<Question> generate(int level, int n, Random r) {
        List<Question> out = new ArrayList<>(); Set<String> seen = new HashSet<>(); int guard = 0;
        while (out.size() < n && guard++ < n * 50) {
            Question q = one(Math.max(1, Math.min(3, level)), r);
            if (q != null && q.answer >= 0 && q.answer < 1_000_000 && seen.add(q.expr)) out.add(q);
        }
        return out;
    }
    private static int between(Random r, int lo, int hi) { return lo + r.nextInt(hi - lo + 1); }
    private static final String CALC_AR = "احسب", CALC_EN = "Calculate", NEXT_AR = "ما العدد التالي؟", NEXT_EN = "What comes next?", MISS_AR = "ما العدد الناقص؟", MISS_EN = "Find the missing number";

    private static Question one(int level, Random r) {
        int k = r.nextInt(level == 1 ? 3 : level == 2 ? 5 : 7);
        int a, b, c, d;
        if (level == 1) {
            switch (k) {
                case 0: a = between(r, 6, 60); b = between(r, 6, 60); return new Question(a + " + " + b, CALC_AR, CALC_EN, a + b);
                case 1: a = between(r, 20, 99); b = between(r, 5, a - 1); return new Question(a + " − " + b, CALC_AR, CALC_EN, a - b);
                default: a = between(r, 2, 10); b = between(r, 2, 10); return new Question(a + " × " + b, CALC_AR, CALC_EN, a * b);
            }
        }
        if (level == 2) {
            switch (k) {
                case 0: a = between(r, 11, 25); b = between(r, 3, 12); return new Question(a + " × " + b, CALC_AR, CALC_EN, a * b);
                case 1: a = between(r, 2, 9); b = between(r, 2, 12); c = a * b; return new Question(c + " ÷ " + a, CALC_AR, CALC_EN, b);
                case 2: a = between(r, 20, 90); b = between(r, 20, 90); c = between(r, 5, 19); return new Question(a + " + " + b + " − " + c, CALC_AR, CALC_EN, a + b - c);
                case 3: a = between(r, 2, 9); b = between(r, 2, 9); c = between(r, 2, 9); return new Question("(" + a + " + " + b + ") × " + c, CALC_AR, CALC_EN, (long) (a + b) * c);
                default: a = between(r, 12, 60); b = between(r, 20, 90); return new Question("□ + " + a + " = " + (a + b), MISS_AR, MISS_EN, b);
            }
        }
        switch (k) {
            case 0: a = between(r, 12, 49); b = between(r, 6, 19); return new Question(a + " × " + b, CALC_AR, CALC_EN, a * b);
            case 1: a = between(r, 11, 30); return new Question(a + " × " + a, CALC_AR, CALC_EN, a * a);
            case 2: return div(r);
            case 3: a = between(r, 2, 9); b = between(r, 2, 9); c = between(r, 2, 9); d = between(r, 2, 9); return new Question(a + " × " + b + " + " + c + " × " + d, CALC_AR, CALC_EN, a * b + c * d);
            case 4: { int[] pct = {10, 20, 25, 50, 75}; int p = pct[r.nextInt(pct.length)]; int base = between(r, 2, 40) * (p == 25 || p == 75 ? 4 : 10); return new Question(p + "% × " + base, CALC_AR, CALC_EN, (long) base * p / 100); }
            case 5: { int start = between(r, 1, 9), step = between(r, 2, 9); boolean mul = r.nextBoolean(); if (mul) { start = between(r, 1, 4); step = between(r, 2, 4); }
                long[] t = new long[5]; t[0] = start; for (int i = 1; i < 5; i++) t[i] = mul ? t[i - 1] * step : t[i - 1] + step;
                return new Question(t[0] + ", " + t[1] + ", " + t[2] + ", " + t[3] + ", ?", NEXT_AR, NEXT_EN, mul ? t[3] * step : t[3] + step); }
            default: a = between(r, 12, 40); b = between(r, 3, a - 2); return new Question(a + "² − " + b + "²", CALC_AR, CALC_EN, (long) a * a - (long) b * b);
        }
    }
    private static Question div(Random r) { int a = between(r, 3, 9), b = between(r, 12, 49), e = between(r, 3, 29); return new Question(a * b + " ÷ " + a + " + " + e, CALC_AR, CALC_EN, b + e); }

    // ---------------------------------------------------------------- the game

    public final int players, total; public final int[] score, gained; public Phase phase = Phase.ASK; public int index; private final List<Question> questions;
    private final long[] typed, at; private final boolean[] in; private int arrived;

    public MathGame(int players, List<Question> questions) {
        if (players < MIN_PLAYERS || players > MAX_PLAYERS || questions.isEmpty()) throw new IllegalArgumentException("players");
        this.players = players; this.questions = questions; total = questions.size(); score = new int[players]; gained = new int[players]; typed = new long[players]; at = new long[players]; in = new boolean[players];
    }
    public Question question() { return questions.get(Math.min(index, total - 1)); }

    /** a phone's answer (once per question; `atMs` is when the TV saw it): false when it is not a whole number, or was already given */
    public boolean submit(int p, String text, long atMs) {
        if (phase != Phase.ASK || p < 0 || p >= players || in[p]) return false;
        Long v = ClosestGame.number(text);
        if (v == null) return false;
        in[p] = true; typed[p] = v; at[p] = atMs * 16 + (arrived++ & 15); return true;
    }
    public boolean allIn() { for (boolean b : in) if (!b) return false; return true; }
    public int answered() { int n = 0; for (boolean b : in) if (b) n++; return n; }
    public boolean right(int p) { return in[p] && typed[p] == question().answer; }
    public long givenBy(int p) { return typed[p]; }
    public boolean gave(int p) { return in[p]; }

    public void reveal() {
        if (phase != Phase.ASK) return;
        List<Integer> ok = new ArrayList<>(); for (int p = 0; p < players; p++) if (right(p)) ok.add(p);
        ok.sort((x, y) -> Long.compare(at[x], at[y]));
        Arrays.fill(gained, 0);
        for (int i = 0; i < ok.size(); i++) gained[ok.get(i)] = i == 0 ? 3 : i == 1 ? 2 : 1;
        for (int p = 0; p < players; p++) score[p] += gained[p];
        phase = Phase.REVEAL;
    }
    public void next() {
        if (phase != Phase.REVEAL) return;
        index++; Arrays.fill(in, false); Arrays.fill(gained, 0); arrived = 0;
        phase = index >= total ? Phase.END : Phase.ASK;
    }
    public void finish() { phase = Phase.END; }
}
