package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * What our own update («تحديث تلفزيوني») must not change, carried over it (tester, build 168 -> 174 on the owner's TCL, Android 14):
 * - the mute: our installer never touches the sound (PhoneUpdater only hands files to PackageInstaller), but the TV unmuted itself when the home
 *   app was replaced (the same TCL unmutes at every restart too). A TV that was muted right before the install is muted again after it.
 * - our accessibility entry: after the replace, our TypeService was no longer in enabled_accessibility_services at all, so A11yHeal had nothing
 *   to heal (not_listed). When the list had it right before the install and lost it, and we hold WRITE_SECURE_SETTINGS (granted by the setup),
 *   it is added back (A11yList.ownLost / with: only ours, only what the install lost); without the permission, a one-time notice says so.
 * before() is called by PhoneUpdater right before the files go to the installer; after() by A11yHeal on MY_PACKAGE_REPLACED and at the home's
 * first show. The record is used once, and only within FRESH_MS of the install.
 */
public final class UpdateCarry {
    private UpdateCarry() { }

    static final String TAG = "UrTVUpdate";
    private static final String PREFS = "update_carry";
    static final long FRESH_MS = 30 * 60_000L;
    /** the TV may unmute a moment after the replace: one more look this long after the first */
    private static final long AGAIN_MS = 20_000;

    /** Right before our own install starts: the mute and the accessibility list as they are now (written at once: the process is replaced next). */
    public static void before(Context c) {
        try {
            AudioManager audio = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            boolean muted = audio != null && audio.isStreamMute(AudioManager.STREAM_MUSIC);
            String list = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong("at", System.currentTimeMillis()).putInt("code", BuildConfig.VERSION_CODE).putBoolean("muted", muted).putString("a11y", list == null ? "" : list).commit();
            Log.i(TAG, "before the install: muted=" + muted + " ours listed=" + (A11yList.has(list, A11yHeal.TYPE) || A11yList.has(list, A11yHeal.SLEEP)));
        } catch (RuntimeException e) {
            Log.w(TAG, "before the install: not recorded (" + e.getClass().getSimpleName() + ")");
        }
    }

    /** True when a record of an install in the last FRESH_MS is there (pure: tests/tester_fixes_test.py reads the rule from the source). */
    static boolean fresh(long at, long now) { return at > 0 && now >= at && now - at <= FRESH_MS; }

    /**
     * The mute only, at once (MY_PACKAGE_REPLACED, before anything waits) and again when the home first shows. The record is NOT used up here: the
     * tester saw «Muted: false» after 174 -> 178 although the single look 8 s after the replace (and the one 20 s later, on a Handler of a receiver
     * process that is gone by then) were meant to hold. Now the mute is asked in the receiver at once, after A11yHeal's wait, at the home's first
     * start, and 20 s after each of those that ran in a living process.
     */
    static void remute(final Context c, final String why) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long at = sp.getLong("at", 0);
        if (at == 0 || sp.getInt("code", -1) == BuildConfig.VERSION_CODE || !sp.getBoolean("muted", false) || !fresh(at, System.currentTimeMillis())) return;
        muteAgain(c, why);
        new Handler(Looper.getMainLooper()).postDelayed(() -> muteAgain(c, why + " +20s"), AGAIN_MS);
    }

    /** After the replace (or the home's first show): the accessibility entry once; the mute record stays until both the replace and the first start have looked. */
    static void after(final Context c, String why) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long at = sp.getLong("at", 0);
        if (at == 0) return;
        if (sp.getInt("code", -1) == BuildConfig.VERSION_CODE) return;   // still the build that recorded it: the home was not replaced yet
        boolean wasMuted = sp.getBoolean("muted", false), a11yDone = sp.getBoolean("a11yDone", false), fresh = fresh(at, System.currentTimeMillis());
        String before = sp.getString("a11y", ""), seen = sp.getString("seen", "");
        if (!seen.contains(why + ";")) seen = seen + why + ";";
        if (fresh && wasMuted) {
            muteAgain(c, why);
            new Handler(Looper.getMainLooper()).postDelayed(() -> muteAgain(c, why + " +20s"), AGAIN_MS);
        }
        if (!fresh || (seen.contains("replaced;") && seen.contains("start;"))) sp.edit().clear().commit();   // used up: a later unmute or change by the owner is theirs
        else sp.edit().putString("seen", seen).putBoolean("a11yDone", true).commit();
        if (!fresh || a11yDone) return;   // the entry is put back once
        try {
            String now = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            List<String> lost = A11yList.ownLost(before, now, A11yHeal.TYPE, A11yHeal.SLEEP);
            if (lost.isEmpty()) return;
            boolean canWrite = c.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
            if (canWrite) {
                Settings.Secure.putString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, A11yList.with(now, lost));
                Settings.Secure.putInt(c.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
                Log.i(TAG, "after the install (" + why + "): our accessibility entry was lost by the update and is back: " + lost);
            } else {
                Log.w(TAG, "after the install (" + why + "): our accessibility entry was lost by the update, no permission to add it back: " + lost);
                new Handler(Looper.getMainLooper()).post(() -> {
                    try { Toast.makeText(c, S.t("بعد التحديث توقفت خدمة الكتابة من الجوال. شغّلها من إعدادات التلفزيون: إمكانية الوصول، تلفزيونك."), Toast.LENGTH_LONG).show(); } catch (RuntimeException ignored) { }
                });
            }
        } catch (RuntimeException e) {   // SecurityException: the permission was taken back meanwhile
            Log.w(TAG, "after the install (" + why + "): the list was not changed (" + e.getClass().getSimpleName() + ")");
        }
    }

    private static void muteAgain(Context c, String why) {
        try {
            AudioManager audio = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            if (audio == null || audio.isStreamMute(AudioManager.STREAM_MUSIC)) return;
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            Log.i(TAG, "after the install (" + why + "): the TV was muted before it and unmuted by it: muted again");
        } catch (RuntimeException e) {
            Log.w(TAG, "after the install (" + why + "): could not mute again (" + e.getClass().getSimpleName() + ")");
        }
    }
}
