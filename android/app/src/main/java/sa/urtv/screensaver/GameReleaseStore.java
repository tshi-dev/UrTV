package sa.urtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Android side of the games' release switch (GameRelease, docs/GAMES-RELEASE.md) and the place the engine's games are kept. Everything the UI thread asks is answered from memory (R1):
 * `visible`, `games` and `signature` read volatile fields. A background thread reads the kept copy of urtv-data's release/games.json (ContentFile.Kept), fetches it again at most every few
 * hours (ContentFile: a bad answer leaves the last good copy), works out the TV's group, reads the definitions (open ones, and the sealed ones with the group key when this TV holds a pass) and
 * the packs they name, and keeps only the games the engine can run. Until that thread has answered, and whenever the file is missing or broken, nothing is on. The pass is kept as its record
 * and the group key (a private preference file), never the code. No account, no id, no request but the one file.
 */
final class GameReleaseStore {
    private GameReleaseStore() {}

    static final String PATH = "release/games.json", NAME = "games-release.json", PREFS = "games_release", PASS = "pass", KEY = "key";
    static final long MAX_AGE_MS = 3 * 3600_000L, TRY_WINDOW_MS = 60_000L; static final int TRIES = 5;

    private static volatile GameRelease.Release current; private static volatile boolean groupB; private static volatile Map<String, GameDef.Def> defs = Collections.emptyMap();
    private static final ContentFile.Kept KEPT = new ContentFile.Kept(NAME);
    private static final AtomicBoolean working = new AtomicBoolean();
    private static final long[] tries = new long[TRIES]; private static int tryAt;

    /** is this game on for this TV now and a game the engine can run: memory only, safe on the UI thread */
    static boolean visible(String id) { return defs.containsKey(id) && GameRelease.on(current, id, groupB, System.currentTimeMillis()); }

    /** the engine games that are on now, in the file's order (memory only) */
    static List<GameDef.Def> games() {
        List<GameDef.Def> out = new ArrayList<>();
        for (Map.Entry<String, GameDef.Def> e : defs.entrySet()) if (visible(e.getKey())) out.add(e.getValue());
        return out;
    }
    static GameDef.Def def(String id) { return visible(id) ? defs.get(id) : null; }

    /** the games that are on now, as one string: the hub draws again when it changes */
    static String signature() { StringBuilder b = new StringBuilder(); for (GameDef.Def d : games()) b.append(d.id).append(','); return b.toString(); }

    /** off the UI thread: the kept copy and the group, then (if the copy is old) a fetch and the same again; `changed` runs after each answer on that thread */
    static void warm(final Context c, final Runnable changed) {
        final Context app = c.getApplicationContext();
        if (!working.compareAndSet(false, true)) { if (changed != null) changed.run(); return; }
        new Thread(() -> {
            try {
                publish(app); if (changed != null) changed.run();
                ContentFile.refresh(app, PATH, NAME, MAX_AGE_MS, () -> { publish(app); if (changed != null) changed.run(); });
            } finally { working.set(false); }
        }, "games-release").start();
    }

    private static synchronized void publish(Context app) {
        KEPT.load(app);
        GameRelease.Release r = GameRelease.parse(KEPT.value());
        SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String kept = p.getString(PASS, null), keyHex = p.getString(KEY, null);
        boolean b = GameRelease.passValid(r, kept) && keyHex != null && keyHex.matches("[0-9a-f]{64}");
        if (kept != null && r != null && !b) p.edit().remove(PASS).remove(KEY).apply();   // the owner took the line out: this TV is everyone's again
        byte[] key = b ? GameCrypt.unhex(keyHex) : null;
        Map<String, GameDef.Def> m = new LinkedHashMap<>();
        if (r != null) for (String id : r.games.keySet()) {
            Object tree = GameRelease.def(r, id, key);
            GameDef.Def d = tree == null ? null : GameDef.parse(tree, null);
            if (d == null || !d.id.equals(id)) continue;   // not a game the engine can run: it stays hidden
            if (!d.pack.isEmpty()) {
                List<GameDef.Item> items = GameDef.packItems(GameBundles.load(app, d.pack), null);
                if (items == null) continue;
                d = d.withItems(items);
            }
            m.put(id, d);
        }
        current = r; groupB = b; defs = m;
    }

    /** off the UI thread: does this typed code open a pass of the file; remembers the record and the group key when it does. Five tries a minute at most; a wrong code leaves nothing behind */
    static void tryCode(final Context c, final String typed, final Runnable ok) {
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            long now = System.currentTimeMillis();
            synchronized (tries) {
                if (tries[tryAt % TRIES] != 0 && now - tries[tryAt % TRIES] < TRY_WINDOW_MS) return;   // five in the last minute
                tries[tryAt++ % TRIES] = now;
            }
            publish(app);
            String[] m = GameRelease.match(current, typed);
            if (m == null) return;
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(PASS, m[0]).putString(KEY, m[1]).apply();
            publish(app);
            if (ok != null) ok.run();
        }, "games-pass").start();
    }
}
