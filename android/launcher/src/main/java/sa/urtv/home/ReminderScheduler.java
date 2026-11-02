package sa.urtv.home;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * One alarm for the next reminder, the way the adhan's is (tilawah/AdhanScheduler): exact when Android allows it, and put back after a reboot,
 * a clock change, an app update, and whenever the tasks change (the receiver below, the screensaver's «SETTINGS_CHANGED» reaches it through
 * the home). The alarm does not wake a TV in standby: a card is only for a screen that is on.
 */
public final class ReminderScheduler {
    static final String ACTION = "sa.urtv.home.REMINDER_ALARM";
    private static final int REQUEST = 48;   // the adhan's is 47

    private ReminderScheduler() { }

    /** The alarm for the next card; none when there is no reminder ahead. `retryAt` > 0 puts it there instead (the adhan is playing: the card waits). */
    static synchronized void schedule(Context c, long retryAt) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long at = retryAt > 0 ? retryAt : ReminderPlan.next(ReminderStore.read(c), System.currentTimeMillis());
        PendingIntent pending = pending(c);
        if (at <= 0) { am.cancel(pending); return; }
        try {
            if (!exactAllowed(c)) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);   // may be late: the card shows when it arrives
            else if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
            else am.setExact(AlarmManager.RTC_WAKEUP, at, pending);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
        }
    }

    public static void schedule(Context c) { schedule(c, 0); }

    static boolean exactAllowed(Context c) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    private static PendingIntent pending(Context c) {
        Intent intent = new Intent(c, ReminderReceiver.class).setAction(ACTION);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, REQUEST, intent, flags);
    }
}
