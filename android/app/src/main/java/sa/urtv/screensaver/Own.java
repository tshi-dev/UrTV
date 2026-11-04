package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The owner's own photos (up to MAX), kept in this app's private folder files/own as 1.jpg, 2.jpg, ... with no
 * gaps. The screensaver page gets them through PhotoCache; the launcher reads them through ConfigProvider (read
 * only, READ_CONFIG). They only arrive through ADB (SaverReceiver PHOTOS, optimize/photos.sh).
 *
 * Every file is checked before it is kept, so the screen never decodes more than it can hold: a JPEG, at most
 * MAX_BYTES, at most MAX_SIDE a side and MAX_PIXELS in all (optimize/photos.sh sends 1920 px wide).
 */
final class Own {
    /** the most kept (4.1 reports asked for more than 10); a weak TV (Prefs.lowRam) keeps MAX_LOW: each photo is decoded at full screen size */
    static final int MAX = 30, MAX_LOW = 10;

    /** how many this TV keeps */
    static int max(Context c) { return Prefs.lowRam(c) ? MAX_LOW : MAX; }
    static final long MAX_BYTES = 3L << 20;
    static final int MAX_SIDE = 3840, MAX_PIXELS = 3840 * 2160;
    static final String MIX = "mix", ONLY = "only";

    private Own() {}

    static File dir(Context c) { return new File(c.getFilesDir(), "own"); }

    static File file(Context c, int n) { return new File(dir(c), n + ".jpg"); }

    /** where ConfigProvider lets the shell write the next photo before SaverReceiver adds it */
    static File incoming(Context c) { return new File(dir(c), "in.tmp"); }

    /** how many photos are kept (1.jpg up to the first gap) */
    static int count(Context c) {
        int n = 0;
        int max = max(c);   // a weak TV shows its first MAX_LOW (more kept from elsewhere stay on disk, unused)
        while (n < max && file(c, n + 1).isFile()) n++;
        return n;
    }

    /** the mode that applies: only counts while there is a photo, else mix (nothing changes for anyone with none) */
    static String mode(Context c) {
        return ONLY.equals(new Prefs(c).ownMode()) && count(c) > 0 ? ONLY : MIX;
    }

    /** "" when src is a photo we can keep, else why not */
    static String check(File src) {
        if (!src.isFile() || src.length() == 0) return "no such file";
        if (src.length() > MAX_BYTES) return "larger than " + (MAX_BYTES >> 20) + " MB (optimize/photos.sh shrinks it first)";
        try (InputStream in = new FileInputStream(src)) {
            if (in.read() != 0xFF || in.read() != 0xD8) return "not a JPEG";
        } catch (IOException e) {
            return "cannot read it";
        }
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(src.getPath(), o);
        if (o.outWidth <= 0 || o.outHeight <= 0) return "not a readable JPEG";
        if (o.outWidth > MAX_SIDE || o.outHeight > MAX_SIDE || (long) o.outWidth * o.outHeight > MAX_PIXELS)
            return "larger than 3840x2160 pixels";
        return "";
    }

    /** Keeps src as the next photo. null when done, else why not; src (in.tmp) is gone either way. */
    static String add(Context c, File src) {
        String why = keep(c, src);
        if (why != null && src.equals(incoming(c))) src.delete();   // never leave a refused file to be taken for the next one
        return why;
    }

    private static synchronized String keep(Context c, File src) {
        if (count(c) >= max(c)) return "already " + max(c) + " photos (delete one first)";
        String why = check(src);
        if (!why.isEmpty()) return why;
        File dir = dir(c);
        if (!dir.isDirectory() && !dir.mkdirs()) return "no room to keep it";
        File dest = file(c, count(c) + 1), tmp = new File(dir, "add.tmp");
        try {
            if (src.equals(incoming(c))) {
                if (!src.renameTo(dest)) return "no room to keep it";
            } else {
                try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[16384];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                if (!tmp.renameTo(dest)) return "no room to keep it";
            }
        } catch (IOException e) {
            return "cannot copy it (" + e.getMessage() + ")";
        } finally {
            tmp.delete();
        }
        new Prefs(c).bumpOwnRev();
        return null;
    }

    /** Removes photo n (from 1); the ones after it move up. */
    static synchronized boolean delete(Context c, int n) {
        int all = count(c);
        if (n < 1 || n > all) return false;
        file(c, n).delete();
        for (int k = n + 1; k <= all; k++) file(c, k).renameTo(file(c, k - 1));
        new Prefs(c).bumpOwnRev();
        return true;
    }

    static synchronized void clear(Context c) {
        File[] all = dir(c).listFiles();
        if (all != null) for (File f : all) f.delete();
        new Prefs(c).bumpOwnRev();
    }

    /** "245 KB 1920x1080" */
    static String describe(Context c, int n) {
        File f = file(c, n);
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        return (f.length() + 1023) / 1024 + " KB " + o.outWidth + "x" + o.outHeight;
    }
}
