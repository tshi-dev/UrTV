package sa.urtv.home.setup;

/** For tests/saver_hold_test.py: SaverHoldRule (how long «إعداد تلفزيوني» keeps the screensaver away while the debugging prompt waits). Prints PASS/FAIL lines. */
public final class SaverHoldRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        long t = 1_000_000;
        check("no wait: the screensaver is free", !SaverHoldRule.held(0, t));
        check("the wait began now: held", SaverHoldRule.held(t, t));
        check("still held at the end of the setup's 180 s wait", SaverHoldRule.held(t, t + 180_000));
        check("still held a minute later (the end comes with the answer or the give-up)", SaverHoldRule.held(t, t + SaverHoldRule.MAX_MS - 1));
        check("free by itself after MAX_MS, whatever happened to the run", !SaverHoldRule.held(t, t + SaverHoldRule.MAX_MS));
        check("free long after", !SaverHoldRule.held(t, t + 3_600_000));
        check("a clock that went back counts as no wait", !SaverHoldRule.held(t, t - 1));
        check("MAX_MS is the 180 s wait and a minute", SaverHoldRule.MAX_MS == 240_000);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
