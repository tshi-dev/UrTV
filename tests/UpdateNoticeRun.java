package sa.urtv.home;

public final class UpdateNoticeRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        check("a screensaver older than the home is offered an update; equal, newer or not installed (-1) is not", UpdateNoticeRule.older(204, 208) && !UpdateNoticeRule.older(208, 208) && !UpdateNoticeRule.older(209, 208) && !UpdateNoticeRule.older(-1, 208));
        check("an unknown home build (0) offers nothing", !UpdateNoticeRule.older(1, 0) && !UpdateNoticeRule.newer(5, 0));
        check("a newer build out is offered; the same, an older one or an unread answer (0) is not", UpdateNoticeRule.newer(209, 208) && !UpdateNoticeRule.newer(208, 208) && !UpdateNoticeRule.newer(207, 208) && !UpdateNoticeRule.newer(0, 208));
        long h = 3600_000L;
        check("4.4: the release is asked at most every 6 hours; never asked, or a clock gone back, is due", UpdateNoticeRule.due(0, 5 * h) && !UpdateNoticeRule.due(5 * h, 10 * h) && UpdateNoticeRule.due(5 * h, 11 * h) && UpdateNoticeRule.due(9 * h, 5 * h));
        int H = UpdateNoticeRule.HOME, S = UpdateNoticeRule.SAVER, E = UpdateNoticeRule.EXTRAS;
        check("4.4 behind: all at the release: nothing", UpdateNoticeRule.behind(300, 300, 300, 300) == 0 && UpdateNoticeRule.behind(300, 300, 0, 300) == 0);
        check("4.4 behind: a newer release: every installed app", UpdateNoticeRule.behind(300, 300, 300, 305) == (H | S | E) && UpdateNoticeRule.behind(300, 300, 0, 305) == (H | S));
        check("4.4 behind: the screensaver older than the home (Downloader brought the home alone), the release unknown", UpdateNoticeRule.behind(305, 300, 0, 0) == S);
        check("4.4 behind: the home older than the screensaver", UpdateNoticeRule.behind(300, 305, 0, 0) == H);
        check("4.4 behind: the add-on older than the others", UpdateNoticeRule.behind(305, 305, 300, 305) == E);
        check("4.4 behind: a missing screensaver (0) or add-on is never behind (the screensaver has «ثبّت الجزء الثاني»)", UpdateNoticeRule.behind(305, 0, 0, 305) == 0);
        check("R-288 behind: the add-on (516) older than the home and screensaver (525) but its file is absent at the release: nothing (the beta bug: the offer stayed on for ever)", UpdateNoticeRule.behind(525, 525, 516, 525, true) == 0 && UpdateNoticeRule.behind(525, 525, 516, 525, false) == E);
        check("R-288 behind: the add-on's file absent does not hide a real update: the home and screensaver behind a newer release still are", UpdateNoticeRule.behind(525, 525, 516, 530, true) == (H | S));
        long d = 24 * h;
        check("4.4 card: something behind and never shown: shown", UpdateNoticeRule.cardDue(S, 305, 0, 0, -1, 10 * d));
        check("4.4 card: shown for this build: not again for 3 days", !UpdateNoticeRule.cardDue(S, 305, 305, 10 * d, -1, 12 * d) && UpdateNoticeRule.cardDue(S, 305, 305, 10 * d, -1, 13 * d));
        check("4.4 card: a newer build comes out: shown again at once", UpdateNoticeRule.cardDue(H | S, 310, 305, 10 * d, -1, 10 * d + h));
        check("4.4 card: it stays up for the visit that shows it", UpdateNoticeRule.cardDue(S, 305, 305, 10 * d, 305, 10 * d + 1));
        check("4.4 card: nothing behind: never", !UpdateNoticeRule.cardDue(0, 305, 0, 0, -1, 10 * d));
        check("4.4 card: a clock gone back counts as old", UpdateNoticeRule.cardDue(S, 305, 305, 10 * d, -1, 5 * d));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
