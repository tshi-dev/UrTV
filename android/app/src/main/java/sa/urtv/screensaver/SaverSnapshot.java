package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    /** the first picture 10 s after the page is live, then one every 3 minutes while it shows */
    static final long FIRST_MS = 10_000, EVERY_MS = 3 * 60 * 1000;
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

    /** one line for the log: what is on disk and why it is or is not used */
    static String why(Context c, String url, int w, int h) {
        try {
            File img = new File(dir(c), FILE), meta = new File(dir(c), META);
            if (!img.isFile() || !meta.isFile()) return "none saved yet";
            String madeKey = new String(java.nio.file.Files.readAllBytes(meta.toPath()), "UTF-8");
            long age = System.currentTimeMillis() - img.lastModified();
            if (!madeKey.equals(key(url, w, h))) return "found, but made for another page address or screen size (age " + age / 1000 + " s, " + img.length() + " bytes): not used";
            if (!usable(madeKey, img.lastModified(), key(url, w, h), System.currentTimeMillis())) return "found, too old (" + age / 1000 + " s): not used";
            return "found, age " + age / 1000 + " s, " + img.length() + " bytes: used";
        } catch (Exception e) { return "unreadable (" + e.getClass().getSimpleName() + ")"; }
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

    /**
     * The picture of the dream's window NOW, half size, while the page is showing (PixelCopy from the window, as the home's does), kept for the next start; a copy that
     * fails or is one flat colour (nothing painted) keeps the old picture. It was taken at the end of the dream before (View.draw of the WebView in onDetachedFromWindow):
     * by then the dream's window is gone, a detached Chromium view draws nothing into a software canvas, so what was kept was a blank, never the page: the tester's TV
     * showed the dark ground for 1 to 3 s at every start. Called a few seconds after the page is live and every few minutes while it shows (ScreenDream).
     */
    static void capture(final Context c, android.view.Window win, final String url, View page) {
        final int w = page.getWidth(), h = page.getHeight();
        if (win == null || w <= 0 || h <= 0 || page.getAlpha() < 1f || !page.isShown()) { android.util.Log.i("UrTVSaver", "snapshot: not taken (window " + (win != null) + ", " + w + "x" + h + ", alpha " + page.getAlpha() + ", shown " + page.isShown() + ")"); return; }
        if (android.os.Build.VERSION.SDK_INT < 26) return;
        final Context app = c.getApplicationContext();
        try {
            final Bitmap b = Bitmap.createBitmap(Math.round(w * SCALE), Math.round(h * SCALE), Bitmap.Config.ARGB_8888);
            android.view.PixelCopy.request(win, null, b, r -> {
                boolean ok = r == android.view.PixelCopy.SUCCESS, flat = ok && flat(b);
                android.util.Log.i("UrTVSaver", "snapshot: copy of the window " + (ok ? (flat ? "is one flat colour (kept the old picture)" : "ok, saving") : "failed, code " + r));
                if (ok && !flat) new Thread(() -> save(app, b, key(url, w, h)), "saver-snapshot").start();
                else b.recycle();
            }, new android.os.Handler(android.os.Looper.getMainLooper()));
        } catch (RuntimeException | OutOfMemoryError ignored) { }
    }

    /** one flat colour in a grid of 64 samples: nothing was painted (a failed copy of a window with no content) */
    static boolean flat(Bitmap b) {
        int first = b.getPixel(b.getWidth() / 16, b.getHeight() / 16);
        for (int y = 1; y < 16; y += 2) for (int x = 1; x < 16; x += 2) if (b.getPixel(x * b.getWidth() / 16, y * b.getHeight() / 16) != first) return false;
        return true;
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
            if (tmp.renameTo(new File(dir(c), FILE))) try (FileOutputStream m = new FileOutputStream(new File(dir(c), META))) { m.write(key.getBytes("UTF-8")); android.util.Log.i("UrTVSaver", "snapshot: saved " + best.length + " bytes"); }
        } catch (Exception ignored) {
        } finally { b.recycle(); }
    }
}
