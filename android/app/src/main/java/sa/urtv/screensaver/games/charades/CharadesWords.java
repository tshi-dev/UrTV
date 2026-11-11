package sa.urtv.screensaver.games.charades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** «بدون كلام / وصّف»: the word deck (no Android, tested on a plain JVM: tests/PartyGamesTest.java).
 *  The file (docs/GAMES-DATA.md: charades.json, the bundled starter in assets/games and the full list from data-games have the same form):
 *  {"v":1,"c":["farm", …],"w":[["بقرة","cow",0,1,1,3], …]}: Arabic, English, the category's index in c, difficulty 1 to 3, kids 0/1, modes (bit 1 act, bit 2
 *  describe). No word twice, so the Arabic word is the id. The starter has 150 words, the published file 200 to 10,000.
 *  games/charades/SPEC.md: the kids filter takes only kids words of difficulty 1; when the deck is used up it is shuffled again and the screen says so. */
public final class CharadesWords {
    private CharadesWords() {}

    public static final int RECENT_MAX = 200, WORDS_MIN = 200, STARTER_MIN = 100, WORDS_MAX = 10000, TEXT_MAX = 200;

    public static final class Word {
        public final String id, cat; public final String[] text; public final int diff; public final boolean kids, act, describe;
        Word(String id, String ar, String en, String cat, int diff, boolean kids, int modes) {
            this.id = id; this.text = new String[] {ar, en}; this.cat = cat; this.diff = diff; this.kids = kids; act = (modes & 1) != 0; describe = (modes & 2) != 0;
        }
        public String text(boolean en) { return text[en ? 1 : 0]; }
    }

    private static String str(Object o) {
        if (!(o instanceof String)) return null;
        String s = ((String) o).trim();
        return s.isEmpty() || s.length() > TEXT_MAX || s.indexOf('·') >= 0 || s.indexOf('•') >= 0 ? null : s;
    }
    private static int num(Object o, int min, int max) {
        if (!(o instanceof Double)) return -1;
        double d = (Double) o;
        return d == Math.rint(d) && d >= min && d <= max ? (int) d : -1;
    }

    /** the parsed JSON tree (what MiniJson gives) → the words; null when anything is not as the file must be (the whole file is refused) */
    public static List<Word> parse(Object tree) { return parse(tree, STARTER_MIN); }
    /** `min`: WORDS_MIN for a published file, STARTER_MIN for the bundled one */
    public static List<Word> parse(Object tree, int min) {
        if (!(tree instanceof java.util.Map)) return null;
        java.util.Map<?, ?> m = (java.util.Map<?, ?>) tree;
        Object ws = m.get("w"), cs = m.get("c"), v = m.get("v");
        if (!(v instanceof Double) || (Double) v != 1 || !(cs instanceof List) || ((List<?>) cs).isEmpty() || !(ws instanceof List) || ((List<?>) ws).size() < min || ((List<?>) ws).size() > WORDS_MAX) return null;
        List<String> cats = new ArrayList<>();
        for (Object c : (List<?>) cs) { String s = str(c); if (s == null) return null; cats.add(s); }
        List<Word> out = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (Object o : (List<?>) ws) {
            if (!(o instanceof List) || ((List<?>) o).size() != 6) return null;
            List<?> l = (List<?>) o;
            String ar = str(l.get(0)), en = str(l.get(1));
            int ci = num(l.get(2), 0, cats.size() - 1), d = num(l.get(3), 1, 3), k = num(l.get(4), 0, 1), md = num(l.get(5), 1, 3);
            if (ar == null || en == null || ci < 0 || d < 0 || k < 0 || md < 0 || !seen.add(ar)) return null;
            out.add(new Word(ar, ar, en, cats.get(ci), d, k == 1, md));
        }
        return out;
    }

    public static boolean matches(Word w, boolean describe, boolean kids) {
        return (describe ? w.describe : w.act) && (!kids || (w.kids && w.diff == 1));
    }

    /** One game's deck: every matching word once, the ones this TV played lately (recent, the oldest first) at the end; used up → shuffled again (reshuffled says so). */
    public static final class Deck {
        private final List<Word> all = new ArrayList<>(); private final List<Word> pile = new ArrayList<>(); private final Random rnd;
        private List<String> recent;
        /** set when the last draw() had to start the deck again; the screen shows its line once and clears it */
        public boolean reshuffled;

        public Deck(List<Word> words, boolean describe, boolean kids, List<String> recent, Random rnd) {
            this.rnd = rnd; this.recent = recent == null ? new ArrayList<String>() : new ArrayList<>(recent);
            for (Word w : words) if (matches(w, describe, kids)) all.add(w);
            fill(null);
        }
        private void fill(Word last) {
            List<Word> fresh = new ArrayList<>(), old = new ArrayList<>();
            for (Word w : all) (recent.contains(w.id) ? old : fresh).add(w);
            Collections.shuffle(fresh, rnd);
            Collections.sort(old, (a, b) -> recent.indexOf(a.id) - recent.indexOf(b.id));
            pile.clear(); pile.addAll(fresh); pile.addAll(old);
            if (last != null && pile.size() > 1 && pile.get(0) == last) Collections.swap(pile, 0, pile.size() - 1);   // never the same word twice in a row
        }
        public int size() { return all.size(); }
        public int left() { return pile.size(); }
        /** the next word; null only when no word matches at all */
        public Word draw() {
            if (all.isEmpty()) return null;
            if (pile.isEmpty()) { Word last = lastDrawn; recent.clear(); fill(last); reshuffled = true; }
            lastDrawn = pile.remove(0);
            recent.remove(lastDrawn.id); recent.add(lastDrawn.id);
            while (recent.size() > RECENT_MAX) recent.remove(0);
            return lastDrawn;
        }
        private Word lastDrawn;
        /** the last words played on this TV, this game's included: the screen keeps it for the next game */
        public List<String> recent() { return new ArrayList<>(recent); }
    }
}
