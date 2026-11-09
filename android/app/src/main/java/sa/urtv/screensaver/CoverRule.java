package sa.urtv.screensaver;

/** UpdateCover's decisions, plain Java (tests/UpdateCoverRun.java): STAY, HOME (press the TV's Home, then close) or CLOSE (close only). */
final class CoverRule {
    private CoverRule() { }

    static final int STAY = 0, HOME = 1, CLOSE = 2;
    static final String HOME_PKG = "sa.urtv.home";

    /** a package broadcast: only the home app's own replace (or add) sends the TV Home */
    static int onPackage(String pkg, String action) {
        if (!HOME_PKG.equals(pkg)) return STAY;
        return "android.intent.action.PACKAGE_REPLACED".equals(action) || "android.intent.action.PACKAGE_ADDED".equals(action) ? HOME : STAY;
    }

    /** the home app said the install was declined or failed: its update screen is under this one and says why */
    static int onClose() { return CLOSE; }

    /** Back, or the time limit: Home when the home app changed since the cover opened (its broadcast was missed), else just close */
    static int onLimit(boolean homeChanged) { return homeChanged ? HOME : CLOSE; }
}
