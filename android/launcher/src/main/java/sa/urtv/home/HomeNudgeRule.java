package sa.urtv.home;

/**
 * 4.3: a TV that got the home app by Downloader and only opened it keeps the stock home on the Home key, and nothing said so. While we are not the home the TV goes to,
 * the top of the home shows one card that opens «إعداد تلفزيوني»; «لاحقاً» hides it for 7 days, then 30, then for good. Never once we are the home, never in kids mode. No Android in here.
 */
final class HomeNudgeRule {
    private HomeNudgeRule() { }

    /** «لاحقاً» hides the card for 7 days, then for 30 after a second press, then for good (the settings' row «إعداد تلفزيوني» stays the way in) */
    static final long FIRST_MS = 7 * 24 * 3600_000L, SECOND_MS = 30 * 24 * 3600_000L, NEVER = Long.MAX_VALUE;

    /** is the card shown: we are not the Home, not kids mode, and no «لاحقاً» is still in force (a clock gone back past the end by more than the longest wait counts as over) */
    static boolean show(boolean isDefaultHome, boolean kids, long laterUntil, long now) {
        if (isDefaultHome || kids || laterUntil == NEVER) return false;
        return laterUntil <= 0 || now >= laterUntil || laterUntil - now > SECOND_MS;
    }

    /** when «لاحقاً» ends if pressed now, as the {@code presses}-th time (0 = the first): 7 days, then 30, then never */
    static long laterUntil(long now, int presses) { return presses <= 0 ? now + FIRST_MS : presses == 1 ? now + SECOND_MS : NEVER; }

    /** the first press */
    static long laterUntil(long now) { return laterUntil(now, 0); }
}
