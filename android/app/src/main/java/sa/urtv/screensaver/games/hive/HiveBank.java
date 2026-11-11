package sa.urtv.screensaver.games.hive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * «خلية الحروف»: the questions by first letter (no Android). GamesBoardData fills it from the published files (docs/GAMES-DATA.md): the quiz bank's items and
 * the letter index of letter-hive.json (the index already skips «ال» and folds the hamza forms, the game only reads it). A question sits under its Arabic
 * letter, and under its English one when it can be played in English. The hints are sample answers for the category question of a letter.
 */
public final class HiveBank {
    public static final String AR = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي", EN = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final int ITEMS_MAX = 5000, HINTS_MAX = 6;

    public static final class Item {
        public final String id; public final char ar, en; public final int level; public final boolean kids;
        public final String[] q, a; public final String[][] w;
        Item(String id, char ar, char en, int level, boolean kids, String[] q, String[] a, String[][] w) {
            this.id = id; this.ar = ar; this.en = en; this.level = level; this.kids = kids; this.q = q; this.a = a; this.w = w;
        }
    }

    private final Map<Character, List<Item>> byLetter = new HashMap<>();
    private final Map<Character, List<String>> hints = new HashMap<>();
    private final Set<String> ids = new HashSet<>();

    public int size() { return ids.size(); }
    /** the questions whose answer starts with this letter (an Arabic letter, or a Latin capital in an English game) */
    public List<Item> of(char letter) { List<Item> v = byLetter.get(letter); return v == null ? Collections.<Item>emptyList() : v; }
    /** sample answers for «اذكر دولة أو مدينة أو حيوانًا يبدأ بحرف…», empty when we have none */
    public String[] hints(char letter) { List<String> v = hints.get(letter); return v == null ? new String[0] : v.toArray(new String[0]); }

    /** one question; ar or en may be 0 (not played in that language), not both. false: not a question this bank can hold (a repeated id, a letter outside the alphabets) */
    public boolean add(String id, char ar, char en, int level, boolean kids, String[] q, String[] a, String[][] w) {
        if (id == null || (ar == 0 && en == 0) || (ar != 0 && AR.indexOf(ar) < 0) || (en != 0 && EN.indexOf(en) < 0) || level < 1 || level > 3) return false;
        if (q == null || q.length != 2 || a == null || a.length != 2 || w == null || w.length != 3 || ids.size() >= ITEMS_MAX || !ids.add(id)) return false;
        Item it = new Item(id, ar, en, level, kids, q, a, w);
        if (ar != 0) put(ar, it);
        if (en != 0) put(en, it);
        return true;
    }

    /** one sample answer for a letter's category question; the first six of a letter are kept, the rest and the ones of no letter are dropped */
    public void hint(char letter, String sample) {
        if ((AR + EN).indexOf(letter) < 0 || sample == null || sample.isEmpty()) return;
        List<String> v = hints.get(letter);
        if (v == null) hints.put(letter, v = new ArrayList<>());
        if (v.size() < HINTS_MAX && !v.contains(sample)) v.add(sample);
    }

    private void put(char letter, Item it) {
        List<Item> v = byLetter.get(letter);
        if (v == null) byLetter.put(letter, v = new ArrayList<>());
        v.add(it);
    }
}
