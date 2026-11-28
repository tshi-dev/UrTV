package sa.urtv.home;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/** For tests/settings_low_ram_test.py (R-407): the Settings key on a weak TV whose stock home was turned off. */
public final class SettingsLowRamRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        String us = "sa.urtv.home", tcl = "com.tcl.cyberui", wraith = "com.google.android.tungsten.setupwraith";
        check("a stock home installed but off is seen", SettingsKey.stockHomeOff(Arrays.asList(us, tcl), new HashSet<>(Collections.singletonList(us)), us));
        check("a stock home that is on is not «off»", !SettingsKey.stockHomeOff(Arrays.asList(us, tcl), new HashSet<>(Arrays.asList(us, tcl)), us));
        check("only ours, the setup wizard or the settings fallback off: not a stock home", !SettingsKey.stockHomeOff(Arrays.asList(us, wraith, "com.android.tv.settings"),
              new HashSet<>(Collections.singletonList(us)), us));
        check("a weak TV, another app in front, its stock home off: the service listens", SettingsKey.lowRamCatch(false, false, true, true, 30));
        check("...never on our home (the home's own key path)", !SettingsKey.lowRamCatch(true, false, true, true, 30));
        check("...never with a stock home on (its receiver answers the key)", !SettingsKey.lowRamCatch(false, true, true, true, 30));
        check("...never when no stock home was turned off (R-281/4.1: no filter on a weak TV)", !SettingsKey.lowRamCatch(false, false, true, false, 30));
        check("...never below Android 9 (4.3.6)", !SettingsKey.lowRamCatch(false, false, true, true, 27));
        check("a TV that is not weak: this rule does not apply (filterWanted's own)", !SettingsKey.lowRamCatch(false, false, false, true, 30));
        check("filterWanted on a weak TV is unchanged (never over another app)", !SettingsKey.filterWanted(false, false, true, false, 30));
        SettingsKey k = new SettingsKey();
        check("the key itself: the press opens the TV's settings, its release is eaten, another key passes",
              k.decide(176, 0, 0, false, false, false) == SettingsKey.OPEN_TV_SETTINGS && k.decide(176, 1, 0, false, false, false) == SettingsKey.EAT
              && k.decide(22, 0, 0, false, false, false) == SettingsKey.PASS);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
