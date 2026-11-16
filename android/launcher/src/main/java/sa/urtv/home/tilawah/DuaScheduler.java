package sa.urtv.home.tilawah;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

/**
 * 4.4: one alarm for the next dua or reminder (the earliest of the ones that are on), set again after each one plays, after a reboot, a clock
 * or time-zone change, an update, a new city, and whenever the list changes. Exact like the adhan's when Android allows it.
 */
final class DuaScheduler {
    static final String ACTION = "sa.urtv.home.DUA_ALARM";
    static final String EXTRA_DUE = "due";
    private static final int REQUEST = 48;

    private DuaScheduler() { }

    /** the next moment over all duas that are on, 0 when none */
    static long next(Context c, long after) {
        List<DuaStore.Dua> all = DuaStore.list(c);
        if (all.isEmpty()) return 0;
        DuaDays days = new DuaDays(c);
        long best = 0;
        for (DuaStore.Dua d : all) {
            if (!d.on) continue;
            long t = d.rule.next(Math.max(after, d.lastDue), days);
            if (t > 0 && (best == 0 || t < best)) best = t;
        }
        return best;
    }

    static synchronized void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long at;
        try { at = next(c, System.currentTimeMillis()); } catch (RuntimeException e) { at = 0; }
        if (at <= 0) { am.cancel(pending(c, 0)); return; }
        PendingIntent p = pending(c, at);
        try {
            if (!AdhanScheduler.exactAllowed(c)) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        }
    }

    private static PendingIntent pending(Context c, long due) {
        Intent i = new Intent(c, DuaReceiver.class).setAction(ACTION).putExtra(EXTRA_DUE, due);
        return PendingIntent.getBroadcast(c, REQUEST, i, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }
}
