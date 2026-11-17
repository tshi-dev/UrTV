package sa.urtv.home.tilawah;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** 4.4 «الإقامة»: one alarm at the next iqama, set by the adhan's alarm when it goes off (IqamaPlan.at); none is made up after a reboot. */
final class IqamaScheduler {
    private IqamaScheduler() { }

    static final String ACTION = "sa.urtv.home.IQAMA_ALARM";
    static final String EXTRA_DUE = "due", EXTRA_PRAYER = "prayer";
    private static final int REQUEST = 53;

    static void set(Context c, int prayer, long at) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null || at <= System.currentTimeMillis()) return;
        Intent i = new Intent(c, IqamaReceiver.class).setAction(ACTION).putExtra(EXTRA_DUE, at).putExtra(EXTRA_PRAYER, prayer);
        PendingIntent p = PendingIntent.getBroadcast(c, REQUEST, i, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        try {
            if (AdhanScheduler.exactAllowed(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        }
    }
}
