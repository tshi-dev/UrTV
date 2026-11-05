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
        deliver(c, now);
        ReminderScheduler.schedule(c);
    }

    /**
     * The cards that came due and were not delivered yet (ReminderPlan.fresh: each its own, by its key), each once: two reminders inside
     * one grace both come, an older unanswered one is not repeated, and an alarm delivered twice delivers nothing twice.
     */
    private static void deliver(Context c, long now) {
        List<ReminderPlan.Row> showing = ReminderPlan.showing(ReminderStore.read(c), now);
        java.util.Set<String> delivered = ReminderStore.delivered(c);
        List<ReminderPlan.Row> fresh = ReminderPlan.fresh(showing, delivered);
        if (fresh.isEmpty() || !screenOn(c)) {
            ReminderStore.setDelivered(c, ReminderPlan.remember(delivered, showing, new java.util.ArrayList<>()));   // old keys go
            return;
        }
        if (NowPlaying.adhanLive()) { ReminderScheduler.schedule(c, now + ADHAN_WAIT); return; }   // the card waits for the adhan to end
        int front = AdhanFront.whereNow(c);
        if (front == AdhanFront.AT_HOME) c.sendBroadcast(new Intent(SHOW).setPackage(c.getPackageName()));
        else if (front == AdhanFront.AT_OTHER) notice(c, fresh, now);   // one notice, a line for each
        else return;   // the screensaver (DREAM) draws its own card from its page's clock; nothing delivered here
        ReminderStore.setDelivered(c, ReminderPlan.remember(delivered, showing, fresh));
    }

    private static boolean screenOn(Context c) {
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        return pm == null || pm.isInteractive();
    }

    /** «تذكير: موعد طبيب الأسنان · بعد ٣٠ دقيقة» for 5 seconds over whatever app is in front: a long toast, then a short one, cut at 5 s */
    static void notice(Context c, ReminderPlan.Row r, long now) { notice(c, java.util.Collections.singletonList(r), now); }

    /** the same, one line for each reminder that came due together */
    static void notice(Context c, List<ReminderPlan.Row> rows, long now) {
        final Context app = c.getApplicationContext();
        S.init(app);
        Digits.init(app);
        StringBuilder lines = new StringBuilder();
        for (ReminderPlan.Row r : rows) lines.append(lines.length() > 0 ? "\n" : "").append(ReminderCard.line(app, r, now));
        final String text = lines.toString();
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
