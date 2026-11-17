package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 4.5 «نسخة الإعدادات» (docs/BACKUP.md): the screensaver's part of the schema-2 file, the very keys and values of optimize/settings_move.py's
 * schema 1 (read_tv), made on the TV from this app's own GET answer (SaverReceiver.get). The adhan and the ★ reciters are the home's and are
 * added there. tests/saver_file_test.py runs this and settings_move.read_tv on the same answers and wants the same JSON. No Android in here.
 */
final class SaverFile {
    private SaverFile() { }

    static final String[] SWITCHES = {"greeting", "sunrise", "prayerbar", "wxmore", "pagesdark"};
    static final String[] MONTHS = {"default", "levant"}, LANGS = {"ar", "en"}, FAV_BARS = {"5", "6", "7", "8", "9", "10", "all"},
            QUICK_BUTTONS = {"markets", "matches", "center", "phone", "inputs"}, HOME_PHOTOS = {"same", "own"},
            ACCENTS = {"default", "amber", "gold", "green", "teal", "sky", "violet", "rose", "coral"}, FOCUS_STYLES = {"solid", "frame", "glow"},
            ALBUM_MODES = {"seconds", "ten", "minute"}, GLOWS = {"off", "light", "on"}, ALBUM_FITS = {"fit", "fill"}, ALBUM_SIZES = {"normal", "tall"},
            PRAYERS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
    static final Pattern PACK_ID = Pattern.compile("[a-z][a-z0-9-]{1,30}");

    /** the GET answer as name → value (the value is everything after «name: »; the first line of a name wins) */
    static Map<String, String> parse(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        if (text == null) return out;
        for (String line : text.split("\r?\n", -1)) {
            int i = line.indexOf(": ");
            if (i >= 0) { String k = line.substring(0, i).trim(); if (!out.containsKey(k)) out.put(k, line.substring(i + 2).trim()); }
            else if (line.endsWith(":")) { String k = line.substring(0, line.length() - 1).trim(); if (!out.containsKey(k)) out.put(k, ""); }
        }
        return out;
    }

    private static boolean in(String v, String[] set) { if (v == null) return false; for (String s : set) if (s.equals(v)) return true; return false; }
    private static boolean onOff(String v) { return "on".equals(v) || "off".equals(v); }
    private static List<String> csv(String v) { List<String> out = new ArrayList<>(); for (String x : v.split(",")) if (!x.isEmpty()) out.add(x); return out; }

    /** the file's "saver" part from the GET answer (settings_move.read_tv without the adhan and the reciters); null when it is not a GET answer */
    static Map<String, Object> fromGet(String got) {
        if (got == null || got.startsWith("error:") || !got.contains("city:")) return null;
        Map<String, String> g = parse(got);
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("schema", 1);
        Matcher m = Pattern.compile("(.*) \\((-?[\\d.]+), (-?[\\d.]+)\\)").matcher(g.getOrDefault("city", ""));
        String tz = g.get("timezone");
        if (m.matches() && tz != null && !tz.isEmpty()) {
            Map<String, Object> city = new LinkedHashMap<>();
            city.put("name", m.group(1));
            city.put("lat", Double.parseDouble(m.group(2)));
            city.put("lon", Double.parseDouble(m.group(3)));
            city.put("tz", tz);
            String cc = g.get("country");
            if (cc != null && cc.matches("(?:[A-Z]{2})?")) city.put("country", cc);
            s.put("city", city);
        }
        String[][] plain = {{"digits", "digits"}, {"clock_size", "clocksize"}, {"card_style", "cardstyle"}};
        for (String[] p : plain) if (g.containsKey(p[1])) s.put(p[0], g.get(p[1]));
        if (g.containsKey("clock24")) s.put("clock24", "on".equals(g.get("clock24")));
        for (String k : SWITCHES) if (onOff(g.get(k))) s.put(k, "on".equals(g.get(k)));
        if (in(g.get("months"), MONTHS)) s.put("months", g.get("months"));
        if (in(g.get("lang"), LANGS)) s.put("lang", g.get("lang"));
        if (onOff(g.get("listenpill"))) s.put("listen_pill", "on".equals(g.get("listenpill")));
        if (onOff(g.get("updatebell"))) s.put("update_bell", "on".equals(g.get("updatebell")));
        if (g.containsKey("pillshidden")) {
            String v = g.get("pillshidden");
            List<String> l = new ArrayList<>();
            if (!v.equals("none") && !v.isEmpty()) for (String k : v.split(",")) if (in(k, QUICK_BUTTONS)) l.add(k);
            s.put("pills_hidden", l);
        }
        if (in(g.get("homephotos"), HOME_PHOTOS)) s.put("home_photos", g.get("homephotos"));
        if (g.containsKey("homepacks")) {
            String v = g.get("homepacks");
            List<String> l = new ArrayList<>();
            if (!v.equals("none") && !v.isEmpty()) for (String k : v.split(",")) if (PACK_ID.matcher(k).matches()) l.add(k);
            s.put("home_packs", l);
        }
        if (in(g.get("accent"), ACCENTS)) s.put("accent", g.get("accent"));
        if (in(g.get("focusstyle"), FOCUS_STYLES)) s.put("focus_style", g.get("focusstyle"));
        int[][] ranges = {{30, 100}, {0, 90}};
        String[][] ranged = {{"bar_opacity", "baropacity"}, {"wall_dim", "walldim"}};
        for (int i = 0; i < 2; i++) {
            String v = g.getOrDefault(ranged[i][1], "x");
            if (v.matches("\\d+") && v.length() < 9 && Integer.parseInt(v) >= ranges[i][0] && Integer.parseInt(v) <= ranges[i][1]) s.put(ranged[i][0], Integer.parseInt(v));
        }
        if (in(g.get("albummode"), ALBUM_MODES)) s.put("album_mode", g.get("albummode"));
        if (onOff(g.get("albumdate"))) s.put("album_date", "on".equals(g.get("albumdate")));
        if (in(g.get("glow"), GLOWS)) s.put("glow", g.get("glow"));
        if (onOff(g.get("cleartext"))) s.put("clear_text", "on".equals(g.get("cleartext")));
        if (onOff(g.get("albumtitle"))) s.put("album_title", "on".equals(g.get("albumtitle")));
        if (in(g.get("albumfit"), ALBUM_FITS)) s.put("album_fit", g.get("albumfit"));
        if (in(g.get("albumsize"), ALBUM_SIZES)) s.put("album_size", g.get("albumsize"));
        if (onOff(g.get("looksync"))) s.put("look_sync", "on".equals(g.get("looksync")));
        if (onOff(g.get("homeours"))) s.put("home_ours", "on".equals(g.get("homeours")));
        if (in(g.get("favbar"), FAV_BARS)) s.put("fav_bar", g.get("favbar"));
        Map<String, Object> prayer = new LinkedHashMap<>();
        if (g.containsKey("prayermethod")) prayer.put("method", g.get("prayermethod"));
        String pa = g.get("prayeradj");
        if (pa != null && !pa.isEmpty()) {
            String[] words = pa.split("\\s+");
            prayer.put("adjust_on", words[0].equals("on"));
            Map<String, Object> adj = new LinkedHashMap<>();
            for (int i = 1; i < words.length; i++) {
                int eq = words[i].indexOf('=');
                String k = eq < 0 ? words[i] : words[i].substring(0, eq), v = eq < 0 ? "" : words[i].substring(eq + 1);
                if (in(k, PRAYERS) && v.matches("[+-]?\\d+")) adj.put(k, Integer.parseInt(v.startsWith("+") ? v.substring(1) : v));
            }
            if (!adj.isEmpty()) prayer.put("adjust", adj);
        }
        if (!prayer.isEmpty()) s.put("prayer", prayer);
        String ho = g.getOrDefault("hijrioffset", "x");
        if (ho.matches("[+-]?\\d+")) s.put("hijri_offset", Integer.parseInt(ho.startsWith("+") ? ho.substring(1) : ho));
        Map<String, Object> cards = new LinkedHashMap<>();
        if (g.containsKey("panel")) cards.put("panel", g.get("panel"));
        if (g.containsKey("panel2")) cards.put("panel2", g.get("panel2"));
        if (g.containsKey("home_panel")) { String v = g.get("home_panel"); cards.put("home", v.equals("none") || v.isEmpty() ? new ArrayList<String>() : new ArrayList<>(java.util.Arrays.asList(v.split(",")))); }
        if (!cards.isEmpty()) s.put("cards", cards);
        if (g.containsKey("markets") || g.containsKey("mkstyle")) {
            Map<String, Object> mk = new LinkedHashMap<>();
            if (g.containsKey("markets")) {
                String v = g.get("markets").trim();
                String[] words = v.isEmpty() ? new String[0] : v.split("\\s+");
                boolean off = words.length > 0 && words[0].equals("off");
                mk.put("on", !off);
                List<String> l = new ArrayList<>();
                for (int i = off ? 1 : 0; i < words.length; i++) l.addAll(csv(words[i]));
                mk.put("list", l);
            }
            if (g.containsKey("mkstyle")) mk.put("style", g.get("mkstyle"));
            s.put("markets", mk);
        }
        if (g.containsKey("teams")) {
            List<Object> teams = new ArrayList<>();
            for (String part : g.get("teams").split(", ", -1)) {
                int bar = part.indexOf('|');
                String tid = (bar < 0 ? part : part.substring(0, bar)).trim(), name = bar < 0 ? "" : part.substring(bar + 1).trim();
                if (tid.isEmpty()) continue;
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("id", tid);
                t.put("name", name.equals("#" + tid) ? "" : name);
                teams.add(t);
            }
            s.put("teams", teams);
        }
        return s;
    }

    /** JSON text of plain values (Map, List, String, Boolean, Integer, Double), Python's json.dumps(ensure_ascii=False, separators=(",", ": ")) style */
    static String json(Object v) {
        StringBuilder b = new StringBuilder();
        write(b, v);
        return b.toString();
    }

    private static void write(StringBuilder b, Object v) {
        if (v == null) b.append("null");
        else if (v instanceof String) str(b, (String) v);
        else if (v instanceof Boolean || v instanceof Integer || v instanceof Long) b.append(v);
        else if (v instanceof Double) { double d = (Double) v; b.append(d == Math.rint(d) && Math.abs(d) < 1e15 ? String.format(Locale.ROOT, "%.1f", d) : Double.toString(d)); }
        else if (v instanceof Map) {
            b.append('{'); boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) { if (!first) b.append(", "); first = false; str(b, String.valueOf(e.getKey())); b.append(": "); write(b, e.getValue()); }
            b.append('}');
        } else if (v instanceof List) {
            b.append('['); boolean first = true;
            for (Object o : (List<?>) v) { if (!first) b.append(", "); first = false; write(b, o); }
            b.append(']');
        } else str(b, String.valueOf(v));
    }

    private static void str(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                case '\b': b.append("\\b"); break;
                case '\f': b.append("\\f"); break;
                default: if (c < 0x20) b.append(String.format(Locale.ROOT, "\\u%04x", (int) c)); else b.append(c);
            }
        }
        b.append('"');
    }
}
