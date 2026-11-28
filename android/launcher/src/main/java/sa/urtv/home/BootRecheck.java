package sa.urtv.home;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 4.4 (R-502, docs/research/LAUNCHERS-GITHUB.md ideas 5 and 6; an idea of aerial-tv, MIT, written our own way): on some TVs the stock launcher takes HOME
 * back, or the key service is dropped, some 20 s into the boot, after our boot checks ran. Two more looks, 25 s and 60 s after BOOT_COMPLETED (alarms to
 * A11yHeal): who holds HOME (SetupHook.roleAtBoot: given back once, else the home says so once; never over the setup wizard or a setup), our key service
 * (A11yHeal.check, and the add-on's Home catch judged again). Settings only: nothing is ever brought to the front, so an HDMI input or a «startup source»
 * the TV booted into, and an app the person opened, stay as they are. The screensaver's choice has its own keeper. Fixed words in the log (UrTVA11y).
 */
public final class BootRecheck {
    private BootRecheck() { }

    static final String ACTION = "sa.urtv.home.BOOT_RECHECK";
    static final long[] AT_MS = {25_000, 60_000};

    /** at BOOT_COMPLETED (A11yHeal): the two late looks */
    static void arm(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            for (int k = 0; k < AT_MS.length; k++) {
                Intent i = new Intent(ACTION).setClass(c, A11yHeal.class);
                PendingIntent pi = PendingIntent.getBroadcast(c, 9150 + k, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + AT_MS[k], pi);
            }
        } catch (RuntimeException e) {
            Log.w(A11yHeal.TAG, "boot recheck: not set");
        }
    }

    /** one late look, on a thread of its own (never the receiver's main thread) */
    static void look(final Context app, android.content.BroadcastReceiver r) {
        final android.content.BroadcastReceiver.PendingResult pending = r.goAsync();
        try {
            new Thread(() -> {
                try {
                    Log.i(A11yHeal.TAG, "boot recheck: home and key service");
                    sa.urtv.home.setup.SetupHook.roleAtBoot(app);
                    A11yHeal.rearm(app, "boot-late");
                    A11yHeal.check(app, "boot");
                } catch (RuntimeException e) {
                    Log.w(A11yHeal.TAG, "boot recheck: failed");
                } finally {
                    if (pending != null) pending.finish();
                }
            }, "boot-recheck").start();
        } catch (RuntimeException e) {
            if (pending != null) pending.finish();
        }
    }
}
