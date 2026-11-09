package sa.urtv.home;

/**
 * 4.3: a TV that got the home app by Downloader and only opened it keeps the stock home on the Home key, and nothing said so. While we are not the home the TV goes to,
 * the top of the home shows one card that opens «إعداد تلفزيوني»; «لاحقاً» hides it for 3 days. Never once we are the home, never in kids mode. No Android in here.
 */
final class HomeNudgeRule {
    private HomeNudgeRule() { }

    static final long LATER_MS = 3 * 24 * 3600_000L;

    /** is the card shown: we are not the Home, not kids mode, and no «لاحقاً» is still in force (a clock gone back past the end by more than the 3 days counts as over) */
    static boolean show(boolean isDefaultHome, boolean kids, long laterUntil, long now) {
        if (isDefaultHome || kids) return false;
        return laterUntil <= 0 || now >= laterUntil || laterUntil - now > LATER_MS;
    }

    /** when «لاحقاً» ends if pressed now */
    static long laterUntil(long now) { return now + LATER_MS; }
}
