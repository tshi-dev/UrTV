package sa.urtv.screensaver;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;

import java.io.InputStream;
import java.util.Calendar;

/**
 * The glass look's backdrop for this app's screens (the settings): the home app makes it once per photo and lends it through its
 * GlassProvider (signature permission sa.urtv.home.READ_GLASS). Read once per photo into one bitmap for the whole process; an
 * older home app, or none yet, gives the look's solid colour.
 */
final class GlassSource {
    private GlassSource() { }

    static final Uri BACKDROP = Uri.parse("content://sa.urtv.home.glass/backdrop");

    private static Bitmap cached;            // the one bitmap of this process
    private static long cachedStamp = -1;
    private static float scrimDark = 0.95f, scrimLight = 0.95f;   // the glass look's, from an older home app without the bounds
    private static int hi = 0xFFFFFFFF, lo = 0xFF000000;            // the backdrop's bounds (GlassStyle.bounds): any look's veil comes from them
    private static boolean bounded;
    private static int homeLight = -1;      // what the home shows: 1 light, 0 dark, -1 not known

    /**
     * The home's answer: the stamp of its backdrop and the veils (one small query); the bitmap read again only when the stamp moved. Both are
     * cross-process work: only ever on a thread of its own (page starts one), and the lock is held for the assignments alone, so a screen's
     * draw on the UI thread never waits on the home.
     */
    private static void refresh(Context c) {
        long stamp = 0;
        float sd = 0, sl = 0;
        int hl = -1, hHi = 0, hLo = 0;
        boolean ok = false, hasBounds = false;
        try (Cursor q = c.getContentResolver().query(BACKDROP, null, null, null, null)) {
            if (q != null && q.moveToFirst()) {
                stamp = q.getLong(0);
                sd = q.getFloat(1);
                sl = q.getFloat(2);
                hl = q.getInt(3);
                int h = q.getColumnIndex("glass_hi"), l = q.getColumnIndex("glass_lo");
                hasBounds = h >= 0 && l >= 0;
                if (hasBounds) { hHi = q.getInt(h); hLo = q.getInt(l); }
                ok = true;
            }
        } catch (RuntimeException e) {   // no home app, an older one without the provider, or no permission
            stamp = 0;
        }
        long had;
        synchronized (GlassSource.class) {
            if (ok) { scrimDark = sd; scrimLight = sl; homeLight = hl; bounded = hasBounds; if (hasBounds) { hi = hHi; lo = hLo; } }
            had = cachedStamp;
        }
        if (stamp == had) return;
        Bitmap bm = null;
        if (stamp != 0) {
            try (InputStream in = c.getContentResolver().openInputStream(BACKDROP)) {
                bm = in == null ? null : BitmapFactory.decodeStream(in);
            } catch (java.io.IOException | RuntimeException ignored) { }
        }
        synchronized (GlassSource.class) {
            cached = bm;
            cachedStamp = bm == null ? 0 : stamp;
        }
    }

    /** Light now? «المظهر» as kept here; automatic follows what the home shows, else the clock (6:00 to 18:30). */
    static boolean light(Context c) {
        String mode = new Prefs(c).appearance();
        if ("light".equals(mode)) return true;
        if (!"auto".equals(mode)) return false;
        if (homeLight >= 0) return homeLight == 1;
        Calendar now = Calendar.getInstance();
        int m = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return m >= 6 * 60 && m < 18 * 60 + 30;
    }

    /** the veil this look needs over this backdrop; an older home app gives only the glass look's two, any other look the strongest (the lock is held) */
    private static float scrim(GlassStyle.Look l) {
        return bounded ? GlassStyle.scrimAlpha(hi, lo, l) : l == GlassStyle.DARK ? scrimDark : l == GlassStyle.LIGHT ? scrimLight : 0.95f;
    }

    /**
     * A screen's page: the backdrop under its veil, in the look of now. Returns that look, for the screen's own colours.
     * 4.4 QA-12/QA-13: it paints what this process already knows of the home's backdrop (the look's solid colour before the first answer) and
     * never asks the home on the UI thread; the home is asked on a thread, and when it brings another picture or veil the page is painted
     * again, under the same look (a look that changed meanwhile waits for the screen's next render, which calls this again).
     */
    static GlassStyle.Look page(final Activity a, final View root) {
        final GlassStyle.Look l = look(a);
        Bitmap bm;
        float scrim;
        synchronized (GlassSource.class) { bm = cached; scrim = scrim(l); }
        root.setBackground(GlassPaint.page(a, bm, l, scrim));
        final Bitmap shown = bm;
        final float shownScrim = scrim;
        final Context app = a.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                refresh(app);
                final GlassStyle.Look now = look(app);
                final Bitmap bm2;
                final float scrim2;
                synchronized (GlassSource.class) { bm2 = cached; scrim2 = scrim(l); }
                if (now != l || (bm2 == shown && scrim2 == shownScrim)) return;
                a.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (!a.isDestroyed() && !a.isFinishing()) root.setBackground(GlassPaint.page(a, bm2, l, scrim2));
                    }
                });
            }
        }, "glass-refresh").start();
        return l;
    }

    /** A page in another look, for the previews of «المظهر»: the same one bitmap under the veil that look needs (no copy; what the screen's page last learned from the home). */
    static android.graphics.drawable.Drawable pageIn(Context c, GlassStyle.Look l) {
        Bitmap bm;
        float scrim;
        synchronized (GlassSource.class) { bm = cached; scrim = scrim(l); }
        return GlassPaint.page(c, bm, l, scrim);
    }

    /** The look of now: «المظهر ← الشكل» (Prefs.look, glass when unknown) in the light or the dark of now. */
    static GlassStyle.Look look(Context c) { return GlassStyle.of(new Prefs(c).look(), light(c)); }
}
