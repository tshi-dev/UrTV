package sa.urtv.screensaver;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

/**
 * 4.4 (R-400): some firmware (a Skyworth on Android 11) writes «never» (Integer.MAX) into the TV's idle delay at each boot, so the screensaver
 * the person chose with «بدء شاشة التوقف بعد» (or `saver.sh idle N`) never started again after a power cycle. The minutes they chose are kept here,
 * in a file of their own (never the screensaver's settings), and at BOOT_COMPLETED they are written back ONLY when the TV says «never» now: a delay
 * the person changed in the TV's own settings is left as it is. This app writes it where it may (WRITE_SETTINGS), else the home over its kept adb
 * link (NowPlayingBridge.setIdle, which never offers a new adb key). The log has fixed words only (tag UrTVIdle).
 */
final class IdleKeep {
    private IdleKeep() { }

    static final String FILE = "idle_keep", KEY = "minutes", TAG = "UrTVIdle";

    /** the minutes the person chose (one of Prefs.IDLE_MINUTES), written to the disk at once (commit: it must outlive a power cut), on a thread of its own (the row calls it on the UI thread) */
    static void keep(Context c, final String minutes) {
        final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        try { new Thread(() -> { try { app.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, minutes).commit(); } catch (RuntimeException ignored) { } }, "idle-keep-save").start(); }
        catch (RuntimeException ignored) { }
    }

    static String kept(Context c) { return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null); }

    /** the boots that may write (not LOCKED_BOOT_COMPLETED: the kept file is in the storage that opens later; not an update of this app) */
    static boolean bootAction(String action) {
        return Intent.ACTION_BOOT_COMPLETED.equals(action) || "android.intent.action.QUICKBOOT_POWERON".equals(action)
            || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action);
    }

    /** at boot, off the main thread (the home's adb link blocks) */
    static void atBoot(Context c) {
        long ms = IdleRule.reapplyMs(kept(c), Prefs.idleMs(c), Prefs.IDLE_MINUTES);
        if (ms <= 0) { Log.i(TAG, "boot: nothing to put back"); return; }
        if (Prefs.canWriteSettings(c)) {
            try {
                if (Settings.System.putInt(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, (int) ms)) { Log.i(TAG, "boot: delay put back"); return; }
            } catch (SecurityException ignored) { /* the home's link below */ }
        }
        String r = NowPlayingBridge.setIdle(c, String.valueOf(ms / 60000));
        Log.i(TAG, "ok".equals(r) ? "boot: delay put back over the home" : "boot: delay not put back");
    }
}
