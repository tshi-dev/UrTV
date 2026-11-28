package sa.urtv.home;

/** For tests/xiaomi_home_catch_test.py (R-405): the Home catch's per-model allow-list on Xiaomi's Google TVs; R-310's rule for every other Xiaomi. */
public final class XiaomiHomeCatchRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    static boolean safe(String model, int sdk) { return RemoteButtons.homeCatchSafe(RemoteButtons.tv("Xiaomi", "Xiaomi", model, "x", sdk), model); }

    public static void main(String[] args) {
        check("MiTV-MZTU0 (Android 14, Google TV) is a Xiaomi of the remote table", RemoteButtons.tv("Xiaomi", "Xiaomi", "MiTV-MZTU0", "x", 34) == RemoteButtons.XIAOMI);
        check("...and its Home catch is on again", safe("MiTV-MZTU0", 34));
        check("MiTV-AFMU0, AFMU1, AXTU0: on", safe("MiTV-AFMU0", 34) && safe("MiTV-AFMU1", 34) && safe("MiTV-AXTU0", 34));
        check("R-310 kept: MiTV-MOOR2 (Android 11, PatchWall remote) stays off", !safe("MiTV-MOOR2", 30));
        check("R-310 kept: MiTV-AFKR0 and MiTV-AXSO2 stay off", !safe("MiTV-AFKR0", 30) && !safe("MiTV-AXSO2", 30));
        check("R-310 kept: a Xiaomi model nobody listed stays off", !safe("MiTV-MXXX9", 34));
        check("the one-argument rule is R-310's, unchanged", !RemoteButtons.homeCatchSafe(RemoteButtons.XIAOMI) && RemoteButtons.homeCatchSafe(RemoteButtons.TCL));
        check("a TCL keeps the catch (build 608)", RemoteButtons.homeCatchSafe(RemoteButtons.TCL, "Smart TV Pro"));
        check("a listed name on another kind of TV changes nothing there", RemoteButtons.homeCatchSafe(RemoteButtons.TCL, "MiTV-MZTU0"));
        check("app buttons stay off on every Xiaomi (filterSafe is not widened)", !RemoteButtons.filterSafe(RemoteButtons.XIAOMI, 34));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
