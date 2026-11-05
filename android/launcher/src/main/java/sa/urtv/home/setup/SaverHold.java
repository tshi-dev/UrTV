package sa.urtv.home.setup;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * «إعداد تلفزيوني» waits for the TV's «السماح بتصحيح USB؟»: a prompt sent while the screensaver is up stays stuck behind it until the TV
 * restarts (TV test, 30 Oct), and TCL starts its screensaver even though the setup screen asks to keep the screen on (its window flag;
 * the prompt's own window is in front then, not ours). While the wait lasts, two things keep it away:
 *
 * 1. a screen wake lock held by the app itself (WAKE_LOCK, already declared for the Quran), which does not depend on our window being in
 *    front: Android starts no screensaver while one is held, and a screensaver that starts anyway ends into a screen that stays on;
 * 2. a flag our screensaver asks for when it starts (NowPlayingProvider ".../setup_hold", signature permission): while it is set,
 *    ScreenDream ends at once instead of covering the prompt.
 *
 * Poking the TV's user activity (PowerManager.userActivity) would be the plain way, but it needs DEVICE_POWER, which no normal app gets.
 * The hold ends when the answer comes or the wait gives up, and by itself after SaverHoldRule.MAX_MS (the wake lock has the same timeout).
 */
public final class SaverHold {
    private static volatile long since;   // SystemClock.elapsedRealtime() when the wait began; 0 = none
    private static volatile long max = SaverHoldRule.MAX_MS;   // how long this hold may last
    private static volatile String what = "debugging prompt";   // what it waits for, for the log
    private static PowerManager.WakeLock lock;

    private SaverHold() { }

    /** The wait for «السماح بتصحيح USB؟» began. */
    @SuppressWarnings("deprecation")   // SCREEN_BRIGHT_WAKE_LOCK: the one kind a normal app can hold that keeps the screen (and so no screensaver)
    public static synchronized void begin(Context c) { begin(c, SaverHoldRule.MAX_MS, "debugging prompt"); }

    /** The same for another wait: the update's install questions (SaverHoldRule.UPDATE_MS), named in the log. */
    @SuppressWarnings("deprecation")
    public static synchronized void begin(Context c, long maxMs, String waitsFor) {
        max = maxMs;
        what = waitsFor;
        since = Math.max(1, SystemClock.elapsedRealtime());
        try {
            if (lock == null) {
                PowerManager pm = (PowerManager) c.getApplicationContext().getSystemService(Context.POWER_SERVICE);
                lock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "urtv:setup-prompt");
                lock.setReferenceCounted(false);
            }
            lock.acquire(maxMs);
        } catch (RuntimeException e) {
            Log.i("UrTVSetup", "screen hold not taken: " + e.getClass().getSimpleName());   // the screensaver's own check still holds it back
        }
        remember(c, maxMs);
        Log.i("UrTVSetup", "waiting for the " + what + ": screensaver held back");
    }

    /** The answer came, or the wait ended: the screensaver is free again. Safe to call any number of times. */
    public static synchronized void end() {
        if (since == 0) return;
        since = 0;
        forget();
        try { if (lock != null && lock.isHeld()) lock.release(); } catch (RuntimeException ignored) { }
        Log.i("UrTVSetup", what + " answered or given up: screensaver free");
    }

    /** The same, only when this is the hold for {@code waitsFor} (the update ending its flow does not free the setup's wait). */
    public static synchronized void endIf(String waitsFor) {
        if (waitsFor != null && waitsFor.equals(what)) end();
    }

    /**
     * The update replaces the launcher itself before the screensaver app's own question (step 28: the screensaver covered the second install
     * question 45 s after the first): the new process has no memory of the wait, so its end is kept on disk (wall clock) and read back.
     */
    private static volatile Context appCtx;
    private static final String PREFS = "saver_hold";

    private static void remember(Context c, long maxMs) {
        try {
            appCtx = c.getApplicationContext();
            appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("until", System.currentTimeMillis() + maxMs).commit();
        } catch (RuntimeException ignored) { }
    }

    private static void forget() {
        try { if (appCtx != null) appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("until").apply(); } catch (RuntimeException ignored) { }
    }

    /** What the screensaver asks when it starts, in a process that may be a new one (after our own replace): the wait kept on disk counts. */
    public static boolean held(Context c) {
        if (held()) return true;
        try {
            SharedPreferences p = c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return SaverHoldRule.heldUntil(p.getLong("until", 0), System.currentTimeMillis(), SaverHoldRule.UPDATE_MS);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** What the screensaver asks when it starts. */
    public static boolean held() {
        return SaverHoldRule.held(since, SystemClock.elapsedRealtime(), max);
    }
}
