package sa.urtv.home;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.List;

/**
 * What our own update («تحديث تلفزيوني») must not change, carried over it. A persisted operation (tester builds 174 to 179, the owner's TCL, Android 14: the
 * TV unmuted itself at 4, 11 and 30 s after the replace, our accessibility entry vanished, and the last app (YouTube) opened by itself):
 *
 * - before(c, expected): right before OUR replace (PhoneUpdater, with the build the file says it holds), the mute and the music volume as they are, and
 *   our accessibility entries. A new generation. The record is on disk (commit): the process is replaced next.
 * - after(c, why, gen): at MY_PACKAGE_REPLACED, 8 s later (A11yHeal), at the home's first start, and by an exact alarm at +10 s and +30 s (and at the
 *   backoff of a stage that has not finished). Each is the same idempotent step, in any order, in a new process after a death; a call with another
 *   generation than the record's is stale and does nothing. The audio stage lives 3 minutes from the first call after the replace, the accessibility
 *   stage 30 minutes (A11yList decides: audioStep, a11yStep, pauseOthers, backoffMs).
 * - audio: muted again only if the TV was muted before, the adhan is not sounding, the owner has not moved the volume, and the OEM window (60 s) is not
 *   over (an unmute later is the owner's); at most three times. If the OEM brought the last app back and it plays while the TV was muted before, one
 *   media pause key, inside the same window, never over the adhan or our own playback.
 * - accessibility: our own entries the install lost are written back (only ours, only with WRITE_SECURE_SETTINGS); the stage is DONE only after the
 *   setting is read back with ours in it, the switch on, and our service bound. A failed write keeps the saved entries and tries again later.
 * Every step logs a fixed reason code under tag UrTVUpdate (stage, a short generation, booleans): nothing personal.
 */
public final class UpdateCarry {
    private UpdateCarry() { }

    static final String TAG = "UrTVUpdate";
    private static final String PREFS = "update_carry", ACTION = "sa.urtv.home.CARRY_CHECK";
    /** between before() and the replace (the owner confirms Android's install screen) */
    static final long FRESH_MS = 30 * 60_000L;

    /** Right before our own replace: the mute, the volume and the accessibility list as they are now. */
    public static void before(Context c, int expected) {
        try {
            AudioManager audio = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            boolean muted = audio != null && audio.isStreamMute(AudioManager.STREAM_MUSIC);
            int vol = audio == null ? -1 : audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            String list = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            long gen = System.currentTimeMillis();
            boolean ours = A11yList.has(list, A11yHeal.TYPE) || A11yList.has(list, A11yHeal.SLEEP);
            synchronized (UpdateCarry.class) {
                c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().putLong("gen", gen).putLong("at", gen).putInt("code", BuildConfig.VERSION_CODE)
                    .putInt("expected", expected).putBoolean("muted", muted).putInt("vol", vol).putString("a11y", list == null ? "" : list).putBoolean("hadOurs", ours).commit();
            }
            log("before", gen, "recorded", "muted=" + muted + " ours=" + ours);
        } catch (RuntimeException e) {
            Log.w(TAG, "carry stage=before result=not_recorded " + e.getClass().getSimpleName());
        }
    }

    /** True when a record of an install in the last FRESH_MS is there (pure: tests/tester_fixes_test.py reads the rule from the source). */
    static boolean fresh(long at, long now) { return at > 0 && now >= at && now - at <= FRESH_MS; }

    /** From A11yHeal, for the replace and the home's first show (why), and for an alarm (gen != 0: the generation it was set for). */
    static void after(final Context c, String why) { after(c, why, 0); }

    static void after(final Context c, String why, long gen) {
        synchronized (UpdateCarry.class) {
            try {
                step(c, why, gen);
            } catch (RuntimeException e) {
                Log.w(TAG, "carry stage=step why=" + why + " result=error " + e.getClass().getSimpleName());
            }
        }
    }

    private static void step(final Context c, final String why, long gen) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final A11yList.Carry rec = new A11yList.Carry();
        rec.gen = sp.getLong("gen", 0);
        if (rec.gen == 0) return;
        rec.at = sp.getLong("at", 0); rec.replacedAt = sp.getLong("replacedAt", 0);
        rec.expected = sp.getInt("expected", -1); rec.code = sp.getInt("code", -1); rec.vol = sp.getInt("vol", -1);
        rec.mutes = sp.getInt("mutes", 0); rec.a11yTries = sp.getInt("a11yTries", 0);
        rec.muted = sp.getBoolean("muted", false); rec.hadOurs = sp.getBoolean("hadOurs", false); rec.audioDone = sp.getBoolean("audioDone", false);
        rec.paused = sp.getBoolean("paused", false); rec.a11yDone = sp.getBoolean("a11yDone", false); rec.toasted = sp.getBoolean("toasted", false);
        rec.saved = sp.getString("a11y", "");
        final AudioManager audio = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        A11yList.carryStep(rec, new A11yList.Tv() {
            @Override public int versionCode() { return BuildConfig.VERSION_CODE; }
            @Override public int volume() { return audio == null ? -1 : audio.getStreamVolume(AudioManager.STREAM_MUSIC); }
            @Override public boolean muted() { return audio != null && audio.isStreamMute(AudioManager.STREAM_MUSIC); }
            @Override public boolean adhanActive() { return sa.urtv.home.tilawah.MusicService.carryAdhanActive(); }
            @Override public boolean ourPlaying() { return sa.urtv.home.tilawah.MusicService.carryOurPlaying(); }
            @Override public boolean musicActive() { return audio != null && audio.isMusicActive(); }
            @Override public boolean mute() {
                try {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                    return audio.isStreamMute(AudioManager.STREAM_MUSIC);
                } catch (RuntimeException e) {
                    return false;
                }
            }
            @Override public void pauseKey() {
                try {
                    audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
                    audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE));
                } catch (RuntimeException ignored) { }
            }
            @Override public String list() { return safe(c, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES); }
            @Override public boolean switchOn() { return "1".equals(safe(c, Settings.Secure.ACCESSIBILITY_ENABLED)); }
            @Override public boolean bound() { return A11yHeal.listedAndBound(c); }
            @Override public boolean canWrite() { return c.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED; }
            @Override public boolean write(String list) {
                try {
                    boolean a = Settings.Secure.putString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, list);
                    boolean b = Settings.Secure.putInt(c.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
                    return a && b && A11yList.ownLost(rec.saved, safe(c, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), A11yHeal.TYPE, A11yHeal.SLEEP).isEmpty();   // read back
                } catch (RuntimeException e) {   // the permission was taken back meanwhile
                    return false;
                }
            }
            @Override public void alarm(long delayMs, int slot) { UpdateCarry.alarm(c, rec.gen, delayMs, slot); }
            @Override public void toast() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try { Toast.makeText(c, S.t("بعد التحديث توقفت خدمة الكتابة من الجوال. شغّلها من إعدادات التلفزيون: إمكانية الوصول، تلفزيونك."), Toast.LENGTH_LONG).show(); } catch (RuntimeException ignored) { }
                });
            }
            @Override public void log(String stage, String result, String more) { UpdateCarry.log(stage, rec.gen, result, more); }
        }, why, gen, System.currentTimeMillis(), FRESH_MS);
        if (rec.finished) { sp.edit().clear().commit(); return; }
        sp.edit().putLong("replacedAt", rec.replacedAt).putInt("mutes", rec.mutes).putInt("a11yTries", rec.a11yTries).putBoolean("audioDone", rec.audioDone)
            .putBoolean("paused", rec.paused).putBoolean("a11yDone", rec.a11yDone).putBoolean("toasted", rec.toasted).commit();
    }

    private static String safe(Context c, String key) {
        try { return Settings.Secure.getString(c.getContentResolver(), key); } catch (RuntimeException e) { return null; }
    }

    /** A durable check: an exact alarm (inexact when the TV does not let us be exact), delivered to A11yHeal with the generation it is for. */
    private static void alarm(Context c, long gen, long delayMs, int slot) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Intent i = new Intent(ACTION).setClass(c, A11yHeal.class).putExtra("gen", gen);
            PendingIntent pi = PendingIntent.getBroadcast(c, 9100 + slot, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            long at = System.currentTimeMillis() + delayMs;
            boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
            try {
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } catch (SecurityException e) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "carry stage=alarm result=not_set slot=" + slot);
        }
    }

    static String actionName() { return ACTION; }

    private static void log(String stage, long gen, String result, String more) {
        Log.i(TAG, "carry stage=" + stage + " gen=" + (gen / 1000 % 1_000_000) + " result=" + result + " " + more);
    }
}
