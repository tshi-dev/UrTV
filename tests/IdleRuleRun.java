package sa.urtv.screensaver;

/** For tests/idle_rule_test.py: IdleRule (what the TV's idle delay means for «بدء شاشة التوقف بعد»). Prints PASS/FAIL lines. */
public final class IdleRuleRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("a user's TV: 2147483647 (Integer.MAX) is never, not 2147483 seconds", IdleRule.kind(2147483647L) == IdleRule.NEVER);
        check("Integer.MAX_VALUE itself", IdleRule.kind(Integer.MAX_VALUE) == IdleRule.NEVER);
        check("a day or more is never too", IdleRule.kind(24L * 3600_000) == IdleRule.NEVER && IdleRule.kind(Long.MAX_VALUE) == IdleRule.NEVER);
        check("30 minutes stays minutes", IdleRule.kind(1_800_000) == IdleRule.MINUTES);
        check("two hours stays minutes (under a day)", IdleRule.kind(7_200_000) == IdleRule.MINUTES);
        check("90 seconds stays seconds", IdleRule.kind(90_000) == IdleRule.SECONDS);
        check("unknown (-1 or 0) is the TV's own", IdleRule.kind(-1) == IdleRule.UNKNOWN && IdleRule.kind(0) == IdleRule.UNKNOWN);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
