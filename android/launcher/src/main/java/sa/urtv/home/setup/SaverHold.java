package sa.urtv.home.setup;

import android.content.Context;
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
    private static PowerManager.WakeLock lock;

    private SaverHold() { }

    /** The wait for «السماح بتصحيح USB؟» began. */
    @SuppressWarnings("deprecation")   // SCREEN_BRIGHT_WAKE_LOCK: the one kind a normal app can hold that keeps the screen (and so no screensaver)
    public static synchronized void begin(Context c) {
        since = Math.max(1, SystemClock.elapsedRealtime());
        try {
            if (lock == null) {
                PowerManager pm = (PowerManager) c.getApplicationContext().getSystemService(Context.POWER_SERVICE);
                lock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "urtv:setup-prompt");
                lock.setReferenceCounted(false);
            }
            lock.acquire(SaverHoldRule.MAX_MS);
        } catch (RuntimeException e) {
            Log.i("UrTVSetup", "screen hold not taken: " + e.getClass().getSimpleName());   // the screensaver's own check still holds it back
        }
        Log.i("UrTVSetup", "waiting for the debugging prompt: screensaver held back");
    }

    /** The answer came, or the wait ended: the screensaver is free again. Safe to call any number of times. */
    public static synchronized void end() {
        if (since == 0) return;
        since = 0;
        try { if (lock != null && lock.isHeld()) lock.release(); } catch (RuntimeException ignored) { }
        Log.i("UrTVSetup", "debugging prompt answered or given up: screensaver free");
    }

    /** What the screensaver asks when it starts. */
    public static boolean held() {
        return SaverHoldRule.held(since, SystemClock.elapsedRealtime());
    }
}
