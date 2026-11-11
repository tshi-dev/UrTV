package sa.urtv.screensaver.games.hive;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * «خلية الحروف»: the questions by first letter (no Android). The file is made by .github/tools/make_hive_bank.py from the verified quiz bank and the letter
 * index of games/letter-hive (the index already skips «ال» and folds the hamza forms, the game only reads it): one line per question, 15 tab-separated fields
 *   id, Arabic letter, English letter (empty: not played in English), level 1..3, kids 0/1, question ar, en, answer ar, en, three wrong options (ar, en each).
 * The hints file has one line per letter: the letter, then sample answers for the category question. Like the quiz bank a file is checked whole: one bad line
 * rejects it (parse gives null) and the caller keeps the last good copy.
 */
public final class HiveBank {
    public static final String AR = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي", EN = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final int BYTES_MAX = 1_500_000, ITEMS_MAX = 5000, TEXT_MAX = 200, HINTS_MAX = 12;

    public static final class Item {
        public final String id; public final char ar, en; public final int level; public final boolean kids;
        public final String[] q, a; public final String[][] w;
        Item(String id, char ar, char en, int level, boolean kids, String[] q, String[] a, String[][] w) {
            this.id = id; this.ar = ar; this.en = en; this.level = level; this.kids = kids; this.q = q; this.a = a; this.w = w;
        }
    }

    private final Map<Character, List<Item>> byLetter = new HashMap<>();
    private final Map<Character, String[]> hints = new HashMap<>();
    private int size;

    private HiveBank() {}

    public int size() { return size; }
    /** the questions whose answer starts with this letter (an Arabic letter, or a Latin capital in an English game) */
    public List<Item> of(char letter) { List<Item> v = byLetter.get(letter); return v == null ? Collections.<Item>emptyList() : v; }
    /** sample answers for «اذكر دولة أو مدينة أو حيوانًا يبدأ بحرف…», empty when we have none */
    public String[] hints(char letter) { String[] v = hints.get(letter); return v == null ? new String[0] : v; }

    /** the bank, or null when anything in either file is wrong */
    public static HiveBank parse(byte[] items, byte[] hintLines) {
        if (items == null || items.length == 0 || items.length > BYTES_MAX || hintLines == null || hintLines.length > BYTES_MAX) return null;
        HiveBank bank = new HiveBank();
        Set<String> ids = new HashSet<>();
        for (String line : new String(items, StandardCharsets.UTF_8).split("\n")) {
            if (line.isEmpty()) continue;
            String[] f = line.split("\t", -1);
            if (f.length != 15 || !f[0].matches("[A-Za-z0-9._:\\-]{1,64}") || !ids.add(f[0]) || ids.size() > ITEMS_MAX) return null;
            if (f[1].length() != 1 || AR.indexOf(f[1].charAt(0)) < 0 || f[2].length() > 1 || (f[2].length() == 1 && EN.indexOf(f[2].charAt(0)) < 0)) return null;
            if (!f[3].matches("[123]") || !f[4].matches("[01]")) return null;
            for (int i = 5; i < 15; i++) if (!text(f[i])) return null;
            for (int lang = 0; lang < 2; lang++) {   // the four options differ in each language
                Set<String> opts = new HashSet<>();
                for (int i = 7 + lang; i < 15; i += 2) opts.add(f[i]);
                if (opts.size() != 4) return null;
            }
            Item it = new Item(f[0], f[1].charAt(0), f[2].isEmpty() ? 0 : f[2].charAt(0), f[3].charAt(0) - '0', f[4].equals("1"), new String[] {f[5], f[6]},
                new String[] {f[7], f[8]}, new String[][] {{f[9], f[10]}, {f[11], f[12]}, {f[13], f[14]}});
            bank.add(it.ar, it);
            if (it.en != 0) bank.add(it.en, it);
            bank.size++;
        }
        if (bank.size == 0) return null;
        for (String line : new String(hintLines, StandardCharsets.UTF_8).split("\n")) {
            if (line.isEmpty()) continue;
            String[] f = line.split("\t", -1);
            if (f.length < 2 || f.length > HINTS_MAX + 1 || f[0].length() != 1 || (AR + EN).indexOf(f[0].charAt(0)) < 0 || bank.hints.containsKey(f[0].charAt(0))) return null;
            String[] v = new String[f.length - 1];
            for (int i = 1; i < f.length; i++) { if (!text(f[i])) return null; v[i - 1] = f[i]; }
            bank.hints.put(f[0].charAt(0), v);
        }
        return bank;
    }

    private void add(char letter, Item it) {
        List<Item> v = byLetter.get(letter);
        if (v == null) byLetter.put(letter, v = new ArrayList<>());
        v.add(it);
    }

    private static boolean text(String s) {
        if (s.isEmpty() || s.length() > TEXT_MAX) return false;
        for (int i = 0; i < s.length(); i++) if (Character.isISOControl(s.charAt(i))) return false;
        return true;
    }
}
