package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * What every game's data file must pass before the TV uses it (no Android, tests/GamesKitTest.java), the same idea as QuizBankCheck: not bigger than the
 * cap, really JSON of the expected kind (an HTML page, an error text or the wrong kind of root is refused), and then the game's own whole-file check
 * (a `Parser`): one bad item refuses the file, and the caller (GameData) keeps the last good copy or the bundled one.
 */
final class GameDataCheck {
    private GameDataCheck() {}

    static final int BYTES_MAX = 1_500_000, TEXT_MAX = 200;

    /** a game's own check: the parsed data, or null when anything at all is wrong */
    interface Parser<T> { T parse(byte[] raw); }

    /** the JSON root when the file is within the cap and starts with `first` ('[' a list, '{' an object); else null */
    static Object root(byte[] raw, char first) {
        if (raw == null || raw.length == 0 || raw.length > BYTES_MAX) return null;
        String text = new String(raw, StandardCharsets.UTF_8);
        int i = 0;
        while (i < text.length() && (Character.isWhitespace(text.charAt(i)) || text.charAt(i) == '﻿')) i++;
        if (i >= text.length() || text.charAt(i) != first) return null;
        Object root;
        try { root = MiniJson.parse(text); } catch (RuntimeException e) { return null; }
        return first == '[' ? (root instanceof List ? root : null) : (root instanceof Map ? root : null);
    }

    /** a shown text: 1 to `max` characters, no control character */
    static boolean text(Object o, int max) {
        if (!(o instanceof String)) return false;
        String s = (String) o;
        if (s.isEmpty() || s.length() > max) return false;
        for (int i = 0; i < s.length(); i++) if (Character.isISOControl(s.charAt(i))) return false;
        return true;
    }

    /** a whole number from `lo` to `hi` (MiniJson gives every number as a Double); else Integer.MIN_VALUE */
    static int whole(Object o, int lo, int hi) {
        if (!(o instanceof Double)) return Integer.MIN_VALUE;
        double d = (Double) o;
        return d == Math.rint(d) && d >= lo && d <= hi ? (int) d : Integer.MIN_VALUE;
    }

    /** a plain id: letters, digits, dot, dash, underscore, colon; 1 to 64 */
    static boolean id(Object o) { return o instanceof String && ((String) o).matches("[A-Za-z0-9._:\\-]{1,64}"); }
}
