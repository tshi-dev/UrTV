package sa.urtv.home.setup;

import android.content.Context;
import android.widget.Toast;

/**
 * The one door the home screen uses (a single call in MainActivity.onCreate): after a Force-stop or a restart, pressing Home starts OUR home, and a home
 * switch that was cut off must not be forgotten. If the journal says «switching», the safety switch is set again from its saved deadline and one attempt
 * to put the stock home back is made on a background thread. After too many failed attempts a notice is shown at every start until it is dealt with.
 * Never throws: it must not be able to break the home screen.
 */
public final class SetupHook {
    private SetupHook() { }

    public static void atHomeStart(final Context c) {
        try {
            final Context app = c.getApplicationContext();
            if (!SetupRecovery.pending(app)) return;
            SetupRecovery.rearmFromDisk(app);
            if (SetupRecovery.gaveUp(app)) {
                Toast.makeText(app, SetupRecovery.GAVE_UP_NOTICE, Toast.LENGTH_LONG).show();
                return;
            }
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        SetupRecovery.attempt(app, new SetupEngine.Out() { @Override public void line(String text) { } });
                    } catch (RuntimeException ignored) { }
                }
            }, "setup-home-start").start();
        } catch (RuntimeException ignored) {
            // the home screen comes first
        }
    }

    /** the stock interface is off by us («تخلّص من واجهة المصنع» is on): the settings' row says so through the provider (read only) */
    public static boolean factoryOff(Context c) {
        try { String f = new FileStore(c).get(SetupEngine.FACTORY); return f != null && f.startsWith("on"); }
        catch (RuntimeException e) { return false; }
    }

    /** Our home reached the screen (MainActivity.onResume): while the stock interface is off, this boot is fine and BootGuard's count starts again. One small read; a write only when it changes. */
    public static void homeUp(Context c) {
        try {
            FileStore store = new FileStore(c.getApplicationContext());
            String f = store.get(SetupEngine.FACTORY);
            if (f == null || !f.startsWith("on")) return;
            BootGuard.State s = BootGuard.State.parse(store.get(BootGuard.FILE));
            if (s.up && s.failed == 0) return;
            store.put(BootGuard.FILE, BootGuard.homeUp(s).text());
        } catch (Exception ignored) {
            // the home screen comes first
        }
    }
}
