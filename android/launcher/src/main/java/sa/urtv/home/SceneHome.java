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
 * 4.3.3 «المشاهد»: the scene home's backdrop (the owner's choice in «الشكل والمشاهد» ← «الواجهة»; the home as it was stays the default). Two slots,
 * each a photo, the big glass clock (SceneClock) and the scenery cut out of the photo over the clock, so the time sits behind the mountains, the
 * towers or the rocks. The scenes take turns (ScenePlan.stayMs) with a 1.6 s fade. The «مشاهد السعودية» pack is in the app's assets; «مشاهد Couch TV»
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
    private List<ScenePlan.Scene> scenes = new ArrayList<>();
    private int index = -1, staySeconds = ScenePlan.STAY_S, gen = 0;
    private boolean on, running, cardsShown, rtl = true;
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
    void set(boolean wanted, String packSetting, int seconds, boolean cards, boolean rightToLeft) {
        boolean changed = wanted != on || !String.valueOf(packSetting).equals(packs) || seconds != staySeconds || cards != cardsShown || rightToLeft != rtl;
        on = wanted;
        packs = String.valueOf(packSetting);
        staySeconds = seconds;
        cardsShown = cards;
        rtl = rightToLeft;
        if (!changed) return;
        if (!on) { stop(); setVisibility(GONE); for (Slot s : slots) s.release(); scenes = new ArrayList<>(); return; }
        setVisibility(VISIBLE);
        final int g = ++gen;
        io.execute(() -> {
            final List<ScenePlan.Scene> all = load(packs);
            ui.post(() -> { if (g != gen) return; scenes = all; index = -1; if (running) show(); });
        });
        if (ScenePlan.packs(packs).contains(ScenePlan.COUCH)) SceneDownload.fetchIfNeeded(app);
    }

    boolean on() { return on; }

    /** the time as the home's clock shows it */
    void setTime(CharSequence t) {
        time = t == null ? "" : t;
        for (Slot s : slots) s.clock.setTime(time);
    }

    void start() { running = true; if (on && index < 0 && !scenes.isEmpty()) show(); else schedule(); }

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
            final Bitmap glass = decode(sc, "-glass.jpg", 960, true);
            final Bitmap mask = sc.mask != null && ScenePlan.mask(tier) ? decode(sc, "-fg.webp", 1920, false) : null;
            ui.post(() -> {
                if (g != gen || !on) return;
                if (photo == null || glass == null) { index = at; schedule(); return; }   // a missing file: the next one, never a blank home
                back.photoBm = photo; back.glassBm = glass; back.maskBm = mask;
                back.photo.setImageBitmap(photo);
                back.clock.setScene(glass, ScenePlan.clockBox(sc, w, h, cardsShown, rtl), sc.dark);
                back.clock.setTime(time);
                if (mask != null) {
                    float k = h / 1080f;
                    LayoutParams lp = new LayoutParams(Math.round(sc.mask[2] * k), Math.round(sc.mask[3] * k));
                    lp.leftMargin = Math.round(sc.mask[0] * k);
                    lp.topMargin = Math.round(sc.mask[1] * k);
                    back.mask.setLayoutParams(lp);
                    back.mask.setImageBitmap(mask);
                    back.mask.setVisibility(VISIBLE);
                } else back.mask.setVisibility(GONE);
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

    /** The packs' scenes, in order: the Saudi pack from the assets, the Couch TV pack from the files when it was downloaded. */
    private List<ScenePlan.Scene> load(String setting) {
        List<ScenePlan.Scene> all = new ArrayList<>();
        for (String p : ScenePlan.packs(setting)) all.addAll(ScenePlan.parse(p, read(p, "scenes.tsv")));
        if (all.isEmpty()) all.addAll(ScenePlan.parse(ScenePlan.SAUDI, read(ScenePlan.SAUDI, "scenes.tsv")));   // Couch TV not downloaded yet: the Saudi pack
        return all;
    }

    private InputStream open(String pack, String name) throws java.io.IOException {
        if (ScenePlan.SAUDI.equals(pack)) return app.getAssets().open("scenes/" + pack + "/" + name);
        return new FileInputStream(new File(SceneDownload.dir(app), name));
    }

    private String read(String pack, String name) {
        try (InputStream in = open(pack, name)) {
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0; ) b.write(buf, 0, n);
            return b.toString("UTF-8");
        } catch (Exception e) {
            return null;
        }
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
