package sa.urtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The games' packs on this TV (docs/GAMES-PACKS.md): where they are, when each was last opened, what the person chose for it, the download and the 14-day sweep.
 * The rules are GamePacks (plain Java, tested). A pack is downloaded only when a game that needs it opens, never while a round is on, never when the TV is
 * low on space, and only after its sha256 matches the index; the file is then written whole (a temporary name, then renamed). Nothing is sent about the TV.
 */
final class GamePackStore {
    private GamePackStore() {}

    static final String BASE = "https://raw.githubusercontent.com/tshi-dev/urtv-data/main/games/packs/";
    private static volatile boolean busy;

    private static SharedPreferences sp(Context c) { return c.getSharedPreferences("gamepacks", Context.MODE_PRIVATE); }
    private static File dir(Context c) { File d = new File(c.getFilesDir(), "gamepacks"); d.mkdirs(); return d; }
    private static File file(Context c, String id) { return new File(dir(c), id + ".gb"); }

    static boolean installed(Context c, String id) { return file(c, id).isFile(); }
    static int mode(Context c, String id) { return sp(c).getInt(id + ".mode", GamePacks.AUTO); }
    static void setMode(Context c, String id, int m) { sp(c).edit().putInt(id + ".mode", m == GamePacks.KEEP ? GamePacks.KEEP : GamePacks.AUTO).apply(); }
    static long size(Context c, String id) { return file(c, id).length(); }

    /** the pack's bytes (a game opens it), or null when it is not here; opening it counts as using it */
    static byte[] open(Context c, String id) {
        File f = file(c, id);
        if (!f.isFile() || f.length() > GamePacks.PACK_MAX) return null;
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            byte[] b = Bounded.readAll(in, (int) GamePacks.PACK_MAX);
            sp(c).edit().putLong(id + ".used", System.currentTimeMillis()).apply();
            return b;
        } catch (Exception e) { return null; }
    }

    static void delete(Context c, String id) {
        file(c, id).delete();
        sp(c).edit().remove(id + ".ver").remove(id + ".used").apply();
    }

    /** «تلقائي» packs nobody opened for 14 days go (called when the games list opens) */
    static void sweep(Context c) {
        File[] fs = dir(c).listFiles();
        if (fs == null) return;
        long now = System.currentTimeMillis();
        for (File f : fs) {
            String n = f.getName();
            if (!n.endsWith(".gb")) continue;
            String id = n.substring(0, n.length() - 3);
            if (GamePacks.expired(mode(c, id), sp(c).getLong(id + ".used", 0), now)) delete(c, id);
        }
    }

    /** the index as last fetched (a file kept beside the packs), empty when never */
    static List<GamePacks.Pack> index(Context c) {
        File f = new File(dir(c), "index.json");
        if (!f.isFile() || f.length() > GamePacks.INDEX_MAX) return new ArrayList<>();
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) { return GamePacks.parse(Bounded.readAll(in, (int) GamePacks.INDEX_MAX)); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    /** in the background: the newest index, and the pack `id` if it is missing or older and there is room; `done` runs on that thread afterwards (null = nothing). Never while a round is on. */
    static void ensure(final Context c, final String id, final Runnable done) {
        final Context app = c.getApplicationContext();
        if (busy || QuizBank.playing) return;
        busy = true;
        Thread t = new Thread(() -> {
            try {
                Net.Reply r = Net.get(BASE + "index.json", null, 10000, 15000, (int) GamePacks.INDEX_MAX);
                if (r.status != 200 || GamePacks.parse(r.body).isEmpty()) return;
                try (FileOutputStream o = new FileOutputStream(new File(dir(app), "index.json"))) { o.write(r.body); }
                for (GamePacks.Pack p : GamePacks.parse(r.body)) {
                    if (!p.id.equals(id)) continue;
                    if (installed(app, id) && sp(app).getInt(id + ".ver", 0) >= p.ver) return;
                    if (!GamePacks.canDownload(app.getFilesDir().getUsableSpace(), p.bytes)) return;   // a TV low on space is left alone
                    Net.Reply d = Net.get(BASE + id + "/pack.gb", null, 20000, 120000, (int) GamePacks.PACK_MAX);
                    if (d.status != 200 || d.body.length != p.bytes || !GamePacks.shaOk(d.body, p.sha)) return;
                    File tmp = new File(dir(app), id + ".gb.tmp");
                    try (FileOutputStream o = new FileOutputStream(tmp)) { o.write(d.body); }
                    if (!tmp.renameTo(file(app, id))) { tmp.delete(); return; }
                    sp(app).edit().putInt(id + ".ver", p.ver).putLong(id + ".used", System.currentTimeMillis()).apply();
                }
            } catch (Exception ignored) {
                // no network, a bad answer: the games use what they have
            } finally {
                busy = false;
                if (done != null) done.run();
            }
        }, "game-packs");
        t.setPriority(Thread.MIN_PRIORITY); t.setDaemon(true); t.start();
    }
}
