package sa.urtv.screensaver;

/**
 * 4.4 couch-tv's rows (CouchStyle): how one of our values is shown. A short one at the row's far end; a long one on a line under the name; an
 * on/off one is the toggle's ring, its words after «مفعّل» or «متوقف» under the name, without the «·» or the brackets around them. Plain Java
 * (tests/settings_couch_test.py runs it).
 */
final class CouchText {
    private CouchText() { }

    /** at most this many characters at the row's far end; a longer value goes under the name */
    static final int SHORT = 26;

    static boolean short_(String v) { return v != null && !v.isEmpty() && v.length() <= SHORT && v.indexOf('\n') < 0; }

    /** the words after the state word: «مفعّل (أفق الليل من المغرب إلى الفجر)» gives «أفق الليل من المغرب إلى الفجر»; nothing when there are none */
    static String after(String v, int wordEnd) {
        if (v == null || wordEnd >= v.length()) return "";
        String s = v.substring(Math.max(0, wordEnd)).trim();
        while (s.startsWith("·") || s.startsWith("•") || s.startsWith("،") || s.startsWith(":")) s = s.substring(1).trim();
        if (s.startsWith("(") && s.endsWith(")")) s = s.substring(1, s.length() - 1).trim();
        return s;
    }
}
