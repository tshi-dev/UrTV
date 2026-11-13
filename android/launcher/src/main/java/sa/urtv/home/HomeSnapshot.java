package sa.urtv.home;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;

/**
 * The home never shows a blank frame while its page and cards load: the last picture of the home is kept (half size, WebP, in this app's private
 * cache) and drawn at once when the home starts again, until the live views have painted their first real frame; then it fades out. Without a
 * usable picture (none, older than 24 h, another look, size or language) a skeleton in the look's own colours stands in.
 * The picture never leaves the cache folder. The rules (key, age, size, the swap) are plain and tested in tests/home_snapshot_test.py.
 */
final class HomeSnapshot {
    private HomeSnapshot() {}

    static final long MAX_AGE_MS = 24L * 60 * 60 * 1000;   // an older picture is not shown: the skeleton is
    static final int MAX_BYTES = 300 * 1024;               // the file's limit; the quality steps down until it fits
    static final int FADE_MS = 220, LIVE_WAIT_MS = 2500;   // the cross-fade; the longest the picture waits for the live frame
    static final long MIN_MS = 400, IDLE_MS = 10 * 60 * 1000;   // the picture stays at least this long; an idle home takes a new one this often
    static final float SCALE = 0.5f;

    private static final String DIR = "home_snapshot", FILE = "home.webp", META = "home.key";

    /** what a picture is good for: the look, the light or dark, the screen size and the language; any change and the picture is not used */
    static String key(Context c, int w, int h) {
        return GlassSource.lookName(c) + "|" + (GlassSource.light(c) ? "l" : "d") + "|" + w + "x" + h + "|" + S.lang();
    }

    /** true when a picture made at {@code madeAt} with {@code madeKey} may be drawn now under {@code nowKey} */
    static boolean usable(String madeKey, long madeAt, String nowKey, long now) {
        return madeKey != null && madeKey.equals(nowKey) && madeAt > 0 && now - madeAt >= 0 && now - madeAt <= MAX_AGE_MS;
    }

    private static File dir(Context c) { File d = new File(c.getCacheDir(), DIR); d.mkdirs(); return d; }

    static void clear(Context c) {
        File[] f = dir(c).listFiles();
        if (f != null) for (File x : f) x.delete();
    }

    /** the picture to draw, or null (none, stale, another key, unreadable) */
    static Bitmap load(Context c, int w, int h) {
        if (DebugFlags.on(c, "nosnapshot")) return null;   // a tester's switch
        try {
            File img = new File(dir(c), FILE), meta = new File(dir(c), META);
            if (!img.isFile() || !meta.isFile()) return null;
            String madeKey = new String(java.nio.file.Files.readAllBytes(meta.toPath()), "UTF-8");
            if (!usable(madeKey, img.lastModified(), key(c, w, h), System.currentTimeMillis())) return null;
            return BitmapFactory.decodeFile(img.getPath());
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    /** Takes the picture of the window now (API 26 PixelCopy, off the UI thread's drawing) and keeps it; any failure keeps the old one. */
    static void capture(Activity a, View root, Runnable done) {
        final int w = root.getWidth(), h = root.getHeight();
        if (w <= 0 || h <= 0 || a.isFinishing() || DebugFlags.on(a, "nosnapshot")) return;
        try {
            final Bitmap full = Bitmap.createBitmap(Math.round(w * SCALE), Math.round(h * SCALE), Bitmap.Config.ARGB_8888);
            final String key = key(a, w, h);
            final Context app = a.getApplicationContext();
            PixelCopy.request(a.getWindow(), null, full, r -> {
                if (r == PixelCopy.SUCCESS) new Thread(() -> { save(app, full, key); if (done != null) new Handler(Looper.getMainLooper()).post(done); }, "home-snapshot").start();
                else { android.util.Log.i("UrTVHome", "snapshot capture failed: PixelCopy result " + r); full.recycle(); }
            }, new Handler(Looper.getMainLooper()));
        } catch (RuntimeException | OutOfMemoryError e) { android.util.Log.i("UrTVHome", "snapshot capture failed: " + e.getClass().getSimpleName()); }
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
            if (best == null || best.length > MAX_BYTES) { android.util.Log.i("UrTVHome", "snapshot capture failed: too heavy (" + (best == null ? 0 : best.length) + " bytes)"); return; }   // too heavy even at the lowest quality: nothing is kept
            File tmp = new File(dir(c), FILE + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(best); }
            File img = new File(dir(c), FILE);
            if (tmp.renameTo(img)) try (FileOutputStream m = new FileOutputStream(new File(dir(c), META))) { m.write(key.getBytes("UTF-8")); android.util.Log.i("UrTVHome", "snapshot capture ok (" + best.length + " bytes)"); }
        } catch (Exception e) { android.util.Log.i("UrTVHome", "snapshot capture failed: " + e.getClass().getSimpleName());
        } finally { b.recycle(); }
    }

    /** the stand-in when there is no usable picture: soft blocks where the card and the tray are, in the look's own panel colour (no shimmer) */
    static View skeleton(Context c, GlassStyle.Look l) {
        return new View(c) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas cv) {
                float w = getWidth(), h = getHeight(), s = w / 1920f;
                p.setColor(l.base);
                cv.drawRect(0, 0, w, h, p);
                p.setColor(l.panel);
                cv.drawRoundRect(new RectF(96 * s, 54 * s, 360 * s, 126 * s), 20 * s, 20 * s, p);       // the top row
                cv.drawRoundRect(new RectF(96 * s, 178 * s, 656 * s, 496 * s), 28 * s, 28 * s, p);      // the card
                cv.drawRoundRect(new RectF(96 * s, 858 * s, 1824 * s, 1026 * s), 28 * s, 28 * s, p);    // the tray
            }
        };
    }
}
