package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * «الأسرع»: what the TV accepts as a question bank (no Android, tested on a plain JVM). A downloaded file is checked whole before it is used: not bigger than
 * 1.5 MB, not an HTML page or anything but a JSON array, at most 5,000 items, every item of exactly the compact form (docs/QUIZ.md), every text 1 to 200
 * characters without control characters, four different options in each language, ids unique and plain. One bad item rejects the file (it is made by CI, a
 * bad one means a bad file); the caller then keeps the last good copy. The bundled starter set goes through the same check.
 */
final class QuizBankCheck {
    private QuizBankCheck() {}

    static final int BYTES_MAX = 1_500_000, ITEMS_MAX = 5000, TEXT_MAX = 200;

    /** the items, or null when anything is wrong */
    static List<QuizPick.Item> parse(byte[] raw) {
        if (raw == null || raw.length == 0 || raw.length > BYTES_MAX) return null;
        String text = new String(raw, StandardCharsets.UTF_8);
        int i = 0;
        while (i < text.length() && (Character.isWhitespace(text.charAt(i)) || text.charAt(i) == '﻿')) i++;
        if (i >= text.length() || text.charAt(i) != '[') return null;   // an HTML page, an error message, an object: not a bank
        Object root = MiniJson.parse(text);
        if (!(root instanceof List)) return null;
        List<?> all = (List<?>) root;
        if (all.isEmpty() || all.size() > ITEMS_MAX) return null;
        List<QuizPick.Item> out = new ArrayList<>(all.size());
        Set<String> ids = new HashSet<>();
        for (Object o : all) {
            QuizPick.Item it = item(o);
            if (it == null || !ids.add(it.id)) return null;
            out.add(it);
        }
        return out;
    }

    private static QuizPick.Item item(Object o) {
        if (!(o instanceof Map)) return null;
        Map<?, ?> m = (Map<?, ?>) o;
        if (m.size() != 7) return null;
        Object id = m.get("i"), cat = m.get("c"), d = m.get("d"), k = m.get("k");
        if (!(id instanceof String) || !((String) id).matches("[A-Za-z0-9._:\\-]{1,64}")) return null;
        if (!(cat instanceof String) || !((String) cat).matches("[a-z\\-]{1,24}")) return null;
        if (!(d instanceof Double) || !isInt((Double) d) || (Double) d < 1 || (Double) d > 3) return null;
        if (!(k instanceof Double) || !isInt((Double) k) || ((Double) k != 0 && (Double) k != 1)) return null;
        String[] q = pair(m.get("q")), a = pair(m.get("a"));
        if (q == null || a == null || !(m.get("w") instanceof List) || ((List<?>) m.get("w")).size() != 3) return null;
        String[][] w = new String[3][];
        for (int n = 0; n < 3; n++) { w[n] = pair(((List<?>) m.get("w")).get(n)); if (w[n] == null) return null; }
        for (int lang = 0; lang < 2; lang++) {   // the four options differ in each language
            Set<String> opts = new HashSet<>();
            opts.add(a[lang]);
            for (String[] x : w) opts.add(x[lang]);
            if (opts.size() != 4) return null;
        }
        return new QuizPick.Item((String) id, (String) cat, ((Double) d).intValue(), ((Double) k) == 1, q, a, w);
    }

    private static boolean isInt(Double v) { return v == Math.rint(v); }

    private static String[] pair(Object o) {
        if (!(o instanceof List) || ((List<?>) o).size() != 2) return null;
        String[] v = new String[2];
        for (int n = 0; n < 2; n++) {
            Object s = ((List<?>) o).get(n);
            if (!(s instanceof String) || !text((String) s)) return null;
            v[n] = (String) s;
        }
        return v;
    }

    private static boolean text(String s) {
        if (s.isEmpty() || s.length() > TEXT_MAX) return false;
        for (int i = 0; i < s.length(); i++) if (Character.isISOControl(s.charAt(i))) return false;
        return true;
    }
}
