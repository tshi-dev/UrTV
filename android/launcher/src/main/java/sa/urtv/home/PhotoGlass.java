package sa.urtv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.security.MessageDigest;

/**
 * 4.4 «الساعة: زجاجية خلف الصورة» with «الخلفية: صوري وحزم الصور»: the big glass clock over the home's own photo, and, when the photo is one of ours with a
 * cut-out (made offline in CI by .github/tools/photo_masks.py, published in urtv-data masks/<set>/), the scenery over the clock. The glass is the photo
 * itself, shrunk to 160x90 and stretched back (a blur for nothing). The cut-out of the photo on the screen only is read: from this app's files when it
 * came before, else downloaded once and kept (its sha256 checked against masks/<set>/index.json); at most 40 are kept. A weak TV (LOW) draws the clock
 * with no cut-out. Made only while «الساعة: زجاجية» is chosen (MainActivity.sceneTick); nothing of it exists otherwise.
 */
final class PhotoGlass extends FrameLayout {
    static final String[] HOSTS = {"https://raw.githubusercontent.com/tshi-dev/urtv-data/data/masks/", "https://cdn.jsdelivr.net/gh/tshi-dev/urtv-data@data/masks/"};
    static final int MAX_MASK = 200 * 1024, KEEP = 40;
    private static final long INDEX_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

    private final Context app;
    private final int tier;
    private final SceneClock clock;
    private final ImageView mask;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean cardsShown, rtl = true, row;
    private Bitmap glass;   // the photo on the screen at 160x90, and its scenery's skyline (null: no cut-out), to place the digits again
    private int[] line;
    private int gen;
    private volatile boolean alive = true;

    PhotoGlass(Context c, int tier, Typeface digits) {
        super(c);
        app = c.getApplicationContext();
        this.tier = tier;
        clock = new SceneClock(c);
        clock.setTypeface(digits);
        mask = new ImageView(c);
        mask.setScaleType(ImageView.ScaleType.FIT_XY);
        addView(clock, new LayoutParams(-1, -1));
        addView(mask, new LayoutParams(0, 0));
        mask.setVisibility(GONE);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    void setTime(CharSequence t) { clock.setTime(t); }

    /** where the digits go follows the cards (beside the clock, or in «كنبة»'s strip under it) and the direction; a change moves them at once */
    void place(boolean cards, boolean rightToLeft, boolean cardsInRow) {
        if (cards == cardsShown && rightToLeft == rtl && cardsInRow == row) return;
        cardsShown = cards;
        rtl = rightToLeft;
        row = cardsInRow;
        if (glass != null) clock.setScene(glass, PhotoGlassPlan.box(line, width(), height(), cardsShown, rtl, row), false);
    }

    private int width() { return getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels; }

    private int height() { return getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels; }

    /**
     * the home showed a new photo (its key, as MainActivity names it): the clock in front of it at once, then its cut-out when it has one.
     * {@code net}: the network may be used now (the home in front, the screen on); a cut-out already kept is used either way.
     */
    void show(Bitmap photo, String wallKey, final boolean net) {
        if (photo == null || !alive) return;
        final int g = ++gen;
        final int w = width(), h = height();
        try { glass = Bitmap.createScaledBitmap(photo, 160, 90, true); } catch (RuntimeException | OutOfMemoryError e) { return; }
        line = null;
        mask.setVisibility(GONE);
        mask.setImageDrawable(null);
        clock.setScene(glass, PhotoGlassPlan.box(null, w, h, cardsShown, rtl, row), false);
        final String[] key = PhotoGlassPlan.key(wallKey);
        if (key == null || !ScenePlan.mask(tier)) return;   // the person's own photo, «الحرمين», or a weak TV: the clock in front
        final Bitmap shown = glass;
        new Thread(() -> {
            final Object[] got = load(key[0], key[1], net);
            if (got == null) return;
            ui.post(() -> {
                if (g != gen || !alive) return;
                int[] box = (int[]) got[1];
                line = (int[]) got[2];
                float k = h / 1080f;
                LayoutParams lp = new LayoutParams(Math.round(box[2] * k), Math.round(box[3] * k));
                lp.leftMargin = Math.round(box[0] * k);
                lp.topMargin = Math.round(box[1] * k);
                mask.setLayoutParams(lp);
                mask.setImageBitmap((Bitmap) got[0]);
                mask.setVisibility(VISIBLE);
                clock.setScene(shown, PhotoGlassPlan.box(line, w, h, cardsShown, rtl, row), false);
            });
        }, "photo-mask").start();
    }

    /** turned off: nothing more is read or drawn */
    void shutdown() { alive = false; gen++; glass = null; line = null; mask.setImageDrawable(null); clock.setScene(null, null, false); }

    /** {bitmap, box, skyline} of a photo's cut-out, from the kept file or downloaded once; null when it has none or it cannot be had */
    private Object[] load(String set, String name, boolean net) {
        try {
            File dir = new File(app.getFilesDir(), "masks/" + set);
            if (!dir.isDirectory() && !dir.mkdirs()) return null;
            File idx = new File(dir, "index.json");
            if (net && (!idx.isFile() || System.currentTimeMillis() - idx.lastModified() > INDEX_DAYS_MS)) {
                byte[] b = get(set + "/index.json", 512 * 1024);
                if (b != null) { File tmp = new File(dir, "index.tmp"); java.nio.file.Files.write(tmp.toPath(), b); if (!tmp.renameTo(idx)) tmp.delete(); }
            }
            if (!idx.isFile()) return null;
            org.json.JSONObject files = new org.json.JSONObject(new String(java.nio.file.Files.readAllBytes(idx.toPath()), "UTF-8")).optJSONObject("files");
            org.json.JSONObject rec = files == null ? null : files.optJSONObject(name);
            if (rec == null) return null;   // this photo has no clean sky edge: the clock in front
            String sha = rec.getString("sha256");
            File f = new File(dir, name + "-fg.webp");
            byte[] data = f.isFile() ? java.nio.file.Files.readAllBytes(f.toPath()) : null;
            if (data == null || !SceneDownload.hex(MessageDigest.getInstance("SHA-256").digest(data)).equals(sha)) {
                if (!net) return null;   // not kept, and no network now: the next photo asks again
                data = get(set + "/" + name + "-fg.webp", MAX_MASK);
                if (data == null || !SceneDownload.hex(MessageDigest.getInstance("SHA-256").digest(data)).equals(sha)) return null;
                File tmp = new File(dir, name + ".tmp");
                java.nio.file.Files.write(tmp.toPath(), data);
                if (!tmp.renameTo(f)) tmp.delete();
                prune(new File(app.getFilesDir(), "masks"));
            } else f.setLastModified(System.currentTimeMillis());
            org.json.JSONArray b = rec.getJSONArray("box"), s = rec.getJSONArray("skyline");
            int[] box = {b.getInt(0), b.getInt(1), b.getInt(2), b.getInt(3)};
            int[] line = new int[s.length()];
            for (int i = 0; i < line.length; i++) line[i] = s.getInt(i);
            Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
            return bm == null ? null : new Object[] {bm, box, line};
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    /** the first host that answers 200 */
    private static byte[] get(String path, int max) {
        for (String h : HOSTS) {
            try {
                Net.Reply r = Net.get(h + path, null, 20000, 40000, max);
                if (r.status == 200) return r.body;
            } catch (java.io.IOException ignored) { }
        }
        return null;
    }

    /** the cut-outs kept: the KEEP last used, the others deleted (the indexes stay) */
    private static void prune(File root) {
        java.util.List<File> all = new java.util.ArrayList<>();
        File[] sets = root.listFiles();
        if (sets != null) for (File s : sets) { File[] in = s.listFiles((d, n) -> n.endsWith("-fg.webp")); if (in != null) java.util.Collections.addAll(all, in); }
        if (all.size() <= KEEP) return;
        java.util.Collections.sort(all, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
        for (int i = KEEP; i < all.size(); i++) all.get(i).delete();
    }
}
