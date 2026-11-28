package sa.urtv.home;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 4.4 (a report: «a reinstall of the screensaver reset its preferences»): the home keeps a copy of the screensaver's settings in its own files
 * (asked from the screensaver's ConfigProvider, «settings_copy», at most every 30 minutes, on a thread, only from a screensaver signed with our key),
 * and gives it back to a fresh install of the screensaver (NowPlayingProvider «saver_settings_copy»; the screensaver's SettingsCopy). An empty or
 * missing answer never replaces the kept copy.
 */
public final class SaverSettingsCopy {
    private SaverSettingsCopy() { }

    static final String SAVER_PKG = "sa.urtv.screensaver", SAVER_AUTHORITY = "sa.urtv.screensaver.config", FILE = "saver-settings-copy.txt";
    static final long EVERY_MS = 30 * 60_000L;
    static final int MAX_CHARS = 256 * 1024;
    private static volatile long lastAsk;
    private static volatile boolean busy;

    /** at most every EVERY_MS: the screensaver's settings are asked for and kept when they are a real copy that differs */
    public static void refreshSoon(Context c) {
        final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        long now = android.os.SystemClock.elapsedRealtime();
        if (busy || (lastAsk != 0 && now - lastAsk < EVERY_MS)) return;
        busy = true;
        lastAsk = now;
        Thread t = new Thread(() -> {
            try {
                if (!saverIsOurs(app)) return;
                Bundle b = app.getContentResolver().call(Uri.parse("content://" + SAVER_AUTHORITY), "settings_copy", null, null);
                String in = b == null ? null : b.getString("copy");
                File f = new File(app.getFilesDir(), FILE);
                if (!worthKeeping(in, read(f))) return;
                File tmp = new File(f.getPath() + ".tmp");
                try (FileOutputStream o = new FileOutputStream(tmp)) { o.write(in.getBytes(StandardCharsets.UTF_8)); o.getFD().sync(); }
                if (!tmp.renameTo(f)) tmp.delete();
            } catch (Exception ignored) {   // an older screensaver, not installed, refused: the kept copy stays
            } finally { busy = false; }
        }, "saver-settings-copy");
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();
    }

    /** the provider's answer to the screensaver (a binder thread): the kept copy, null when there is none */
    public static String kept(Context c) { return read(new File(c.getFilesDir(), FILE)); }

    /** a copy the screensaver gave: lines «type TAB key …», not empty, not too big, and not the one kept already */
    static boolean worthKeeping(String in, String kept) {
        if (in == null || in.isEmpty() || in.length() > MAX_CHARS || in.equals(kept)) return false;
        for (String line : in.split("\n")) if (!line.isEmpty() && line.indexOf('\t') < 1) return false;
        return true;
    }

    static String read(File f) {
        if (!f.isFile() || f.length() > MAX_CHARS * 4L) return null;
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] b = new byte[(int) f.length()];
            int n = 0;
            while (n < b.length) { int r = in.read(b, n, b.length - n); if (r < 0) break; n += r; }
            return new String(b, 0, n, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** the provider is the screensaver's and it carries our signature (a squatting app cannot be asked, or answer, in its place) */
    static boolean saverIsOurs(Context c) {
        try {
            PackageManager pm = c.getPackageManager();
            ProviderInfo pi = pm.resolveContentProvider(SAVER_AUTHORITY, 0);
            return pi != null && SAVER_PKG.equals(pi.packageName) && pm.checkSignatures(c.getPackageName(), SAVER_PKG) == PackageManager.SIGNATURE_MATCH;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
