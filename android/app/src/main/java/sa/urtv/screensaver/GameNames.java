package sa.urtv.screensaver;

/**
 * The names the people playing give their teams and themselves (no Android: tests/GamesKitTest.java). Written on the TV (or a phone), kept on this TV for next
 * time («أسماء جديدة» forgets them), never sent anywhere. An empty slot falls back to the game's suggestion («الصقور»، «لاعب 1»). At most 14 characters, letters,
 * digits and single spaces, nothing that reads as a link. Also the on-screen keyboard's layout and cursor.
 */
final class GameNames {
    private GameNames() {}

    static final int MAX = 14, SLOTS = 12;

    /** the name as it will be kept and shown: letters, digits and single spaces, at most MAX characters, no link; "" when nothing is left */
    static String clean(String raw) {
        if (raw == null) return "";
        StringBuilder b = new StringBuilder();
        int n = 0;
        for (int i = 0; i < raw.length() && n < MAX; ) {
            int cp = raw.codePointAt(i); i += Character.charCount(cp);
            if (Character.isLetterOrDigit(cp)) { b.appendCodePoint(cp); n++; }
            else if (cp == ' ' && b.length() > 0 && b.charAt(b.length() - 1) != ' ') { b.append(' '); n++; }
        }
        String s = b.toString().trim(), low = s.toLowerCase(java.util.Locale.ROOT);
        return low.contains("http") || low.contains("www") ? "" : s;
    }

    /** the names of `defaults.length` slots: the saved name where there is one, else the suggestion; two the same get a number so they can be told apart */
    static String[] resolve(String[] saved, String[] defaults, boolean latin) {
        String[] v = new String[defaults.length];
        for (int i = 0; i < v.length; i++) { String s = saved != null && i < saved.length ? clean(saved[i]) : ""; v[i] = s.isEmpty() ? defaults[i] : s; }
        for (int i = 1; i < v.length; i++) {
            int k = 1; String cand = v[i];
            while (taken(v, i, cand)) { k++; cand = v[i] + " " + DigitsText.convert(String.valueOf(k), latin); }
            v[i] = cand;
        }
        return v;
    }
    private static boolean taken(String[] v, int upTo, String s) { for (int i = 0; i < upTo; i++) if (v[i].equals(s)) return true; return false; }

    /** one string for the preferences: the slots, one per line (a name has no line end: clean() drops it) */
    static String join(String[] v) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < v.length && i < SLOTS; i++) b.append(i > 0 ? "\n" : "").append(clean(v[i]));
        return b.toString();
    }
    static String[] split(String s) {
        String[] v = new String[SLOTS];
        java.util.Arrays.fill(v, "");
        if (s != null && !s.isEmpty()) { String[] p = s.split("\n", -1); for (int i = 0; i < p.length && i < SLOTS; i++) v[i] = clean(p[i]); }
        return v;
    }

    // ---------------------------------------------------------------- the keyboard

    /** the letter rows of the two languages; the last row is the keys that do things */
    static final String[] AR = {"ضصثقفغعهخحج", "شسيبلاتنمكط", "ئءؤرىةوزظذد"}, EN = {"qwertyuiop", "asdfghjkl'", "zxcvbnm-.&"}, DIGITS = {"1234567890"};
    static final int SPACE = 0, BACK = 1, DONE = 2, LANG = 3, ACTIONS = 4;

    /** the cursor over the keys of one language: rows of letters, a row of digits, a row of 4 actions */
    static final class Pad {
        final String[] rows; int row, col;
        Pad(boolean arabic) {
            String[] l = arabic ? AR : EN;
            rows = new String[] {l[0], l[1], l[2], DIGITS[0], "    "};   // the last row is the 4 actions
            row = 0; col = 0;
        }
        int cols(int r) { return rows[r].length(); }
        int rowCount() { return rows.length; }
        boolean isAction(int r) { return r == rows.length - 1; }
        /** the character under (r, c) or -1 for an action row */
        char ch(int r, int c) { return rows[r].charAt(c); }
        /** a step: dx toward the next key of the line, dy toward the next row; the column keeps its relative place between rows */
        void move(int dx, int dy) {
            if (dy != 0) {
                int to = Math.max(0, Math.min(rows.length - 1, row + dy));
                if (to != row) { col = Math.min(cols(to) - 1, (int) Math.round(col * (cols(to) - 1) / (double) Math.max(1, cols(row) - 1))); row = to; }
            } else if (dx != 0) col = Math.max(0, Math.min(cols(row) - 1, col + dx));
        }
        /** press: the text after it. An action key does its action: DONE and LANG are told to the caller by the return of `action()` */
        String press(String text) {
            if (isAction(row)) { return col == BACK ? backspace(text) : col == SPACE ? add(text, ' ') : text; }
            return add(text, ch(row, col));
        }
        int action() { return isAction(row) ? col : -1; }
    }
    /** one more character: a space only after a letter, nothing past MAX */
    static String add(String text, char c) {
        if (text.length() >= MAX) return text;
        if (c == ' ') return text.isEmpty() || text.endsWith(" ") ? text : text + " ";
        return text + c;
    }
    static String backspace(String text) { return text.isEmpty() ? text : text.substring(0, text.offsetByCodePoints(text.length(), -1)); }
}
