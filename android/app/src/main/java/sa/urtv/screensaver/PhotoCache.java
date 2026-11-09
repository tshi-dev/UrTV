package sa.urtv.screensaver;

import android.content.Context;
import android.webkit.WebResourceResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Answers the page's font and photo requests from the device instead of the network, so the screensaver
 * opens at once. Fonts and bundled photos come from the APK; hosted photos are downloaded once and kept.
 */
final class PhotoCache {
    static final String REPO = "https://raw.githubusercontent.com/tshi-dev/UrTV/" + BuildConfig.BRANCH + "/";
    private static final String BUNDLED = REPO + "assets/backgrounds/", HOSTED = REPO + "photos/",
        FONTS = REPO + "assets/fonts/";
    /** the owner's own photos (Own): not a real address, the page asks for N.jpg here and only this app answers */
    static final String OWN = "https://own.urtv.invalid/";
    /** the downloaded photo packs (PackStore): not a real address either; the page asks for ID/N.jpg and ID/credits.json here */
    static final String PACKS = "https://packs.urtv.invalid/";
    private static final int KEEP = 12;   // the 5-photo set plus room for the next one

    private PhotoCache() {}

    /** null lets the WebView load the URL itself */
    static WebResourceResponse serve(Context c, String requested) {
        String url = Channel.own(requested, REPO);   // a next build gets the page's master URLs too
        try {
            if (url.startsWith(FONTS)) {
                String name = url.substring(FONTS.length());
                if (!name.matches("[A-Za-z0-9_-]+\\.woff2")) return null;
                // fonts are fetched with CORS, and the page's origin is file://
                java.util.Map<String, String> h = new java.util.HashMap<>();
                h.put("Access-Control-Allow-Origin", "*");
                return new WebResourceResponse("font/woff2", null, 200, "OK", h, c.getAssets().open("fonts/" + name));
            }
            if (url.startsWith(OWN)) {
                String name = url.substring(OWN.length());
                if ("dates.json".equals(name)) return ownDates(c);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("([1-9]|[1-4][0-9]|50)\\.jpg(\\?s=([0-9]{3,4}))?").matcher(name);
                if (!m.matches()) return null;
                File f = Own.file(c, Integer.parseInt(m.group(1)));
                if (!f.isFile()) return null;
                // «ألبوم صوري»: one photo at a time at the card's size (?s= the long side), so a weak TV decodes a small one, not the whole photo
                if (m.group(3) != null) {
                    byte[] small = PhotoShrink.toSize(readAll(f), Math.min(1920, Integer.parseInt(m.group(3))));
                    return jpeg(new java.io.ByteArrayInputStream(small));
                }
                return jpeg(new FileInputStream(f));
            }
            if (url.startsWith(PACKS)) return pack(c, url.substring(PACKS.length()));
            if (url.startsWith(BUNDLED)) {
                String name = safeName(url.substring(BUNDLED.length()));
                return name == null ? null : jpeg(c.getAssets().open("backgrounds/" + name));
            }
            if (url.startsWith(HOSTED)) {
                String name = safeName(url.substring(HOSTED.length()));
                if (name == null) return null;   // index.json and anything else go to the network
                File f;
                try { f = hosted(c, url, name); } catch (java.io.IOException e) { f = null; }
                // S4: a photo that could not be had is an empty 404, not null: null made the WebView ask the network again itself, a second download of what just failed
                return f == null ? notFound() : jpeg(new FileInputStream(f));
            }
        } catch (Exception ignored) { }
        return null;
    }

    /** ID/N.jpg from the pack's folder, or ID/credits.json ("1": "author · licence") for the page; nothing else, and nothing for a pack not on the TV */
    private static WebResourceResponse pack(Context c, String rest) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([a-z][a-z0-9-]{1,30})/([1-9][0-9]{0,2}\\.jpg|credits\\.json)").matcher(rest);
        if (!m.matches()) return null;
        String id = m.group(1), name = m.group(2);
        try {
            if (name.equals("credits.json")) {
                String json = PackStore.creditsJson(c, id);
                if (json == null) return null;
                java.util.Map<String, String> h = new java.util.HashMap<>();
                h.put("Access-Control-Allow-Origin", "*");
                return new WebResourceResponse("application/json", "UTF-8", 200, "OK", h, new java.io.ByteArrayInputStream(json.getBytes("UTF-8")));
            }
            File f = PackStore.photo(c, id, Integer.parseInt(name.substring(0, name.indexOf('.'))));
            return f.isFile() ? jpeg(new FileInputStream(f)) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeName(String s) {
        return s.matches("[A-Za-z0-9_-]+\\.jpg") ? s : null;
    }

    private static WebResourceResponse notFound() {
        return new WebResourceResponse("image/jpeg", null, 404, "Not Found", new java.util.HashMap<String, String>(), new java.io.ByteArrayInputStream(new byte[0]));
    }

    /** dates.json: ["2026-03-14", ...], the day each own photo was added (Own.date), in the list's order; the album card shows it under the photo */
    private static WebResourceResponse ownDates(Context c) {
        StringBuilder b = new StringBuilder("[");
        int n = Own.count(c);
        for (int k = 1; k <= n; k++) b.append(k > 1 ? "," : "").append('"').append(Own.date(c, k)).append('"');
        byte[] out = b.append(']').toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new WebResourceResponse("application/json", "UTF-8", new java.io.ByteArrayInputStream(out));
    }

    private static byte[] readAll(File f) throws java.io.IOException {
        try (InputStream in = new FileInputStream(f)) {
            java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream((int) Math.max(1024, f.length()));
            byte[] buf = new byte[16384];
            for (int r; (r = in.read(buf)) > 0; ) o.write(buf, 0, r);
            return o.toByteArray();
        }
    }

    private static WebResourceResponse jpeg(InputStream in) {
        return new WebResourceResponse("image/jpeg", null, in);
    }

    private static final Object DOWNLOAD = new Object();
    /** a weak TV (Prefs.lowRam): the long side of a kept photo */
    static final int LOW_PX = 1280;
    private static final long TOUCH_EVERY_MS = 3600_000L;

    /** S9: a kept photo is answered at once, without the lock (the lock is for downloads: one at a time); P6: its time is renewed at most once an hour, not at every hit */
    private static File hosted(Context c, String url, String name) throws java.io.IOException {
        File dir = new File(c.getFilesDir(), "photos");
        File f = new File(dir, name);
        if (kept(f)) return f;
        synchronized (DOWNLOAD) {
            if (kept(f)) return f;   // another request fetched it while this one waited
            return download(c, url, name, dir, f);
        }
    }

    private static boolean kept(File f) {
        if (!f.isFile() || f.length() <= 0) return false;
        long now = System.currentTimeMillis();
        if (now - f.lastModified() > TOUCH_EVERY_MS) f.setLastModified(now);   // served: the next prune keeps it (it keeps the newest, by this time)
        return true;
    }

    /** Runs on the WebView's network thread, so a blocking download is fine. */
    private static File download(Context c, String url, String name, File dir, File f) throws java.io.IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(15000);
        con.setReadTimeout(30000);
        File tmp = new File(dir, name + ".tmp");
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            if (con.getContentLength() > Bounded.IMAGE_MAX) return null;
            byte[] data;
            try (InputStream in = con.getInputStream()) { data = Bounded.readAll(in, Bounded.IMAGE_MAX); }
            if (!validImage(data)) return null;   // an error page or a cut download is not kept as a photo
            data = Prefs.lowRam(c) ? PhotoShrink.toSize(data, LOW_PX) : PhotoShrink.toScreen(data);   // P8: kept at screen size (a weak TV: 1280 on the long side, a third fewer pixels to decode and draw)
            try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(data); }
            if (!tmp.renameTo(f)) return null;
        } finally {
            con.disconnect();
            tmp.delete();
        }
        prune(dir);
        return f;
    }

    /** Whether these bytes are a picture that decodes (its size is read, not the pixels) and is not absurdly large. */
    static boolean validImage(byte[] data) {
        android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length, o);
        return o.outWidth > 0 && o.outHeight > 0 && o.outWidth <= 12000 && o.outHeight <= 12000;
    }

    /** Keep the newest few, and no more than the size budget; older sets are not shown again for months. */
    private static void prune(File dir) {
        File[] all = dir.listFiles((d, n) -> n.endsWith(".jpg"));
        if (all == null) return;
        Arrays.sort(all, new Comparator<File>() {
            @Override public int compare(File a, File b) { return Long.compare(b.lastModified(), a.lastModified()); }
        });
        long[] sizes = new long[Math.min(all.length, KEEP)];
        for (int i = 0; i < sizes.length; i++) sizes[i] = all[i].length();
        int keep = Bounded.fitting(sizes, Bounded.CACHE_MAX);
        for (int i = Math.max(1, keep); i < all.length; i++) all[i].delete();   // the one just fetched always stays
    }
}
