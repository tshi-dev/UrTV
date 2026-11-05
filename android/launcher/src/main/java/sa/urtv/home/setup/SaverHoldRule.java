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

    /** ms: the update's install questions («اضغط للتحديث» to the last answer): five minutes, then the screensaver is free again (step 27) */
    public static final long UPDATE_MS = 300_000;

    /** since: when the wait began (0 = no wait); now: the clock (the same one). A clock that went back counts as no wait. */
    public static boolean held(long since, long now) {
        return held(since, now, MAX_MS);
    }

    /** the same for a hold of its own length */
    public static boolean held(long since, long now, long max) {
        return since > 0 && now >= since && now - since < max;
    }

    /** until: the wall-clock end kept on disk (0 = none); now: the wall clock; maxLen: the longest a hold may be. A clock far off counts as no wait. */
    public static boolean heldUntil(long until, long now, long maxLen) {
        return until > 0 && now < until && until - now <= maxLen;
    }
}
