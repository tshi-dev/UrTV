package sa.urtv.home;

/** For tests/home_tile_test.py: HomeBack (what Back does on the home screen, and whether this app is the default home). Prints PASS/FAIL lines. */
public final class HomeBackRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("as the default home Back never leaves it (it has nowhere to go back to), scrolled or not", !HomeBack.leaves(true, false) && !HomeBack.leaves(true, true));
        check("opened from its tile while the TV keeps its own home, Back at the top leaves", HomeBack.leaves(false, false));
        check("…but Back in the scrolled grid goes to the dock first", !HomeBack.leaves(false, true));
        check("the default home is the app the HOME intent resolves to", HomeBack.isDefaultHome("sa.urtv.home", "sa.urtv.home"));
        check("another home, no home, or a chooser (no package) is not ours", !HomeBack.isDefaultHome("com.google.android.apps.tv.launcherx", "sa.urtv.home") && !HomeBack.isDefaultHome(null, "sa.urtv.home") && !HomeBack.isDefaultHome("android", "sa.urtv.home"));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
