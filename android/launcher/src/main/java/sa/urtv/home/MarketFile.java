package sa.urtv.home;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * The markets page's one download: CI's file (MarketData.URL), about 20 kB, kept in this app's files so the page opens at once with the last
 * prices and the phone's search can check a code without a request. Asked again only when the copy is older than FRESH_MS, never more than
 * once per open of the page and its refresh. No other source, no key, nothing sent but the request itself.
 */
final class MarketFile {
    private MarketFile() { }

    static final long FRESH_MS = 10 * 60_000L;
    private static final int MAX_BYTES = 512 * 1024, TIMEOUT_MS = 12_000;
    private static final String NAME = "markets.json";

    /** the copy on the TV, or null */
    static MarketData cached(Context c) {
        File f = new File(c.getFilesDir(), NAME);
        if (!f.isFile()) return null;
        try (InputStream in = new FileInputStream(f)) {
            return MarketData.parse(read(in));
        } catch (Exception e) {
            return null;
        }
    }

    /** when the copy was saved (ms), 0 for none */
    static long savedAt(Context c) { return new File(c.getFilesDir(), NAME).lastModified(); }

    /** On a background thread: a fresh copy (downloaded when the one here is old), else the one here, else null. */
    static MarketData load(Context c, boolean force) {
        if (!force && System.currentTimeMillis() - savedAt(c) < FRESH_MS) {
            MarketData d = cached(c);
            if (d != null) return d;
        }
        HttpURLConnection h = null;
        try {
            h = (HttpURLConnection) new URL(MarketData.URL).openConnection();
            h.setConnectTimeout(TIMEOUT_MS);
            h.setReadTimeout(TIMEOUT_MS);
            h.setUseCaches(false);
            if (h.getResponseCode() == 200) {
                String text;
                try (InputStream in = h.getInputStream()) { text = read(in); }
                MarketData d = MarketData.parse(text);
                if (d != null) {
                    File tmp = new File(c.getFilesDir(), NAME + ".tmp");
                    try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(text.getBytes(StandardCharsets.UTF_8)); }
                    if (!tmp.renameTo(new File(c.getFilesDir(), NAME))) tmp.delete();
                    return d;
                }
            }
        } catch (Exception ignored) {
            // offline, a timeout: the copy here
        } finally {
            if (h != null) h.disconnect();
        }
        return cached(c);
    }

    private static String read(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) {
            b.write(buf, 0, n);
            if (b.size() > MAX_BYTES) throw new java.io.IOException("too big");
        }
        return new String(b.toByteArray(), StandardCharsets.UTF_8);
    }
}
