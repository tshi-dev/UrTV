package sa.urtv.home;

public final class HomeNudgeRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        long d = 24 * 3600_000L, now = 100 * d;
        check("not the Home, no «لاحقاً»: the card shows", HomeNudgeRule.show(false, false, 0, now));
        check("once we are the Home it never shows, whatever was pressed", !HomeNudgeRule.show(true, false, 0, now) && !HomeNudgeRule.show(true, false, now - d, now));
        check("kids mode never shows it", !HomeNudgeRule.show(false, true, 0, now));
        check("«لاحقاً» the first time hides it for 7 days, not a minute more", !HomeNudgeRule.show(false, false, HomeNudgeRule.laterUntil(now, 0), now) && !HomeNudgeRule.show(false, false, HomeNudgeRule.laterUntil(now, 0), now + 7 * d - 1000)
            && HomeNudgeRule.show(false, false, HomeNudgeRule.laterUntil(now, 0), now + 7 * d));
        check("a second «لاحقاً» hides it for 30 days", !HomeNudgeRule.show(false, false, HomeNudgeRule.laterUntil(now, 1), now + 29 * d) && HomeNudgeRule.show(false, false, HomeNudgeRule.laterUntil(now, 1), now + 30 * d));
        check("a third «لاحقاً» hides it for good (the settings' row stays the way in)", HomeNudgeRule.laterUntil(now, 2) == HomeNudgeRule.NEVER && !HomeNudgeRule.show(false, false, HomeNudgeRule.NEVER, now + 5000 * d) && HomeNudgeRule.laterUntil(now, 9) == HomeNudgeRule.NEVER);
        check("the one-argument form is the first press", HomeNudgeRule.laterUntil(now) == HomeNudgeRule.laterUntil(now, 0));
        check("a clock set far back does not hide it for ever", HomeNudgeRule.show(false, false, HomeNudgeRule.laterUntil(now, 0), now - 30 * d) && HomeNudgeRule.show(false, false, HomeNudgeRule.laterUntil(now, 1), now - 60 * d));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
