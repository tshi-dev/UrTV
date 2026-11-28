package sa.urtv.home.tilawah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import sa.urtv.home.OverlayNotice;
import sa.urtv.home.S;
import sa.urtv.home.sports.KickoffPlan;

/**
 * 4.4 «الإقامة»: at the iqama (IqamaScheduler), the person's iqama sound through the dua's way (its own volume, the recitation paused and brought
 * back, never over the adhan: MusicService.dua) and «تنبيه الإقامة» where the person is: a text notice over our home and the screensaver, over
 * another app the kick-off's overlay rule (KickoffPlan.where: the add-on's overlay with the TV's permission for it, else a text notice, nothing in kids mode).
 */
public class IqamaReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        if (intent == null || !IqamaScheduler.ACTION.equals(intent.getAction())) return;
        final Context app = c.getApplicationContext();
        long due = intent.getLongExtra(IqamaScheduler.EXTRA_DUE, 0), now = System.currentTimeMillis();
        int prayer = intent.getIntExtra(IqamaScheduler.EXTRA_PRAYER, -1);
        Prefs prefs = new Prefs(app);
        if (!IqamaPlan.onTime(due, now) || prayer < 0) { Log.i("UrTVAdhan", "iqama skipped: late or no prayer"); return; }
        if (sa.urtv.home.PartsHome.off(app, "prayer")) { Log.i("UrTVAdhan", "iqama skipped: the prayer part is off"); return; }   // 4.4 «أجزاء UrTV»
        PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        boolean screenOn = pm == null || pm.isInteractive();
        if (!screenOn) return;   // as the adhan: never to a dark screen, never made up
        AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        boolean others = am != null && am.isMusicActive() && !MusicService.carryOurPlaying();
        String sound = prefs.iqamaSound();
        if (IqamaPlan.sound(sound, true, MusicService.carryAdhanActive(), prefs.quietNow(app), others)) {
            String path = source(app, sound);
            if (path != null) MusicService.dua(app, path, S.n("الإقامة"), prefs.iqamaVolume());
        }
        if (prefs.iqamaNotice()) {
            final PendingResult done = goAsync();   // the add-on is asked on a thread (IPC, never the UI thread)
            new Thread(() -> { try { notice(app, prayer); } catch (RuntimeException e) { Log.i("UrTVAdhan", "iqama notice: " + e.getClass().getSimpleName()); } finally { done.finish(); } }, "iqama-notice").start();
        }
        Log.i("UrTVAdhan", "iqama: prayer=" + prayer + " sound=" + (sound.isEmpty() ? "none" : sound) + " notice=" + (prefs.iqamaNotice() ? 1 : 0));
    }

    /** the file to play: our chime (copied once from the APK into the app's files), or the person's sound; null when it is gone */
    static String source(Context c, String sound) {
        if ("chime".equals(sound)) {
            File f = new File(c.getFilesDir(), "iqama_chime.ogg");
            if (!f.isFile()) {
                try (InputStream in = c.getResources().openRawResource(sa.urtv.home.R.raw.iqama_chime); FileOutputStream out = new FileOutputStream(f)) {
                    byte[] b = new byte[8192];
                    int n;
                    while ((n = in.read(b)) > 0) out.write(b, 0, n);
                } catch (Exception e) {
                    return null;
                }
            }
            return f.getAbsolutePath();
        }
        if (sound.startsWith("a")) {
            AudioStore.Item it = AudioStore.get(c, Integer.parseInt(sound.substring(1)));
            File f = it == null ? null : AudioStore.file(c, it);
            return f != null && f.isFile() ? f.getAbsolutePath() : null;
        }
        return null;
    }

    private static void notice(Context app, int prayer) {
        S.init(app);
        String line = IqamaText.line(prayer);
        int front = AdhanFront.whereNow(app);
        boolean kids = OverlayNotice.kids(app);
        int where = KickoffPlan.where(front == AdhanFront.AT_HOME ? 0 : front == AdhanFront.AT_SAVER ? 1 : 2, kids, true, front == AdhanFront.AT_OTHER && !kids && OverlayNotice.ready());
        if (where == KickoffPlan.NONE) return;
        final boolean drawn = where == KickoffPlan.OVERLAY && OverlayNotice.show(S.t("الإقامة"), line);
        if (drawn) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            try { Toast.makeText(app, line, Toast.LENGTH_LONG).show(); } catch (RuntimeException ignored) { }
        });
    }
}
