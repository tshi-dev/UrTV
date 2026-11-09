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
        factoryGuard(app);
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

    /**
     * While the stock interface is off (SetupEngine.FACTORY says «on»): this boot is counted by BootGuard from the last one (did our home come up, did our process crash), and two failed boots
     * in a row give the factory interface back (SetupEngine.restoreFactory, over the TV's own ADB: the key is kept while the step is on). A restore that did not finish is tried again at the next boot.
     */
    private void factoryGuard(final Context app) {
        try {
            final FileStore store = new FileStore(app);
            String f = store.get(SetupEngine.FACTORY);
            if (f == null || !f.startsWith("on")) return;
            BootGuard.State prev = BootGuard.State.parse(store.get(BootGuard.FILE));
            long now = System.currentTimeMillis();
            final BootGuard.Decision d = BootGuard.onBoot(prev, crashesSince(app, prev.bootAt), now);
            store.put(BootGuard.FILE, d.state.text());
            if (!d.restore) return;
            final PendingResult pending = goAsync();
            new Thread(new Runnable() {
                @Override public void run() {
                    try (SetupSession s = SetupSession.open(app, 0, null, new SetupEngine.Out() { @Override public void line(String text) { } })) {
                        SetupEngine.Result r = s.engine.restoreFactory();
                        if (r.status == SetupEngine.Status.DONE) store.put(BootGuard.FILE, BootGuard.fresh(System.currentTimeMillis()).text());
                    } catch (Exception e) {
                        // the TV did not answer (ADB off): the count stays at the limit and the next boot tries again
                    } finally {
                        pending.finish();
                    }
                }
            }, "factory-restore").start();
        } catch (Exception ignored) {
            // the boot must not be broken by this
        }
    }

    /** how many times our process crashed (or was not answering) since {@code since}; -1 when this Android cannot say (before 11) */
    static int crashesSince(Context c, long since) {
        if (android.os.Build.VERSION.SDK_INT < 30) return -1;
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            int n = 0;
            for (android.app.ApplicationExitInfo e : am.getHistoricalProcessExitReasons(c.getPackageName(), 0, 20)) {
                int r = e.getReason();
                if (e.getTimestamp() > since && (r == android.app.ApplicationExitInfo.REASON_CRASH || r == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE
                        || r == android.app.ApplicationExitInfo.REASON_ANR || r == android.app.ApplicationExitInfo.REASON_INITIALIZATION_FAILURE)) n++;
            }
            return n;
        } catch (RuntimeException e) {
            return -1;
        }
    }
}
