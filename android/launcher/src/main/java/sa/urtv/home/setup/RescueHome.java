package sa.urtv.home.setup;

/**
 * 4.4 (R7, a user: «I deleted the app and the screen went black»): a TV whose stock home our setup turned off has no home at all once UrTV is removed (from the
 * TV's Settings, past our own warning). The rescue app («إنقاذ تلفزيونك», sa.urtv.rescue) carries a HOME activity-alias that is OFF in its manifest, so it never
 * competes with any home. It is armed (turned on) in the same adb batch that turns the stock home off, and read back before the stock home goes; it is disarmed
 * the moment the stock home is back (here, and by the rescue app itself when it sees one). With it armed and UrTV gone, the TV opens the rescue's small screen
 * by itself: «رجّع واجهة المصنع», the TV's settings with the way to turn the stock home on by hand, and the TV's apps. No broadcast or running process is needed.
 * Shared with the rescue app (android/rescue/build.gradle copies it). No Android in here (tests/RescueHomeRun.java).
 */
final class RescueHome {
    private RescueHome() { }

    static final String PKG = "sa.urtv.rescue";
    /** the alias's class: sa.urtv.home.setup.RescueHome inside the rescue app (android/rescue/src/main/AndroidManifest.xml) */
    static final String ALIAS_CLASS = "sa.urtv.home.setup.RescueHome";
    static final String ALIAS = PKG + "/" + ALIAS_CLASS;
    static final String ARM = "pm enable " + ALIAS;
    static final String DISARM = "pm disable " + ALIAS + " 2> /dev/null";
    /** the TV's enabled homes, one component a line (disabled packages and components are left out by Android) */
    static final String HOMES = "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2> /dev/null";

    /** HOME answerers that are not a stock home (the same as SettingsKey.NOT_A_STOCK_HOME, and Settings' FallbackHome) */
    static final String[] NOT_A_STOCK_HOME = {
        "com.google.android.tungsten.setupwraith", "com.android.tv.settings", "com.android.settings",
    };

    static final String NEED = "«عطّل واجهة المصنع» يحتاج تطبيق «إنقاذ تلفزيونك» مثبّتاً أولاً، حتى لا تبقى الشاشة سوداء لو حُذف UrTV يوماً. ثبّته من «جهّز كل شي» ثم ارجع هنا. ما تغيّر شيء.";
    static final String NOT_ARMED = "ما قبل التلفزيون تجهيز «إنقاذ تلفزيونك» كشاشة احتياطية، فما عطّلنا واجهة المصنع. ما تغيّر شيء.";
    static final String NOT_NEEDED = "زر الرئيسية في هذا التلفزيون يفتح UrTV بدون تعطيل واجهة المصنع، فما نحتاج نعطّلها. تبقى شغّالة احتياطاً.";

    /** the rescue's alias answers Home now (the HOMES answer, read back after ARM) */
    static boolean armed(String homesText) {
        if (homesText == null) return false;
        for (String l : homesText.replace("\r", "").split("\n")) if (l.trim().startsWith(PKG + "/")) return true;
        return false;
    }

    /** the packages of the HOMES answer */
    static java.util.List<String> packages(String homesText) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (homesText == null) return out;
        for (String l : homesText.replace("\r", "").split("\n")) {
            String s = l.trim();
            if (s.matches("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+")) out.add(s.substring(0, s.indexOf('/')));
        }
        return out;
    }

    /** a stock home answers Home: a package that is not ours (sa.urtv.*: the home and the rescue), not the setup wizard, not a settings app's fallback */
    static boolean stockHomeOn(Iterable<String> homePackages) {
        if (homePackages == null) return false;
        outer:
        for (String p : homePackages) {
            if (p == null || p.isEmpty() || p.startsWith("sa.urtv.")) continue;
            for (String n : NOT_A_STOCK_HOME) if (n.equals(p)) continue outer;
            return true;
        }
        return false;
    }
}
