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

    /**
     * Sets the alarm again when the place, zone, adjustments or method in this fragment are not the ones it was last set from. The fragment is
     * null when the read failed or only an old copy of it is left (the ticket is the one taken before that read began): then the alarm stays as it was set and the remembered place serves the rest.
     */
    public static void onConfig(Context c, String fragment, long ticket) {
        Place p = Place.get(c, fragment, ticket);   // a null fragment: the last good place
        PrayerTimes.refreshIfDue(c, p);   // Aladhan's times for today, once the TV is online: the home screen agrees with the page
        Adhan.prefetchIfSkipped(c);   // a recording skipped in standby comes now; nothing is asked when none was skipped
        if (fragment == null) return;   // a failed read says nothing about the alarm
        SharedPreferences sp = c.getSharedPreferences("tilawah", Context.MODE_PRIVATE);
        if (!SchedIdentity.due(sp.getString("sched_identity", ""), p.identity())) return;
        AdhanScheduler.schedule(c, p);   // records the identity of the place it is set from
    }

    /** The shell asks: whatever the settings say now, set the alarm for it. */
    static void reschedule(Context c) {
        AdhanScheduler.schedule(c, Place.get(c));
    }
}
