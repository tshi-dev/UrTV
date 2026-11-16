package sa.urtv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 4.3.3 «الآفاق»: the scene home's backdrop (the owner's choice in «المظهر» ← «الواجهة»; the home as it was stays the default). Two slots,
 * each a photo, the big glass clock (SceneClock) and the scenery cut out of the photo over the clock, so the time sits behind the mountains, the
 * towers or the rocks. The scenes take turns (ScenePlan.stayMs) with a 1.6 s fade. The «آفاق السعودية» pack is in the app's assets; «آفاق عالمية»
 * is downloaded once when chosen (SceneDownload) and read from the app's files. The rules for weak TVs are ScenePlan's (docs/mockups/PERFORMANCE.md):
 * LOW keeps one photo at 1280, no cut-out, no fade, a scene every 5 minutes. Pictures are decoded off the UI thread; only the scene showing and the
 * next one are in memory, and stop() (the home behind another app) lets the timer go.
 */
final class SceneHome extends FrameLayout {
    private final Context app;
    private final int tier;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Slot[] slots = new Slot[2];
    private int front = 0;
    private List<ScenePlan.Scene> all = new ArrayList<>(), scenes = new ArrayList<>();   // all: the packs' scenes; scenes: the ones taking turns now (ScenePlan.choose)
    private boolean byTime, night, byWeather, fog;
    private int index = -1, staySeconds = ScenePlan.STAY_S, gen = 0;
    private boolean on, running, cardsShown, rtl = true, glass = true;   // glass: «الساعة: زجاجية» (the clock and the cut-out); plain: the photo only
    private String packs = "";
    private CharSequence time = "";

    private final class Slot {
        final FrameLayout box = new FrameLayout(getContext());
        final ImageView photo = new ImageView(getContext()), mask = new ImageView(getContext());
        final SceneClock clock = new SceneClock(getContext());
        Bitmap photoBm, glassBm, maskBm;

        Slot() {
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mask.setScaleType(ImageView.ScaleType.FIT_XY);
            box.addView(photo, new LayoutParams(-1, -1));
            box.addView(clock, new LayoutParams(-1, -1));
            box.addView(mask, new LayoutParams(0, 0));
            box.setAlpha(0f);
        }

        void release() {
            photo.setImageDrawable(null);
            mask.setImageDrawable(null);
            clock.setScene(null, null, false);
            photoBm = glassBm = maskBm = null;   // no recycle(): a frame may still be drawing them; the GC takes them
        }
    }

    SceneHome(Context c, int tier, Typeface digits) {
        super(c);
        app = c.getApplicationContext();
        this.tier = tier;
        for (int i = 0; i < 2; i++) {
            slots[i] = new Slot();
            slots[i].clock.setTypeface(digits);
            addView(slots[i].box, new LayoutParams(-1, -1));
        }
        setVisibility(GONE);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    /** The settings, from the home's config read: the scene home on or off, the packs that are on, how long each scene stays, whether the cards are shown. */
    void set(boolean wanted, String packSetting, int seconds, boolean cards, boolean rightToLeft, boolean glassClock) {
        boolean changed = wanted != on || !String.valueOf(packSetting).equals(packs) || seconds != staySeconds || cards != cardsShown || rightToLeft != rtl || glassClock != glass;
        glass = glassClock;
        on = wanted;
        packs = String.valueOf(packSetting);
        staySeconds = seconds;
        cardsShown = cards;
        rtl = rightToLeft;
        if (!changed) return;
        if (!on) { stop(); setVisibility(GONE); for (Slot s : slots) s.release(); all = new ArrayList<>(); scenes = all; return; }
        setVisibility(VISIBLE);
        final int g = ++gen;
        io.execute(() -> {
            final List<ScenePlan.Scene> all = load(packs);
            ui.post(() -> { if (g != gen) return; this.all = all; scenes = ScenePlan.choose(all, byTime, night, byWeather, fog); index = -1; if (running) show(); });
        });
        for (String p : ScenePlan.packs(packs))   // the files not in the app, once; when they arrive the scenes are read again
            SceneDownload.fetchIfNeeded(app, p, () -> ui.post(() -> { if (on) { String keep = packs; packs = ""; set(true, keep, staySeconds, cardsShown, rtl, glass); } }));
    }

    boolean on() { return on; }

    /** The scene home turned off: the timer, the pictures and the decoding thread go (MainActivity then drops the whole layer). */
    void shutdown() {
        on = false;
        gen++;
        stop();
        for (Slot sl : slots) sl.release();
        all = new ArrayList<>();
        scenes = all;
        io.shutdownNow();
    }

    /**
     * «الأفق يتبع الوقت» and «الأفق يتبع الطقس», with the time of day (night: Maghrib to Fajr) and the city's weather (fog): when the scenes to show
     * change (Maghrib came, the fog lifted), the new set starts at once; else the turns go on as they were.
     */
    void mood(boolean followTime, boolean isNight, boolean followWeather, boolean isFog) {
        byTime = followTime; night = isNight; byWeather = followWeather; fog = isFog;
        if (!on || all.isEmpty()) return;
        List<ScenePlan.Scene> now = ScenePlan.choose(all, byTime, night, byWeather, fog);
        if (now.equals(scenes)) return;
        scenes = now;
        index = -1;
        if (running) { ui.removeCallbacksAndMessages(null); show(); }
    }

    /** the time as the home's clock shows it */
    void setTime(CharSequence t) {
        time = t == null ? "" : t;
        for (Slot s : slots) s.clock.setTime(time);
    }

    /** the home is in front: the scenes take turns (called on every return and every minute: a running timer is left as it is) */
    void start() {
        if (running) return;
        running = true;
        if (on && index < 0 && !scenes.isEmpty()) show(); else schedule();
    }

    void stop() { running = false; ui.removeCallbacksAndMessages(null); }

    /** the system asks for memory (Ram.trimNow): the slot not showing lets its pictures go */
    void trim() { slots[1 - front].release(); }

    private void schedule() {
        ui.removeCallbacksAndMessages(null);
        if (!running || !on || scenes.size() < 2) return;
        ui.postDelayed(this::show, ScenePlan.stayMs(tier, staySeconds));
    }

    /** the next scene: decoded off the UI thread into the slot behind, then faded in over the one showing */
    private void show() {
        if (!on || scenes.isEmpty()) return;
        final int at = ScenePlan.next(index, scenes.size());
        final ScenePlan.Scene sc = scenes.get(at);
        final int w = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        final int h = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
        final int g = gen;
        final Slot back = index < 0 ? slots[front] : slots[1 - front];
        io.execute(() -> {
            final Bitmap photo = decode(sc, ".jpg", ScenePlan.decodeWidth(tier, w), tier != ScenePlan.NORMAL);
            final boolean withClock = this.glass;   // «الساعة: عادية»: no glass copy, no cut-out (the header's clock shows)
            final Bitmap glass = withClock ? decode(sc, "-glass.jpg", 960, true) : null;
            final Bitmap mask = withClock && sc.mask != null && ScenePlan.mask(tier) ? decode(sc, "-fg.webp", 1920, false) : null;
            final float zoom = mask == null ? 1f : zoomOf(mask, sc);   // the owner: more of the digits above the scenery, the clock where it is
            ui.post(() -> {
                if (g != gen || !on) return;
                if (photo == null || (withClock && glass == null)) { index = at; schedule(); return; }   // a missing file: the next one, never a blank home
                back.photoBm = photo; back.glassBm = glass; back.maskBm = mask;
                back.photo.setImageBitmap(photo);
                back.clock.setScene(glass, withClock ? ScenePlan.clockBox(sc, w, h, cardsShown, rtl) : null, sc.dark);
                back.clock.setTime(time);
                if (mask != null) {
                    float k = h / 1080f;
                    LayoutParams lp = new LayoutParams(Math.round(sc.mask[2] * k), Math.round(sc.mask[3] * k));
                    lp.leftMargin = Math.round(sc.mask[0] * k);
                    lp.topMargin = Math.round(sc.mask[1] * k);
                    back.mask.setLayoutParams(lp);
                    back.mask.setImageBitmap(mask);
                    back.mask.setVisibility(VISIBLE);
                    back.mask.setPivotX(w / 2f - lp.leftMargin);   // the same zoom as the photo's: about the screen's top centre
                    back.mask.setPivotY(-lp.topMargin);
                } else back.mask.setVisibility(GONE);
                for (View v : new View[] {back.photo, back.mask}) { v.setScaleX(zoom); v.setScaleY(zoom); }
                back.photo.setPivotX(w / 2f);
                back.photo.setPivotY(0);
                back.clock.setZoom(zoom);
                final Slot was = slots[front];
                boolean first = index < 0;
                index = at;
                front = back == slots[0] ? 0 : 1;
                back.box.bringToFront();
                if (first || !ScenePlan.fade(tier)) {
                    back.box.setAlpha(1f);
                    if (was != back) { was.box.setAlpha(0f); was.release(); }
                } else {
                    back.box.setAlpha(0f);
                    back.box.animate().alpha(1f).setDuration(ScenePlan.FADE_MS).withEndAction(() -> { if (was != slots[front]) { was.box.setAlpha(0f); was.release(); } }).start();
                }
                schedule();
            });
        });
    }

    /** the zoom for this scene: its cut-out's skyline (made small first) against where its clock is (ScenePlan.zoom); 1 when it cannot be read */
    private float zoomOf(Bitmap mask, ScenePlan.Scene sc) {
        try {
            int sw = 96, sh = Math.max(1, Math.round(96f * mask.getHeight() / mask.getWidth()));
            Bitmap small = Bitmap.createScaledBitmap(mask, sw, sh, false);
            int[] px = new int[sw * sh];
            small.getPixels(px, 0, sw, 0, 0, sw, sh);
            if (small != mask) small.recycle();
            return ScenePlan.zoom(ScenePlan.skyline(px, sw, sh, sc.mask), ScenePlan.clockBox(sc, 1920, 1080, cardsShown, rtl), 1920, 1080);
        } catch (RuntimeException | OutOfMemoryError e) {
            return 1f;
        }
    }

    /**
     * The packs' scenes, in order: the Saudi pack's scene in the app (Riyadh) and its four others when they were downloaded; the Couch TV pack when it
     * was. Nothing downloaded yet: the scene in the app.
     */
    private List<ScenePlan.Scene> load(String setting) {
        List<ScenePlan.Scene> all = new ArrayList<>();
        for (String p : ScenePlan.packs(setting)) {
            if (ScenePlan.SAUDI.equals(p)) all.addAll(ScenePlan.parse(p, readAsset("scenes/saudi/scenes.tsv")));
            if (SceneDownload.have(app, p)) all.addAll(ScenePlan.parse(p, readFile(new File(SceneDownload.dir(app, p), "scenes.tsv"))));
        }
        if (all.isEmpty()) all.addAll(ScenePlan.parse(ScenePlan.SAUDI, readAsset("scenes/saudi/scenes.tsv")));
        return all;
    }

    /** a scene's file: in the app's assets (the Saudi pack's own scene) or in the downloaded pack's folder */
    private InputStream open(String pack, String name) throws java.io.IOException {
        File f = new File(SceneDownload.dir(app, pack), name);
        if (f.isFile()) return new FileInputStream(f);
        return app.getAssets().open("scenes/" + pack + "/" + name);
    }

    private String readAsset(String path) {
        try (InputStream in = app.getAssets().open(path)) { return text(in); } catch (Exception e) { return null; }
    }

    private String readFile(File f) {
        try (InputStream in = new FileInputStream(f)) { return text(in); } catch (Exception e) { return null; }
    }

    private static String text(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; (n = in.read(buf)) > 0; ) b.write(buf, 0, n);
        return b.toString("UTF-8");
    }

    /** a scene's file at no more than {@code width} px wide; RGB_565 for a picture with no transparency on a weak TV (half the memory) */
    private Bitmap decode(ScenePlan.Scene s, String suffix, int width, boolean cheap) {
        try (InputStream in = open(s.pack, s.id + suffix)) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            if (cheap && !suffix.endsWith(".webp")) o.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap b = BitmapFactory.decodeStream(in, null, o);
            if (b == null || b.getWidth() <= width) return b;
            Bitmap scaled = Bitmap.createScaledBitmap(b, width, Math.round(b.getHeight() * (width / (float) b.getWidth())), true);
            if (scaled != b) b.recycle();   // never drawn: safe to let go at once
            return scaled;
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }
}
