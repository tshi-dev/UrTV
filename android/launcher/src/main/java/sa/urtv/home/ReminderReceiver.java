package sa.urtv.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import sa.urtv.home.tilawah.AdhanFront;
import sa.urtv.home.tilawah.NowPlaying;

/**
 * A reminder's alarm goes off (and the alarm is put back after a reboot, a clock change or an update). What it does depends on what is in front:
 * the home draws its card (ReminderCard, told by a broadcast of our own), the screensaver draws its own from its page's clock, and over another
 * app a text notice shows for 5 seconds (a toast: it takes no focus and needs no overlay permission, like «تنبيه عند الأذان»). While the adhan
 * plays the card waits a minute and asks again. A TV in standby shows nothing and makes nothing up. Silent: the chime is the card's, and only when asked.
 */
public class ReminderReceiver extends BroadcastReceiver {
    /** the home draws the card now (an own broadcast, with setPackage) */
    static final String SHOW = "sa.urtv.home.REMINDER_SHOW";
    private static final long ADHAN_WAIT = 60_000L;
    private static final long NOTICE_MS = 5_000L, FIRST_MS = 3_500L;

    @Override public void onReceive(Context c, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (!ReminderScheduler.ACTION.equals(intent.getAction())) {   // boot, a clock change, an update, the tasks changed
            ReminderScheduler.schedule(c);
            return;
        }
        long now = System.currentTimeMillis();
        List<ReminderPlan.Row> showing = ReminderPlan.showing(ReminderStore.read(c), now);
        if (!showing.isEmpty() && screenOn(c)) {
            if (NowPlaying.adhanLive()) { ReminderScheduler.schedule(c, now + ADHAN_WAIT); return; }   // the card waits for the adhan to end
            ReminderPlan.Row r = showing.get(0);
            int front = AdhanFront.whereNow(c);
            if (front == AdhanFront.AT_HOME) c.sendBroadcast(new Intent(SHOW).setPackage(c.getPackageName()));
            else if (front == AdhanFront.AT_OTHER) notice(c, r, now);
            // the screensaver (DREAM) draws its own card from its page's clock
        }
        ReminderScheduler.schedule(c);
    }

    private static boolean screenOn(Context c) {
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        return pm == null || pm.isInteractive();
    }

    /** «تذكير: موعد طبيب الأسنان · بعد ٣٠ دقيقة» for 5 seconds over whatever app is in front: a long toast, then a short one, cut at 5 s */
    static void notice(Context c, ReminderPlan.Row r, long now) {
        final Context app = c.getApplicationContext();
        S.init(app);
        Digits.init(app);
        final String text = ReminderCard.line(app, r, now);
        try {
            final Handler h = new Handler(Looper.getMainLooper());
            Toast.makeText(app, text, Toast.LENGTH_LONG).show();
            h.postDelayed(() -> {
                try {
                    final Toast second = Toast.makeText(app, text, Toast.LENGTH_SHORT);
                    second.show();
                    h.postDelayed(() -> { try { second.cancel(); } catch (RuntimeException ignored) { } }, NOTICE_MS - FIRST_MS);
                } catch (RuntimeException ignored) { }
            }, FIRST_MS);
            Log.i("UrTVReminder", "notice shown");   // never the words of a task
        } catch (RuntimeException e) {
            Log.i("UrTVReminder", "notice could not be shown: " + e.getClass().getSimpleName());
        }
    }
}
