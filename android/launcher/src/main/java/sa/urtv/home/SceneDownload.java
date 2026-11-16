package sa.urtv.home;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 4.3.3 «مشاهد Couch TV»: the second scene pack is not in the app; it is downloaded once, the first time it is chosen, from this repository's
 * release (the same rule as the photo packs: that one address only, its sha256 checked before anything is opened), unpacked into a work folder
 * and moved into place whole. A name in the zip that is not a scene file, a wrong digest or a zip over 12 MB: nothing is kept. A failure waits a
 * day before the next try; the Saudi pack shows meanwhile (SceneHome.load).
 */
final class SceneDownload {
    private SceneDownload() { }

    static final String URL = "https://github.com/tshi-dev/UrTV/releases/download/scenes-couch/couch.zip";
    /** the zip's sha256 (docs/mockups/PERFORMANCE.md): the release must hold exactly this file */
    static final String SHA256 = "COUCH_ZIP_SHA256";
    static final long MAX_BYTES = 12L * 1024 * 1024;
    private static final long RETRY_MS = 24L * 60 * 60 * 1000;
    private static volatile boolean busy;

    static File dir(Context c) { return new File(c.getFilesDir(), "scenes/couch"); }

    static boolean have(Context c) { return new File(dir(c), "scenes.tsv").isFile(); }

    /** a file of the zip that may be kept: the list, the credits, and a scene's photo, glass copy and cut-out */
    static boolean allowed(String name) {
        return name.equals("scenes.tsv") || name.equals("CREDITS.txt") || name.matches("[a-z][a-z0-9-]{1,23}(\\.jpg|-glass\\.jpg|-fg\\.webp)");
    }

    static void fetchIfNeeded(final Context app) {
        if (busy || have(app)) return;
        final android.content.SharedPreferences sp = app.getSharedPreferences("scenes", Context.MODE_PRIVATE);
        if (System.currentTimeMillis() - sp.getLong("failedAt", 0) < RETRY_MS) return;
        busy = true;
        new Thread(() -> {
            boolean ok = false;
            try { ok = fetch(app); } catch (Exception | OutOfMemoryError ignored) { }
            if (!ok) sp.edit().putLong("failedAt", System.currentTimeMillis()).apply();
            busy = false;
        }, "scene-pack").start();
    }

    private static boolean fetch(Context app) throws Exception {
        File zip = new File(app.getCacheDir(), "couch.zip");
        HttpURLConnection c = (HttpURLConnection) new URL(URL).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);   // the release's file is served from GitHub's own download host
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        long total = 0;
        try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(zip)) {
            byte[] buf = new byte[16384];
            for (int n; (n = in.read(buf)) > 0; ) {
                total += n;
                if (total > MAX_BYTES) return false;
                sha.update(buf, 0, n);
                out.write(buf, 0, n);
            }
        } finally {
            c.disconnect();
        }
        if (!hex(sha.digest()).equals(SHA256)) { zip.delete(); return false; }
        File work = new File(app.getFilesDir(), "scenes/couch.part");
        delete(work);
        if (!work.mkdirs()) return false;
        try (ZipInputStream z = new ZipInputStream(new java.io.FileInputStream(zip))) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) {
                if (e.isDirectory() || !allowed(e.getName())) { delete(work); zip.delete(); return false; }
                try (FileOutputStream out = new FileOutputStream(new File(work, e.getName()))) {
                    byte[] buf = new byte[16384];
                    for (int n; (n = z.read(buf)) > 0; ) out.write(buf, 0, n);
                }
            }
        }
        zip.delete();
        if (!new File(work, "scenes.tsv").isFile()) { delete(work); return false; }
        File done = dir(app);
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
