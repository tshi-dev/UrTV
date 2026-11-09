package sa.urtv.home;

public final class HomeNudgeRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        long d = 24 * 3600_000L, now = 100 * d;
        check("not the Home, no «لاحقاً»: the card shows", HomeNudgeRule.show(false, false, 0, now));
        check("once we are the Home it never shows, whatever was pressed", !HomeNudgeRule.show(true, false, 0, now) && !HomeNudgeRule.show(true, false, now - d, now));
        check("kids mode never shows it", !HomeNudgeRule.show(false, true, 0, now));
        check("«لاحقاً» hides it for 3 days, not a minute more", !HomeNudgeRule.show(false, false, HomeNudgeRule.laterUntil(now), now) && !HomeNudgeRule.show(false, false, HomeNudgeRule.laterUntil(now), now + 3 * d - 1)
            && HomeNudgeRule.show(false, false, HomeNudgeRule.laterUntil(now), now + 3 * d));
        check("a clock set far back does not hide it for ever", HomeNudgeRule.show(false, false, HomeNudgeRule.laterUntil(now), now - 30 * d));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
