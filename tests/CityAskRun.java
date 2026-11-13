package sa.urtv.home;

public final class CityAskRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        long d = 24 * 3600_000L, now = 100 * d;
        String berlin = "Europe/Berlin";
        check("city never chosen, the TV on another zone: the question is due", CityAskRule.show(true, false, berlin, false, 0, now));
        check("a city was chosen once: never asked", !CityAskRule.show(true, true, berlin, false, 0, now));
        check("the TV on Riyadh's zone: the default is right, not asked", !CityAskRule.show(true, false, "Asia/Riyadh", false, 0, now));
        check("kids mode, no screensaver app, no zone: not asked", !CityAskRule.show(true, false, berlin, true, 0, now) && !CityAskRule.show(false, false, berlin, false, 0, now)
            && !CityAskRule.show(true, false, null, false, 0, now) && !CityAskRule.show(true, false, "", false, 0, now));
        check("«لاحقاً» waits 7 days, then 30, then never (the nudge's waits)", !CityAskRule.show(true, false, berlin, false, HomeNudgeRule.laterUntil(now, 0), now + 6 * d)
            && CityAskRule.show(true, false, berlin, false, HomeNudgeRule.laterUntil(now, 0), now + 7 * d)
            && !CityAskRule.show(true, false, berlin, false, HomeNudgeRule.laterUntil(now, 1), now + 29 * d)
            && !CityAskRule.show(true, false, berlin, false, HomeNudgeRule.NEVER, now + 5000 * d));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
