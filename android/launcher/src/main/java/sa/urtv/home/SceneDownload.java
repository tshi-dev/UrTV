package sa.urtv.home;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 4.4 the scene files not in the app: «مشاهد Couch TV» (couch) and the Saudi pack's other four scenes (saudi; the app has Riyadh only, the owner's
 * choice to keep the APK light), downloaded once, the first time the scenes are shown, from the data repository tshi-dev/urtv-data (branch data,
 * scenes/; the same host as DataHost, the jsDelivr copy of it when GitHub's raw host cannot be reached). scenes/<pack>.json says the zip's version,
 * sha256 and size; scenes/<pack>.zip is checked against them before anything is opened, unpacked into a work folder and moved into place whole.
 * A name in the zip that is not a scene file, a wrong digest or size, or a zip over 12 MB: nothing is kept. A failure waits a day before the next
 * try; the scenes the app has show meanwhile (SceneHome.load).
 */
final class SceneDownload {
    private SceneDownload() { }

    /** the data repository's scene folder, then its CDN copy (docs/mockups/PERFORMANCE.md) */
    static final String[] HOSTS = {"https://raw.githubusercontent.com/tshi-dev/urtv-data/data/scenes/", "https://cdn.jsdelivr.net/gh/tshi-dev/urtv-data@data/scenes/"};
    static final int MAX_BYTES = 12 * 1024 * 1024;
    private static final long RETRY_MS = 24L * 60 * 60 * 1000;
    private static final java.util.Set<String> busy = java.util.Collections.synchronizedSet(new java.util.HashSet<String>());

    /** the packs that can be downloaded: their names are the files' names (scenes/<pack>.json, scenes/<pack>.zip) and the folder's */
    static boolean known(String pack) { return ScenePlan.SAUDI.equals(pack) || ScenePlan.COUCH.equals(pack); }

    static File dir(Context c, String pack) { return new File(c.getFilesDir(), "scenes/" + pack); }

    static boolean have(Context c, String pack) { return new File(dir(c, pack), "scenes.tsv").isFile(); }

    /** a file of the zip that may be kept: the list, the credits, and a scene's photo, glass copy and cut-out */
    static boolean allowed(String name) {
        return name.equals("scenes.tsv") || name.equals("CREDITS.txt") || name.matches("[a-z][a-z0-9-]{1,23}(\\.jpg|-glass\\.jpg|-fg\\.webp)");
    }

    /** the index's sha256 and size, when they are a real digest and a size within MAX_BYTES; null for a bad index */
    static String[] index(String json) {
        if (json == null) return null;
        java.util.regex.Matcher sha = java.util.regex.Pattern.compile("\"sha256\"\\s*:\\s*\"([0-9a-f]{64})\"").matcher(json);
        java.util.regex.Matcher len = java.util.regex.Pattern.compile("\"bytes\"\\s*:\\s*(\\d{1,9})").matcher(json);
        if (!sha.find() || !len.find()) return null;
        long n = Long.parseLong(len.group(1));
        return n > 0 && n <= MAX_BYTES ? new String[] {sha.group(1), len.group(1)} : null;
    }

    /** the pack's files, once, in the background; {@code done} runs on that thread when they arrived (the home takes them at once) */
    static void fetchIfNeeded(final Context app, final String pack, final Runnable done) {
        if (!known(pack) || have(app, pack) || !busy.add(pack)) return;
        final android.content.SharedPreferences sp = app.getSharedPreferences("scenes", Context.MODE_PRIVATE);
        if (System.currentTimeMillis() - sp.getLong("failedAt:" + pack, 0) < RETRY_MS) { busy.remove(pack); return; }
        new Thread(() -> {
            boolean ok = false;
            try { ok = fetch(app, pack); } catch (Exception | OutOfMemoryError ignored) { }
            if (!ok) sp.edit().putLong("failedAt:" + pack, System.currentTimeMillis()).apply();
            busy.remove(pack);
            if (ok && done != null) done.run();
        }, "scene-pack").start();
    }

    /** the first host that answers 200 */
    private static byte[] get(String name, int maxBytes) {
        for (String h : HOSTS) {
            try {
                Net.Reply r = Net.get(h + name, null, 30000, 120000, maxBytes);
                if (r.status == 200) return r.body;
            } catch (java.io.IOException ignored) { }
        }
        return null;
    }

    private static boolean fetch(Context app, String pack) throws Exception {
        byte[] idx = get(pack + ".json", 16 * 1024);
        String[] want = index(idx == null ? null : new String(idx, "UTF-8"));
        if (want == null) return false;
        byte[] zip = get(pack + ".zip", MAX_BYTES);
        if (zip == null || zip.length != Integer.parseInt(want[1])) return false;
        if (!hex(MessageDigest.getInstance("SHA-256").digest(zip)).equals(want[0])) return false;
        File work = new File(app.getFilesDir(), "scenes/" + pack + ".part");
        delete(work);
        if (!work.mkdirs()) return false;
        try (ZipInputStream z = new ZipInputStream(new java.io.ByteArrayInputStream(zip))) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) {
                if (e.isDirectory() || !allowed(e.getName())) { delete(work); return false; }
                try (FileOutputStream out = new FileOutputStream(new File(work, e.getName()))) {
                    byte[] buf = new byte[16384];
                    for (int n; (n = z.read(buf)) > 0; ) out.write(buf, 0, n);
                }
            }
        }
        if (!new File(work, "scenes.tsv").isFile()) { delete(work); return false; }
        File done = dir(app, pack);
        delete(done);
        return work.renameTo(done);
    }

    static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format(java.util.Locale.ROOT, "%02x", x));
        return s.toString();
    }

    private static void delete(File f) {
        File[] in = f.listFiles();
        if (in != null) for (File x : in) delete(x);
        f.delete();
    }
}
