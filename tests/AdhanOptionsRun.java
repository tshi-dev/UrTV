package sa.urtv.home.tilawah;

/** For tests/adhan_options_test.py: AdhanPlan (what the adhan's alarm does) and the adhan's length. One line per check: name TAB ok (or FAIL). */
public final class AdhanOptionsRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] args) {
        // ---- what is in front
        check("front: our home wins", AdhanPlan.front(true, 0) == AdhanPlan.HOME && AdhanPlan.front(true, 1) == AdhanPlan.HOME);
        check("front: the screensaver showing", AdhanPlan.front(false, 1) == AdhanPlan.DREAM);
        check("front: neither is another app or an input", AdhanPlan.front(false, 0) == AdhanPlan.OTHER);
        check("front: no answer from the screensaver app is not known", AdhanPlan.front(false, -1) == AdhanPlan.UNKNOWN);

        // ---- «الأذان على شاشة التوقف فقط»
        for (int f : new int[]{AdhanPlan.HOME, AdhanPlan.DREAM, AdhanPlan.OTHER, AdhanPlan.UNKNOWN})
            check("saver-only off: the adhan plays whatever is in front (" + f + ")", AdhanPlan.mayPlay(false, f));
        check("saver-only on: plays over our home", AdhanPlan.mayPlay(true, AdhanPlan.HOME));
        check("saver-only on: plays over the screensaver", AdhanPlan.mayPlay(true, AdhanPlan.DREAM));
        check("saver-only on: another app or an input in front gets none", !AdhanPlan.mayPlay(true, AdhanPlan.OTHER));
        check("saver-only on: when it cannot be known, it plays (a missed adhan is worse)", AdhanPlan.mayPlay(true, AdhanPlan.UNKNOWN));
    }
}
