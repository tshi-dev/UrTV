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

    /** 4.4 the home's photo layer zoomed by k about the screen's top centre (the cut-out and the glass here follow it); 1 puts it back */
    interface Zoom { void zoom(float k); }
    private Zoom zoomTo = k -> { };

    void onZoom(Zoom z) { zoomTo = z; }

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
        if (glass != null) { int[] b = PhotoGlassPlan.box(line, width(), height(), cardsShown, rtl, row); clock.setScene(glass, b, pale(b)); }
    }

    private int[] glassPx;

    /** a pale photo behind the digits: the dark glass (PhotoGlassPlan.pale) */
    private boolean pale(int[] digits) { return PhotoGlassPlan.pale(glassPx, 160, 90, digits, width(), height()); }

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
        glassPx = new int[160 * 90];
        glass.getPixels(glassPx, 0, 160, 0, 0, 160, 90);
        line = null;
        mask.setVisibility(GONE);
        mask.setImageDrawable(null);
        clock.setZoom(1f);
        zoomTo.zoom(1f);
        int[] first = PhotoGlassPlan.box(null, w, h, cardsShown, rtl, row);
        clock.setScene(glass, first, pale(first));
        final String[] key = PhotoGlassPlan.key(wallKey);
        final int own = PhotoGlassPlan.own(wallKey);
        if ((key == null && own == 0) || !ScenePlan.mask(tier)) return;   // «الحرمين», an own photo with no mask, or a weak TV: the clock in front
        final Bitmap shown = glass;
        final int sw = w, sh = h;
        new Thread(() -> {
            final Object[] got = own > 0 ? loadOwn(own, photo, sw, sh) : load(key[0], key[1], net);
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
                int[] digits = PhotoGlassPlan.box(line, w, h, cardsShown, rtl, row);
                float z = ScenePlan.zoom(line, digits, w, h);   // the owner: the clock where it is, the photo a little lower: more of the digits above the scenery
                mask.setPivotX(w / 2f - lp.leftMargin);
                mask.setPivotY(-lp.topMargin);
                mask.setScaleX(z);
                mask.setScaleY(z);
                mask.setVisibility(VISIBLE);
                clock.setScene(shown, digits, pale(digits));
                clock.setZoom(z);
                zoomTo.zoom(z);
            });
        }, "photo-mask").start();
    }

    /** turned off: nothing more is read or drawn */
    void shutdown() { alive = false; gen++; glass = null; line = null; mask.setImageDrawable(null); clock.setScene(null, null, false); zoomTo.zoom(1f); }

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

    /**
     * 4.4 {bitmap, box, skyline} of the person's own photo N with the mask they sent with it (docs/OWN-PHOTO-MASK.md; the screensaver app keeps it
     * beside the photo and serves it read only): the photo at half the screen, cropped as the home shows it, its alpha the mask's light; null with no mask.
     */
    private Object[] loadOwn(int n, Bitmap photo, int sw, int sh) {
        try (java.io.InputStream in = app.getContentResolver().openInputStream(android.net.Uri.parse("content://sa.urtv.screensaver.config/own/" + n + "/mask"))) {
            if (in == null) return null;
            byte[] data = Bounded.readAll(in, 1_000_000);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            if (o.outWidth <= 0) return null;
            o.inJustDecodeBounds = false;
            o.inSampleSize = Math.max(1, Math.min(o.outWidth / 960, o.outHeight / 540));
            Bitmap m = BitmapFactory.decodeByteArray(data, 0, data.length, o);
            if (m == null || !alive) return null;
            int w = 960, h = Math.max(1, Math.round(960f * sh / sw));
            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
            android.graphics.Rect to = new android.graphics.Rect(0, 0, w, h);
            int[] cp = PhotoGlassPlan.crop(photo.getWidth(), photo.getHeight(), sw, sh), cm = PhotoGlassPlan.crop(m.getWidth(), m.getHeight(), sw, sh);
            Bitmap fg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888), mm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            new android.graphics.Canvas(fg).drawBitmap(photo, new android.graphics.Rect(cp[0], cp[1], cp[2], cp[3]), to, paint);
            new android.graphics.Canvas(mm).drawBitmap(m, new android.graphics.Rect(cm[0], cm[1], cm[2], cm[3]), to, paint);
            m.recycle();
            int[] a = new int[w * h], b = new int[w * h];
            fg.getPixels(a, 0, w, 0, 0, w, h);
            mm.getPixels(b, 0, w, 0, 0, w, h);
            mm.recycle();
            for (int i = 0; i < a.length; i++) a[i] = (a[i] & 0x00FFFFFF) | (PhotoGlassPlan.luma(b[i]) << 24);
            fg.setPixels(a, 0, w, 0, 0, w, h);
            return new Object[] {fg, new int[] {0, 0, Math.round(1080f * sw / sh), 1080}, PhotoGlassPlan.skyline(b, w, h)};
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
