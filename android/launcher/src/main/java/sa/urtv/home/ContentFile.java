package sa.urtv.home;

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
 * The same as the screensaver's sa.urtv.screensaver.ContentFile.
 */
final class ContentFile {
    private ContentFile() { }

    static final int MAX_BYTES = 64 * 1024;
    private static final Set<String> fetching = Collections.synchronizedSet(new HashSet<String>());

    static File kept(Context c, String name) { return new File(new File(c.getFilesDir(), "content"), name); }

    /** the kept copy, parsed (MiniJson), when it is an object with "v": 1; else null */
    /** 4.4 (QA-10, R1): a kept copy per file, parsed again only when the file's time or size changed; read off the UI thread (Kept.load) */
    static final class Kept {
        private final String name;
        private volatile Object value;
        private long at = -1, size = -1;
        Kept(String name) { this.name = name; }

        /** what the last load found (null: nothing yet, or no valid file); never touches the disk */
        Object value() { return value; }

        /** off the UI thread: the file again when its time or size moved */
        synchronized void load(Context c) {
            File f = kept(c, name);
            long t = f.isFile() ? f.lastModified() : 0, n = f.isFile() ? f.length() : 0;
            if (t == at && n == size) return;
            value = t == 0 ? null : read(c, name);
            at = t;
            size = n;
        }
    }

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
        fetch(c, name, maxAgeMs, () -> Net.get(DataHost.NEW + path, null, 15000, 20000, MAX_BYTES));
    }

    /**
     * 4.4: refresh() for a file CI still writes to an old branch of UrTV too: {@code oldUrl} is its DataHost.OLD address, and DataHost.get
     * asks urtv-data first and the old branch when urtv-data answers anything but 200 (a 404 too) or cannot be reached.
     */
    static void refreshFrom(final Context c, final String oldUrl, final String name, long maxAgeMs) {
        fetch(c, name, maxAgeMs, () -> DataHost.get(oldUrl, 15000, 20000, MAX_BYTES));
    }

    /** refresh() that tells when the attempt is over (done, failed or not needed, on a thread of its own or the caller's): the caller reads the kept copy again then; the games' release file uses it */
    static void refresh(final Context c, final String path, final String name, long maxAgeMs, final Runnable after) {
        fetch(c, name, maxAgeMs, () -> Net.get(DataHost.NEW + path, null, 15000, 20000, MAX_BYTES), after);
    }

    private interface Fetch { Net.Reply get() throws Exception; }

    private static void fetch(final Context c, final String name, long maxAgeMs, final Fetch how) { fetch(c, name, maxAgeMs, how, null); }

    private static void fetch(final Context c, final String name, long maxAgeMs, final Fetch how, final Runnable after) {
        final File f = kept(c, name);
        if ((f.isFile() && System.currentTimeMillis() - f.lastModified() < maxAgeMs) || !fetching.add(name)) { if (after != null) { try { after.run(); } catch (RuntimeException ignored) { } } return; }
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            try {
                Net.Reply r = how.get();
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
                if (after != null) { try { after.run(); } catch (RuntimeException ignored) { } }
            }
        }, "content-" + name).start();
    }
}
