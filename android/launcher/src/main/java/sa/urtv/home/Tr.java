package sa.urtv.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * «اللغة» (docs/ENGLISH.md), the part with no Android in it (tests/TrRun.java runs it on a JVM): the setting's two values, the names of the
 * per-language files, the translation table (i18n/en/*.tsv, keyed by the Arabic text) and the placeholders. S is the Android side.
 * The same file is in both apps with its own package (tests/i18n_test.py compares them).
 *
 * Arabic, the default, never touches the table: t("ar", any, s) is s itself, and with arguments each %s (or %1$s…%9$s) is the
 * argument's String.valueOf, so S.t("بقي %s دقيقة", n) is byte for byte the "بقي " + n + " دقيقة" it replaces.
 */
public final class Tr {
    private Tr() { }

    public static final String AR = "ar", EN = "en";
    /** the values «اللغة» takes, the default first */
    public static final String[] LANGS = {AR, EN};

    /** what the receiver, the phone and the settings accept */
    public static boolean valid(String v) { return AR.equals(v) || EN.equals(v); }

    /** anything that is not "en" (null, empty, a value from a newer app) is Arabic, the default */
    public static String norm(String v) { return EN.equals(v) ? EN : AR; }

    /** «&lang=en» in the settings fragment, only for English: with Arabic the fragment, the page and the home are exactly what they were */
    public static String fragment(String lang) { return EN.equals(lang) ? "&lang=en" : ""; }

    /** the language a settings fragment carries ("city=..&lang=en&..."): Arabic when it says nothing, as every older screensaver */
    public static String fromFragment(String fragment) {
        if (fragment == null) return AR;
        for (String kv : fragment.split("&")) if (kv.equals("lang=en")) return EN;
        return AR;
    }

    /** The page's file: tv.html (Arabic) or tv-en.html, in the APK's assets, the download and dist/. */
    public static String pageName(String lang) { return EN.equals(lang) ? "tv-en.html" : "tv.html"; }

    /** The phone page's asset: phone/index.html (Arabic) or phone/index.en.html. */
    public static String phonePageName(String lang) { return EN.equals(lang) ? "phone/index.en.html" : "phone/index.html"; }

    /** The translation of ar in this language: ar itself for Arabic, for an unknown text or with no table; then the arguments put in. */
    public static String t(String lang, Map<String, String> table, String ar, Object... args) {
        String s = ar;
        if (EN.equals(lang) && table != null) {
            String en = table.get(ar);
            if (en != null) s = en;
        }
        return args == null || args.length == 0 ? s : format(s, args);
    }

    /**
     * %s takes the next argument, %1$s…%9$s the one by its number, %% is one %; any other % stays as it is. Only with arguments: a text
     * with no arguments is never read for them, so «٥٠%» passes untouched.
     */
    public static String format(String s, Object... args) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        int next = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '%' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 's') { out.append(next < args.length ? String.valueOf(args[next]) : "%s"); next++; i++; continue; }
                if (n == '%') { out.append('%'); i++; continue; }
                if (n >= '1' && n <= '9' && i + 3 < s.length() && s.charAt(i + 2) == '$' && s.charAt(i + 3) == 's') {
                    int k = n - '1';
                    out.append(k < args.length ? String.valueOf(args[k]) : s.substring(i, i + 4));
                    i += 3;
                    continue;
                }
            }
            out.append(ch);
        }
        return out.toString();
    }

    /** The arguments a text uses, by number from 1, sorted, each once: "%s و%s" and "%2$s and %1$s" both give [1, 2]. */
    public static List<Integer> placeholders(String s) {
        List<Integer> out = new ArrayList<>();
        int next = 0;
        for (int i = 0; i + 1 < s.length(); i++) {
            if (s.charAt(i) != '%') continue;
            char n = s.charAt(i + 1);
            int k = -1;
            if (n == 's') { k = ++next; i++; }
            else if (n == '%') i++;
            else if (n >= '1' && n <= '9' && i + 3 < s.length() && s.charAt(i + 2) == '$' && s.charAt(i + 3) == 's') { k = n - '0'; i += 3; }
            if (k > 0 && !out.contains(k)) out.add(k);
        }
        java.util.Collections.sort(out);
        return out;
    }

    /**
     * One table file: "arabic<TAB>english" per line; # starts a comment line, empty lines are skipped. In both fields \t, \n, \\ and
     * \\uXXXX are the character; an English «=» is the Arabic itself. A line without exactly one tab is skipped here (tests/i18n_test.py fails the build on it).
     */
    public static void parse(String text, Map<String, String> into) {
        for (String line : text.split("\n", -1)) {
            if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
            if (line.isEmpty() || line.startsWith("#")) continue;
            int tab = line.indexOf('\t');
            if (tab <= 0 || tab != line.lastIndexOf('\t') || tab == line.length() - 1) continue;
            String ar = unescape(line.substring(0, tab)), en = line.substring(tab + 1);
            into.put(ar, "=".equals(en) ? ar : unescape(en));   // «=»: this one stays Arabic (a verse, a club's name)
        }
    }

    public static Map<String, String> parse(String text) {
        Map<String, String> m = new HashMap<>();
        parse(text, m);
        return m;
    }

    public static String unescape(String s) {
        if (s.indexOf('\\') < 0) return s;
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\\' || i + 1 >= s.length()) { out.append(ch); continue; }
            char n = s.charAt(++i);
            if (n == 't') out.append('\t');
            else if (n == 'n') out.append('\n');
            else if (n == '\\') out.append('\\');
            else if (n == 'u' && i + 4 < s.length() && hex4(s, i + 1)) { out.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); i += 4; }
            else out.append('\\').append(n);
        }
        return out.toString();
    }

    private static boolean hex4(String s, int from) {
        for (int k = from; k < from + 4; k++) if (Character.digit(s.charAt(k), 16) < 0 || s.charAt(k) > 'f') return false;
        return true;
    }
}
