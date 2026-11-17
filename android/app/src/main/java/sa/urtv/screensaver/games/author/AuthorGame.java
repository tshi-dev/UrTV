package sa.urtv.screensaver.games.author;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * «من كاتبها؟»: the prompts and the rules (no Android, tests/NewGamesTest.java). Everyone types an answer to a question; the answers are shown one at a time without
 * names and everyone else votes who wrote it. A right guess scores 1 for the voter; every wrong guess scores 1 for the writer (the answer fooled someone).
 * The file: {"v":1,"items":[{"i":"au-001","q":["ar","en"],"k":1}]}; an unusable item is skipped, not the file. docs/GAMES-NEW.md.
 */
public final class AuthorGame {
    public static final int MIN_PLAYERS = 3, MAX_PLAYERS = 10, NAME_MAX = 30, ITEMS_MIN = 40;
    public static final int[] QUESTIONS = {6, 8, 10}, WRITE_SECONDS = {45, 60, 90};

    public static final class Prompt {
        public final String id; public final String[] q; public final boolean kids;
        Prompt(String id, String ar, String en, boolean kids) { this.id = id; this.q = new String[] {ar, en}; this.kids = kids; }
        public String text(boolean en) { return en && !q[1].isEmpty() ? q[1] : q[0]; }
    }

    /** the prompts of the parsed JSON tree (null when the file is not the file at all) */
    public static List<Prompt> parse(Object tree) {
        if (!(tree instanceof Map)) return null;
        Map<?, ?> m = (Map<?, ?>) tree;
        if (!(m.get("v") instanceof Number) || ((Number) m.get("v")).intValue() != 1 || !(m.get("items") instanceof List)) return null;
        List<Prompt> out = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (Object o : (List<?>) m.get("items")) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> p = (Map<?, ?>) o;
            Object id = p.get("i"), q = p.get("q"), k = p.get("k");
            if (!(id instanceof String) || !ids.add((String) id) || !(q instanceof List) || ((List<?>) q).size() < 1 || !(((List<?>) q).get(0) instanceof String)) continue;
            String ar = ((String) ((List<?>) q).get(0)).trim(), en = ((List<?>) q).size() > 1 && ((List<?>) q).get(1) instanceof String ? ((String) ((List<?>) q).get(1)).trim() : "";
            if (ar.isEmpty() || ar.length() > 200 || en.length() > 240 || ar.indexOf('·') >= 0 || en.indexOf('·') >= 0) continue;
            out.add(new Prompt((String) id, ar, en, k instanceof Number && ((Number) k).intValue() == 1));
        }
        return out.size() >= ITEMS_MIN ? out : null;
    }

    /** `n` prompts, not among the recent ones when there are enough others, in a random order; kids only when asked */
    public static List<Prompt> pick(List<Prompt> all, int n, boolean kids, Set<String> recent, Random rnd) {
        List<Prompt> fresh = new ArrayList<>(), old = new ArrayList<>();
        for (Prompt p : all) if (!kids || p.kids) (recent.contains(p.id) ? old : fresh).add(p);
        Collections.shuffle(fresh, rnd); Collections.shuffle(old, rnd);
        fresh.addAll(old);
        return new ArrayList<>(fresh.subList(0, Math.min(n, fresh.size())));
    }

    // ---------------------------------------------------------------- the game

    public enum Phase { WRITE, VOTE, REVEAL, END }
    public final int players, total; public final List<Prompt> prompts; public final int[] score;
    public Phase phase = Phase.WRITE; public int index;                 // index: the question
    private final String[] written; private final List<Integer> order = new ArrayList<>();   // order: the writers whose answers are shown, shuffled
    public int shown;                                                    // which of the order is on the screen
    private final int[] vote; public final int[] gained; private final Random rnd;

    public AuthorGame(int players, List<Prompt> prompts, Random rnd) {
        if (players < MIN_PLAYERS || players > MAX_PLAYERS) throw new IllegalArgumentException("players");
        this.players = players; this.prompts = prompts; this.total = prompts.size(); this.rnd = rnd;
        score = new int[players]; written = new String[players]; vote = new int[players]; gained = new int[players];
        java.util.Arrays.fill(vote, -1);
        phase = total == 0 ? Phase.END : Phase.WRITE;
    }
    public Prompt prompt() { return phase == Phase.END ? null : prompts.get(index); }

    /** a player's answer (at most NAME_MAX characters, letters, digits and spaces; the first one counts) */
    public boolean submit(int p, String text) {
        if (phase != Phase.WRITE || p < 0 || p >= players || written[p] != null) return false;
        String t = clean(text);
        if (t.isEmpty()) return false;
        written[p] = t; return true;
    }
    public static String clean(String raw) {
        if (raw == null) return "";
        StringBuilder b = new StringBuilder(); int n = 0;
        for (int i = 0; i < raw.length() && n < NAME_MAX; ) {
            int cp = raw.codePointAt(i); i += Character.charCount(cp);
            if (Character.isLetterOrDigit(cp)) { b.appendCodePoint(cp); n++; } else if (cp == ' ' && b.length() > 0 && b.charAt(b.length() - 1) != ' ') { b.append(' '); n++; }
        }
        return b.toString().trim();
    }
    public int submitted() { int n = 0; for (String s : written) if (s != null) n++; return n; }
    public boolean allSubmitted() { return submitted() == players; }

    /** the writing is over: the answers are shuffled and shown one at a time; with fewer than two answers the question is skipped */
    public void endWrite() {
        if (phase != Phase.WRITE) return;
        order.clear();
        for (int p = 0; p < players; p++) if (written[p] != null) order.add(p);
        Collections.shuffle(order, rnd);
        shown = 0;
        if (order.size() < 2) { nextQuestion(); return; }
        startVote();
    }
    private void startVote() { phase = Phase.VOTE; java.util.Arrays.fill(vote, -1); java.util.Arrays.fill(gained, 0); }

    public String answer() { return phase == Phase.VOTE || phase == Phase.REVEAL ? written[order.get(shown)] : ""; }
    public int writer() { return phase == Phase.REVEAL ? order.get(shown) : -1; }
    public int shownWriter() { return order.get(shown); }
    public int answersCount() { return order.size(); }

    /** a vote: `p` says `guess` wrote the shown answer; the writer does not vote, nobody votes for themselves, once each */
    public boolean vote(int p, int guess) {
        if (phase != Phase.VOTE || p < 0 || p >= players || guess < 0 || guess >= players || p == guess || p == shownWriter() || vote[p] >= 0) return false;
        vote[p] = guess; return true;
    }
    public int votes() { int n = 0; for (int i = 0; i < players; i++) if (vote[i] >= 0) n++; return n; }
    public boolean allVoted() { return votes() == players - 1; }

    /** the writer is shown; points: a right guess 1 for the voter, a wrong one 1 for the writer */
    public void reveal() {
        if (phase != Phase.VOTE) return;
        int w = shownWriter();
        java.util.Arrays.fill(gained, 0);
        for (int p = 0; p < players; p++) if (vote[p] >= 0) { if (vote[p] == w) gained[p]++; else gained[w]++; }
        for (int p = 0; p < players; p++) score[p] += gained[p];
        phase = Phase.REVEAL;
    }
    public int voteOf(int p) { return vote[p]; }

    /** the next answer, the next question, or the end */
    public void next() {
        if (phase != Phase.REVEAL) return;
        if (shown + 1 < order.size()) { shown++; startVote(); } else nextQuestion();
    }
    private void nextQuestion() {
        index++; java.util.Arrays.fill(written, null); order.clear();
        phase = index >= total ? Phase.END : Phase.WRITE;
    }
    public void finish() { phase = Phase.END; }
    public List<String> asked() { List<String> v = new ArrayList<>(); for (int i = 0; i < Math.min(total, index + 1); i++) v.add(prompts.get(i).id); return v; }
}
