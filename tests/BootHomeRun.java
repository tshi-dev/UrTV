package sa.urtv.home;

/** For tests/boot_home_test.py (R-410): «ابدأ بالرئيسية بعد التشغيل», on by default on Changhong and Skyworth only. */
public final class BootHomeRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("Changhong: on by default", BootHomeRule.on(null, "Changhong", "CHANGHONG"));
        check("Skyworth (by its brand): on by default", BootHomeRule.on(null, "MStar", "Skyworth"));
        check("TCL, Xiaomi, Sony, Google: off by default", !BootHomeRule.on(null, "TCL", "TCL") && !BootHomeRule.on(null, "Xiaomi", "Xiaomi")
              && !BootHomeRule.on(null, "Sony", "Sony") && !BootHomeRule.on(null, "Google", "google"));
        check("nothing known: off", !BootHomeRule.on(null, null, null));
        check("the person's choice wins both ways", !BootHomeRule.on(false, "Changhong", "Changhong") && BootHomeRule.on(true, "TCL", "TCL"));
        check("acts only when on, our home the person's, no setup running", BootHomeRule.act(true, true, false) && !BootHomeRule.act(false, true, false)
              && !BootHomeRule.act(true, false, false) && !BootHomeRule.act(true, true, true));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
