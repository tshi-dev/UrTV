package sa.urtv.home.tilawah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * Plays the adhan when its alarm goes off (and shows «تنبيه عند الأذان», and starts the screensaver for «شاشة التوقف وقت الأذان», when those are on, with the adhan on or not) — pausing a surah that is playing and resuming it after —
 * and sets the next alarm again after a reboot, a clock change or an update.
 *
 * The adhan only starts while the TV is on: an alarm that arrives in standby is skipped, and none is
 * made up after a reboot; one that is playing stops when the screen goes off. (Without the exact-alarm
 * permission Android may deliver the alarm itself minutes late; the adhan then plays when it arrives.)
 */
public class AdhanReceiver extends BroadcastReceiver {
    /** an alarm without its prayer time (one set by an older version): the same adhan never plays twice within this */
    private static final long DUPLICATE = 5 * 60_000L;

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        Prefs prefs = new Prefs(context);
        boolean alarm = AdhanScheduler.ACTION.equals(intent.getAction());

        if (!alarm) {
            // boot, clock or timezone change, or the app was just updated: set the next alarm, and ask Aladhan for today
            AdhanScheduler.schedule(context);
            PrayerTimes.refresh(context, null);
            return;
        }

        long now = System.currentTimeMillis();
        long due = intent.getLongExtra(AdhanScheduler.EXTRA_DUE, 0);
        if (prefs.alarmWanted() && screenOn(context) && !alreadyPlayed(prefs, due, now)) {
            if (due > 0) prefs.setLastDue(due); else prefs.setLastAlarmAt(now);
            int prayer = intent.getIntExtra(AdhanScheduler.EXTRA_PRAYER, -1);   // an alarm of an older version has none: the usual voice
            int front = AdhanFront.now(context);
            boolean play = prefs.adhanOn() && AdhanPlan.mayPlay(prefs.adhanSaverOnly(), front);
            boolean notice = prefs.adhanNotice();
            if (play) MusicService.adhan(context, prefs.playVoiceFor(context, prayer == PrayerTimes.FAJR), null, 0, prayer);
            else if (prefs.adhanOn()) Log.i("UrTVAdhan", "adhan skipped: another app is in front (saver-only is on)");
            if (notice) AdhanNotice.show(context, prayer);
            boolean saver = AdhanPlan.startSaver(prefs.adhanSaverAt(), front) && AdhanSaver.start(context);   // after the adhan and the notice: nothing waits for it
            prefs.setLastAlarmNote(AdhanPlan.note(front, play, notice, saver), now);
        }
        AdhanScheduler.schedule(context);
        PrayerTimes.refresh(context, null);
    }

    /**
     * The alarm for this prayer time already played. Judged by the prayer's own time, not by when something last
     * played: a preview of the muezzin (a tap on a prayer in «تلاوة») five minutes before must not silence the real adhan.
     */
    private static boolean alreadyPlayed(Prefs prefs, long due, long now) {
        return due > 0 ? prefs.lastDue() == due : now - prefs.lastAlarmAt() <= DUPLICATE;
    }

    /** Standby is off: the adhan is never played to a dark screen, and never made up later. */
    private static boolean screenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm == null || pm.isInteractive();
    }
}
