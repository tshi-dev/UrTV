package sa.urtv.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

/**
 * Keeps a muted TV muted across its own sleep and a restart (MuteKeepRule has the rules). Remembers the mute when the screen goes off and at shutdown;
 * after the screen comes on (or the boot) checks a few times inside 30 s and mutes once if the TV's audio service unmuted it. Log tag UrTVMute: fixed words only.
 */
public final class MuteKeep {
    private MuteKeep() { }

    static final String TAG = "UrTVMute", PREFS = "mute_keep";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static boolean registered;
    private static int wake;   // the number of this wake: a check of an older wake does nothing

    /** From Application.onCreate: the screen and shutdown broadcasts (they cannot be declared in the manifest), and the boot's own wake. */
    static synchronized void start(final Context app) {
        if (!registered) {
            registered = true;
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_SCREEN_OFF);
            f.addAction(Intent.ACTION_SCREEN_ON);
            f.addAction(Intent.ACTION_SHUTDOWN);
            app.registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    String a = i.getAction();
                    if (Intent.ACTION_SCREEN_ON.equals(a)) woke(app, "screen_on");
                    else remember(app, Intent.ACTION_SHUTDOWN.equals(a) ? "shutdown" : "screen_off");
                }
            }, f);
        }
        try {   // the music stream's mute flips are logged, with who did it (ours() within 3 s: us)
            app.registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    if (i.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) != AudioManager.STREAM_MUSIC) return;
                    boolean m = i.getBooleanExtra("android.media.EXTRA_STREAM_VOLUME_MUTED", false);
                    Log.i(TAG, "mute changed: muted=" + m + " uptime_ms=" + SystemClock.elapsedRealtime() + " by=" + (SystemClock.elapsedRealtime() - oursAt < 3000 ? "us" : "not us (the system, the TV or another app)"));
                }
            }, new IntentFilter("android.media.STREAM_MUTE_CHANGED_ACTION"));
        } catch (RuntimeException ignored) { }
        if (!app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("has", false)) remember(app, "first_start");   // a fresh install has no record yet (the mute was set before it): the first start writes one, or a restart soon after would find none (step 28)
        if (SystemClock.elapsedRealtime() < 120_000) woke(app, "boot");   // this process started with the TV: the wake is the boot
    }

    private static volatile long oursAt;

    /**
     * Every change of the volume or the mute that WE make says so in the log (step 29: the TV was found unmuted and nothing said by whom): what we did and the state after it.
     * The system's own change broadcast is logged below with «by=us» when one of these came within 3 s of it, «by=not us» otherwise.
     */
    public static void ours(Context c, String why) {
        oursAt = SystemClock.elapsedRealtime();
        try { Log.i(TAG, "mute keep stage=ours why=" + why + " muted_now=" + muted(c) + " vol=" + volume(c)); } catch (RuntimeException ignored) { }
    }

    private static AudioManager audio(Context c) { return (AudioManager) c.getSystemService(Context.AUDIO_SERVICE); }

    private static boolean muted(Context c) { AudioManager a = audio(c); return a != null && a.isStreamMute(AudioManager.STREAM_MUSIC); }

    private static int volume(Context c) { AudioManager a = audio(c); return a == null ? -1 : a.getStreamVolume(AudioManager.STREAM_MUSIC); }

    /** the screen went off, or the TV shuts down: the mute and the volume as the owner left them */
    static void remember(Context c, String why) {
        try {
            boolean m = muted(c);
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("muted", m).putInt("vol", volume(c)).putBoolean("has", true).putBoolean("done", false).commit();
            Log.i(TAG, "mute keep stage=remember why=" + why + " muted=" + m);
        } catch (RuntimeException e) {
            Log.w(TAG, "mute keep stage=remember result=error " + e.getClass().getSimpleName());
        }
    }

    private static void woke(final Context app, final String why) {
        final int mine;
        synchronized (MuteKeep.class) { mine = ++wake; }
        final long at = SystemClock.elapsedRealtime();
        final boolean boot = "boot".equals(why);
        final long window = MuteKeepRule.windowFor(boot, at);
        SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean("done", false).apply();   // a new wake: at most once for it
        // uptime in the log: the boot's guard started this long after the TV did, and the checks run this long (step 39: was the unmute before the guard or after its window?)
        Log.i(TAG, "mute keep stage=wake why=" + why + " uptime_ms=" + at + " window_ms=" + window + " muted_now=" + muted(app));
        for (final long delay : boot ? MuteKeepRule.BOOT_CHECKS_MS : MuteKeepRule.CHECKS_MS) {
            if (delay > window) continue;
            MAIN.postDelayed(new Runnable() {
                @Override public void run() { check(app, why, mine, at, window); }
            }, delay);
        }
    }

    private static void check(Context app, String why, int mine, long wakeAt, long window) {
        synchronized (MuteKeep.class) {
            if (mine != wake) return;   // a newer wake took over
            SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long now = SystemClock.elapsedRealtime();
            String d = MuteKeepRule.decide(sp.getBoolean("has", false), sp.getBoolean("muted", false), sp.getInt("vol", -1), sp.getBoolean("done", false), wakeAt, now,
                muted(app), volume(app), sa.urtv.home.tilawah.MusicService.carryAdhanActive(), window);
            if (MuteKeepRule.STILL_MUTED.equals(d)) return;   // nothing to do yet: the next check looks again
            if (MuteKeepRule.MUTE.equals(d)) {
                boolean ok;
                try {
                    audio(app).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                    ok = muted(app);
                    ours(app, "restore_mute");
                } catch (RuntimeException e) {
                    ok = false;
                }
                sp.edit().putBoolean("done", true).apply();
                Log.i(TAG, "mute keep stage=restore why=" + why + " result=" + (ok ? "muted_again" : "failed") + " after_ms=" + (now - wakeAt) + " uptime_ms=" + now);
                return;
            }
            Log.i(TAG, "mute keep stage=check why=" + why + " result=" + d + " uptime_ms=" + now);
        }
    }
}
