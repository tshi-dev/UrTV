package sa.urtv.screensaver;

import android.content.Context;

/**
 * 4.4 what this app notes once at each boot (BootReceiver, on its thread). Never a setting written back from a kept copy (tests/saver_flush_test.py):
 * only the values this app works out itself. R-470: the settings' style is fixed at the first boot, while a fresh install is still fresh.
 */
final class BootFirst {
    private BootFirst() { }

    static void atBoot(Context app) {
        Prefs p = new Prefs(app);
        p.settingsStyle();
    }
}
