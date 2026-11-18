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
    /** the decrypted JSON bytes of the downloaded pack `name`, or null: for a reader that has its own parser (the quiz bank) */
    static byte[] bytes(Context c, String name) {
        byte[] pack = GamePackStore.open(c, name);
        return pack == null ? null : GameBundle.open(name, pack);
    }
    /** the same as bytes, without counting as a use of the pack (GamePackStore.peek) */
    static byte[] peekBytes(Context c, String name) {
        byte[] pack = GamePackStore.peek(c, name);
        return pack == null ? null : GameBundle.open(name, pack);
    }
    private static Object parse(byte[] json) {
        if (json == null) return null;
        try { return MiniJson.parse(new String(json, java.nio.charset.StandardCharsets.UTF_8)); } catch (RuntimeException e) { return null; }
    }
}
