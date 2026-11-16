package sa.urtv.home.sports;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 4.4 the games the page handed over (its last list) and the ones already announced (by id, kept a week) */
public final class KickoffStore {
    private KickoffStore() { }

    private static SharedPreferences sp(Context c) { return c.getSharedPreferences("kickoffs", Context.MODE_PRIVATE); }

    public static void save(Context c, String json) { try { sp(c).edit().putString("games", json == null ? "[]" : json).apply(); } catch (RuntimeException ignored) { } }

    public static List<KickoffPlan.Game> games(Context c) {
        try { return KickoffPlan.parse(sp(c).getString("games", "[]")); } catch (RuntimeException e) { return new java.util.ArrayList<>(); }
    }

    public static Set<String> done(Context c) {
        try { return new HashSet<>(sp(c).getStringSet("done", new HashSet<String>())); } catch (RuntimeException e) { return new HashSet<>(); }
    }

    /** this game is announced (or dropped): never again; the ids of games no longer in the list go */
    public static void markDone(Context c, String id) {
        Set<String> keep = new HashSet<>();
        for (KickoffPlan.Game g : games(c)) keep.add(g.id);
        Set<String> d = done(c);
        d.retainAll(keep);
        d.add(id);
        try { sp(c).edit().putStringSet("done", d).apply(); } catch (RuntimeException ignored) { }
    }
}
