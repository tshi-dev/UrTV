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
 * The markets page's downloads: CI's 15-minute file (MarketData.URL), about 20 kB, kept in this app's files so the page opens at once with the last
 * prices and the phone's search can check a code without a request. Asked again only when the copy is older than FRESH_MS, never more than
 * once per open of the page and its refresh; and its daily file (loadDaily), once a day. No other source, no key, nothing sent but the requests.
 */
final class MarketFile {
    private MarketFile() { }

    static final long FRESH_MS = 10 * 60_000L;
    private static final int MAX_BYTES = 512 * 1024, TIMEOUT_MS = 12_000;
    private static final String NAME = "markets.json";

    private static final String DAILY = "markets-daily.json";

    /** the copy on the TV, with the year's daily closes when the TV has them; or null */
    static MarketData cached(Context c) {
        MarketData d = MarketData.parse(file(c, NAME));
        return d == null ? null : d.withDaily(file(c, DAILY));
    }

    private static String file(Context c, String name) {
        File f = new File(c.getFilesDir(), name);
        if (!f.isFile()) return null;
        try (InputStream in = new FileInputStream(f)) {
            return read(in);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * On a background thread, when the markets page opens: CI's daily file (a year of daily closes, about 140 kB), downloaded only when the
     * copy here is not from today, so a TV pays for it once a day and only if the page is opened. The 15-minute file never carries it.
     */
    static void loadDaily(Context c) {
        File have = new File(c.getFilesDir(), DAILY);
        java.util.Calendar now = java.util.Calendar.getInstance(), then = java.util.Calendar.getInstance();
        then.setTimeInMillis(have.lastModified());
        if (have.isFile() && now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR)
                && now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)) return;
        String text = download(MarketData.DAILY_URL);
        if (text != null && text.contains("\"daily\"")) save(c, DAILY, text);
    }

    /** when the copy was saved (ms), 0 for none */
    static long savedAt(Context c) { return new File(c.getFilesDir(), NAME).lastModified(); }

    /** On a background thread: a fresh copy (downloaded when the one here is old), else the one here, else null. */
    static MarketData load(Context c, boolean force) {
        if (!force && System.currentTimeMillis() - savedAt(c) < FRESH_MS) {
            MarketData d = cached(c);
            if (d != null) return d;
        }
        String text = download(MarketData.URL);
        if (text != null && MarketData.parse(text) != null) save(c, NAME, text);
        return cached(c);
    }

    /** the file's text, or null (offline, a timeout, not 200) */
    private static String download(String url) {
        HttpURLConnection h = null;
        try {
            h = (HttpURLConnection) new URL(url).openConnection();
            h.setConnectTimeout(TIMEOUT_MS);
            h.setReadTimeout(TIMEOUT_MS);
            h.setUseCaches(false);
            if (h.getResponseCode() != 200) return null;
            try (InputStream in = h.getInputStream()) { return read(in); }
        } catch (Exception e) {
            return null;
        } finally {
            if (h != null) h.disconnect();
        }
    }

    private static void save(Context c, String name, String text) {
        File tmp = new File(c.getFilesDir(), name + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            tmp.delete();
            return;
        }
        if (!tmp.renameTo(new File(c.getFilesDir(), name))) tmp.delete();
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
