package sa.urtv.home;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.tv.TvContract;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Reads «كمّل المشاهدة» for the home screen from Android TV's Watch Next list (the way talkies does, MIT: THIRD_PARTY_NOTICES.md): what
 * one is in the middle of and the next episode. Only that row: the rows apps publish were the owner's «a mess, they look like ads»
 * (the TV test of 3.145). Needs READ_TV_LISTINGS, which the owner allows once in WatchNextActivity; without it nothing is read and the
 * home has no row, as before. Two steps, both blocking, off the main thread: {@link #load} the
 * rows and their words (the provider only, quick), then {@link #pictures} within a time budget, each picture small on disk
 * (files/tvrows) and in memory, never decoded at full size. TvRowsPick decides the order, the words and what may be read or opened.
 */
final class TvRows {
    static final String PERMISSION = "android.permission.READ_TV_LISTINGS";
    static final String EPG = "com.android.providers.tv.permission.READ_EPG_DATA";   // some TVs want it too to show other apps' rows
    private static final int MAX_WATCH_NEXT = 16;
    static final int ART_W = 400, ART_H = 400;   // a card is about a fifth of the screen wide; a tall poster no taller than this
    private static final long PICTURE_MS = 4000;   // one picture: a provider or a server that does not answer is given up on

    /** One card: what it says, where its picture is, and the link it opens (into its own app only, see {@link #open}). */
    static final class Item {
        String title = "", meta = "", pkg, link, artUri;
        float progress = -1f;
    }

    /** A row ready for the screen. */
    static final class Row {
        String key, title;
        final List<Item> items = new ArrayList<>();
    }

    /** A card's picture, ready (called on the thread that fetches; the screen posts it to its own). */
    interface Picture { void got(Item it, Bitmap b); }

    private TvRows() { }

    static boolean allowed(Context c) { return c.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED; }

    /** «كمّل المشاهدة» with its cards' words, no pictures yet; none without the permission or with nothing in it. */
    static List<Row> load(Context c) {
        List<Row> out = new ArrayList<>();
        if (!allowed(c)) return out;
        Row row = new Row();
        row.key = TvRowsPick.WATCH_NEXT;
        row.title = TvRowsPick.WATCH_NEXT_TITLE;
        row.items.addAll(watchNext(c));
        if (!row.items.isEmpty()) out.add(row);
        return out;
    }

    /** One fetch at a time, each given PICTURE_MS: a picture that hangs costs its own seconds, never the rows. */
    private static ExecutorService fetcher = Executors.newSingleThreadExecutor();

    /**
     * The cards' pictures, one by one as they come, until {@code budgetMs} is spent (what is left keeps its title on a plain card).
     * A picture already on disk costs no fetch. Files no card uses any more are deleted once every picture was tried.
     */
    static void pictures(Context c, List<Row> rows, long budgetMs, Picture then) {
        long end = System.currentTimeMillis() + budgetMs;
        File dir = new File(c.getFilesDir(), "tvrows");
        Set<String> keep = new HashSet<>();
        boolean all = true;
        for (Row r : rows) for (Item it : r.items) {
            if (it.artUri == null) continue;
            long left = end - System.currentTimeMillis();
            File f = onDisk(dir, it.artUri);
            if (f == null) {
                if (left <= 0) { all = false; continue; }
                f = fetch(c, dir, it, Math.min(left, PICTURE_MS));
            }
            if (f == null) continue;
            keep.add(f.getName());
            Bitmap b = decode(f);
            if (b != null) then.got(it, b);
        }
        if (!all) return;   // the budget ran out: keep the files, the next visit uses them
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) if (!keep.contains(f.getName())) f.delete();
    }

    /** «كمّل المشاهدة»: continue and next-episode entries, newest first (TvRowsPick.watchNextShown). */
    private static List<Item> watchNext(Context c) {
        List<Item> out = new ArrayList<>();
        PackageManager pm = c.getPackageManager();
        boolean latin = Digits.latin();
        String[] cols = {TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME, TvContract.WatchNextPrograms.COLUMN_TITLE,
                TvContract.WatchNextPrograms.COLUMN_EPISODE_TITLE, TvContract.WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER,
                TvContract.WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER, TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE,
                TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS,
                TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI, TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI,
                TvContract.WatchNextPrograms.COLUMN_INTENT_URI, TvContract.WatchNextPrograms.COLUMN_BROWSABLE};
        try (Cursor cur = c.getContentResolver().query(TvContract.WatchNextPrograms.CONTENT_URI, cols, null, null,
                TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS + " DESC")) {
            while (cur != null && cur.moveToNext() && out.size() < MAX_WATCH_NEXT) {
                String pkg = cur.getString(0);
                int type = cur.isNull(5) ? -1 : cur.getInt(5);
                boolean browsable = cur.isNull(11) || cur.getInt(11) != 0;
                if (!TvRowsPick.watchNextShown(type, browsable, pkg != null && pm.getLeanbackLaunchIntentForPackage(pkg) != null)) continue;
                Item it = new Item();
                it.pkg = pkg;
                it.title = str(cur, 1);
                it.meta = TvRowsPick.meta(str(cur, 3), str(cur, 4), str(cur, 2), it.title, "", label(pm, pkg), latin);
                it.progress = TvRowsPick.progress(cur.isNull(6) ? 0 : cur.getLong(6), cur.isNull(7) ? 0 : cur.getLong(7));
                it.link = str(cur, 10).isEmpty() ? null : str(cur, 10);
                String art = !str(cur, 8).isEmpty() ? str(cur, 8) : str(cur, 9);
                it.artUri = art.isEmpty() ? null : art;
                out.add(it);
            }
        } catch (RuntimeException e) {
            // no permission, or no TV provider: an empty «كمّل المشاهدة» (not shown)
        }
        return out;
    }

    /**
     * Opens a card: its link when it goes into the app that published the card (TvRowsPick.linkAllowed), pinned to that app and with
     * no permission grants of its own; else that app itself. True when something was started.
     */
    static boolean open(Context c, Item it) {
        if (it.pkg == null) return false;
        try {
            if (it.link != null) {
                Intent i = Intent.parseUri(it.link, Intent.URI_INTENT_SCHEME);
                String comp = i.getComponent() == null ? null : i.getComponent().getPackageName();
                if (TvRowsPick.linkAllowed(it.pkg, i.getPackage(), comp)) {
                    i.setSelector(null);
                    if (comp == null) i.setPackage(it.pkg);   // a plain https link too: it opens inside its own app, never another that answers it
                    i.removeFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);   // the home grants nothing
                    c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    return true;
                }
            }
        } catch (Exception e) {
            // a bad or refused link: the app itself instead
        }
        Intent app = c.getPackageManager().getLeanbackLaunchIntentForPackage(it.pkg);
        if (app == null) return false;
        try {
            c.startActivity(app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static File onDisk(File dir, String uri) {
        String base = Integer.toHexString(uri.hashCode()) + "_" + ART_W;
        File jpg = new File(dir, base + ".jpg"), png = new File(dir, base + ".png");
        return jpg.isFile() ? jpg : png.isFile() ? png : null;
    }

    /** Fetches a picture to disk within {@code ms} (a hung provider or server is left behind with its thread), or null. */
    private static synchronized File fetch(Context c, File dir, Item it, long ms) {
        Future<File> f = fetcher.submit(() -> store(c, dir, it));
        try {
            return f.get(ms, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            f.cancel(true);
            if (!(e instanceof java.util.concurrent.ExecutionException)) {   // timed out: that thread may hang on; a fresh one for the next
                fetcher.shutdownNow();
                fetcher = Executors.newSingleThreadExecutor();
            }
            return null;
        }
    }

    /** The picture as a small JPEG (PNG when it has transparency) on disk, within ART_W × ART_H; null for none. */
    private static File store(Context c, File dir, Item it) {
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        String base = Integer.toHexString(it.artUri.hashCode()) + "_" + ART_W;
        Bitmap b = null;
        try (InputStream in = stream(c, it)) {
            if (in == null) return null;
            byte[] bytes = Bounded.readAll(in, Bounded.IMAGE_MAX);   // a picture, not whatever the address sends
            b = small(bytes, Bitmap.Config.ARGB_8888);
            if (b == null) return null;
            boolean alpha = b.hasAlpha();
            File done = new File(dir, base + (alpha ? ".png" : ".jpg")), part = new File(dir, done.getName() + ".part");
            try (OutputStream out = new FileOutputStream(part)) {
                b.compress(alpha ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 85, out);
            }
            return part.renameTo(done) ? done : null;
        } catch (Exception | OutOfMemoryError e) {
            return null;   // no picture: the card keeps its title on its own colour
        } finally {
            if (b != null) b.recycle();
        }
    }

    /**
     * Decodes a picture no bigger than the card: its size read first, then decoded at a power-of-two fraction (TvRowsPick.sampleSize),
     * then scaled to fit ART_W × ART_H (the bigger one let go). Never the full size of what an app sends; null when it is no picture.
     */
    private static Bitmap small(byte[] bytes, Bitmap.Config config) {
        BitmapFactory.Options size = new BitmapFactory.Options();
        size.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, size);
        int sample = TvRowsPick.sampleSize(size.outWidth, size.outHeight, ART_W, ART_H);
        if (sample == 0) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        o.inPreferredConfig = config;
        Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
        if (b == null) return null;
        int[] fit = TvRowsPick.fitted(b.getWidth(), b.getHeight(), ART_W, ART_H);
        if (fit[0] == b.getWidth() && fit[1] == b.getHeight()) return b;
        Bitmap s = Bitmap.createScaledBitmap(b, fit[0], fit[1], true);
        if (s != b) b.recycle();
        return s;
    }

    /** A picture from disk for the screen (RGB_565: half the memory, a card has no transparency to keep), the same bounds. */
    private static Bitmap decode(File f) {
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
            return bytes.length > Bounded.IMAGE_MAX ? null : small(bytes, Bitmap.Config.RGB_565);
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    /**
     * A picture's bytes: over http(s) with timeouts, or a content:// picture from the publishing app's own provider only
     * (TvRowsPick.pictureProviderAllowed); nothing else (no file://, no other app's provider, no android.resource).
     */
    private static InputStream stream(Context c, Item it) throws Exception {
        String uri = it.artUri;
        if (uri.startsWith("https://") || uri.startsWith("http://")) {
            HttpURLConnection h = (HttpURLConnection) new URL(uri).openConnection();
            NetLog.count(String.valueOf(h.getURL()));   // G3: the request log (NetLog)
            h.setConnectTimeout((int) PICTURE_MS);
            h.setReadTimeout((int) PICTURE_MS);
            return h.getInputStream();
        }
        if (uri.startsWith("content://")) {
            Uri u = Uri.parse(uri);
            ProviderInfo p = u.getAuthority() == null ? null : c.getPackageManager().resolveContentProvider(u.getAuthority(), 0);
            if (p == null || !TvRowsPick.pictureProviderAllowed(it.pkg, p.packageName)) return null;
            ContentResolver cr = c.getContentResolver();
            return cr.openInputStream(u);
        }
        return null;
    }

    private static String str(Cursor c, int i) {
        String s = c.isNull(i) ? null : c.getString(i);
        return s == null || "null".equals(s) ? "" : s.trim();
    }

    static String label(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString().trim();
        } catch (Exception e) {
            return pkg;
        }
    }
}
