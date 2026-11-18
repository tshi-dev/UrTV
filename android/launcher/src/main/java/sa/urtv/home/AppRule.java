package sa.urtv.home;

/**
 * Why a launchable app stays off the home screen, or null: it shows. Pure (no Android), so tests/sideload_apps_test.py runs it.
 * Next to a Google home (it lists only TV apps) an app with only an ordinary launcher activity stays out when it came with the
 * TV: those are mostly the maker's helpers. One the person put on the TV themselves (a sideloaded APK, issue #57: a phone app
 * with no TV activity) is not a system app, and comes in, in «كل التطبيقات» and as a favourite.
 */
final class AppRule {
    private AppRule() {}

    static final String HIDDEN = "hidden with dock.sh hide or the × of «تعديل التطبيقات»";
    static final String CONFIG = "hidden in config.xml";
    static final String HOME = "a home screen";
    static final String NO_TV = "no TV activity, and the TV's own home lists only TV apps";

    /**
     * @param launcherOnly found as an ordinary launcher activity (the app has no TV one)
     * @param system       came with the TV (FLAG_SYSTEM, or an update of such an app)
     */
    static String reason(boolean hidden, boolean shown, boolean config, boolean home, boolean usbViewer,
                         boolean googleHome, boolean launcherOnly, boolean system) {
        if (hidden) return HIDDEN;
        if (shown) return null;
        if (config) return CONFIG;
        if (home) return HOME;
        if (usbViewer) return null;   // 4.3 B8: the TV's viewer of a USB stick is always in the row (issue #50)
        if (googleHome && launcherOnly && system) return NO_TV;
        return null;   // #57: a sideloaded app comes in even with only a phone launcher activity
    }
}
