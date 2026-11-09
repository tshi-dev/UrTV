package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 4.3 «شاهد المباراة»: what the match page hands over for a game, checked before any of it leaves this app (MatchApp sends it to UrTV Player).
 * Only these keys, each a short plain text (no control character), the kick-off a time in milliseconds near now, at most a few channel names:
 * home_ar, home_en, away_ar, away_en, competition_ar, competition_en, kickoff (milliseconds as a string of digits), channels. Anything else, or anything malformed, is refused whole.
 * Plain Java (tests/match_app_test.py runs it).
 */
final class MatchFacts {
    static final String[] TEXTS = {"home_ar", "home_en", "away_ar", "away_en", "competition_ar", "competition_en"};
    static final int MAX_TEXT = 80, MAX_CHANNELS = 4, MAX_CHANNEL = 60;
    /** a kick-off more than this far from now is not a game the page shows */
    static final long WINDOW_MS = 14L * 24 * 3600_000L;

    final String[] texts = new String[TEXTS.length];
    long kickoff;
    final List<String> channels = new ArrayList<>();

    private MatchFacts() { }

    String text(String key) { for (int i = 0; i < TEXTS.length; i++) if (TEXTS[i].equals(key)) return texts[i]; return null; }

    /** The page's object (as the phone's JSON reader gives it: String, Long, List), or null when anything is off. */
    static MatchFacts of(Map<String, Object> m, long now) {
        if (m == null) return null;
        for (String k : m.keySet()) if (!known(k)) return null;
        MatchFacts f = new MatchFacts();
        for (int i = 0; i < TEXTS.length; i++) {
            Object v = m.get(TEXTS[i]);
            String s = v == null ? "" : v instanceof String ? clean((String) v, MAX_TEXT) : null;
            if (s == null) return null;
            f.texts[i] = s;
        }
        if (f.text("home_en").isEmpty() && f.text("home_ar").isEmpty() || f.text("away_en").isEmpty() && f.text("away_ar").isEmpty()) return null;   // a game needs its two sides
        Object k = m.get("kickoff");   // whole milliseconds, as a string of digits (the JSON reader takes nine-digit numbers only)
        if (!(k instanceof String) || !((String) k).matches("\\d{10,15}")) return null;
        long at = Long.parseLong((String) k);
        if (Math.abs(at - now) > WINDOW_MS) return null;
        f.kickoff = at;
        Object ch = m.get("channels");
        if (ch != null) {
            if (!(ch instanceof List) || ((List<?>) ch).size() > MAX_CHANNELS) return null;
            for (Object c : (List<?>) ch) {
                String s = c instanceof String ? clean((String) c, MAX_CHANNEL) : null;
                if (s == null) return null;
                if (!s.isEmpty()) f.channels.add(s);
            }
        }
        return f;
    }

    private static boolean known(String k) {
        if ("kickoff".equals(k) || "channels".equals(k)) return true;
        for (String t : TEXTS) if (t.equals(k)) return true;
        return false;
    }

    /** spaces folded and trimmed; null when too long or with a control character */
    static String clean(String s, int max) {
        for (int i = 0; i < s.length(); i++) if (Character.isISOControl(s.charAt(i))) return null;
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > max ? null : t;
    }
}
