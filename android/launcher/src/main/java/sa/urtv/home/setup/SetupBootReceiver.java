package sa.urtv.home.setup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * The TV has started again: an alarm is gone with the old boot, so a home switch that was cut off (the journal says «switching») gets its safety switch
 * back at the saved deadline, and the first attempt to put the stock home back is made now. A full setup run that was cut off by the restart (the
 * measurement restarts the TV on purpose) goes on from its step pointer (SetupRunner.resume, after the home is dealt with). Only the system can send
 * BOOT_COMPLETED to it.
 */
public class SetupBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        if (SetupRunner.awaitingRestart(new FileStore(app))) SetupRunner.get().resume(new SetupHost(app));   // its own thread; it waits for a pending home first
        if (!SetupRecovery.pending(app)) return;
        SetupRecovery.rearmFromDisk(app);
        final PendingResult pending = goAsync();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    SetupRecovery.attempt(app, new SetupEngine.Out() { @Override public void line(String text) { } });
                } finally {
                    pending.finish();
                }
            }
        }, "setup-boot").start();
    }
}
