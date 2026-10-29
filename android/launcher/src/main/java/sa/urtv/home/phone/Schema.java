package sa.urtv.home.phone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the phone page may send, per endpoint: the keys that exist (anything else is refused), their types and their values. A value that passes
 * here is still checked again by the code that applies it (the same code the TV's own settings and optimize/*.sh use: SaverReceiver.set,
 * the adhan's AdhanSettings); this is the first gate, so a free-form string never gets past it into anything but a text field.
 * No Android in it.
 */
public final class Schema {
    public static final String[] KEYS = {"up", "down", "left", "right", "ok", "back", "home", "vol_up", "vol_down", "mute", "play_pause"};
    public static final String[] QURAN_OPS = {"play", "pause", "resume", "toggle", "next", "prev", "stop", "sleep"};
    public static final int MAX_TEXT = 120, MAX_CITY = 40, MAX_QUERY = 30, MAX_HOME_CARDS = 3, SURAHS = 114;
    /** AdhanVoices.COUNT: the number of adhan recordings of ours (tests/phone_test.py checks they agree) */
    public static final int ADHAN_VOICES = 21;
    public static final int MIN_SLEEP = 0, MAX_SLEEP = 180;
    /** «مؤقت النوم» of the home (SleepTimer.MINUTES, and 0 = cancel) and the screensaver's idle minutes (Prefs.IDLE_MINUTES): tests/phone_test.py checks they agree. */
    public static final int[] SLEEP_MINUTES = {0, 15, 30, 60, 90};
    public static final int[] SAVER_IDLE = {1, 2, 5, 10, 15, 30};
    public static final String[] DO_OPS = {"screensaver", "sleep_cancel"};
    public static final String[] TOOLS = {"@tasks", "@saver", "@inputs", "@settings", "@listen"};

    /** A checked body: the allowed keys with their typed values, or why it was refused (a fixed word, never the input). */
    public static final class Clean {
        public final Map<String, Object> values;
        public final String error;
        Clean(Map<String, Object> values, String error) { this.values = values; this.error = error; }
        public boolean ok() { return error == null; }
    }

    private Schema() { }

    private static Clean no(String why) { return new Clean(null, why); }

    /** Checks the body of one endpoint; the clean map has only the allowed keys, typed (Boolean, Long, String, List of String). */
    public static Clean check(String endpoint, Map<String, Object> body) {
        switch (endpoint) {
            case "pair": {
                Clean c = only(body, "token", "code");
                if (!c.ok()) return c;
                boolean t = body.containsKey("token"), k = body.containsKey("code");
                if (t == k) return no("one of token or code");
                if (t && !hex(body.get("token"), 32)) return no("bad token");
                if (k && !digits(body.get("code"), 6)) return no("bad code");
                return c;
            }
            case "pairstatus": {
                Clean c = only(body, "id");
                if (!c.ok()) return c;
                if (!hex(body.get("id"), 32)) return no("bad id");
                return c;
            }
            case "key": {
                Clean c = only(body, "key");
                if (!c.ok()) return c;
                if (!(body.get("key") instanceof String) || !Arrays.asList(KEYS).contains(body.get("key"))) return no("bad key");
                return c;
            }
            case "text": {
                Clean c = only(body, "text", "mode", "search");
                if (!c.ok()) return c;
                if (!plainText(body.get("text"), MAX_TEXT, true)) return no("bad text");
                if (body.containsKey("mode") && !"append".equals(body.get("mode")) && !"replace".equals(body.get("mode"))) return no("bad mode");
                if (body.containsKey("search") && !(body.get("search") instanceof Boolean)) return no("bad search");
                return c;
            }
            case "cities": {
                Clean c = only(body, "q");
                if (!c.ok()) return c;
                if (!name(body.get("q"), MAX_QUERY)) return no("bad query");
                return c;
            }
            case "set": return set(body);
            case "quran": return quran(body);
            case "update": {
                Clean c = only(body, "op", "pin");
                if (!c.ok()) return c;
                Object op = body.get("op");
                if (!"check".equals(op) && !"install".equals(op)) return no("bad op");
                if (!pin(body)) return no("bad pin");
                return c;
            }
            case "launch": {
                Clean c = only(body, "target");
                if (!c.ok()) return c;
                if (!target(body.get("target"))) return no("bad target");
                return c;
            }
            case "adhanstop": return only(body);
            case "do": {
                Clean c = only(body, "op", "pin");
                if (!c.ok()) return c;
                if (!(body.get("op") instanceof String) || !Arrays.asList(DO_OPS).contains(body.get("op"))) return no("bad op");
                if (!pin(body)) return no("bad pin");
                return c;
            }
            case "logout": return only(body);
            case "restorehome": {   // «رجّع واجهة التلفزيون الأصلية»: nothing but the PIN
                Clean c = only(body, "pin");
                if (!c.ok()) return c;
                if (!pin(body)) return no("bad pin");
                return c;
            }
            default: return no("unknown endpoint");
        }
    }

    private static Clean set(Map<String, Object> b) {
        Clean c = only(b, "city", "home_cards", "adhan_on", "adhan_voice", "adhan_fajr_voice", "adhan_short", "adhan_stopkey", "adhan_volume", "sleep_minutes", "saver_idle", "pin");
        if (!c.ok()) return c;
        if (!pin(b)) return no("bad pin");
        boolean any = false;
        for (String k : b.keySet()) {
            if (k.equals("pin")) continue;
            any = true;
            Object v = b.get(k);
            switch (k) {
                case "city": if (!name(v, MAX_CITY)) return no("bad city"); break;
                case "home_cards": {
                    if (!(v instanceof List) || ((List<?>) v).size() > MAX_HOME_CARDS) return no("bad home_cards");
                    List<String> seen = new ArrayList<>();
                    for (Object x : (List<?>) v) {
                        if (!(x instanceof String) || !((String) x).matches("[a-z]{3,12}") || seen.contains(x)) return no("bad home_cards");
                        seen.add((String) x);
                    }
                    break;
                }
                case "adhan_on": case "adhan_short": case "adhan_stopkey": if (!(v instanceof Boolean)) return no("bad " + k); break;
                case "adhan_voice": case "adhan_fajr_voice": if (!range(v, 1, ADHAN_VOICES)) return no("bad " + k); break;
                case "adhan_volume": if (!range(v, 10, 100)) return no("bad adhan_volume"); break;
                case "sleep_minutes": if (!oneOf(v, SLEEP_MINUTES)) return no("bad sleep_minutes"); break;
                case "saver_idle": if (!oneOf(v, SAVER_IDLE)) return no("bad saver_idle"); break;
                default: return no("unknown key");
            }
        }
        return any ? c : no("nothing to set");
    }

    private static Clean quran(Map<String, Object> b) {
        Clean c = only(b, "op", "reciter", "surah", "minutes");
        if (!c.ok()) return c;
        Object op = b.get("op");
        if (!(op instanceof String) || !Arrays.asList(QURAN_OPS).contains(op)) return no("bad op");
        if (b.containsKey("reciter") && !(b.get("reciter") instanceof String && ((String) b.get("reciter")).matches("[A-Za-z0-9_-]{1,40}"))) return no("bad reciter");
        if (b.containsKey("surah") && !range(b.get("surah"), 1, SURAHS)) return no("bad surah");
        if (b.containsKey("minutes") && !range(b.get("minutes"), MIN_SLEEP, MAX_SLEEP)) return no("bad minutes");
        if ("play".equals(op) && !(b.containsKey("reciter") && b.containsKey("surah"))) return no("play needs reciter and surah");
        if ("sleep".equals(op) && !b.containsKey("minutes")) return no("sleep needs minutes");
        if (!"play".equals(op) && (b.containsKey("reciter") || b.containsKey("surah"))) return no("reciter and surah go with play");
        if (!"sleep".equals(op) && b.containsKey("minutes")) return no("minutes goes with sleep");
        return c;
    }

    /** The body may only have these keys; the clean map is a copy of it. */
    private static Clean only(Map<String, Object> body, String... allowed) {
        List<String> ok = Arrays.asList(allowed);
        for (String k : body.keySet()) if (!ok.contains(k)) return no("unknown key");
        return new Clean(new LinkedHashMap<>(body), null);
    }

    private static boolean pin(Map<String, Object> b) {
        return !b.containsKey("pin") || (b.get("pin") instanceof String && ((String) b.get("pin")).matches("[0-9]{4}"));
    }

    private static boolean oneOf(Object v, int[] allowed) {
        if (!(v instanceof Long)) return false;
        for (int a : allowed) if (a == (Long) v) return true;
        return false;
    }

    /** An app's package name as the system writes it (letters, digits, underscores, at least two dotted parts). */
    public static boolean packageName(Object v) {
        return v instanceof String && ((String) v).length() <= 150 && ((String) v).matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+");
    }

    /** What /api/launch may name: a package, or one of TOOLS. Whether it is one of the favourites is checked again where it is applied. */
    public static boolean target(Object v) {
        return packageName(v) || (v instanceof String && Arrays.asList(TOOLS).contains(v));
    }

    private static boolean range(Object v, int lo, int hi) {
        return v instanceof Long && (Long) v >= lo && (Long) v <= hi;
    }

    private static boolean hex(Object v, int n) {
        return v instanceof String && ((String) v).matches("[0-9a-f]{" + n + "}");
    }

    private static boolean digits(Object v, int n) {
        return v instanceof String && ((String) v).matches("[0-9]{" + n + "}");
    }

    /** A name or a search word: letters (any script), marks, digits, spaces, hyphens, the two Arabic parentheses; no control, no markup. */
    public static boolean name(Object v, int max) {
        if (!(v instanceof String)) return false;
        String s = (String) v;
        int n = s.codePointCount(0, s.length());
        if (n < 1 || n > max) return false;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            boolean okChar = Character.isLetterOrDigit(cp) || cp == ' ' || cp == '-' || cp == '(' || cp == ')'
                || Character.getType(cp) == Character.NON_SPACING_MARK;
            if (!okChar) return false;
        }
        return true;
    }

    /** Typed text: any character but controls, unpaired surrogates and the invisible direction overrides that could hide what is typed. */
    public static boolean plainText(Object v, int max, boolean oneLine) {
        if (!(v instanceof String)) return false;
        String s = (String) v;
        int n = s.codePointCount(0, s.length());
        if (n < 1 || n > max) return false;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            int t = Character.getType(cp);
            if (t == Character.CONTROL || t == Character.SURROGATE || t == Character.UNASSIGNED || t == Character.PRIVATE_USE) return false;
            if (cp >= 0x202a && cp <= 0x202e || cp >= 0x2066 && cp <= 0x2069) return false;
            if (oneLine && (cp == 0x2028 || cp == 0x2029)) return false;
        }
        return !s.trim().isEmpty();
    }
}
