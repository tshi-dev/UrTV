package sa.urtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What the family games remember (docs/GAMES-KIT.md): the chips of each game's last round, the game opened last, the sound switch (one for all the
 * remote-only games) and, for a game that asks items, the ids of the last 100 asked. Never a name, a team or a score.
 */
final class GamePrefs {
    private GamePrefs() {}

    static final int RECENT_MAX = 100;

    private static SharedPreferences sp(Context c) { return c.getSharedPreferences("games", Context.MODE_PRIVATE); }

    static String chips(Context c, String id) { return sp(c).getString("chips." + id, ""); }
    static void saveChips(Context c, String id, String v) { sp(c).edit().putString("chips." + id, v).apply(); }
    static String last(Context c) { return sp(c).getString("last", ""); }
    static void setLast(Context c, String id) { sp(c).edit().putString("last", id).apply(); }
    static boolean sound(Context c) { return sp(c).getBoolean("sound", true); }
    static void setSound(Context c, boolean on) { sp(c).edit().putBoolean("sound", on).apply(); }

    /** the night look: 0 off (the default), 1 on, 2 automatic (after Maghrib until sunrise at the TV's city) */
    static final int NIGHT_OFF = 0, NIGHT_ON = 1, NIGHT_AUTO = 2, PALETTES = 3;
    static int night(Context c) { int v = sp(c).getInt("night", NIGHT_OFF); return v < 0 || v > NIGHT_AUTO ? NIGHT_OFF : v; }
    static void setNight(Context c, int v) { sp(c).edit().putInt("night", v).apply(); }
    /** which night palette: 0 warm charcoal (the default), 1 blue-black, 2 dim sepia */
    static int palette(Context c) { int v = sp(c).getInt("palette", 0); return v < 0 || v >= PALETTES ? 0 : v; }
    static void setPalette(Context c, int v) { sp(c).edit().putInt("palette", v).apply(); }
    /** is the night look on right now: on, or automatic and the sun is down at the city the person set for the prayer times (Riyadh until set) */
    static boolean nightNow(Context c) {
        int st = night(c);
        if (st != NIGHT_AUTO) return st == NIGHT_ON;
        double lat = 24.7136, lon = 46.6753;
        try { String[] city = new Prefs(c).city(); lat = Double.parseDouble(city[1]); lon = Double.parseDouble(city[2]); } catch (RuntimeException ignored) { /* Riyadh */ }
        return SunPos.isNight(lat, lon, System.currentTimeMillis());
    }

    static Set<String> recent(Context c, String id) {
        String s = sp(c).getString("recent." + id, "");
        Set<String> v = new LinkedHashSet<>();
        if (!s.isEmpty()) for (String x : s.split(",")) v.add(x);
        return v;
    }
    /** this round's ids go to the front of the last 100 */
    static void remember(Context c, String id, List<String> asked) {
        List<String> all = new ArrayList<>(asked);
        for (String x : recent(c, id)) if (!all.contains(x)) all.add(x);
        if (all.size() > RECENT_MAX) all = all.subList(0, RECENT_MAX);
        sp(c).edit().putString("recent." + id, String.join(",", all)).apply();
    }
}
