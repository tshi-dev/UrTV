package sa.urtv.home;

/** For tests/home_caller_test.py (R-412). */
public final class HomeCallerRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] args) {
        check("the Home key (the system)", HomeCaller.word("android").equals("system"));
        check("the TV's settings (Android TV's, a TCL's own)", HomeCaller.word("com.android.tv.settings").equals("the TV's settings")
              && HomeCaller.word("com.tcl.settings").equals("the TV's settings"));
        check("a TCL app", HomeCaller.word("com.tcl.cyberui").equals("a maker's app"));
        check("ours", HomeCaller.word("sa.urtv.extras").equals("ours"));
        check("an app the person installed is never named", HomeCaller.word("com.example.player").equals("another app"));
        check("nothing said", HomeCaller.word(null).equals("none said"));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
