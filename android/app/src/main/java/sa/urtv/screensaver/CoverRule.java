package sa.urtv.screensaver;

/** UpdateCover's decisions, plain Java (tests/UpdateCoverRun.java): STAY, HOME (press the TV's Home, then close), CLOSE (close only) or HOME_WAIT (press Home, stay until the new home is up). */
final class CoverRule {
    private CoverRule() { }

    static final int STAY = 0, HOME = 1, CLOSE = 2, HOME_WAIT = 3;
    /** after the TV's Home was pressed for the new home, the cover waits this long for the home to say it is up (HOME_WAIT); then it closes anyway */
    static final long HOME_WAIT_MS = 20_000;
    static final String HOME_PKG = "sa.urtv.home";

    /**
     * a package broadcast: only the home app's own replace (or add) sends the TV Home, and the cover STAYS in front (HOME_WAIT): the new home's process takes some seconds to come up, and a cover that
     * closed at once left the app used before on the screen for about 4 s (the tester's step 44); the home's own «I am up» (onClose) is what closes it
     */
    static int onPackage(String pkg, String action) {
        if (!HOME_PKG.equals(pkg)) return STAY;
        return "android.intent.action.PACKAGE_REPLACED".equals(action) || "android.intent.action.PACKAGE_ADDED".equals(action) ? HOME_WAIT : STAY;
    }

    /** the home app said the install was declined or failed: its update screen is under this one and says why */
    static int onClose() { return CLOSE; }

    /** Back, or the time limit: Home when the home app changed since the cover opened (its broadcast was missed), else just close */
    static int onLimit(boolean homeChanged) { return homeChanged ? HOME : CLOSE; }
}
