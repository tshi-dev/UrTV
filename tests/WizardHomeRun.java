package sa.urtv.home.setup;

/** For tests/wizard_home_test.py (R-409): the setup wizard keeps Home; how the result and the report say it. */
public final class WizardHomeRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        String w = "com.google.android.tungsten.setupwraith";
        check("Home opened the wizard: it keeps Home", WizardHome.holds("  mResumedActivity: ActivityRecord{1 u0 " + w + "/.FallbackHome t2}", "sa.urtv.home/.MainActivity"));
        check("HOME resolves to the wizard: it keeps Home", WizardHome.holds(null, w + "/.SetupWraithActivity"));
        check("Google TV's home keeps it: not the wizard (the watch's case)", !WizardHome.holds("mResumedActivity: ActivityRecord{1 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t2}",
              "com.google.android.apps.tv.launcherx/.home.HomeActivity"));
        check("ours: not the wizard", !WizardHome.holds("mResumedActivity: ActivityRecord{1 u0 sa.urtv.home/.MainActivity t2}", "sa.urtv.home/.MainActivity"));
        check("nothing known: not the wizard", !WizardHome.holds(null, null));
        String on = WizardHome.reportLine(w + "/.FallbackHome", true), off = WizardHome.reportLine(w + "/.FallbackHome", false);
        check("the report names the catch and the tile, and says never to turn the wizard off", on != null && on.contains("Home key catch (on)") && on.contains("«تلفزيونك» tile")
              && on.contains("never turn the wizard off"));
        check("the catch off is said too", off != null && off.contains("catch is OFF") && off.contains("«تلفزيونك» tile"));
        check("no line when Home does not open the wizard", WizardHome.reportLine("sa.urtv.home/.MainActivity", true) == null && WizardHome.reportLine("?", true) == null);
        check("the person's words name the tile and the add-on, no «·»", WizardHome.SAID_CATCH.contains("«تلفزيونك»") && WizardHome.SAID_CATCH.contains("«إضافات تلفزيونك»")
              && !WizardHome.SAID_CATCH.contains("·") && !WizardHome.SAID_NO_CATCH.contains("·") && WizardHome.SAID_NO_CATCH.contains("«تلفزيونك»"));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
