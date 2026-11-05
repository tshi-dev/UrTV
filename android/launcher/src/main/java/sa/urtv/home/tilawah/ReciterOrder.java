package sa.urtv.home.tilawah;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The reciters tab's order and its letter index (step 27, item 20): the favourite reciters first under their own heading, then the rest, each in the
 * catalogue's order; and a column of letters that jumps to the first reciter whose name starts with the letter (a letter no reciter starts with is dimmed
 * and skipped). Arabic letters in Arabic, Latin letters for the English names. No Android in here: tests/ReciterOrderRun.java runs it.
 */
final class ReciterOrder {
    private ReciterOrder() { }

    /** a row of the tab: a heading, or a reciter (its index in the catalogue, and whether it is a favourite) */
    static final class Item {
        final boolean header;
        final int reciter;
        final boolean fav;
        Item(boolean header, int reciter, boolean fav) { this.header = header; this.reciter = reciter; this.fav = fav; }
    }

    static final String[] ARABIC = {"ا", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر", "ز", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ك", "ل", "م", "ن", "ه", "و", "ي"};
    static final String[] LATIN = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    /**
     * The rows: with favourites, a heading and then them (in the catalogue's order), then the rest (not repeated); without any, just the reciters.
     * @param ids the reciters' ids in the catalogue's order; favs the ids marked ★ (an unknown id is ignored)
     */
    static List<Item> order(List<String> ids, Set<String> favs) {
        List<Item> out = new ArrayList<>();
        boolean any = false;
        for (String id : ids) if (favs != null && favs.contains(id)) any = true;
        if (any) {
            out.add(new Item(true, -1, false));
            for (int i = 0; i < ids.size(); i++) if (favs.contains(ids.get(i))) out.add(new Item(false, i, true));
        }
        for (int i = 0; i < ids.size(); i++) if (favs == null || !favs.contains(ids.get(i))) out.add(new Item(false, i, false));
        return out;
    }

    /** the letter a name starts with, in the alphabet of its script: marks and the tatweel dropped, alef forms one alef, ى one yeh, ة one heh; "" when it starts with no letter */
    static String letterOf(String name) {
        if (name == null) return "";
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= 0x064B && c <= 0x065F || c == 0x0640 || c == 0x0670) continue;   // harakat, shadda, tatweel, dagger alef
            if (c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ') return "ا";
            if (c == 'ى') return "ي";
            if (c == 'ة') return "ه";
            if (c >= 0x0621 && c <= 0x064A) return String.valueOf(c);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) return String.valueOf(Character.toUpperCase(c));
            if (Character.isLetterOrDigit(c)) return "";
        }
        return "";
    }

    /** the alphabet for the names' script: Latin for the English names (lang en), else Arabic */
    static String[] alphabet(boolean latin) { return latin ? LATIN : ARABIC; }

    /** for each letter of the alphabet: does any of these names start with it (a dimmed letter is false) */
    static boolean[] present(String[] alphabet, List<String> names) {
        boolean[] out = new boolean[alphabet.length];
        for (String n : names) {
            String l = letterOf(n);
            for (int i = 0; i < alphabet.length; i++) if (alphabet[i].equals(l)) out[i] = true;
        }
        return out;
    }

    /** the position (in the rows) of the first reciter whose name starts with the letter, else -1 */
    static int jump(List<Item> rows, List<String> names, String letter) {
        for (int p = 0; p < rows.size(); p++) {
            Item it = rows.get(p);
            if (!it.header && letter.equals(letterOf(names.get(it.reciter)))) return p;
        }
        return -1;
    }

    /** the next letter that has a reciter, from index {@code from} in direction {@code dir} (+1 / -1); the same index when there is none that way (dimmed letters are skipped) */
    static int nextPresent(boolean[] present, int from, int dir) {
        for (int i = from + dir; i >= 0 && i < present.length; i += dir) if (present[i]) return i;
        return from;
    }

    /** the favourites after a toggle: the id added or taken out, as a comma-separated list kept in catalogue order for a stable file */
    static String toggle(String saved, String id, boolean on) {
        List<String> l = new ArrayList<>();
        if (saved != null) for (String x : saved.split(",")) { x = x.trim(); if (!x.isEmpty() && !x.equals(id) && !l.contains(x)) l.add(x); }
        if (on) l.add(id);
        return String.join(",", l);
    }

    /** the ids in a saved list; only plain ids (letters, digits, dash, underscore) are kept */
    static List<String> parse(String saved) {
        List<String> l = new ArrayList<>();
        if (saved != null) for (String x : saved.split(",")) { x = x.trim(); if (x.matches("[A-Za-z0-9_-]{1,60}") && !l.contains(x)) l.add(x); }
        return l;
    }
}
