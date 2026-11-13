package sa.urtv.home;

/**
 * 4.3.1: a TV whose city was never chosen still has the install's Riyadh, so its weather and prayer times are Riyadh's. When the TV's own time zone
 * is somewhere else, the home asks once, on the bar the nudge uses («اختر المدينة» / «لاحقاً»). «لاحقاً» waits like the nudge's (7 days, 30, then never).
 * Never in kids mode, never for a TV on Riyadh's zone (the default is right there). No Android in here.
 */
final class CityAskRule {
    private CityAskRule() { }

    static final String RIYADH = "Asia/Riyadh";

    /** is the question due: no city was ever chosen, the TV's zone is not Riyadh's, not kids mode, and no «لاحقاً» is still in force */
    static boolean show(boolean saverInstalled, boolean cityChosen, String tvZone, boolean kids, long laterUntil, long now) {
        if (!saverInstalled || cityChosen || kids || tvZone == null || tvZone.isEmpty() || RIYADH.equals(tvZone)) return false;
        return HomeNudgeRule.show(false, false, laterUntil, now);   // the same waits as the nudge's
    }
}
