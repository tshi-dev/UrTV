package sa.urtv.home.setup;

/**
 * How long «إعداد تلفزيوني» may keep the screensaver away while it waits for the TV's «السماح بتصحيح USB؟» (SaverHold). No Android in here:
 * tests/SaverHoldRun.java runs it. The hold ends when the answer comes (or the wait gives up), and on its own after {@link #MAX_MS} in any
 * case, so a run that was cut off can never keep the screensaver away for good.
 */
public final class SaverHoldRule {
    private SaverHoldRule() { }

    /** ms: the setup's wait for the answer (180 s, SetupRunner) and a minute more; past it the screensaver is free again */
    public static final long MAX_MS = 240_000;

    /** since: when the wait began (0 = no wait); now: the clock (the same one). A clock that went back counts as no wait. */
    public static boolean held(long since, long now) {
        return since > 0 && now >= since && now - since < MAX_MS;
    }
}
