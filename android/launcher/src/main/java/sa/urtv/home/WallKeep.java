package sa.urtv.home;

/**
 * 4.4 beta 4 (Xiaomi A9, Skyworth: the return from an app to the home flickered or was blurred): on a tight TV the home let its photos go behind
 * another app (onTrimMemory), and the way back showed the small stand-in, blurred, until the photo was decoded again and faded in. Now the photo's
 * own bitmap is kept through the light trims and put back as it was before the first frame of the return is drawn: no blur, no fade. It is
 * let go, as before, when the system asks for more (MODERATE and worse: it is about to end processes). No Android in here (tests/WallKeepRun.java).
 */
final class WallKeep {
    private WallKeep() { }

    /** ComponentCallbacks2.TRIM_MEMORY_MODERATE */
    static final int MODERATE = 60;

    /** the shown photo's bitmap stays through a trim at this level */
    static boolean keep(int level) { return level < MODERATE; }

    /** the return can put the kept photo back: there is one, it is the photo the return shows (not due to move on), and it was not a scene's */
    static boolean reuse(boolean kept, boolean due, boolean scenes) { return kept && !due && !scenes; }
}
