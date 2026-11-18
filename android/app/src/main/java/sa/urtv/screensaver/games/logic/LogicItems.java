package sa.urtv.screensaver.games.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import sa.urtv.screensaver.games.emoji.EmojiGame;

/**
 * The puzzles of «ألغاز المنطق» (no Android, tests/NewGamesTest.java), for the multiple-choice engine EmojiGame. Most are made by code, so they are always right and never
 * run out: an order puzzle (who is tallest or shortest), the next number of a sequence, the odd number out, an age puzzle. The rest are written by hand and kept in the
 * bundle assets/games/logic.gb: {"v":1,"items":[{"i","q":["ar","en"],"a":["ar","en"],"w":[["ar","en"],...3]}]}. A prompt is the Arabic text, a line, then the English text; an option
 * is «ar / en» (or just a number or a name), so both languages show together.
 */
public final class LogicItems {
    private LogicItems() {}
    public static final int BANK_MIN = 12;
    private static final String[][] NAMES = {{"سارة", "Sara"}, {"أحمد", "Ahmed"}, {"ليلى", "Layla"}, {"خالد", "Khalid"}, {"نورة", "Noura"}, {"فيصل", "Faisal"}, {"هند", "Hind"}, {"ماجد", "Majed"}};

    private static String both(String ar, String en) { return ar.equals(en) ? ar : ar + " / " + en; }
    /** the two languages of a prompt are told apart by this separator (the screen puts them on two lines, the phone on two paragraphs) */
    public static final char SEP = '\u2029';
    private static String ask(String ar, String en) { return ar + SEP + en; }
    private static List<String> opts(String right, String... wrong) { List<String> l = new ArrayList<>(); l.add(right); for (String w : wrong) l.add(w); return l; }

    /** the hand-written puzzles of the bundle */
    public static List<EmojiGame.Puzzle> bank(Object tree) {
        if (!(tree instanceof Map) || !(((Map<?, ?>) tree).get("items") instanceof List)) return null;
        List<EmojiGame.Puzzle> out = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (Object o : (List<?>) ((Map<?, ?>) tree).get("items")) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) o; Object id = m.get("i"), q = m.get("q"), a = m.get("a"), w = m.get("w");
            if (!(id instanceof String) || !ids.add((String) id) || !pair(q) || !pair(a) || !(w instanceof List) || ((List<?>) w).size() != 3) continue;
            List<String> options = new ArrayList<>(); options.add(both(s(a, 0), s(a, 1))); boolean ok = true;
            for (Object x : (List<?>) w) { if (!pair(x)) { ok = false; break; } options.add(both(s(x, 0), s(x, 1))); }
            if (!ok || new HashSet<>(options).size() != 4) continue;
            String text = s(q, 0) + SEP + s(q, 1);
            if ((text + options).indexOf('·') >= 0 || text.length() > 400) continue;
            out.add(EmojiGame.make("lg-" + id, text, options.get(0), "bank", options));
        }
        return out.size() >= BANK_MIN ? out : null;
    }
    private static boolean pair(Object o) { return o instanceof List && ((List<?>) o).size() == 2 && ((List<?>) o).get(0) instanceof String && ((List<?>) o).get(1) instanceof String && !((String) ((List<?>) o).get(0)).trim().isEmpty() && !((String) ((List<?>) o).get(1)).trim().isEmpty(); }
    private static String s(Object o, int i) { return ((String) ((List<?>) o).get(i)).trim(); }

    /** `n` puzzles made by code, mixed kinds, never the same prompt twice */
    public static List<EmojiGame.Puzzle> generate(int n, Random r) {
        List<EmojiGame.Puzzle> out = new ArrayList<>(); Set<String> seen = new HashSet<>(); int guard = 0;
        while (out.size() < n && guard++ < n * 60) {
            EmojiGame.Puzzle p;
            switch (r.nextInt(5)) { case 0: case 1: p = order(r); break; case 2: p = sequence(r); break; case 3: p = odd(r); break; default: p = ages(r); }
            if (p != null && seen.add(p.emoji)) out.add(p);
        }
        return out;
    }

    /** «A is taller than B, B is taller than C: who is the tallest / the shortest?» with 3 or 4 people */
    static EmojiGame.Puzzle order(Random r) {
        int n = 3 + r.nextInt(2); List<String[]> pool = new ArrayList<>(); for (String[] p : NAMES) pool.add(p); Collections.shuffle(pool, r);
        List<String[]> chain = new ArrayList<>(pool.subList(0, n));   // index 0 = tallest ... n-1 = shortest
        List<String> ar = new ArrayList<>(), en = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) { ar.add(chain.get(i)[0] + " أطول من " + chain.get(i + 1)[0]); en.add(chain.get(i)[1] + " is taller than " + chain.get(i + 1)[1]); }
        List<Integer> idx = new ArrayList<>(); for (int i = 0; i < n - 1; i++) idx.add(i); Collections.shuffle(idx, r);   // the facts in a mixed order
        StringBuilder a = new StringBuilder(), e = new StringBuilder();
        for (int i : idx) { a.append(ar.get(i)).append("، "); e.append(en.get(i)).append(", "); }
        boolean tallest = r.nextBoolean(); String[] right = chain.get(tallest ? 0 : n - 1);
        List<String> o = new ArrayList<>(); o.add(both(right[0], right[1])); for (String[] c : chain) if (c != right) o.add(both(c[0], c[1]));
        while (o.size() < 4) { String[] x = pool.get(n + o.size() - n); String t = both(x[0], x[1]); if (!o.contains(t)) o.add(t); else break; }
        if (o.size() < 4) for (String[] x : pool) { String t = both(x[0], x[1]); if (!o.contains(t) && o.size() < 4) o.add(t); }
        String prompt = ask(a + (tallest ? "من الأطول؟" : "من الأقصر؟"), e + (tallest ? "who is the tallest?" : "who is the shortest?"));
        return EmojiGame.make("lg-order-" + prompt.hashCode(), prompt, o.get(0), "gen", o.subList(0, 4));
    }

    /** «2, 4, 8, 16, ?»: arithmetic, geometric, squares, or two steps that alternate */
    static EmojiGame.Puzzle sequence(Random r) {
        long[] t = new long[6]; int kind = r.nextInt(4);
        if (kind == 0) { long s = 1 + r.nextInt(20), d = 2 + r.nextInt(9); for (int i = 0; i < 6; i++) t[i] = s + d * i; }
        else if (kind == 1) { long s = 1 + r.nextInt(4), q = 2 + r.nextInt(2); t[0] = s; for (int i = 1; i < 6; i++) t[i] = t[i - 1] * q; }
        else if (kind == 2) { int s = 1 + r.nextInt(4); for (int i = 0; i < 6; i++) t[i] = (long) (s + i) * (s + i); }
        else { long s = 1 + r.nextInt(10), a = 2 + r.nextInt(4), b = 1 + r.nextInt(3); t[0] = s; for (int i = 1; i < 6; i++) t[i] = t[i - 1] + (i % 2 == 1 ? a : b); }
        long right = t[5]; String q = t[0] + ", " + t[1] + ", " + t[2] + ", " + t[3] + ", " + t[4] + ", ?";
        List<String> o = new ArrayList<>(); o.add(String.valueOf(right)); long[] off = {1, -1, 2, -2, 3, 5, 10, -3};
        for (int k = 0; k < off.length && o.size() < 4; k++) { long w = right + off[(k + r.nextInt(2)) % off.length]; if (w > 0 && !o.contains(String.valueOf(w))) o.add(String.valueOf(w)); }
        for (long w = right + 7; o.size() < 4; w++) if (!o.contains(String.valueOf(w))) o.add(String.valueOf(w));
        return EmojiGame.make("lg-seq-" + q.hashCode(), ask(q + "     ما العدد التالي؟", "What comes next?"), o.get(0), "gen", o);
    }

    /** four numbers, three share a property and one does not */
    static EmojiGame.Puzzle odd(Random r) {
        int k = 2 + r.nextInt(8); int[] ms = new int[3]; Set<Integer> used = new HashSet<>();
        for (int i = 0; i < 3; i++) { int m; do { m = (1 + r.nextInt(12)) * k; } while (!used.add(m)); ms[i] = m; }
        int x; do { x = 4 + r.nextInt(120); } while (x % k == 0 || used.contains(x));
        List<Integer> all = new ArrayList<>(); for (int m : ms) all.add(m); all.add(x); Collections.shuffle(all, r);
        List<String> o = new ArrayList<>(); o.add(String.valueOf(x)); for (int v : all) if (v != x) o.add(String.valueOf(v));
        String nums = all.get(0) + "   " + all.get(1) + "   " + all.get(2) + "   " + all.get(3);
        return EmojiGame.make("lg-odd-" + nums.hashCode(), ask("أي عدد ليس من مضاعفات " + k + "؟\n" + nums, "Which number is not a multiple of " + k + "?\n" + nums), o.get(0), "gen", o);
    }

    /** «the father is X years older than his son; the son is Y: how old is the father?» (and the sum) */
    static EmojiGame.Puzzle ages(Random r) {
        String[] p = NAMES[r.nextInt(NAMES.length)], q = NAMES[(r.nextInt(NAMES.length - 1) + 1 + indexOf(p)) % NAMES.length];
        int younger = 4 + r.nextInt(12), diff = 2 + r.nextInt(9); int older = younger + diff;
        boolean sum = r.nextBoolean(); int right = sum ? older + younger : older;
        String ar = p[0] + " أكبر من " + q[0] + " بـ " + diff + " سنوات، وعمر " + q[0] + " " + younger + " سنة. " + (sum ? "كم مجموع عمريهما؟" : "كم عمر " + p[0] + "؟");
        String en = p[1] + " is " + diff + " years older than " + q[1] + ", and " + q[1] + " is " + younger + ". " + (sum ? "What is the sum of their ages?" : "How old is " + p[1] + "?");
        List<String> o = new ArrayList<>(); o.add(String.valueOf(right)); int[] off = {1, -1, 2, -2, diff, -diff, younger};
        for (int k = 0; k < off.length && o.size() < 4; k++) { int w = right + off[k]; if (w > 0 && !o.contains(String.valueOf(w))) o.add(String.valueOf(w)); }
        for (int w = right + 3; o.size() < 4; w++) if (!o.contains(String.valueOf(w))) o.add(String.valueOf(w));
        return EmojiGame.make("lg-age-" + ar.hashCode(), ask(ar, en), o.get(0), "gen", o);
    }
    private static int indexOf(String[] p) { for (int i = 0; i < NAMES.length; i++) if (NAMES[i] == p) return i; return 0; }
}
