package sa.urtv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 4.5 «تغيير الأيقونة»: an app's icon as the person made it in the editor (IconEditorActivity), kept as one small PNG in this app's own
 * files (icons/&lt;package&gt;.png, never on shared storage); the glass bar and «كل التطبيقات» draw it in place of the app's own. «الأيقونة
 * الأصلية» deletes it. Read and written off the main thread.
 */
final class IconStore {
    private IconStore() {}

    /** the side of a kept icon: the glass bar's biggest (96 × 150 % at 1080p) */
    static final int SIZE = 144;

    static File file(Context c, String pkg) {
        String safe = pkg == null ? "" : pkg.replaceAll("[^A-Za-z0-9._]", "_");
        return new File(new File(c.getFilesDir(), "icons"), safe + ".png");
    }

    /** a stamp that changes with the kept icon (0: none), for the caches */
    static long stamp(Context c, String pkg) {
        File f = file(c, pkg);
        return f.isFile() ? f.lastModified() : 0;
    }

    /** the person's icon for this app, or null */
    static Bitmap read(Context c, String pkg) {
        File f = file(c, pkg);
        if (!f.isFile()) return null;
        try { return BitmapFactory.decodeFile(f.getPath()); }
        catch (RuntimeException | OutOfMemoryError e) { return null; }
    }

    /** keeps it (written to a temporary file first, so a half-written icon is never read) */
    static boolean write(Context c, String pkg, Bitmap b) {
        File f = file(c, pkg), tmp = new File(f.getPath() + ".tmp");
        File dir = f.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return false;
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            if (!b.compress(Bitmap.CompressFormat.PNG, 100, out)) return false;
        } catch (java.io.IOException e) {
            return false;
        }
        return tmp.renameTo(f);
    }

    /** «الأيقونة الأصلية»: the app's own icon again */
    static void clear(Context c, String pkg) {
        File f = file(c, pkg);
        if (f.isFile() && !f.delete()) f.deleteOnExit();
    }
}
