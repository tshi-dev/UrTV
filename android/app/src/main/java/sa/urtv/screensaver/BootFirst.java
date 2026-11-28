package sa.urtv.screensaver;

import android.content.Context;

/**
 * 4.4 what this app notes once at each boot (BootReceiver, on its thread). Never a setting written back from a kept copy (tests/saver_flush_test.py):
 * only the values this app works out itself. R-470: the settings' style is fixed at the first boot, while a fresh install is still fresh;
 * R-473 (#42): whether this app may draw over apps (a grant noted, so one lost at a later boot is known).
 */
final class BootFirst {
    private BootFirst() { }

    static void atBoot(Context app) {
        Prefs p = new Prefs(app);
        p.settingsStyle();
        p.overlayNeeded();   // 4.4 #42 (R-473): a grant there at this boot is noted, so one lost at a later boot is known
    }
}
