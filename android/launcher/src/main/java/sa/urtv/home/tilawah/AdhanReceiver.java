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
            DuaScheduler.schedule(context);   // 4.4: the duas' alarm too (a reboot, a clock or time-zone change, an update)
            PrayerTimes.refresh(context, null);
            return;
        }

        long now = System.currentTimeMillis();
        long due = intent.getLongExtra(AdhanScheduler.EXTRA_DUE, 0);
        // the prayer's own minute only: an alarm that came hours late (standby, a clock that jumped at boot) or early is skipped and logged, never played
        String late = prefs.alarmWanted() ? AdhanLate.skipReason(due, now, PrayerTimes.aroundToday(context)) : null;
        if (late != null) {
            Log.i("UrTVAdhan", "adhan skipped: " + late);
            prefs.setLastAlarmNote("skipped: " + late, now);
        } else if (prefs.alarmWanted() && screenOn(context) && !alreadyPlayed(prefs, due, now)) {
            Log.i("UrTVAdhan", "adhan start: prayer=" + intent.getIntExtra(AdhanScheduler.EXTRA_PRAYER, -1) + " due=" + due + " late=" + Math.max(0, (now - due) / 1000) + " s, reason=alarm");
            if (due > 0) prefs.setLastDue(due); else prefs.setLastAlarmAt(now);
            int prayer = intent.getIntExtra(AdhanScheduler.EXTRA_PRAYER, -1);   // an alarm of an older version has none: the usual voice
            int front = AdhanFront.now(context);
            int voice = prayer < 0 ? prefs.playVoiceFor(context, false) : prefs.playFor(context, prayer);   // 4.4 «أذان كل صلاة»
            boolean silent = voice == PrayerSound.SILENT
                    || QuietHours.silencesAdhan(prefs.quietOn(), prefs.quietNow(context), prefs.quietAdhan(), prayer);   // 4.4 «ساعات الهدوء»
            boolean play = prefs.adhanOn() && AdhanPlan.mayPlay(prefs.adhanSaverOnly(), front) && !silent;
            boolean notice = prefs.adhanNotice() && prefs.screenOn(prayer);   // 4.4 beta 4: a prayer set to no screen has no notice either
            if (play) MusicService.adhan(context, voice, null, 0, prayer);
            else if (prefs.adhanOn() && silent) Log.i("UrTVAdhan", "adhan silent: this prayer is set to no sound (its time and card stay)");
            else if (prefs.adhanOn()) Log.i("UrTVAdhan", "adhan skipped: another app is in front (saver-only is on)");
            if (notice) AdhanNotice.show(context, prayer);
            boolean saver = AdhanPlan.startSaver(prefs.adhanSaverAt(), front) && AdhanSaver.start(context);   // after the adhan and the notice: nothing waits for it
            prefs.setLastAlarmNote(AdhanPlan.note(front, play, notice, saver), now);
        }
        if (late == null && due > 0) iqama(context, prefs, intent.getIntExtra(AdhanScheduler.EXTRA_PRAYER, -1), due);   // 4.4 «الإقامة»: its alarm, from this adhan's time
        AdhanScheduler.schedule(context);
        PrayerTimes.refresh(context, null);
    }

    /** 4.4 «الإقامة»: the alarm at this prayer's iqama, when its sound or its notice is on (none on a Friday's Dhuhr) */
    private static void iqama(Context c, Prefs prefs, int prayer, long due) {
        if (!IqamaPlan.wanted(prefs.iqamaSound(), prefs.iqamaNotice())) return;
        java.util.Calendar cal = java.util.Calendar.getInstance(sa.urtv.home.ZoneFix.zone(Place.get(c).tz), java.util.Locale.US);
        cal.setTimeInMillis(due);
        long at = IqamaPlan.at(prayer, due, cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY);
        if (at > 0) IqamaScheduler.set(c, prayer, at);
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
