package sa.urtv.screensaver;

/** For tests/update_cover_test.py: CoverRule's decisions. One line per check: name TAB ok|FAIL. */
public final class UpdateCoverRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] a) {
        check("the home app replaced: Home, and STAY until the new home says it is up", CoverRule.onPackage("sa.urtv.home", "android.intent.action.PACKAGE_REPLACED") == CoverRule.HOME_WAIT);
        check("the home app added (some TVs say added for a replace): Home, and stay", CoverRule.onPackage("sa.urtv.home", "android.intent.action.PACKAGE_ADDED") == CoverRule.HOME_WAIT);
        check("the wait for the new home is bounded (20 s)", CoverRule.HOME_WAIT_MS == 20_000 && CoverRule.HOME_WAIT != CoverRule.HOME && CoverRule.HOME_WAIT != CoverRule.CLOSE);
        check("another app replaced meanwhile: stay", CoverRule.onPackage("com.apple.atve.androidtv.appletv", "android.intent.action.PACKAGE_REPLACED") == CoverRule.STAY);
        check("no package: stay", CoverRule.onPackage(null, "android.intent.action.PACKAGE_REPLACED") == CoverRule.STAY);
        check("the home app removed: stay (the limit decides)", CoverRule.onPackage("sa.urtv.home", "android.intent.action.PACKAGE_REMOVED") == CoverRule.STAY);
        check("declined or failed (the home app says so): close only, the update screen is under it", CoverRule.onClose() == CoverRule.CLOSE);
        check("Back or the limit after the home app changed (its broadcast missed): Home", CoverRule.onLimit(true) == CoverRule.HOME);
        check("Back or the limit with the home app unchanged: close only", CoverRule.onLimit(false) == CoverRule.CLOSE);
    }
}
