package sa.urtv.home.tilawah;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 4.4 radio reliability: what the stations check (.github/tools/check_stations.py, CI, once a day) published about each live station, in
 * urtv-data's stations/status.json ({"v": 1, "checked_ms": …, "stations": {"<Arabic name>": {"fails": n, "dead": bool, "mirrors": […]}}}),
 * read as MiniJson gives it (Maps, Lists, Doubles, Booleans, Strings). A station that failed DEAD_AFTER checks in a row is hidden from the
 * list and stepped over by next and previous, never the one the person is on, never all of them, and never from a status older than
 * MAX_AGE_MS (the check stopped: the stations are shown as before). The mirrors it found that play are tried after the station's own
 * (StationSources). No file, a bad one or an old app: everything as before. Pure logic (no Android classes): tests/station_status_test.py.
 */
final class StationStatus {
    private StationStatus() { }

    /** this many failed checks in a row before a station is hidden (the check runs once a day) */
    static final int DEAD_AFTER = 3;
    /** a status older than this is not used */
    static final long MAX_AGE_MS = 4L * 24 * 3600_000L;

    private static Map<?, ?> entry(Map<?, ?> status, String key, long nowMs) {
        if (status == null || key == null) return null;
        Object v = status.get("v"), at = status.get("checked_ms"), all = status.get("stations");
        if (!(v instanceof Double) || (Double) v != 1.0 || !(at instanceof Double) || !(all instanceof Map)) return null;
        long age = nowMs - ((Double) at).longValue();
        if (age < -3600_000L || age > MAX_AGE_MS) return null;   // old, or from a clock far ahead of the TV's
        Object e = ((Map<?, ?>) all).get(key);
        return e instanceof Map ? (Map<?, ?>) e : null;
    }

    /** the check found the station down DEAD_AFTER times in a row (in a fresh status) */
    static boolean dead(Map<?, ?> status, String key, long nowMs) {
        Map<?, ?> e = entry(status, key, nowMs);
        if (e == null) return false;
        Object dead = e.get("dead"), fails = e.get("fails");
        return Boolean.TRUE.equals(dead) && fails instanceof Double && (Double) fails >= DEAD_AFTER;
    }

    /** the station's published mirrors (StationSources keeps the https ones), empty when none */
    static List<String> mirrors(Map<?, ?> status, String key, long nowMs) {
        List<String> out = new ArrayList<>();
        Map<?, ?> e = entry(status, key, nowMs);
        Object m = e == null ? null : e.get("mirrors");
        if (m instanceof List) for (Object u : (List<?>) m) if (u instanceof String) out.add((String) u);
        return out;
    }

    /** which stations are hidden: the dead ones, never `current` (-1: none), and none at all when every one would be */
    static boolean[] hidden(Map<?, ?> status, List<String> keys, int current, long nowMs) {
        boolean[] h = new boolean[keys.size()];
        boolean any = false;
        for (int i = 0; i < h.length; i++) { h[i] = i != current && dead(status, keys.get(i), nowMs); any |= !h[i]; }
        return any ? h : new boolean[h.length];
    }

    /** the station after (dir 1) or before (dir -1) `from` that is not hidden, round and round; `from` itself when there is no other */
    static int step(int from, int dir, boolean[] hidden) {
        int n = hidden.length;
        if (n == 0) return from;
        int k = from;
        for (int i = 0; i < n; i++) {
            k = ((k + (dir < 0 ? -1 : 1)) % n + n) % n;
            if (!hidden[k]) return k;
        }
        return from;
    }
}
