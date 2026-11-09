package sa.urtv.home;

public final class UpdateNoticeRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        check("a screensaver older than the home is offered an update; equal, newer or not installed (-1) is not", UpdateNoticeRule.older(204, 208) && !UpdateNoticeRule.older(208, 208) && !UpdateNoticeRule.older(209, 208) && !UpdateNoticeRule.older(-1, 208));
        check("an unknown home build (0) offers nothing", !UpdateNoticeRule.older(1, 0) && !UpdateNoticeRule.newer(5, 0));
        check("a newer build out is offered; the same, an older one or an unread answer (0) is not", UpdateNoticeRule.newer(209, 208) && !UpdateNoticeRule.newer(208, 208) && !UpdateNoticeRule.newer(207, 208) && !UpdateNoticeRule.newer(0, 208));
        long h = 3600_000L;
        check("the release is asked at most every 12 hours; never asked, or a clock gone back, is due", UpdateNoticeRule.due(0, 5 * h) && !UpdateNoticeRule.due(5 * h, 16 * h) && UpdateNoticeRule.due(5 * h, 17 * h) && UpdateNoticeRule.due(9 * h, 5 * h));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
