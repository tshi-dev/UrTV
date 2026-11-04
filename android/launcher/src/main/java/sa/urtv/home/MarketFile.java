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

    static final long FRESH_MS = 5 * 60_000L;   // CI publishes every 5 minutes (a late file is shown with its own time)
    private static final int MAX_BYTES = 512 * 1024, TIMEOUT_MS = 12_000;
    private static final String NAME = "markets.json";
    /** generic and honest, with the "Mozilla/5.0" start that bot-like clients use so a plain client is not refused (same as the CI collector) */
    static final String UA = "Mozilla/5.0 (compatible; UrTV-markets/1.0; +https://github.com/tshi-dev/UrTV)";

    private static final String DAILY = "markets-daily.json";

    /** Yahoo's spark endpoint (build 179, «مباشر»; the one CI uses, no crumb): one request for the symbols on screen, asked only while the markets page is in front */
    private static final String LIVE_URL = "https://query1.finance.yahoo.com/v8/finance/spark?symbols=%s&range=1d&interval=5m";

    /**
     * On a background thread: the live answer for these symbols; an empty map for any failure (the caller backs off). Nothing is stored. Each request is
     * logged under tag UrTVMarkets with fixed words: the HTTP status, how many symbols were asked and parsed, the size, and the reason for a failure
     * (http_<status>, redirected (another host answered: a consent page), empty_body, parsed_none, <exception class>). No symbol, no price, nothing personal.
     */
    static java.util.Map<String, MarketData.Live> live(java.util.Collection<String> symbols) {
        HttpURLConnection h = null;
        int status = -1, bytes = 0;
        String reason = "ok";
        java.util.Map<String, MarketData.Live> out = new java.util.LinkedHashMap<>();
        try {
            StringBuilder s = new StringBuilder();
            for (String x : symbols) s.append(s.length() > 0 ? "," : "").append(java.net.URLEncoder.encode(x, "UTF-8"));
            h = (HttpURLConnection) new URL(String.format(LIVE_URL, s.toString().replace("%2C", ","))).openConnection();
            h.setConnectTimeout(TIMEOUT_MS);
            h.setReadTimeout(TIMEOUT_MS);
            h.setUseCaches(false);
            h.setRequestProperty("User-Agent", UA);
            h.setRequestProperty("Accept", "application/json");
            status = h.getResponseCode();   // gzip is undone by the platform (it asked for it)
            if (!"query1.finance.yahoo.com".equals(h.getURL().getHost())) reason = "redirected";   // a consent or error page from another host
            else if (status != 200) reason = "http_" + status;
            else {
                String body;
                try (InputStream in = h.getInputStream()) { body = read(in); }
                bytes = body.length();
                if (body.isEmpty()) reason = "empty_body";
                else {
                    out = MarketData.parseLive(body);
                    if (out.isEmpty()) {
                        reason = "parsed_none";
                        // what the answer looks like, so the next test says what it was (the first 300 characters, single-lined; prices only, nothing personal)
                        android.util.Log.i("UrTVMarkets", "live body_start=" + body.substring(0, Math.min(300, body.length())).replaceAll("[\\r\\n]+", " "));
                    }
                }
            }
        } catch (Exception e) {
            reason = e.getClass().getSimpleName();
        } finally {
            if (h != null) h.disconnect();
        }
        if (!"ok".equals(reason)) out = new java.util.LinkedHashMap<>();
        android.util.Log.i("UrTVMarkets", "live status=" + status + " asked=" + symbols.size() + " parsed=" + out.size() + " bytes=" + bytes + " reason=" + reason);
        return out;
    }

    /** the last copy that parsed, kept in memory: a page opens with it at once, and nothing reads a file on the UI thread (build 179 review) */
    private static volatile MarketData lastGood;

    /** the copy in memory (never a file read); null before the first load of this process */
    static MarketData last() { return lastGood; }

    /** the copy on the TV, with the year's daily closes when the TV has them; or null. Reads and parses files: a background thread only. */
    static MarketData cached(Context c) {
        MarketData d = MarketData.parse(file(c, NAME));
        if (d == null) return lastGood;   // a broken or missing file keeps the last good copy
        d = d.withDaily(file(c, DAILY));
        lastGood = d;
        return d;
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

    // ---- «صرف العملات»: the currencies' file (CI, once a day; units per dollar), downloaded only by the markets page, once a day, kept in memory for the screen
    private static final String FX = "markets-fx.json";
    private static volatile String fxText;

    /** the currencies' text in memory (null before the first load of this process): never a file read on the UI thread */
    static String fxText() { return fxText; }

    /** On a background thread: the copy on the TV into memory, and a fresh one downloaded when it is not from today. */
    static void loadFx(Context c) {
        File have = new File(c.getFilesDir(), FX);
        if (fxText == null) fxText = file(c, FX);
        java.util.Calendar now = java.util.Calendar.getInstance(), then = java.util.Calendar.getInstance();
        then.setTimeInMillis(have.lastModified());
        if (have.isFile() && now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR)
                && now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)) return;
        String text = download(MarketData.FX_URL);
        if (text != null && !MarketData.parseFx(text).isEmpty()) { save(c, FX, text); fxText = text; return; }
        // CI's file is not there (the data branch had none for a while: a TV installed then showed «لا أسعار بعد» for hours, steps 27 and 28b): with no rates at all
        // yet, the app asks Yahoo itself for the currencies, the way it asks for the live prices, and keeps the result like CI's file
        if (fxText == null || MarketData.parseFx(fxText).isEmpty()) {
            String own = fxFromYahoo();
            if (own != null) { save(c, FX, own); fxText = own; }
        }
    }

    /** the currencies' rates (units per dollar) from Yahoo's spark answer, two requests; null when none came */
    private static String fxFromYahoo() {
        java.util.Map<String, Double> rates = new java.util.LinkedHashMap<>();
        String[] codes = MarketData.FX_CODES;
        for (int from = 0; from < codes.length; from += 18) {
            java.util.List<String> syms = new java.util.ArrayList<>();
            for (int i = from; i < Math.min(codes.length, from + 18); i++) syms.add(codes[i] + "=X");
            for (java.util.Map.Entry<String, MarketData.Live> e : live(syms).entrySet())
                if (e.getKey().endsWith("=X") && e.getValue().p > 0) rates.put(e.getKey().substring(0, e.getKey().length() - 2), e.getValue().p);
        }
        String text = MarketData.fxFile(rates, System.currentTimeMillis());
        return text.isEmpty() || MarketData.parseFx(text).isEmpty() ? null : text;
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
