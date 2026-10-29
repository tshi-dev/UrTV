package sa.urtv.home.tilawah;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The adhan alarm was set for the prayer times as they were. When «تعديل وقت الصلاة», the city, its zone or the calculation method
 * changes (on the TV's screen or from the shell) the alarm has to be set again, or it rings at the old minute. The home screen calls this once a minute with the
 * screensaver's settings it has read anyway; the shell's {@code ADHAN_RESCHEDULE} does the same at once.
 */
public final class AdhanSync {
    private AdhanSync() { }

    /** Sets the alarm again when the place, zone, adjustments or method in this fragment are not the ones it was last set with. */
    public static void onConfig(Context c, String fragment) {
        Place p = Place.get(c, fragment);   // a null fragment (the read failed): the last good place
        SharedPreferences sp = c.getSharedPreferences("tilawah", Context.MODE_PRIVATE);
        PrayerTimes.refreshIfDue(c, p);   // Aladhan's times for today, once the TV is online: the home screen agrees with the page
        Adhan.prefetchIfSkipped(c);   // a recording skipped in standby comes now; nothing is asked when none was skipped
        if (fragment == null) return;   // a failed read says nothing about the alarm: it stays as it was set
        String now = identity(p);
        if (now.equals(sp.getString("sched_identity", ""))) return;
        sp.edit().putString("sched_identity", now).apply();
        AdhanScheduler.schedule(c, p);   // the place the identity was made from
    }

    /** The shell asks: whatever the settings say now, set the alarm for it. */
    static void reschedule(Context c) {
        SharedPreferences sp = c.getSharedPreferences("tilawah", Context.MODE_PRIVATE);
        Place p = Place.get(c);
        sp.edit().putString("sched_identity", identity(p)).apply();
        AdhanScheduler.schedule(c, p);
    }

    private static String identity(Place p) {
        return PrayerMath.identity(p.lat, p.lng, p.tz, PrayerAdjust.text(p.adj), p.method);
    }
}
