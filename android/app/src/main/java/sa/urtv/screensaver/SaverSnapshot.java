package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;

/**
 * The screensaver page never starts blank: the last picture of it (half size, WebP, at most 300 KB, in this app's private cache) is drawn when the dream
 * starts, until the live page has painted, then it fades out over 220 ms. A picture older than 24 h, or made for another page address (the address holds the look, the
 * language and the settings) or screen size, is not used: the page's own ground colour stands in. Same rules as the launcher's HomeSnapshot.
 */
final class SaverSnapshot {
    private SaverSnapshot() {}

    static final long MAX_AGE_MS = 24L * 60 * 60 * 1000;
    static final int MAX_BYTES = 300 * 1024, FADE_MS = 220, LIVE_WAIT_MS = 3000;
    static final float SCALE = 0.5f;
    private static final String DIR = "saver_snapshot", FILE = "saver.webp", META = "saver.key";

    static String key(String url, int w, int h) { return Integer.toHexString(url.hashCode()) + "|" + w + "x" + h; }

    static boolean usable(String madeKey, long madeAt, String nowKey, long now) {
        return madeKey != null && madeKey.equals(nowKey) && madeAt > 0 && now - madeAt >= 0 && now - madeAt <= MAX_AGE_MS;
    }

    private static File dir(Context c) { File d = new File(c.getCacheDir(), DIR); d.mkdirs(); return d; }

    static void clear(Context c) {
        File[] f = dir(c).listFiles();
        if (f != null) for (File x : f) x.delete();
    }

    static Bitmap load(Context c, String url, int w, int h) {
        try {
            File img = new File(dir(c), FILE), meta = new File(dir(c), META);
            if (!img.isFile() || !meta.isFile()) return null;
            String madeKey = new String(java.nio.file.Files.readAllBytes(meta.toPath()), "UTF-8");
            if (!usable(madeKey, img.lastModified(), key(url, w, h), System.currentTimeMillis())) return null;
            return BitmapFactory.decodeFile(img.getPath());
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    /** the live page drawn at half size (only once it has painted: the view is shown), kept for the next start; a failure keeps the old one */
    static void capture(Context c, View page, String url) {
        int w = page.getWidth(), h = page.getHeight();
        if (w <= 0 || h <= 0 || page.getAlpha() < 1f) return;
        try {
            Bitmap b = Bitmap.createBitmap(Math.round(w * SCALE), Math.round(h * SCALE), Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(b);
            cv.scale(SCALE, SCALE);
            page.draw(cv);
            save(c, b, key(url, w, h));
        } catch (RuntimeException | OutOfMemoryError ignored) { }
    }

    @SuppressWarnings("deprecation")
    private static void save(Context c, Bitmap b, String key) {
        try {
            byte[] best = null;
            for (int q = 70; q >= 30; q -= 10) {
                java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
                b.compress(Bitmap.CompressFormat.WEBP, q, o);
                best = o.toByteArray();
                if (best.length <= MAX_BYTES) break;
            }
            if (best == null || best.length > MAX_BYTES) return;
            File tmp = new File(dir(c), FILE + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(best); }
            if (tmp.renameTo(new File(dir(c), FILE))) try (FileOutputStream m = new FileOutputStream(new File(dir(c), META))) { m.write(key.getBytes("UTF-8")); }
        } catch (Exception ignored) {
        } finally { b.recycle(); }
    }
}
