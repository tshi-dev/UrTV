package sa.urtv.home;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

/**
 * The glass look's one backdrop, for the home app's screens: the home's photo at 1/8, blurred ONCE when the photo changes
 * ({@link #update}, on the photo's own thread), kept as one small file (glass/backdrop.png) and as one bitmap for the whole process.
 * The screensaver app reads the same file through GlassProvider (signature permission). Until there is one: the look's solid colour.
 */
public final class GlassSource {
    private GlassSource() { }

    static final String FILE = "glass/backdrop.png";

    private static Bitmap cached;            // the one bitmap of this process
    private static long cachedStamp = -1;

    static File file(Context c) { return new File(c.getFilesDir(), FILE); }

    private static SharedPreferences prefs(Context c) { return Dock.prefs(c); }

    /** The backdrop (one bitmap for every screen of this process), read again only when the file changed; null while there is none. */
    static synchronized Bitmap backdrop(Context c) {
        File f = file(c);
        long stamp = f.isFile() ? f.lastModified() : 0;
        if (stamp != cachedStamp) {
            cached = stamp == 0 ? null : BitmapFactory.decodeFile(f.getPath());
            cachedStamp = stamp;
        }
        return cached;
    }

    /**
     * The two colours that bound every pixel of the backdrop (GlassStyle.bounds), kept when it was made. Not known (a backdrop from
     * before «الشكل»): white and black, so every look takes the veil the brightest and the darkest photo would need.
     */
    static int[] bounds(Context c) {
        return new int[] {prefs(c).getInt("glass_hi", 0xFFFFFFFF), prefs(c).getInt("glass_lo", 0xFF000000)};
    }

    /** The veil this backdrop needs under this look (GlassStyle.scrimAlpha from its bounds: a few contrasts, not a pass over the photo). */
    static float scrim(Context c, GlassStyle.Look l) {
        int[] b = bounds(c);
        return GlassStyle.scrimAlpha(b[0], b[1], l);
    }

    /** Light now? «المظهر» as the screensaver keeps it; automatic follows what the home shows (it writes it), else the clock. */
    public static boolean light(Context c) {
        String mode = ConfigSnapshot.read(c).appearance();
        if (Look.AUTO.equals(mode) && prefs(c).contains("glass_home_light")) return prefs(c).getBoolean("glass_home_light", false);
        Calendar now = Calendar.getInstance();
        return Look.light(mode, null, now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE));
    }

    /** «المظهر ← الشكل» as the screensaver keeps it (glass, graphite, navy, clean); unknown or missing: glass. */
    public static String lookName(Context c) { return GlassStyle.look(ConfigSnapshot.read(c).look()); }

    /** The look of now: «الشكل» in the light or the dark of now. */
    public static GlassStyle.Look look(Context c) { return GlassStyle.of(lookName(c), light(c)); }

    /** A screen's page: the backdrop under its veil, in the look of now. Returns that look, for the screen's own colours. */
    public static GlassStyle.Look page(Activity a, View root) {
        GlassStyle.Look l = look(a);
        root.setBackground(GlassPaint.page(a, backdrop(a), l, scrim(a, l)));
        return l;
    }

    /** The backdrop's PNG for the phone page (/api/backdrop), or null while there is none; read bounded: it is small. */
    public static byte[] png(Context c) {
        File f = file(c);
        if (!f.isFile() || f.length() > 512 * 1024) return null;   // the small PNG update() makes; anything bigger is not it
        try (java.io.InputStream in = new java.io.FileInputStream(f)) {
            return Bounded.readAll(in, 512 * 1024);
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * For the phone page (/api/state): the backdrop's stamp (0: none), «الشكل», and the veils of that look in the dark and the light, in
     * thousandths (the JSON has whole numbers only).
     */
    public static java.util.Map<String, Object> phoneState(Context c) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        File f = file(c);
        String look = lookName(c);
        m.put("glass_stamp", f.isFile() ? f.lastModified() : 0L);
        m.put("glass_look", look);
        m.put("glass_dark", (long) Math.round(scrim(c, GlassStyle.of(look, false)) * 1000));
        m.put("glass_light", (long) Math.round(scrim(c, GlassStyle.of(look, true)) * 1000));
        return m;
    }

    /** Two colours channel by channel: the brighter (or the darker) of each channel. */
    private static int channels(int a, int b, boolean brighter) {
        int out = 0xFF000000;
        for (int shift : new int[] {16, 8, 0}) {
            int x = (a >> shift) & 255, y = (b >> shift) & 255;
            out |= (brighter ? Math.max(x, y) : Math.min(x, y)) << shift;
        }
        return out;
    }

    /** The home tells the other screens which look it shows («تلقائي» follows it). */
    static void setHomeLight(Context c, boolean light) {
        if (prefs(c).contains("glass_home_light") && prefs(c).getBoolean("glass_home_light", false) == light) return;
        prefs(c).edit().putBoolean("glass_home_light", light).apply();
    }

    /**
     * Off the main thread, with the photo the home just showed: the backdrop made again if it is another photo ({@code key}). The
     * photo is scaled down by Android first (to 2x the backdrop), then averaged and blurred by GlassStyle: a few small arrays.
     */
    static void update(Context c, Bitmap photo, String key) {
        if (photo == null || key == null) return;
        SharedPreferences p = prefs(c);
        File f = file(c);
        if (key.equals(p.getString("glass_key", null)) && f.isFile()) return;
        try {
            Bitmap small = Bitmap.createScaledBitmap(photo, GlassStyle.W * 2, GlassStyle.H * 2, true);
            int[] px = new int[GlassStyle.W * 2 * GlassStyle.H * 2];
            small.getPixels(px, 0, GlassStyle.W * 2, 0, 0, GlassStyle.W * 2, GlassStyle.H * 2);
            if (small != photo) small.recycle();
            int[] blurred = GlassStyle.blur(GlassStyle.downscale(px, GlassStyle.W * 2, GlassStyle.H * 2), GlassStyle.W, GlassStyle.H);
            int[] b = GlassStyle.bounds(blurred);
            Bitmap out = Bitmap.createBitmap(blurred, GlassStyle.W, GlassStyle.H, Bitmap.Config.ARGB_8888);
            File dir = f.getParentFile();
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            File tmp = new File(dir, "backdrop.tmp");
            try (FileOutputStream o = new FileOutputStream(tmp)) { out.compress(Bitmap.CompressFormat.PNG, 100, o); }
            out.recycle();
            // while the file changes a reader may get either photo: bounds that hold both first (the stronger veil), then this photo's own
            int[] was = bounds(c);
            p.edit().putInt("glass_hi", channels(b[0], was[0], true)).putInt("glass_lo", channels(b[1], was[1], false)).commit();
            if (!tmp.renameTo(f)) { tmp.delete(); return; }
            p.edit().putString("glass_key", key).putInt("glass_hi", b[0]).putInt("glass_lo", b[1]).commit();
        } catch (java.io.IOException | RuntimeException ignored) {
            // no backdrop this time: the screens keep the last one, or the solid colour
        }
    }
}
