package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * «الكلمة المفقودة»: the data, the picking and the rules of one game (no Android, tests/GamesKitTest.java; games/missing-word/SPEC.md, the remote-only flow).
 * The file is games/missing-word.json of docs/GAMES-DATA.md: {"v":1, "w":[{"i" id, "k" "f" fact | "s" saying, "d" 1..3, "a" 1 = for kids, "t" [Arabic,
 * English] the statement with ___ once (at most 90 characters), "w" [Arabic, English] the hidden word}]}, at most 300 KB and 5,000 items; a download must
 * hold 100 items or more, the bundled starter 40. A saying has no English (both empty): an English game plays only the items with real English, as
 * «الأسرع» does.
 */
final class MissingWord {
    private MissingWord() {}

    static final String BLANK = "___", MIX = "mix", FACT = "fact", SAYING = "saying";
    /** the «النوع» chips, in order, and their Arabic names */
    static final String[] DECKS = {MIX, SAYING, FACT}, DECK_NAMES = {"مزيج", "حكمة ومثل", "معلومة"};
    static final int RECENT_MAX = 100;
    static final int[] COUNTS = {5, 10, 15}, SECONDS = {10, 15, 30};
    static final int BYTES_MAX = 300_000, ITEMS_MAX = 5000, PUBLISHED_MIN = 100, STARTER_MIN = 40, TEXT_LEN = 90, WORD_MAX = 60;

    static final class Item {
        final String id, deck; final int diff; final boolean kids; final String[] text, word;
        Item(String id, String deck, int diff, boolean kids, String[] text, String[] word) { this.id = id; this.deck = deck; this.diff = diff; this.kids = kids; this.text = text; this.word = word; }
        boolean hasEnglish() { return !text[1].isEmpty(); }
        String text(boolean en) { return text[en ? 1 : 0]; }
        String word(boolean en) { return word[en ? 1 : 0]; }
        /** the first letter of the hidden word (the hint); «ال» is skipped so the hint is the word's own letter */
        String firstLetter(boolean en) {
            String w = word(en);
            if (!en && w.startsWith("ال") && w.length() > 3) w = w.substring(2);
            return w.isEmpty() ? "" : w.substring(0, w.offsetByCodePoints(0, 1));
        }
    }

    // ---------------------------------------------------------------- the file

    static final GameDataCheck.Parser<List<Item>> PUBLISHED = new GameDataCheck.Parser<List<Item>>() {
        @Override public List<Item> parse(byte[] raw) { return MissingWord.parse(raw, PUBLISHED_MIN); }
    };
    static final GameDataCheck.Parser<List<Item>> STARTER = new GameDataCheck.Parser<List<Item>>() {
        @Override public List<Item> parse(byte[] raw) { return MissingWord.parse(raw, STARTER_MIN); }
    };

    /** the items, or null when anything is wrong (one bad item refuses the file) */
    static List<Item> parse(byte[] raw, int min) {
        if (raw == null || raw.length > BYTES_MAX) return null;
        Object root = GameDataCheck.root(raw, '{');
        if (root == null || GameDataCheck.whole(((Map<?, ?>) root).get("v"), 1, 1) != 1 || !(((Map<?, ?>) root).get("w") instanceof List)) return null;
        List<?> all = (List<?>) ((Map<?, ?>) root).get("w");
        if (all.size() < min || all.size() > ITEMS_MAX) return null;
        List<Item> out = new ArrayList<>(all.size());
        Set<String> ids = new HashSet<>();
        for (Object o : all) {
            Item it = item(o);
            if (it == null || !ids.add(it.id)) return null;
            out.add(it);
        }
        return out;
    }

    private static Item item(Object o) {
        if (!(o instanceof Map)) return null;
        Map<?, ?> m = (Map<?, ?>) o;
        if (m.size() != 6 || !GameDataCheck.id(m.get("i"))) return null;
        Object k = m.get("k");
        int d = GameDataCheck.whole(m.get("d"), 1, 3), a = GameDataCheck.whole(m.get("a"), 0, 1);
        if (!("f".equals(k) || "s".equals(k)) || d < 0 || a < 0) return null;
        String[] t = pair(m.get("t")), w = pair(m.get("w"));
        if (t == null || w == null) return null;
        if (!GameDataCheck.text(t[0], TEXT_LEN) || !oneBlank(t[0]) || !GameDataCheck.text(w[0], WORD_MAX)) return null;
        boolean noEn = t[1].isEmpty() || w[1].isEmpty();
        if (noEn) { t[1] = ""; w[1] = ""; }
        else if (!GameDataCheck.text(t[1], GameDataCheck.TEXT_MAX) || !oneBlank(t[1]) || !GameDataCheck.text(w[1], WORD_MAX) || arabic(t[1] + w[1])) { t[1] = ""; w[1] = ""; }   // no real English: an Arabic-only item
        return new Item((String) m.get("i"), "f".equals(k) ? FACT : SAYING, d, a == 1, t, w);
    }

    private static String[] pair(Object o) {
        if (!(o instanceof List) || ((List<?>) o).size() != 2) return null;
        Object a = ((List<?>) o).get(0), b = ((List<?>) o).get(1);
        return a instanceof String && b instanceof String ? new String[]{(String) a, (String) b} : null;
    }
    static boolean oneBlank(String s) { int i = s.indexOf(BLANK); return i >= 0 && s.indexOf(BLANK, i + BLANK.length()) < 0; }
    private static boolean arabic(String s) { for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); if (c >= 0x0600 && c <= 0x06FF) return true; } return false; }

    // ---------------------------------------------------------------- the picking

    static boolean matches(Item it, String deck, boolean kids, boolean en) {
        return (MIX.equals(deck) || it.deck.equals(deck)) && (!kids || it.kids) && (!en || it.hasEnglish());
    }
    static int available(List<Item> all, String deck, boolean kids, boolean en) { int n = 0; for (Item it : all) if (matches(it, deck, kids, en)) n++; return n; }
    /** a deck chip is offered when it can fill the longest game twice over (so two games in a row differ); «مزيج» whenever anything matches */
    static boolean offered(List<Item> all, String deck, boolean kids, boolean en) {
        int n = available(all, deck, kids, en);
        return MIX.equals(deck) ? n > 0 : n >= 2 * COUNTS[COUNTS.length - 1];
    }

    /** `count` items, none twice, the ones not asked lately first; «مزيج» takes turns between the decks that have items */
    static List<Item> pick(List<Item> all, String deck, boolean kids, boolean en, int count, Set<String> recent, Random rnd) {
        List<Item> fresh = new ArrayList<>(), old = new ArrayList<>();
        for (Item it : all) if (matches(it, deck, kids, en)) (recent != null && recent.contains(it.id) ? old : fresh).add(it);
        Collections.shuffle(fresh, rnd); Collections.shuffle(old, rnd);
        fresh.addAll(old);   // an item asked lately comes back only when nothing else is left
        if (!MIX.equals(deck)) return new ArrayList<>(fresh.subList(0, Math.min(count, fresh.size())));
        List<Item> out = new ArrayList<>();
        String want = rnd.nextBoolean() ? FACT : SAYING;
        while (out.size() < count && !fresh.isEmpty()) {
            int at = -1;
            for (int i = 0; i < fresh.size(); i++) if (fresh.get(i).deck.equals(want)) { at = i; break; }
            out.add(fresh.remove(Math.max(0, at)));
            want = FACT.equals(want) ? SAYING : FACT;
        }
        return out;
    }

    // ---------------------------------------------------------------- the rules

    /**
     * One game. A turn: the statement with its blank is shown to the team whose turn it is; it says the word aloud (a hint, once, shows the first letter
     * and halves the points); OK shows the word, and the host says how it went: right (10, or 5 after the hint), stolen (only with «الخطف» on and two or
     * more teams: the NEXT team said the right word before the reveal, 5 to it), or wrong (0). Then the next team's turn with the next statement.
     */
    static final class Engine {
        static final int RIGHT = 10, HINT_COST = 5, STEAL = 5;
        static final int WRONG = 0, CORRECT = 1, STOLEN = 2;

        enum Phase { ASK, SHOWN, END }

        final GameScore score; final List<Item> items; final boolean steal;
        Phase phase; int index, turn, lastPoints, lastSide = -1; boolean hinted;

        Engine(int teams, List<Item> items, boolean steal) {
            this.score = new GameScore(teams); this.items = new ArrayList<>(items); this.steal = steal && score.n > 1;
            phase = this.items.isEmpty() ? Phase.END : Phase.ASK;
        }

        Item current() { return phase == Phase.END || index >= items.size() ? null : items.get(index); }
        int total() { return items.size(); }
        boolean canSteal() { return steal; }
        int stealer() { return score.after(turn); }
        /** what a right answer is worth now */
        int worth() { return hinted ? RIGHT - HINT_COST : RIGHT; }

        /** «تلميح»: once per statement, before the word is shown */
        boolean hint() { if (phase != Phase.ASK || hinted) return false; hinted = true; return true; }
        /** OK: the word is shown */
        void reveal() { if (phase == Phase.ASK) phase = Phase.SHOWN; }

        /** the host's verdict, then the next statement (or the end) */
        void judge(int verdict) {
            if (phase != Phase.SHOWN) return;
            score.newRound(); lastPoints = 0; lastSide = -1;
            if (verdict == CORRECT) { lastPoints = worth(); lastSide = turn; }
            else if (verdict == STOLEN && steal) { lastPoints = STEAL; lastSide = stealer(); }
            if (lastSide >= 0) score.add(lastSide, lastPoints);
            index++; hinted = false; turn = score.after(turn);
            phase = index >= items.size() ? Phase.END : Phase.ASK;
        }

        /** «إنهاء الجولة» on the pause card */
        void finish() { phase = Phase.END; }
    }
}
