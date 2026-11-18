package sa.urtv.screensaver;

/** tests/saver_gate_test.py: StartGate's rules on a plain JVM (4.4 review F1). */
public class StartGateRun {
    static int failed = 0;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        StartGate g = new StartGate();
        int t1 = g.begin();
        boolean kept = g.changedWhileStarting();
        g.created(t1);
        check("a change while the start is on its way is kept, and handed back once when the page exists", kept && g.takeDirty() && !g.takeDirty());
        check("with the page there, a change is for the receiver to act on now", !g.changedWhileStarting() && !g.takeDirty());
        int t2 = g.begin(), t3 = g.begin();
        check("a start that a newer one replaced is not current (never two pages)", !g.current(t2) && g.current(t3));
        g.changedWhileStarting();
        g.created(t2);
        check("the late page of an old start does not clear the newer one's pending state", g.changedWhileStarting());
        g.created(t3);
        check("the newest start's page clears it", !g.changedWhileStarting());
        int t4 = g.begin();
        g.cancel();
        check("a window that went: no start may create a page and nothing stays pending", !g.current(t4) && !g.changedWhileStarting() && !g.takeDirty());
        System.exit(failed == 0 ? 0 : 1);
    }
}
