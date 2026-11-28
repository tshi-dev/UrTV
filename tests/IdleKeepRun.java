package sa.urtv.screensaver;

/** For tests/idle_keep_test.py: IdleRule.reapplyMs (R-400: the idle delay put back at boot when the firmware reset it to «never»). */
public final class IdleKeepRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        String[] ours = {"1", "2", "5", "10", "15", "30"};
        check("a Skyworth at boot: kept 10, the TV says never (Integer.MAX): 10 minutes back", IdleRule.reapplyMs("10", 2147483647L, ours) == 600_000L);
        check("kept 30, a day or more is never too", IdleRule.reapplyMs("30", 24L * 3600_000, ours) == 1_800_000L);
        check("the TV still has a delay (5 min): left alone, even if another was kept", IdleRule.reapplyMs("10", 300_000, ours) == 0);
        check("the person set 90 s in the TV's settings: left alone", IdleRule.reapplyMs("10", 90_000, ours) == 0);
        check("the TV does not say (-1): left alone", IdleRule.reapplyMs("10", -1, ours) == 0);
        check("nothing kept: nothing written", IdleRule.reapplyMs(null, 2147483647L, ours) == 0);
        check("a kept value that is not ours: nothing written", IdleRule.reapplyMs("7", 2147483647L, ours) == 0 && IdleRule.reapplyMs("", 2147483647L, ours) == 0);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
