package sa.urtv.home.sports;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import sa.urtv.home.KickoffReceiver;

/** 4.4 one alarm for the next kick-off of a followed club, the way the reminders' is (ReminderScheduler): exact when Android allows it */
public final class KickoffScheduler {
    public static final String ACTION = "sa.urtv.home.KICKOFF_ALARM";
    private static final int REQUEST = 49;   // the adhan's is 47, the reminders' 48

    private KickoffScheduler() { }

    public static synchronized void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long at = KickoffPlan.next(KickoffStore.games(c), System.currentTimeMillis(), KickoffStore.done(c));
        Intent intent = new Intent(c, KickoffReceiver.class).setAction(ACTION);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pending = PendingIntent.getBroadcast(c, REQUEST, intent, flags);
        if (at <= 0) { am.cancel(pending); return; }
        try {
            boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
            if (!exact) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
            else if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
            else am.setExact(AlarmManager.RTC_WAKEUP, at, pending);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
        }
    }

    /** the page's list (the home card's bridge): kept, and the alarm put on the soonest */
    public static void handOver(Context c, String json) {
        KickoffStore.save(c, json);
        schedule(c);
    }
}
