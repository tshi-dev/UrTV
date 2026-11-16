package sa.urtv.screensaver;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 4.3.3 A small hand-edited file of tshi-dev/urtv-data (docs/SCHEMAS.md there: content/), kept in this app's files: read from the kept copy
 * at once (never the network on the caller's thread), and fetched again in the background at most once in {@code maxAgeMs}. Only a parsed
 * object with "v": 1 replaces the kept copy, so a bad answer leaves the last good one. No file, no network: null, and the app is as before.
 */
final class ContentFile {
    private ContentFile() { }

    static final int MAX_BYTES = 64 * 1024;
    private static final Set<String> fetching = Collections.synchronizedSet(new HashSet<String>());

    static File kept(Context c, String name) { return new File(new File(c.getFilesDir(), "content"), name); }

    /** the kept copy, parsed (MiniJson), when it is an object with "v": 1; else null */
    static Object read(Context c, String name) {
        File f = kept(c, name);
        if (!f.isFile()) return null;
        try (InputStream in = new FileInputStream(f)) {
            return valid(MiniJson.parse(new String(Bounded.readAll(in, MAX_BYTES), "UTF-8")));
        } catch (Exception e) {
            return null;
        }
    }

    static Object valid(Object o) {
        if (!(o instanceof java.util.Map)) return null;
        Object v = ((java.util.Map<?, ?>) o).get("v");
        return v instanceof Double && (Double) v == 1.0 ? o : null;
    }

    /** a background fetch of urtv-data's {@code path} into the kept {@code name} when the copy is older than {@code maxAgeMs} (or missing) */
    static void refresh(final Context c, final String path, final String name, long maxAgeMs) {
        final File f = kept(c, name);
        if ((f.isFile() && System.currentTimeMillis() - f.lastModified() < maxAgeMs) || !fetching.add(name)) return;
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            try {
                Net.Reply r = Net.get(DataHost.NEW + path, null, 15000, 20000, MAX_BYTES);
                if (r.status != 200 || valid(MiniJson.parse(r.text())) == null) return;
                File dir = f.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()) return;
                File tmp = new File(dir, name + ".tmp");
                try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(r.body); }
                if (!tmp.renameTo(f)) tmp.delete();
            } catch (Exception ignored) {
                // offline: the kept copy stays; tried again after maxAgeMs
            } finally {
                fetching.remove(name);
            }
        }, "content-" + name).start();
    }
}
