package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The English of a text the settings built in Arabic (Words.of, «اللغة» English only; docs/ENGLISH.md). The settings make their words in
 * Arabic as they always did, and the few places that draw them (a row, a button, a header, a dialog, a toast) ask here, so the Arabic code
 * and its tests stay as they are. In this order: the whole text in the table; a sentence with values in it (one of the patterns, «تمت
 * متابعة %s»: each value in English too); its parts («يعمل · …», «أ، ب»: each part, and the English separator); a name with a bracket after
 * it («الأسواق (٢/٤)»); a place name (Places). Anything else stays as it is (a club, a show, a city of one's own: data). A digit written
 * either way finds the entry (the screen may have turned them already). Plain Java: tests/WordMatchRun.java runs it.
 */
final class WordMatch {
    /** the table, as the app reads it */
    interface Table {
        /** the English of an Arabic text with these values filled in, null when the table has no entry for it */
        String en(String ar, Object... args);
        /** a place's English (Places.name), null for none */
        String name(String ar);
    }

    /** an Arabic letter (not a digit: «٣» alone stays as it is, conv turns it) */
    private static final Pattern ARABIC = Pattern.compile("[\\u0600-\\u065f\\u066a-\\u06ef\\u06fa-\\u06ff]");
    private static final Pattern BRACKET = Pattern.compile("(.+?) +\\(([^()]+)\\)");
    /** Arabic separators and their English, the widest first */
    private static final String[][] PARTS = {{"  •  ", "  •  "}, {"  ·  ", "  ·  "}, {" · ", " · "}, {"؛ ", "; "}, {"، ", ", "}};
    /** a value inside a pattern: any text (put into English itself), but never one that starts with a bracket («الأسواق (٢/٤)» is a name) */
    private static final String VALUE = "((?!\\().+?)";

    private final Table table;
    private final String[] patterns;
    private final Pattern[] regex;

    WordMatch(Table table, String[] patterns) {
        this.table = table;
        this.patterns = patterns.clone();
        regex = new Pattern[patterns.length];
        for (int i = 0; i < patterns.length; i++) regex[i] = compile(patterns[i]);
    }

    /** «تمت متابعة %s» as a regular expression: the words as they are, each %s one value */
    static Pattern compile(String pattern) {
        StringBuilder b = new StringBuilder();
        String[] bits = pattern.split("%s", -1);
        for (int i = 0; i < bits.length; i++) {
            if (i > 0) b.append(VALUE);
            if (!bits[i].isEmpty()) b.append(Pattern.quote(bits[i]));
        }
        return Pattern.compile(b.toString(), Pattern.DOTALL);
    }

    String of(String s) { return of(s, 0); }

    private String of(String s, int depth) {
        if (s == null || s.isEmpty() || depth > 8 || !ARABIC.matcher(s).find()) return s;
        String e = exact(s);
        if (e != null) return e;
        if (s.indexOf('\n') >= 0) {   // lines one under the other (the version lines): each on its own
            List<String> out = new ArrayList<>();
            for (String line : s.split("\n", -1)) out.add(of(line, depth + 1));
            return join(out, "\n");
        }
        Matcher b = BRACKET.matcher(s);   // a known name and its count: «الفرق المتابعة (٣/٢٠)», never «الفرق %s»
        if (b.matches() && !ARABIC.matcher(b.group(2)).find() && exact(b.group(1)) != null) return of(b.group(1), depth + 1) + " (" + of(b.group(2), depth + 1) + ")";
        for (int i = 0; i < patterns.length; i++) {
            Matcher m = regex[i].matcher(s);
            if (!m.matches()) continue;
            Object[] args = new Object[m.groupCount()];
            for (int g = 0; g < args.length; g++) args[g] = of(m.group(g + 1), depth + 1);
            String r = table.en(patterns[i], args);
            if (r != null) return r;
        }
        for (String[] sep : PARTS) {
            if (s.indexOf(sep[0]) <= 0) continue;
            List<String> out = new ArrayList<>();
            for (String part : s.split(Pattern.quote(sep[0]), -1)) out.add(of(part, depth + 1));
            return join(out, sep[1]);
        }
        if (b.matches()) return of(b.group(1), depth + 1) + " (" + of(b.group(2), depth + 1) + ")";
        String n = table.name(s.trim());
        return n != null ? n : s;
    }

    /** the whole text's entry; a text whose digits were turned to 0-9 already finds the entry written with ٠-٩ */
    private String exact(String s) {
        String e = table.en(s);
        if (e != null) return e;
        String hindi = DigitsText.convert(s, false);
        return hindi.equals(s) ? null : table.en(hindi);
    }

    private static String join(List<String> v, String sep) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < v.size(); i++) { if (i > 0) b.append(sep); b.append(v.get(i)); }
        return b.toString();
    }
}
