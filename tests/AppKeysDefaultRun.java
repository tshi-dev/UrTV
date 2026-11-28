package sa.urtv.home;

/** For tests/app_keys_default_test.py (R-406): the remote's app buttons on their default open their own app from the add-on, on a TCL and a Xiaomi Google TV. */
public final class AppKeysDefaultRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    static boolean eq(String a, String b) { return a == null ? b == null : a.equals(b); }

    public static void main(String[] args) {
        int tcl = RemoteButtons.TCL, mi = RemoteButtons.XIAOMI;
        check("TCL YouTube (BUTTON_3 or 4065): YouTube", eq(KeyApps.globalOwn(tcl, "youtube", true), RemoteButtons.YOUTUBE_APP));
        check("TCL Netflix (4062): Netflix", eq(KeyApps.globalOwn(tcl, "netflix", true), RemoteButtons.NETFLIX_APP));
        check("TCL Prime Video (4099): Prime Video", eq(KeyApps.globalOwn(tcl, "prime", true), RemoteButtons.PRIME_APP));
        check("TCL Channel (4020): no app known, the TV's own", KeyApps.globalOwn(tcl, "tclchannel", true) == null);
        check("the person kept the TV's home: nothing of ours", KeyApps.globalOwn(tcl, "netflix", false) == null && KeyApps.globalOwn(tcl, "prime", false) == null);
        check("the ids are the TCL remote's (4062, 4099)", "netflix".equals(RemoteButtons.idOf(tcl, 4062, 0, false, -1, -1)) && "prime".equals(RemoteButtons.idOf(tcl, 4099, 0, false, -1, -1)));
        check("another brand: nothing (its buttons reach its own handler)", KeyApps.globalOwn(RemoteButtons.SONY, "netflix", true) == null);
        check("a Xiaomi Google TV's YouTube button (BUTTON_3): YouTube", eq(KeyApps.xiaomiGtvOwn(mi, true, "youtube", true), RemoteButtons.YOUTUBE_APP)
              && "youtube".equals(RemoteButtons.idOf(mi, 190, 0, false, -1, -1)));
        check("a Xiaomi button with no app known (Okko): the TV's own", KeyApps.xiaomiGtvOwn(mi, true, "okko", true) == null);
        check("a Xiaomi off the list: nothing (R-310)", KeyApps.xiaomiGtvOwn(mi, false, "youtube", true) == null && !KeyApps.xiaomiGtvWanted(false, true));
        check("a Xiaomi Google TV whose person kept the TV's home: nothing", KeyApps.xiaomiGtvOwn(mi, true, "youtube", false) == null && !KeyApps.xiaomiGtvWanted(true, false));
        check("xiaomiGtv: a listed Xiaomi model only", RemoteButtons.xiaomiGtv(mi, "MiTV-MZTU0") && !RemoteButtons.xiaomiGtv(mi, "MiTV-MOOR2")
              && !RemoteButtons.xiaomiGtv(tcl, "MiTV-MZTU0"));
        KeyApps.Outside o = new KeyApps.Outside();
        check("a press opens Netflix once; its release is eaten", o.decide("netflix", 4062, 0, 0, 1000, false, KeyApps.globalOwn(tcl, "netflix", true), 0) == KeyApps.OPEN
              && RemoteButtons.NETFLIX_APP.equals(o.pkg) && o.decide("netflix", 4062, 1, 0, 1100, false, null, 0) == KeyApps.EAT);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
