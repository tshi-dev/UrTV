package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * games/meta.json (games/SCHEMA.md on branch work/games-data; no Android, tested on a plain JVM): the names the setup screens build their chips from, so a category,
 * a deck or a game variant added to the data shows up on its own, with no app update. The reader is TOLERANT by rule: unknown fields are skipped at every level; an
 * entry without a usable id or without both names is skipped, never the whole file; a file whose "v" is not 1 is refused (the app keeps its built-in lists). Which
 * chip is OFFERED is decided from the bank that is really on the TV (QuizPick.offered: 40 or more items), not from the counts in the file.
 */
final class GamesMeta {

    static final int BYTES_MAX = 200_000;
    private static final java.util.regex.Pattern ID = java.util.regex.Pattern.compile("[a-z\\-]{1,24}");

    /** a quiz chip: its key (a group id, or a category id that is in no group), Arabic and English names, and the bank categories it covers */
    static final class Chip {
        final String key, ar, en; final Set<String> cats;
        Chip(String key, String ar, String en, Set<String> cats) { this.key = key; this.ar = ar; this.en = en; this.cats = cats; }
    }

    private final List<Chip> chips = new ArrayList<>();
    final int minItems;

    private GamesMeta(int minItems) { this.minItems = minItems; }

    /** the chips of «الفئة» (without «مزيج», which the app always has), in the file's order: the groups, then every category that is in no group, under its own name */
    List<Chip> quizChips() { return Collections.unmodifiableList(chips); }

    /** key -> the bank categories it covers, for QuizPick */
    Map<String, Set<String>> quizGroups() {
        Map<String, Set<String>> m = new HashMap<>();
        for (Chip c : chips) m.put(c.key, c.cats);
        return m;
    }

    /** the parsed file, or null when it is not a games meta file at all (the caller keeps its built-in lists) */
    static GamesMeta parse(byte[] raw) {
        if (raw != null && raw.length > BYTES_MAX) return null;
        Object root = GameDataCheck.root(raw, '{');
        if (root == null) return null;
        Map<?, ?> m = (Map<?, ?>) root;
        if (GameDataCheck.whole(m.get("v"), 1, 1) != 1) return null;
        int min = GameDataCheck.whole(m.get("min_items"), 1, 1000);
        GamesMeta out = new GamesMeta(min == Integer.MIN_VALUE ? QuizPick.MIN_ITEMS : min);
        Map<String, String[]> cats = new LinkedHashMap<>();
        Object games = m.get("games");
        Object quiz = games instanceof Map ? ((Map<?, ?>) games).get("quiz") : null;
        Object cl = quiz instanceof Map ? ((Map<?, ?>) quiz).get("cats") : null;
        if (cl instanceof List) for (Object o : (List<?>) cl) { String[] c = named(o); if (c != null) cats.put(c[0], c); }
        Set<String> grouped = new HashSet<>();
        Object gl = m.get("groups");
        if (gl instanceof List) for (Object o : (List<?>) gl) {
            String[] g = named(o);
            Object gc = ((Map<?, ?>) o).get("cats");
            if (g == null || !(gc instanceof List)) continue;
            Set<String> set = new HashSet<>();
            for (Object x : (List<?>) gc) if (x instanceof String && ID.matcher((String) x).matches()) set.add((String) x);
            if (set.isEmpty()) continue;
            grouped.addAll(set);
            out.chips.add(new Chip(g[0], g[1], g[2], set));
        }
        Set<String> keys = new HashSet<>();
        for (Chip c : out.chips) keys.add(c.key);
        for (String[] c : cats.values()) if (!grouped.contains(c[0]) && keys.add(c[0])) out.chips.add(new Chip(c[0], c[1], c[2], Collections.singleton(c[0])));   // a category new in the data
        return out.chips.isEmpty() ? null : out;
    }

    /** {id, ar, en} when the entry has a plain id and both names, else null */
    private static String[] named(Object o) {
        if (!(o instanceof Map)) return null;
        Map<?, ?> m = (Map<?, ?>) o;
        Object id = m.get("id"), ar = m.get("ar"), en = m.get("en");
        if (!(id instanceof String) || !ID.matcher((String) id).matches()) return null;
        if (!GameDataCheck.text(ar, 40) || !GameDataCheck.text(en, 40)) return null;
        return new String[] {(String) id, (String) ar, (String) en};
    }
}
