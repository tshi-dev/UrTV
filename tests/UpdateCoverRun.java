package sa.urtv.screensaver;

/** For tests/update_cover_test.py: CoverRule's decisions. One line per check: name TAB ok|FAIL. */
public final class UpdateCoverRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] a) {
        check("the home app replaced: Home, then close", CoverRule.onPackage("sa.urtv.home", "android.intent.action.PACKAGE_REPLACED") == CoverRule.HOME);
        check("the home app added (some TVs say added for a replace): Home", CoverRule.onPackage("sa.urtv.home", "android.intent.action.PACKAGE_ADDED") == CoverRule.HOME);
        check("another app replaced meanwhile: stay", CoverRule.onPackage("com.apple.atve.androidtv.appletv", "android.intent.action.PACKAGE_REPLACED") == CoverRule.STAY);
        check("no package: stay", CoverRule.onPackage(null, "android.intent.action.PACKAGE_REPLACED") == CoverRule.STAY);
        check("the home app removed: stay (the limit decides)", CoverRule.onPackage("sa.urtv.home", "android.intent.action.PACKAGE_REMOVED") == CoverRule.STAY);
        check("declined or failed (the home app says so): close only, the update screen is under it", CoverRule.onClose() == CoverRule.CLOSE);
        check("Back or the limit after the home app changed (its broadcast missed): Home", CoverRule.onLimit(true) == CoverRule.HOME);
        check("Back or the limit with the home app unchanged: close only", CoverRule.onLimit(false) == CoverRule.CLOSE);
    }
}
