package sa.urtv.home;

/** For tests/role_boot_test.py: RoleBootRule (R-401: the HOME role read again at boot, given back once, else said once). */
public final class RoleBootRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        String g = "com.google.android.apps.tv.launcherx", w = StockHome.WRAITH;
        check("the person's home is ours, the boot gave the role to Google TV, never given back yet: given back", RoleBootRule.decide(true, false, g, false, "", false) == RoleBootRule.REASSERT);
        check("given back once already and lost again at this boot: the home says so once", RoleBootRule.decide(true, false, g, true, "", false) == RoleBootRule.NOTICE);
        check("already said (or waiting to be said): nothing more", RoleBootRule.decide(true, false, g, true, RoleBootRule.TOLD, false) == RoleBootRule.NONE
              && RoleBootRule.decide(true, false, g, true, RoleBootRule.PENDING, false) == RoleBootRule.NONE);
        check("a Chromecast's setup wizard holds Home: never taken from it, said once", RoleBootRule.decide(true, false, w, false, "", false) == RoleBootRule.NOTICE
              && RoleBootRule.decide(true, false, w, false, RoleBootRule.TOLD, false) == RoleBootRule.NONE);
        check("ours: nothing", RoleBootRule.decide(true, true, "sa.urtv.home", false, "", false) == RoleBootRule.NONE);
        check("the person kept the TV's home (not wanted): nothing, ever", RoleBootRule.decide(false, false, g, false, "", false) == RoleBootRule.NONE
              && RoleBootRule.decide(false, false, g, true, "", false) == RoleBootRule.NONE);
        check("a setup runs or the stock home was offered back: nothing", RoleBootRule.decide(true, false, g, false, "", true) == RoleBootRule.NONE);
        check("the log names a known stock home", RoleBootRule.holderWord(g, "x.Main", "sa.urtv.home").equals(g));
        check("the log says ours / setup wizard / none", RoleBootRule.holderWord("sa.urtv.home", null, "sa.urtv.home").equals("ours")
              && RoleBootRule.holderWord(w, null, "sa.urtv.home").equals("setup wizard") && RoleBootRule.holderWord(null, null, "sa.urtv.home").equals("none"));
        check("a settings stand-in is «settings»", RoleBootRule.holderWord("com.android.tv.settings", null, "sa.urtv.home").equals("settings")
              && RoleBootRule.holderWord("com.android.settings", "com.android.settings.FallbackHome", "sa.urtv.home").equals("settings"));
        check("an app the person installed is never named in the log", RoleBootRule.holderWord("com.example.mylauncher", null, "sa.urtv.home").equals("another app"));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
