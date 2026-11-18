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
    static void ensure(final Context c, final String id, final Runnable done) { ensure(c, id, false, done); }
    /** the same, for EVERY pack of questions (kind «quiz») the index lists: the ones the TV does not have or has older, one after the other, each only when there is room */
    static void ensureQuiz(final Context c, final Runnable done) { ensure(c, "", true, done); }
    /** the installed packs of questions, in the index's order (the quiz bank adds them to itself) */
    static List<GamePacks.Pack> quizPacks(Context c) {
        List<GamePacks.Pack> out = new ArrayList<>();
        for (GamePacks.Pack p : index(c)) if (p.quiz() && installed(c, p.id)) out.add(p);
        return out;
    }
    private static void ensure(final Context c, final String id, final boolean allQuiz, final Runnable done) {
        final Context app = c.getApplicationContext();
        if (busy || QuizBank.playing) return;
        busy = true;
        Thread t = new Thread(() -> {
            try {
                Net.Reply r = Net.get(BASE + "index.json", null, 10000, 15000, (int) GamePacks.INDEX_MAX);
                if (r.status != 200 || GamePacks.parse(r.body).isEmpty()) return;
                try (FileOutputStream o = new FileOutputStream(new File(dir(app), "index.json"))) { o.write(r.body); }
                for (GamePacks.Pack p : GamePacks.parse(r.body)) {
                    if (!(allQuiz ? p.quiz() : p.id.equals(id))) continue;
                    if (installed(app, p.id) && sp(app).getInt(p.id + ".ver", 0) >= p.ver) continue;
                    if (!GamePacks.canDownload(app.getFilesDir().getUsableSpace(), p.bytes)) return;   // a TV low on space is left alone
                    if (QuizBank.playing) return;   // a round started while the index came: not a big download now
                    if (!download(app, p)) return;
                    sp(app).edit().putInt(p.id + ".ver", p.ver).putLong(p.id + ".used", System.currentTimeMillis()).apply();
                }
            } catch (Throwable ignored) {   // also OutOfMemoryError: a TV with little memory must not crash a round for a download
                // no network, a bad answer: the games use what they have
            } finally {
                busy = false;
                if (done != null) done.run();
            }
        }, "game-packs");
        t.setPriority(Thread.MIN_PRIORITY); t.setDaemon(true); t.start();
    }
    /** one pack to a temporary file as it arrives (never held whole in memory), hashed on the way, then renamed over the pack: true when it is in place. A round that starts
     *  while it downloads stops it (the sink throws), and the temporary file never stays. */
    private static boolean download(Context app, GamePacks.Pack p) throws Exception {
        File tmp = new File(dir(app), p.id + ".gb.tmp");
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        boolean ok = false;
        try {
            try (java.io.OutputStream o = new java.io.BufferedOutputStream(new FileOutputStream(tmp), 32768)) {
                java.io.OutputStream sink = new java.io.OutputStream() {
                    @Override public void write(int b) throws java.io.IOException { write(new byte[] {(byte) b}, 0, 1); }
                    @Override public void write(byte[] b, int off, int len) throws java.io.IOException {
                        if (QuizBank.playing) throw new IllegalStateException("a round started")   /* not an IOException: Net would try the next address */;
                        md.update(b, off, len); o.write(b, off, len);
                    }
                };
                Net.Reply d = Net.get(BASE + p.id + "/pack.gb", null, 20000, 120000, (int) GamePacks.PACK_MAX, sink);
                if (d.status != 200) return false;
            }
            ok = tmp.length() == p.bytes && GamePacks.hex(md.digest()).equals(p.sha) && tmp.renameTo(file(app, p.id));
            return ok;
        } finally { if (!ok) tmp.delete(); }
    }
}
