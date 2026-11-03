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
        if (SystemClock.elapsedRealtime() < 120_000) woke(app, "boot");   // this process started with the TV: the wake is the boot
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
        SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean("done", false).apply();   // a new wake: at most once for it
        for (final long delay : MuteKeepRule.CHECKS_MS) {
            MAIN.postDelayed(new Runnable() {
                @Override public void run() { check(app, why, mine, at); }
            }, delay);
        }
    }

    private static void check(Context app, String why, int mine, long wakeAt) {
        synchronized (MuteKeep.class) {
            if (mine != wake) return;   // a newer wake took over
            SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long now = SystemClock.elapsedRealtime();
            String d = MuteKeepRule.decide(sp.getBoolean("has", false), sp.getBoolean("muted", false), sp.getInt("vol", -1), sp.getBoolean("done", false), wakeAt, now,
                muted(app), volume(app), sa.urtv.home.tilawah.MusicService.carryAdhanActive());
            if (MuteKeepRule.STILL_MUTED.equals(d)) return;   // nothing to do yet: the next check looks again
            if (MuteKeepRule.MUTE.equals(d)) {
                boolean ok;
                try {
                    audio(app).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                    ok = muted(app);
                } catch (RuntimeException e) {
                    ok = false;
                }
                sp.edit().putBoolean("done", true).apply();
                Log.i(TAG, "mute keep stage=restore why=" + why + " result=" + (ok ? "muted_again" : "failed") + " after_ms=" + (now - wakeAt));
                return;
            }
            Log.i(TAG, "mute keep stage=check why=" + why + " result=" + d);
        }
    }
}
