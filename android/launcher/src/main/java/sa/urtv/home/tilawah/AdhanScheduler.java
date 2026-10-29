package sa.urtv.home.tilawah;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * One alarm for the next adhan, put back after a reboot, a clock change, an app update and when Aladhan's times arrive.
 * It is exact when Android allows it; from Android 12 an app needs the «المنبّهات الدقيقة» permission for that, and
 * without it the system may deliver the alarm minutes late (exactAllowed(), and the warning in the adhan section).
 */
final class AdhanScheduler {
    /** Not «sa.urtv.home.ADHAN»: that one is the shell's «adhan now» and must reach MusicReceiver alone. */
    static final String ACTION = "sa.urtv.home.ADHAN_ALARM";
    private static final String LEGACY_ACTION = "sa.urtv.home.ADHAN";
    /** the prayer time this alarm is for, so a repeated alarm for the same prayer is recognised */
    static final String EXTRA_DUE = "due";
    /** which prayer it is for (PrayerTimes.FAJR…): Fajr has a muezzin of its own */
    static final String EXTRA_PRAYER = "prayer";
    private static final int REQUEST = 47;

    private AdhanScheduler() {}

    static void schedule(Context c) { schedule(c, Place.get(c)); }

    /** The alarm for the next adhan of this place (the caller read it once, so what it was judged by is what the alarm is set for). */
    static void schedule(Context c, Place place) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        cancelLegacy(c, am);
        if (!new Prefs(c).adhanOn()) { am.cancel(pending(c, 0, -1)); return; }
        // the replacement first, then the swap: an alarm is never cancelled for nothing, and a prayer that is due in the next seconds is
        // still the one set (nextAlarm, not next: the screen's 30-second rule would skip it). Setting the same pending intent replaces
        // the old alarm, so there is never one for the old time beside the new.
        PrayerTimes.Next next = PrayerTimes.nextAlarm(c, place);
        if (next == null) { am.cancel(pending(c, 0, -1)); return; }
        Adhan.prefetchChosen(c);   // the recording is on the TV before its prayer, not fetched while it should be playing
        PendingIntent pending = pending(c, next.at, next.index);
        try {
            if (!exactAllowed(c)) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pending);   // may be late: see exactAllowed
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pending);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, next.at, pending);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pending);
        }
    }

    /** False from Android 12 when the exact-alarm permission is not granted: the alarm may then be minutes late. */
    static boolean exactAllowed(Context c) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    /** The alarm a version before this one left behind (another action), so it cannot fire a second time. */
    private static void cancelLegacy(Context c, AlarmManager am) {
        Intent intent = new Intent(c, AdhanReceiver.class).setAction(LEGACY_ACTION);
        PendingIntent old = PendingIntent.getBroadcast(c, REQUEST, intent,
                PendingIntent.FLAG_NO_CREATE | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        if (old != null) am.cancel(old);
    }

    private static PendingIntent pending(Context c, long due, int prayer) {
        Intent intent = new Intent(c, AdhanReceiver.class).setAction(ACTION).putExtra(EXTRA_DUE, due).putExtra(EXTRA_PRAYER, prayer);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, REQUEST, intent, flags);
    }

    /** When the next adhan will play, 0 when it is off; printed by optimize/music.sh adhan. */
    static long scheduledAt(Context c) {
        if (!new Prefs(c).adhanOn()) return 0;
        PrayerTimes.Next next = PrayerTimes.nextAlarm(c);
        return next == null ? 0 : next.at;
    }
}
