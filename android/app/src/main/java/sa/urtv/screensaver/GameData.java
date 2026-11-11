package sa.urtv.screensaver;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A game's data, loaded on demand (docs/GAMES-KIT.md), the way QuizBank does it for «الأسرع»: a starter ships with the app (assets/games/<id>.json, so
 * the first round works with no network); the published file (the data-games branch, games/<id>.json, made and verified by
 * .github/tools/make_game_data.py) is fetched in the background when the kept copy is older than 3 days, read with a size cap through Net, checked WHOLE
 * by the game's own parser (the limits of docs/GAMES-DATA.md), written to a temporary file and renamed. Anything wrong and the last good copy stays; a kept copy that no longer passes
 * falls back to the starter. Only when a game's screen opens; nothing about the people playing is sent or stored.
 */
final class GameData {
    private GameData() {}

    static final String BASE = "https://raw.githubusercontent.com/tshi-dev/UrTV/data-games/games/";
    static final long REFRESH_MS = 3L * 24 * 3600 * 1000;
    private static final Set<String> fetching = Collections.synchronizedSet(new HashSet<String>());

    private static File kept(Context c, String name) { return new File(c.getFilesDir(), "game-" + name + ".json"); }

    /** the kept copy when it passes the game's check for a published file, else the bundled starter when it passes its own (the same form, a lower
     *  floor of items), else null; never throws. `name` is the file's name without «.json» (a game may have more than one file). */
    static <T> T load(Context c, String name, GameDataCheck.Parser<T> published, GameDataCheck.Parser<T> starter) {
        T v = null;
        File f = kept(c, name);
        if (f.isFile() && f.length() <= GameDataCheck.BYTES_MAX) {
            try (InputStream in = new java.io.FileInputStream(f)) { v = published.parse(Bounded.readAll(in, GameDataCheck.BYTES_MAX)); } catch (Exception e) { v = null; }
        }
        if (v == null) {
            try (InputStream in = c.getAssets().open("games/" + name + ".json")) { v = starter.parse(Bounded.readAll(in, GameDataCheck.BYTES_MAX)); } catch (Exception e) { v = null; }
        }
        return v;
    }

    /** the background fetch, read with the file's own cap (docs/GAMES-DATA.md); the new file is used from the next time the game opens */
    static <T> void refresh(Context c, final String name, final GameDataCheck.Parser<T> published, final int maxBytes) {
        final Context app = c.getApplicationContext();
        final File f = kept(app, name);
        if ((f.isFile() && System.currentTimeMillis() - f.lastModified() < REFRESH_MS) || !fetching.add(name)) return;
        new Thread(() -> {
            try {
                Net.Reply r = Net.get(BASE + name + ".json", null, 15000, 20000, Math.min(maxBytes, GameDataCheck.BYTES_MAX));   // no redirect, a size cap, IPv4 first (Net)
                if (r.status != 200) return;
                byte[] b = r.body;
                if (published.parse(b) == null) return;   // never replace a good file with a bad one
                File tmp = new File(app.getFilesDir(), "game-" + name + ".json.tmp");
                try (FileOutputStream o = new FileOutputStream(tmp)) { o.write(b); }
                if (!tmp.renameTo(f)) tmp.delete();
            } catch (Exception ignored) {
                // no network, a file too big, a bad answer: the kept copy or the starter is used
            } finally {
                fetching.remove(name);
            }
        }, "game-data").start();
    }
}
