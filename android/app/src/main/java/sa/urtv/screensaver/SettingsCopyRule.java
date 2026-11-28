package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 4.4 (a report: «a reinstall of the screensaver reset its preferences»): the settings file as text, so the home can keep a copy and a fresh install
 * can take it back (SettingsCopy). One line per value, «type TAB key TAB value» (a set: its items after the key), with TAB, newline and backslash
 * escaped. Plain Java (tests/SettingsCopyRun.java). Never in the copy: the update channel (a test build is an agent's choice) and the memory-tier
 * debug switch; nothing of the home's (the parental PIN is the home's, never here).
 */
final class SettingsCopyRule {
    private SettingsCopyRule() { }

    static final int MAX_CHARS = 256 * 1024;
    /** keys that never go in the copy and are never taken from one */
    static final Set<String> NEVER = new LinkedHashSet<>(java.util.Arrays.asList("updateChannel", "ramtier"));

    static String encode(Map<String, ?> all) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, ?> e : new java.util.TreeMap<String, Object>(all).entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (k == null || NEVER.contains(k) || v == null) continue;
            String t = v instanceof Boolean ? "b" : v instanceof Integer ? "i" : v instanceof Long ? "l" : v instanceof Float ? "f" : v instanceof String ? "s" : v instanceof Set ? "S" : null;
            if (t == null) continue;
            b.append(t).append('\t').append(esc(k));
            if (v instanceof Set) { for (Object x : (Set<?>) v) if (x != null) b.append('\t').append(esc(x.toString())); }
            else b.append('\t').append(esc(v.toString()));
            b.append('\n');
        }
        return b.toString();
    }

    /** the values of a copy; null when it is not one (empty, too big, a line that does not read): a bad copy is never taken in part */
    static Map<String, Object> decode(String text) {
        if (text == null || text.isEmpty() || text.length() > MAX_CHARS) return null;
        Map<String, Object> out = new LinkedHashMap<>();
        for (String line : text.split("\n", -1)) {
            if (line.isEmpty()) continue;
            String[] f = line.split("\t", -1);
            if (f.length < 2) return null;
            String k = unesc(f[1]);
            if (k == null || k.isEmpty()) return null;
            if (NEVER.contains(k)) continue;
            try {
                switch (f[0]) {
                    case "b": if (f.length != 3 || !(f[2].equals("true") || f[2].equals("false"))) return null; out.put(k, Boolean.valueOf(f[2])); break;
                    case "i": if (f.length != 3) return null; out.put(k, Integer.valueOf(f[2])); break;
                    case "l": if (f.length != 3) return null; out.put(k, Long.valueOf(f[2])); break;
                    case "f": if (f.length != 3) return null; out.put(k, Float.valueOf(f[2])); break;
                    case "s": { if (f.length != 3) return null; String s = unesc(f[2]); if (s == null) return null; out.put(k, s); break; }
                    case "S": { Set<String> s = new LinkedHashSet<>(); for (int i = 2; i < f.length; i++) { String x = unesc(f[i]); if (x == null) return null; s.add(x); } out.put(k, s); break; }
                    default: return null;
                }
            } catch (NumberFormatException e) { return null; }
        }
        return out.isEmpty() ? null : out;
    }

    /** a fresh install takes the copy back: only when nothing of its own was saved yet (at most the keys a first start writes by itself) and only once */
    static boolean shouldRestore(Set<String> ownKeys, boolean triedBefore) {
        if (triedBefore) return false;
        for (String k : ownKeys) if (!k.equals("tournaments")) return false;   // Prefs writes «tournaments» on the first start (4.3.1): the only key of its own that does not count
        return true;
    }

    /** the home keeps a new copy only when it is a real one; an empty or broken answer never wipes the kept copy */
    static boolean worthKeeping(String incoming, String kept) {
        return decode(incoming) != null && !incoming.equals(kept);
    }

    static String esc(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            if (ch == '\\') b.append("\\\\"); else if (ch == '\t') b.append("\\t"); else if (ch == '\n') b.append("\\n"); else if (ch == '\r') b.append("\\r"); else b.append(ch);
        }
        return b.toString();
    }

    static String unesc(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\\') { b.append(ch); continue; }
            if (++i >= s.length()) return null;
            char n = s.charAt(i);
            if (n == '\\') b.append('\\'); else if (n == 't') b.append('\t'); else if (n == 'n') b.append('\n'); else if (n == 'r') b.append('\r'); else return null;
        }
        return b.toString();
    }
}
