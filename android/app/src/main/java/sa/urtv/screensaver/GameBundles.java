package sa.urtv.screensaver;

import android.content.Context;

import java.io.InputStream;

/**
 * The games' bundled banks (assets/games/&lt;name&gt;.gb, GameBundle): read from the app, opened under the bank's own name, parsed. Offline-first: the bundled bank is
 * always there; a newer one may come as a pack (GamePackStore) and is preferred only when it opens and parses. null when neither does; never throws.
 */
final class GameBundles {
    private GameBundles() {}

    /** the JSON tree of the bank `name` (what MiniJson gives), or null */
    static Object load(Context c, String name) {
        byte[] pack = GamePackStore.open(c, name);
        Object v = pack == null ? null : parse(GameBundle.open(name, pack));
        if (v != null) return v;
        try (InputStream in = c.getAssets().open("games/" + name + ".gb")) { return parse(GameBundle.open(name, Bounded.readAll(in, 8_000_000))); }
        catch (Exception e) { return null; }
    }
    private static Object parse(byte[] json) {
        if (json == null) return null;
        try { return MiniJson.parse(new String(json, java.nio.charset.StandardCharsets.UTF_8)); } catch (RuntimeException e) { return null; }
    }
}
