package sa.urtv.screensaver.games.charades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** «بدون كلام / وصّف»: the word deck (no Android, tested on a plain JVM: tests/PartyGamesTest.java).
 *  The file (assets/games/charades.json, made by .github/tools/make_party_words.py from games/charades/words.json):
 *  {"v":1,"words":[["chc-0","بقرة","cow","farm",1,1,3], …]}: id, Arabic, English, category, difficulty 1 to 3, kids 0/1, modes (1 act, 2 describe, 3 both).
 *  games/charades/SPEC.md: the kids filter takes only kids words of difficulty 1; when the deck is used up it is shuffled again and the screen says so. */
public final class CharadesWords {
    private CharadesWords() {}

    public static final int RECENT_MAX = 200, WORDS_MIN = 40, TEXT_MAX = 40;

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
    public static List<Word> parse(Object tree) {
        if (!(tree instanceof java.util.Map)) return null;
        Object ws = ((java.util.Map<?, ?>) tree).get("words");
        if (!(ws instanceof List) || ((List<?>) ws).size() < WORDS_MIN) return null;
        List<Word> out = new ArrayList<>(); Set<String> ids = new HashSet<>(), seen = new HashSet<>();
        for (Object o : (List<?>) ws) {
            if (!(o instanceof List) || ((List<?>) o).size() != 7) return null;
            List<?> l = (List<?>) o;
            String id = str(l.get(0)), ar = str(l.get(1)), en = str(l.get(2)), cat = str(l.get(3));
            int d = num(l.get(4), 1, 3), k = num(l.get(5), 0, 1), m = num(l.get(6), 1, 3);
            if (id == null || ar == null || en == null || cat == null || d < 0 || k < 0 || m < 0 || !ids.add(id) || !seen.add(ar)) return null;
            out.add(new Word(id, ar, en, cat, d, k == 1, m));
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
