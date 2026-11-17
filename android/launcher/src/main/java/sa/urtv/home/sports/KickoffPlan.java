package sa.urtv.home.sports;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sa.urtv.home.phone.Json;

/**
 * 4.4 the kick-off alert's rules, no Android in them (tests/KickoffRun.java): the games the page hands over, which alarm is next, which games
 * are due, what the one check at kick-off means, where the notice goes and whether the whistle sounds.
 */
public final class KickoffPlan {
    private KickoffPlan() { }

    public static final int MAX = 12;
    /** a game is still announced this long after its kick-off (an alarm a little late), never later */
    public static final long LATE_MS = 15 * 60_000L;
    public static final int NONE = 0, HOME = 1, SAVER = 2, OVERLAY = 3, TOAST = 4;

    /** a followed club's coming game as the page hands it: its TheSportsDB id, the kick-off (ms), the two short names, their colours, the competition */
    public static final class Game {
        public final String id, h, a, lg;
        public final long t;
        public final String[] hc, ac;
        Game(String id, long t, String h, String a, String[] hc, String[] ac, String lg) { this.id = id; this.t = t; this.h = h; this.a = a; this.hc = hc; this.ac = ac; this.lg = lg; }

        /** the game again in the form parse() reads (the home's notice gets it in a broadcast and draws it itself) */
        public String json() {
            return "{\"id\":" + q(id) + ",\"t\":" + q(String.valueOf(t)) + ",\"h\":" + q(h) + ",\"a\":" + q(a) + ",\"hc\":[" + q(hc[0]) + "," + q(hc[1]) + "],\"ac\":["
                + q(ac[0]) + "," + q(ac[1]) + "],\"lg\":" + q(lg) + "}";
        }

        /** what spKick draws: {"kind":"start","h":…,"a":…,"hc":[…],"ac":[…],"lg":…} */
        public String noticeJson() {
            return "{\"kind\":\"start\",\"h\":" + q(h) + ",\"a\":" + q(a) + ",\"hc\":[" + q(hc[0]) + "," + q(hc[1]) + "],\"ac\":[" + q(ac[0]) + "," + q(ac[1]) + "],\"lg\":" + q(lg) + "}";
        }
        /** what the toast says over another app: «بدأت المباراة: الهلال × النصر» */
        public String line(String started) { return started + ": " + h + " × " + a; }
    }

    static String q(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            if (ch == '"' || ch == '\\') b.append('\\').append(ch);
            else if (ch < 0x20) b.append(' ');
            else b.append(ch);
        }
        return b.append('"').toString();
    }

    private static String text(Object o, int max) {
        String s = o instanceof String ? ((String) o).replaceAll("[\\p{Cntrl}<>]", " ").trim() : "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String[] colours(Object o) {
        String[] out = {"#8A8F98", "#5A5F68"};
        if (o instanceof List) {
            List<?> l = (List<?>) o;
            for (int i = 0; i < 2 && i < l.size(); i++) if (l.get(i) instanceof String && ((String) l.get(i)).matches("#[0-9A-Fa-f]{6}")) out[i] = (String) l.get(i);
        }
        return out;
    }

    /** the page's list, checked: at most MAX, an id of digits, a time, two names; anything else is left out */
    public static List<Game> parse(String json) {
        List<Game> out = new ArrayList<>();
        Object root;
        try { root = Json.parse(json == null ? "" : json); } catch (Json.Bad e) { return out; }
        if (!(root instanceof List)) return out;
        for (Object o : (List<?>) root) {
            if (out.size() >= MAX || !(o instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) o;
            String id = text(m.get("id"), 12), h = text(m.get("h"), 40), a = text(m.get("a"), 40);
            String t = text(m.get("t"), 13);   // the kick-off in ms, as a string (13 digits: more than Json takes as a number)
            if (!id.matches("\\d{1,12}") || h.isEmpty() || a.isEmpty() || !t.matches("\\d{12,13}")) continue;
            out.add(new Game(id, Long.parseLong(t), h, a, colours(m.get("hc")), colours(m.get("ac")), text(m.get("lg"), 60)));
        }
        return out;
    }

    /** the next alarm: the soonest game not announced and not past its LATE_MS; 0 for none */
    public static long next(List<Game> games, long now, Set<String> done) {
        long best = 0;
        for (Game g : games) if (!done.contains(g.id) && g.t + LATE_MS > now && (best == 0 || g.t < best)) best = Math.max(g.t, now);
        return best;
    }

    /** the games to announce now: at or after their kick-off (30 s early is on time), not past LATE_MS, not announced */
    public static List<Game> due(List<Game> games, long now, Set<String> done) {
        List<Game> out = new ArrayList<>();
        for (Game g : games) if (!done.contains(g.id) && g.t <= now + 30_000 && now < g.t + LATE_MS) out.add(g);
        return out;
    }

    /** the one check at kick-off: no answer (no network) is nothing; postponed, cancelled, abandoned… is nothing; anything else is the game */
    public static boolean confirmed(boolean answered, String status, boolean postponed) {
        if (!answered || postponed) return false;
        String s = status == null ? "" : status.trim().toUpperCase(java.util.Locale.ROOT);
        return !s.matches("^(MATCH\\s+)?(CANC|POSTP|PST|POST$|ABAN|ABD|SUSP|AWD|AWARD|WO$|W\\.O|WALK).*");
    }

    /** where the notice goes: front is 0 the home, 1 the screensaver, 2 another app; kids mode keeps it on the home only */
    public static int where(int front, boolean kids, boolean over, boolean canDraw) {
        if (front == 0) return HOME;
        if (kids) return NONE;
        if (front == 1) return SAVER;
        if (!over) return NONE;
        return canDraw ? OVERLAY : TOAST;
    }

    /** the whistle sounds: chosen, not in kids mode, not in the quiet hours, never with the adhan, and only with a notice somewhere */
    public static boolean sound(boolean whistle, boolean kids, boolean quiet, boolean adhan, int where) {
        return whistle && !kids && !quiet && !adhan && where != NONE;
    }
}
