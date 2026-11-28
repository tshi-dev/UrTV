package sa.urtv.screensaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Some TVs (TCL) leave the app cold after a power cycle and the first dream fails.
 * Any boot broadcast that does arrive brings the process up and re-arms the wake-up job (UpdateJob, which
 * does no work of its own); the persisted job covers TVs that never send one.
 * 4.4 (R-400): at a boot, the idle delay the person chose goes back when the firmware reset it to «never» (IdleKeep; the TV's
 * system setting only, never this app's own settings).
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        UpdateJob.schedule(c);
        if (i == null || !IdleKeep.bootAction(i.getAction())) return;
        final PendingResult pr = goAsync();
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            try {
                BootFirst.atBoot(app);   // 4.4: what this app notes at a boot (R-470 the settings style, R-473 the overlay grant)
                IdleKeep.atBoot(app);
            } catch (RuntimeException ignored) { } finally { pr.finish(); }
        }, "idle-keep").start();
    }
}
