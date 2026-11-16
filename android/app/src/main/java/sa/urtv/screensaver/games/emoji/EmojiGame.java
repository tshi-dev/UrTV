package sa.urtv.screensaver.games.emoji;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * «اقرأ الإيموجي»: the puzzles and the rules (no Android, tests/NewGamesTest.java). A line of emoji stands for a word; everyone picks the right one of four on their
 * phone; the first right answer scores 3, the second 2, every other right answer 1. The wrong options are other answers of the same category. The file:
 * {"v":1,"items":[{"i","e":"🍚🍗","a":["ar","en"],"c":"food","d":1,"k":1}]}; an unusable item is skipped, not the file. docs/GAMES-NEW.md.
 */
public final class EmojiGame {
    public static final int MIN_PLAYERS = 2, MAX_PLAYERS = 10, ITEMS_MIN = 60, OPTIONS = 4;
    public static final int[] QUESTIONS = {10, 15, 20}, SECONDS = {10, 15, 20};

    public static final class Puzzle {
        public final String id, emoji, cat; public final String[] a; public final int diff; public final boolean kids;
        Puzzle(String id, String emoji, String ar, String en, String cat, int diff, boolean kids) { this.id = id; this.emoji = emoji; a = new String[] {ar, en}; this.cat = cat; this.diff = diff; this.kids = kids; }
        public String answer(boolean en) { return en && !a[1].isEmpty() ? a[1] : a[0]; }
    }

    public static List<Puzzle> parse(Object tree) {
        if (!(tree instanceof Map)) return null;
        Map<?, ?> m = (Map<?, ?>) tree;
        if (!(m.get("v") instanceof Number) || ((Number) m.get("v")).intValue() != 1 || !(m.get("items") instanceof List)) return null;
        List<Puzzle> out = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (Object o : (List<?>) m.get("items")) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> p = (Map<?, ?>) o;
            Object id = p.get("i"), e = p.get("e"), a = p.get("a"), c = p.get("c"), d = p.get("d"), k = p.get("k");
            if (!(id instanceof String) || !ids.add((String) id) || !(e instanceof String) || ((String) e).isEmpty() || ((String) e).length() > 40 || !(a instanceof List) || ((List<?>) a).size() < 1 || !(((List<?>) a).get(0) instanceof String) || !(c instanceof String)) continue;
            List<?> al = (List<?>) a; String ar = ((String) al.get(0)).trim(), en = al.size() > 1 && al.get(1) instanceof String ? ((String) al.get(1)).trim() : "";
            if (ar.isEmpty() || ar.length() > 80 || en.length() > 100 || (ar + en).indexOf('·') >= 0) continue;
            out.add(new Puzzle((String) id, (String) e, ar, en, (String) c, d instanceof Number ? ((Number) d).intValue() : 1, k instanceof Number && ((Number) k).intValue() == 1));
        }
        return out.size() >= ITEMS_MIN ? out : null;
    }

    public static List<Puzzle> pick(List<Puzzle> all, int n, boolean kids, Set<String> recent, Random rnd) {
        List<Puzzle> fresh = new ArrayList<>(), old = new ArrayList<>();
        for (Puzzle p : all) if (!kids || p.kids) (recent.contains(p.id) ? old : fresh).add(p);
        Collections.shuffle(fresh, rnd); Collections.shuffle(old, rnd);
        fresh.addAll(old);
        return new ArrayList<>(fresh.subList(0, Math.min(n, fresh.size())));
    }

    // ---------------------------------------------------------------- the game

    public enum Phase { ASK, REVEAL, END }
    public final int players, total; public final List<Puzzle> puzzles; public final int[] score, gained; public final boolean en;
    public Phase phase; public int index;
    private final List<Puzzle> pool; private final Random rnd; private final int[] choice; private final long[] at; private List<String> options = new ArrayList<>(); private int right;

    public EmojiGame(int players, List<Puzzle> puzzles, List<Puzzle> pool, boolean en, Random rnd) {
        if (players < MIN_PLAYERS || players > MAX_PLAYERS) throw new IllegalArgumentException("players");
        this.players = players; this.puzzles = puzzles; this.pool = pool; this.en = en; this.rnd = rnd; total = puzzles.size();
        score = new int[players]; gained = new int[players]; choice = new int[players]; at = new long[players];
        phase = total == 0 ? Phase.END : Phase.ASK;
        if (total > 0) deal();
    }
    public Puzzle puzzle() { return phase == Phase.END ? null : puzzles.get(index); }
    /** the four options of this puzzle, in the order every phone sees */
    public List<String> options() { return options; }
    public int rightIndex() { return right; }

    private void deal() {
        Puzzle p = puzzles.get(index);
        List<String> wrong = new ArrayList<>(); List<Puzzle> same = new ArrayList<>(), other = new ArrayList<>();
        for (Puzzle q : pool) if (!q.id.equals(p.id) && !q.answer(en).equals(p.answer(en))) (q.cat.equals(p.cat) ? same : other).add(q);
        Collections.shuffle(same, rnd); Collections.shuffle(other, rnd); same.addAll(other);
        for (Puzzle q : same) { if (wrong.size() == OPTIONS - 1) break; if (!wrong.contains(q.answer(en))) wrong.add(q.answer(en)); }
        options = new ArrayList<>(wrong); options.add(p.answer(en)); Collections.shuffle(options, rnd);
        right = options.indexOf(p.answer(en));
        java.util.Arrays.fill(choice, -1); java.util.Arrays.fill(gained, 0);
    }

    /** a player's choice (once), with the time it came (ms): earlier right answers score more */
    public boolean answer(int p, int pick, long atMs) {
        if (phase != Phase.ASK || p < 0 || p >= players || choice[p] >= 0 || pick < 0 || pick >= options.size()) return false;
        choice[p] = pick; at[p] = atMs; return true;
    }
    public int answered() { int n = 0; for (int c : choice) if (c >= 0) n++; return n; }
    public boolean allIn() { return answered() == players; }
    public int choiceOf(int p) { return choice[p]; }

    public void reveal() {
        if (phase != Phase.ASK) return;
        List<Integer> okOnes = new ArrayList<>();
        for (int p = 0; p < players; p++) if (choice[p] == right) okOnes.add(p);
        okOnes.sort((a, b) -> Long.compare(at[a], at[b]));
        for (int i = 0; i < okOnes.size(); i++) gained[okOnes.get(i)] = i == 0 ? 3 : i == 1 ? 2 : 1;
        for (int p = 0; p < players; p++) score[p] += gained[p];
        phase = Phase.REVEAL;
    }
    public boolean wasRight(int p) { return choice[p] == right; }
    public void next() {
        if (phase != Phase.REVEAL) return;
        index++;
        if (index >= total) phase = Phase.END; else { phase = Phase.ASK; deal(); }
    }
    public void finish() { phase = Phase.END; }
    public List<String> asked() { List<String> v = new ArrayList<>(); for (int i = 0; i < Math.min(total, index + 1); i++) v.add(puzzles.get(i).id); return v; }
}
