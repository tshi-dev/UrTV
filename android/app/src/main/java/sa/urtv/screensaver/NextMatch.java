package sa.urtv.screensaver;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** The next real match of the followed teams, for the hub's «المباريات» line. TheSportsDB eventsnext (the same free source the page
 *  reads), the earliest future match that is not called off, kept 30 min in the app's prefs. Unknown (no teams, no network, nothing
 *  coming) = null, and the hub keeps its generic line. Pure parts (pick, when) are tested on the JVM. */
final class NextMatch {
    private NextMatch() {}

    static final String URL = "https://www.thesportsdb.com/api/v1/json/123/eventsnext.php?id=";
    static final long KEEP_MS = 30L * 60 * 1000;
    static final int TEAMS_MAX = 3;

    /** one upcoming match: names as the source gives them (English), the start in UTC ms */
    static final class Ev {
        final String home, away, homeId, awayId;
        final long at;
        Ev(String home, String away, String homeId, String awayId, long at) {
            this.home = home; this.away = away; this.homeId = homeId; this.awayId = awayId; this.at = at;
        }
    }

    /** the earliest match after `now` in an eventsnext answer that is not postponed, cancelled or abandoned; null if none */
    static Ev pick(String json, long now) {
        Ev best = null;
        try {
            JSONArray a = new JSONObject(json).optJSONArray("events");
            if (a == null) return null;
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                String status = o.optString("strStatus", "").toLowerCase(Locale.ROOT);
                if (status.matches(".*(postp|cancel|abandon|suspend).*")) continue;
                if (o.optString("strPostponed", "no").equalsIgnoreCase("yes")) continue;
                long at = NextMatchTime.start(o.optString("strTimestamp", ""), o.optString("dateEvent", ""), o.optString("strTime", ""));
                if (at <= now) continue;
                String h = o.optString("strHomeTeam", ""), w = o.optString("strAwayTeam", "");
                if (h.isEmpty() || w.isEmpty()) continue;
                if (best == null || at < best.at) best = new Ev(h, w, o.optString("idHomeTeam", ""), o.optString("idAwayTeam", ""), at);
            }
        } catch (Exception e) {
            return null;
        }
        return best;
    }

    /** The cached line if it is fresh and still ahead, else null; and a refresh in the background when it is not fresh.
     *  `onNew` runs on the worker thread when a new answer was stored. */
    static String cached(Context c, Prefs prefs, Runnable onNew) {
        long now = System.currentTimeMillis();
        android.content.SharedPreferences sp = c.getSharedPreferences("nextmatch", Context.MODE_PRIVATE);
        String key = String.join(",", ids(prefs.teams()));
        if (key.isEmpty()) return null;
        boolean same = key.equals(sp.getString("key", ""));
        long when = sp.getLong("start", 0);
        boolean fresh = same && now - sp.getLong("got", 0) < KEEP_MS;
        if (!fresh) refresh(c, prefs, key, onNew);
        if (!same || when <= now) return null;
        Ev e = new Ev(sp.getString("home", ""), sp.getString("away", ""), sp.getString("homeId", ""), sp.getString("awayId", ""), when);
        return describe(c, prefs, e, now);
    }

    static String describe(Context c, Prefs prefs, Ev e, long now) {
        boolean en = Tr.EN.equals(prefs.lang());
        String l = NextMatchTime.line(e.at, name(c, prefs, e.homeId, e.home, en), name(c, prefs, e.awayId, e.away, en), now,
            zone(prefs), prefs.clock24(), en);
        return "latin".equals(prefs.digits()) ? NextMatchTime.latin(l) : l;   // «الأرقام» عربية 123
    }

    private static TimeZone zone(Prefs prefs) {
        String z = prefs.cityTz();
        return z == null || z.isEmpty() ? ZoneFix.tvZone() : ZoneFix.zone(z);
    }

    private static String name(Context c, Prefs prefs, String id, String fallback, boolean en) {
        if (en) return fallback;
        TeamSearch.Team t = id.isEmpty() ? null : Teams.byId(c, id);
        if (t != null && !t.ar.isEmpty()) return t.ar;
        for (String s : prefs.teams()) {
            int bar = s.indexOf('|');
            if (bar > 0 && s.substring(0, bar).equals(id) && bar + 1 < s.length()) return s.substring(bar + 1);
        }
        return fallback;
    }

    static List<String> ids(List<String> teams) {
        List<String> v = new ArrayList<>();
        for (String s : teams) {
            int bar = s.indexOf('|');
            String id = bar < 0 ? s : s.substring(0, bar);
            if (!id.isEmpty() && v.size() < TEAMS_MAX) v.add(id);
        }
        return v;
    }

    private static final java.util.concurrent.atomic.AtomicBoolean BUSY = new java.util.concurrent.atomic.AtomicBoolean();

    private static void refresh(Context c, Prefs prefs, String key, Runnable onNew) {
        if (!BUSY.compareAndSet(false, true)) return;
        Context app = c.getApplicationContext();
        new Thread(() -> {
            try {
                long now = System.currentTimeMillis();
                Ev best = null;
                for (String id : key.split(",")) {
                    Ev e = pick(get(URL + id), now);
                    if (e != null && (best == null || e.at < best.at)) best = e;
                }
                android.content.SharedPreferences.Editor ed = app.getSharedPreferences("nextmatch", Context.MODE_PRIVATE).edit()
                    .putString("key", key).putLong("got", now);
                if (best == null) ed.putLong("start", 0);
                else ed.putString("home", best.home).putString("away", best.away).putString("homeId", best.homeId)
                    .putString("awayId", best.awayId).putLong("start", best.at);
                ed.apply();
                if (onNew != null) onNew.run();
            } catch (Exception ignored) {
                // no network: the cached line (if any) stays, the next open tries again
            } finally {
                BUSY.set(false);
            }
        }, "next-match").start();
    }

    private static String get(String url) throws Exception {
        Net.Reply r = Net.get(url, null, 12000, 12000, Bounded.JSON_MAX);   // IPv4 first, 4 s per address (Net)
        if (r.status != 200) throw new java.io.IOException("status " + r.status);
        return r.text();
    }
}
