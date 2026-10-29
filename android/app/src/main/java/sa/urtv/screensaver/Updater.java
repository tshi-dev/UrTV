package sa.urtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/** Pulls the latest page from GitHub so screen changes reach the TV without a new APK. */
final class Updater {
    static final String URL_PAGE =
        PhotoCache.REPO + "dist/tv.html";
    static final long INTERVAL_MS = 6L * 3600 * 1000;

    private static final String OFF = "التحديث التلقائي متوقف";
    private static final String RECENT = "تم الفحص مؤخراً";
    private static final String LATEST_URL = "https://github.com/tshi-dev/UrTV/releases/latest";
    private static final String VERSION_URL = "https://github.com/tshi-dev/UrTV/releases/download/apk-latest/version.txt";
    private static final String NEXT_VERSION_URL = "https://github.com/tshi-dev/UrTV/releases/download/apk-next/version.txt";

    private Updater() {}

    static File pageFile(Context c) { return new File(c.getFilesDir(), "web/tv.html"); }

    /** the build stamp of the page the app carries ("" if it cannot be read) */
    static String assetStamp(Context c) {
        try { return PageStamp.read(c.getAssets().open("tv.html")); } catch (Exception e) { return ""; }
    }

    /** the build stamp of a downloaded page file */
    static String fileStamp(File f) {
        try { return PageStamp.read(new java.io.FileInputStream(f)); } catch (Exception e) { return ""; }
    }

    private static final AtomicBoolean DUE_RUNNING = new AtomicBoolean();

    /**
     * Called when the screensaver starts: if auto-update is on and the last check is 6 hours old or more, checks on a
     * background thread (one at a time). Does nothing otherwise, so the TV asks the network only when it is used.
     */
    static void checkIfDue(Context c) {
        final Context app = c.getApplicationContext();
        Prefs prefs = new Prefs(app);
        if (!prefs.autoUpdate()) return;
        if (!displayOn(app)) return;   // standby: not now; the next screensaver start asks (lastCheck is not touched)
        if (System.currentTimeMillis() - prefs.raw().getLong("lastCheck", 0) < INTERVAL_MS) return;
        if (!DUE_RUNNING.compareAndSet(false, true)) return;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    check(app, false);
                } finally {
                    DUE_RUNNING.set(false);
                }
            }
        }).start();
    }

    /** PowerManager.isInteractive(); true when it cannot be read (a check is not lost for that). */
    private static boolean displayOn(Context c) {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) c.getSystemService(Context.POWER_SERVICE);
            return pm == null || pm.isInteractive();
        } catch (RuntimeException e) {
            return true;
        }
    }

    /** Blocking; call off the main thread. Returns an Arabic status line for the settings screen. */
    static synchronized String check(Context c, boolean force) {
        String msg = checkPage(c, force);
        if (!OFF.equals(msg) && !RECENT.equals(msg)) checkLatest(c);
        return msg;
    }

    private static String checkPage(Context c, boolean force) {
        Prefs prefs = new Prefs(c);
        SharedPreferences sp = prefs.raw();
        if (!prefs.autoUpdate() && !force) return OFF;
        long now = System.currentTimeMillis();
        if (!force && now - sp.getLong("lastCheck", 0) < INTERVAL_MS) return RECENT;

        File out = pageFile(c);
        new File(out.getParentFile(), "urtv.html").delete();   // the old, heavier page
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(URL_PAGE).openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(30000);
            String etag = sp.getString("etag", null);
            if (etag != null && out.isFile()) con.setRequestProperty("If-None-Match", etag);
            int code = con.getResponseCode();
            sp.edit().putLong("lastCheck", now).apply();
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) return "الصفحة محدثة";
            if (code != HttpURLConnection.HTTP_OK) return "تعذر التحديث (" + code + ")";

            byte[] body = readAll(con.getInputStream());
            String head = new String(body, 0, Math.min(body.length, 4096), "UTF-8");
            String tail = new String(body, Math.max(0, body.length - 4096), Math.min(body.length, 4096), "UTF-8");
            // never swap in a truncated or wrong file
            if (body.length < 30_000 || !head.contains("<html") || !tail.contains("</html>")) return "ملف التحديث غير صالح";
            // a copy older than the page the app carries (a CDN serves old ones for minutes) is not kept, whatever its file time will be
            if (PageStamp.newer(assetStamp(c), PageStamp.of(head))) return "الصفحة المرفقة أحدث من المنزّلة";

            File dir = out.getParentFile();
            if (!dir.isDirectory() && !dir.mkdirs()) return "تعذر الحفظ";
            File tmp = new File(dir, "page.tmp");
            FileOutputStream fo = new FileOutputStream(tmp);
            try { fo.write(body); } finally { fo.close(); }
            if (!tmp.renameTo(out)) return "تعذر الحفظ";
            sp.edit().putString("etag", con.getHeaderField("ETag")).apply();
            return "تم التحديث";
        } catch (Exception e) {
            return "لا يوجد اتصال";
        } finally {
            if (con != null) con.disconnect();
        }
    }

    /**
     * The newest version: the version.txt the master build attaches to apk-latest (the release /releases/latest points to,
     * so its tag is not a version), else the numbered tag /releases/latest points to. Keeps the old value on any failure.
     */
    private static void checkLatest(Context c) {
        boolean next = "next".equals(new Prefs(c).updateChannel());   // the test channel (agents only): apk-next's version.txt, never the latest's redirect
        String v = latestFromAsset(next ? NEXT_VERSION_URL : VERSION_URL);
        if (v == null && !next) v = latestFromRedirect();
        if (v == null) return;
        new Prefs(c).raw().edit().putString("latestVersion", v).putString("latestChannel", next ? "next" : "latest").putLong("latestCheck", System.currentTimeMillis()).apply();
    }

    /** After the update channel was changed (agents only): read the newest version of the new channel's release in the background. */
    static void refreshLatestAsync(Context c) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() { @Override public void run() { checkLatest(app); } }).start();
    }

    private static String latestFromAsset(String url) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();   // GitHub redirects to its CDN: followed
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            InputStream in = con.getInputStream();
            try {
                byte[] buf = new byte[64];   // "3.51\n": anything longer is not it
                int n = 0, r;
                while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
                return Versions.fromText(new String(buf, 0, n, "UTF-8"));
            } finally {
                in.close();
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String latestFromRedirect() {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(LATEST_URL).openConnection();
            con.setInstanceFollowRedirects(false);
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.getResponseCode();
            return Versions.fromTag(con.getHeaderField("Location"));
        } catch (Exception e) {
            return null;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String arabic(String v) {
        StringBuilder b = new StringBuilder();
        for (char ch : v.toCharArray()) {
            if (ch >= '0' && ch <= '9') b.append(DigitsText.convert(String.valueOf(ch), false));
            else if (ch == '.') b.append('٫');
            else b.append(ch);
        }
        return b.toString();
    }

    /** The three lines under the settings: this app's version, the newest release, and how to update. */
    static String[] versionLines(Context c) {
        int mine = BuildConfig.VERSION_CODE;   // the build; the name is the VERSION file's («4.0»), never taken apart
        int newest = Versions.buildOf(new Prefs(c).latestVersion());   // version.txt is "3.<build>": the build is what grows
        String available = newest < 0 ? "لم يُفحص بعد"
            : newest <= mine ? "لا يوجد أحدث" : arabic(Versions.display(BuildConfig.MARKETING, newest));
        return new String[] {
            "الإصدار الحالي: " + arabic(Versions.display(BuildConfig.MARKETING, mine)),
            "الإصدار المتاح: " + available,
            "طريقة التحديث: اطلب من مساعد الذكاء الاصطناعي «حدّث تلفزيوني»، وإعداداتك كلها تبقى.",
        };
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            return b.toByteArray();
        } finally {
            in.close();
        }
    }
}
