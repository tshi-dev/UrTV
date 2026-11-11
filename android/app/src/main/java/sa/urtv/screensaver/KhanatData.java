package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * «خمس خانات»: the letters and the sample answers (no Android, tests/GamesKitTest.java). The file is games/khanat.json of docs/GAMES-DATA.md:
 * {"v":1, "L" letters, "H" hard, "R" rare, "B" boxes, "X" extra boxes, "s":{letter:{box:[[ar,en],…]}}}, at most 200 KB and 4,000 samples of at most 24
 * characters; a download must hold 200 samples or more, the bundled starter 100. The samples are advisory (a few examples to settle an argument), never
 * the only right answers. The English game has its own letters: the samples' English names, by their first letter (a letter with examples in fewer than
 * three of the five boxes is a rare one). `parse` checks the whole file; null = refused.
 */
final class KhanatData {
    static final int BYTES_MAX = 200_000, SAMPLES_MAX = 4000, SAMPLE_LEN = 24, PUBLISHED_MIN = 200, STARTER_MIN = 100;

    /** one language's letters and its samples: letter, then box key, then the words */
    static final class Part {
        final List<String> letters = new ArrayList<>(), rare = new ArrayList<>(); final Map<String, Map<String, List<String>>> samples = new HashMap<>();
        void put(String letter, String box, String word) {
            Map<String, List<String>> m = samples.get(letter);
            if (m == null) { m = new HashMap<>(); samples.put(letter, m); }
            List<String> v = m.get(box);
            if (v == null) { v = new ArrayList<>(); m.put(box, v); }
            if (!v.contains(word)) v.add(word);
        }
    }

    /** the five boxes of every round, then the extra ones a chip may add */
    final List<String> boxes = new ArrayList<>(), extra = new ArrayList<>(); final Part ar = new Part(), en = new Part(); int count;

    Part part(boolean english) { return english ? en : ar; }

    /** up to `max` examples for a letter and a box (empty when the file has none) */
    List<String> samples(boolean english, String letter, String box, int max) {
        List<String> v = new ArrayList<>();
        Map<String, List<String>> m = part(english).samples.get(letter);
        if (m == null || m.get(box) == null) return v;
        for (String s : m.get(box)) { if (v.size() >= max) break; v.add(s); }
        return v;
    }

    /** the letters of a game: the usual ones, plus the rare ones with «حروف صعبة» */
    List<String> letters(boolean english, boolean withRare) {
        List<String> v = new ArrayList<>(part(english).letters);
        if (withRare) v.addAll(part(english).rare);
        return v;
    }

    static final GameDataCheck.Parser<KhanatData> PUBLISHED = new GameDataCheck.Parser<KhanatData>() {
        @Override public KhanatData parse(byte[] raw) { return KhanatData.parse(raw, PUBLISHED_MIN); }
    };
    static final GameDataCheck.Parser<KhanatData> STARTER = new GameDataCheck.Parser<KhanatData>() {
        @Override public KhanatData parse(byte[] raw) { return KhanatData.parse(raw, STARTER_MIN); }
    };

    static KhanatData parse(byte[] raw, int min) {
        if (raw == null || raw.length > BYTES_MAX) return null;
        Object root = GameDataCheck.root(raw, '{');
        if (root == null) return null;
        Map<?, ?> m = (Map<?, ?>) root;
        if (GameDataCheck.whole(m.get("v"), 1, 1) != 1 || !(m.get("s") instanceof Map)) return null;
        KhanatData d = new KhanatData();
        if (!letters(m.get("L"), d.ar.letters, d.ar.rare, 10, 40) || !letters(m.get("R"), d.ar.rare, d.ar.letters, 0, 12)) return null;
        if (!keys(m.get("B"), d.boxes, d.extra) || d.boxes.size() != 5 || !keys(m.get("X"), d.extra, d.boxes) || d.extra.size() > 8) return null;
        for (Map.Entry<?, ?> e : ((Map<?, ?>) m.get("s")).entrySet()) {
            String letter = String.valueOf(e.getKey());
            if (!(d.ar.letters.contains(letter) || d.ar.rare.contains(letter)) || !(e.getValue() instanceof Map)) return null;
            for (Map.Entry<?, ?> b : ((Map<?, ?>) e.getValue()).entrySet()) {
                String box = String.valueOf(b.getKey());
                if (!(d.boxes.contains(box) || d.extra.contains(box)) || !(b.getValue() instanceof List)) return null;
                for (Object o : (List<?>) b.getValue()) {
                    if (!(o instanceof List) || ((List<?>) o).size() != 2) return null;
                    Object a = ((List<?>) o).get(0), x = ((List<?>) o).get(1);
                    if (!GameDataCheck.text(a, SAMPLE_LEN) || !(x instanceof String) || !(((String) x).isEmpty() || GameDataCheck.text(x, GameDataCheck.TEXT_MAX))) return null;
                    if (++d.count > SAMPLES_MAX) return null;
                    d.ar.put(letter, box, (String) a);
                    String w = ((String) x).trim();
                    if (w.matches("[A-Za-z][A-Za-z '\\-]{0,23}")) d.en.put(w.substring(0, 1).toUpperCase(java.util.Locale.ROOT), box, w.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + w.substring(1));
                }
            }
        }
        if (d.count < min) return null;
        for (char c = 'A'; c <= 'Z'; c++) {   // the English letters: usual with examples in three of the five boxes or more, else rare
            Map<String, List<String>> got = d.en.samples.get(String.valueOf(c));
            if (got == null) continue;
            int n = 0;
            for (String b : d.boxes) if (got.containsKey(b)) n++;
            (n >= 3 ? d.en.letters : d.en.rare).add(String.valueOf(c));
        }
        return d;
    }

    /** one letter per character, none twice in the two lists */
    private static boolean letters(Object o, List<String> into, List<String> other, int min, int max) {
        if (!(o instanceof String)) return false;
        String s = (String) o;
        if (s.length() < min || s.length() > max) return false;
        for (int i = 0; i < s.length(); i++) {
            String c = String.valueOf(s.charAt(i));
            if (Character.isWhitespace(s.charAt(i)) || Character.isISOControl(s.charAt(i)) || into.contains(c) || other.contains(c)) return false;
            into.add(c);
        }
        return true;
    }
    private static boolean keys(Object o, List<String> into, List<String> other) {
        if (!(o instanceof List)) return false;
        for (Object b : (List<?>) o) {
            if (!(b instanceof String) || !((String) b).matches("[a-z_]{1,16}") || into.contains(b) || other.contains(b)) return false;
            into.add((String) b);
        }
        return true;
    }
}
