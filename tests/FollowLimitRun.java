package sa.urtv.screensaver;

/** tests/follow_limit_test.py: FollowLimit's decision on a plain JVM (4.4: 8 leagues and 20 clubs as before, one warning once, 24 and 40 at most). */
public class FollowLimitRun {
    static int failed = 0;
    static void check(String name, boolean ok, Object detail) { System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail)); if (!ok) failed++; }

    public static void main(String[] a) {
        String L = FollowLimit.LEAGUES, T = FollowLimit.TEAMS;
        check("up to the soft limit: added without a word (0 to 7 leagues, 0 to 19 clubs)", FollowLimit.check(L, 0, false) == FollowLimit.ADD && FollowLimit.check(L, 7, false) == FollowLimit.ADD
            && FollowLimit.check(T, 19, false) == FollowLimit.ADD, "");
        check("the 9th league and the 21st club: the warning, while it was never answered «أضف»", FollowLimit.check(L, 8, false) == FollowLimit.WARN && FollowLimit.check(T, 20, false) == FollowLimit.WARN, "");
        check("after «أضف»: no warning, up to the ceiling", FollowLimit.check(L, 8, true) == FollowLimit.ADD && FollowLimit.check(L, 23, true) == FollowLimit.ADD
            && FollowLimit.check(T, 39, true) == FollowLimit.ADD, "");
        check("the ceiling: 24 leagues, 40 clubs, warned or not", FollowLimit.check(L, 24, true) == FollowLimit.FULL && FollowLimit.check(L, 24, false) == FollowLimit.FULL
            && FollowLimit.check(T, 40, true) == FollowLimit.FULL && FollowLimit.check(T, 55, false) == FollowLimit.FULL, "");
        check("the owner's words, each kind its own number", FollowLimit.warning(L).equals("إضافة أكثر من ٨ قد تبطّئ التلفزيون إذا كان ضعيف")
            && FollowLimit.warning(T).equals("إضافة أكثر من ٢٠ قد تبطّئ التلفزيون إذا كان ضعيف") && FollowLimit.full(L).contains("٢٤") && FollowLimit.full(T).contains("٤٠"), FollowLimit.warning(T));
        check("the words the launcher's page reads", FollowLimit.word(FollowLimit.ADD).equals("add") && FollowLimit.word(FollowLimit.WARN).equals("warn") && FollowLimit.word(FollowLimit.FULL).equals("full"), "");
        System.exit(failed == 0 ? 0 : 1);
    }
}
