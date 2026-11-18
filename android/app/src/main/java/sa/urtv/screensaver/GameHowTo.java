package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * «طريقة اللعب» (the panel behind the small «!» of a game's start screen, docs/GAMES-KIT.md; no Android, no S, tests/HowToTest.java): what the panel says about a game. Five
 * blocks: the goal, 1 to 8 numbered steps, what the phones change (empty when the game has no phone mode), the points, how it ends. A game's content comes from the registry's
 * HOWTO table (Arabic; the panel shows it through S.tv) or, for a game whose rules live in its data file, from an optional `howto` block of that file (goal, steps, phones,
 * scoring, end; each a text, or {"ar": "…", "en": "…"}), read by `fromJson` with the same limits.
 */
final class GameHowTo {
    private GameHowTo() {}

    static final int TEXT_MAX = 400, STEPS_MAX = 8;

    static final class Content {
        final String goal, phones, scoring, end; final List<String> steps;
        Content(String goal, List<String> steps, String phones, String scoring, String end) { this.goal = goal; this.steps = steps; this.phones = phones; this.scoring = scoring; this.end = end; }
        boolean hasPhones() { return !phones.isEmpty(); }
    }

    /** the content of a registry row `{id, goal, phones, scoring, end, step…}`; null for a row that is missing or has no goal or no step */
    static Content of(String[] row) {
        if (row == null || row.length < 6) return null;
        List<String> steps = new ArrayList<>();
        for (int i = 5; i < row.length && steps.size() < STEPS_MAX; i++) { String s = clean(row[i]); if (s != null) steps.add(s); }
        String goal = clean(row[1]);
        if (goal == null || steps.isEmpty()) return null;
        return new Content(goal, steps, orEmpty(clean(row[2])), orEmpty(clean(row[3])), orEmpty(clean(row[4])));
    }

    /** the content of a `howto` block of a data file (an object parsed by MiniJson), in the language asked; null when it has no goal or no steps, or when anything is of the wrong kind */
    static Content fromJson(Object node, boolean english) {
        if (!(node instanceof Map)) return null;
        Map<?, ?> m = (Map<?, ?>) node;
        String goal = text(m.get("goal"), english);
        Object st = m.get("steps");
        if (goal == null || !(st instanceof List)) return null;
        List<String> steps = new ArrayList<>();
        for (Object o : (List<?>) st) { if (steps.size() >= STEPS_MAX) break; String s = text(o, english); if (s != null) steps.add(s); }
        if (steps.isEmpty()) return null;
        return new Content(goal, steps, orEmpty(text(m.get("phones"), english)), orEmpty(text(m.get("scoring"), english)), orEmpty(text(m.get("end"), english)));
    }

    /** a text of a data file: a string, or {"ar", "en"} (the language asked, else the other one); null when empty, too long or of another kind */
    static String text(Object v, boolean english) {
        if (v instanceof String) return clean((String) v);
        if (v instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) v; Object a = m.get(english ? "en" : "ar"), b = m.get(english ? "ar" : "en");
            String s = a instanceof String ? clean((String) a) : null;
            return s != null ? s : b instanceof String ? clean((String) b) : null;
        }
        return null;
    }

    /** one to 400 characters, no control character (a newline becomes a space); anything else is no text */
    static String clean(String s) {
        if (s == null) return null;
        String t = s.replace('\n', ' ').replace('\r', ' ').trim();
        if (t.isEmpty() || t.length() > TEXT_MAX) return null;
        for (int i = 0; i < t.length(); i++) if (Character.isISOControl(t.charAt(i))) return null;
        return t;
    }
    private static String orEmpty(String s) { return s == null ? "" : s; }
}
