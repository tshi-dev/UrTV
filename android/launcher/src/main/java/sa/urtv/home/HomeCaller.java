package sa.urtv.home;

/**
 * 4.4 (R-412, a TCL on Android 11): inside the TV's settings one option sent the person back to our home, and nothing said who asked for HOME. Each HOME
 * start of our home now logs, in fixed words (tag UrTVHomeCaller), which kind of app started it (Activity.getReferrer): the system (the remote's Home key,
 * or Android after a configuration change), the TV's settings, a maker's app (TCL), our own apps, another app, or none said. A report with that line
 * tells what fires HOME there; nothing is changed by it. No Android in here (tests/HomeCallerRun.java).
 */
final class HomeCaller {
    private HomeCaller() { }

    /** the log's word for the referrer's package (null: Android did not say); never an app the person installed by name */
    static String word(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "none said";
        if (pkg.equals("android") || pkg.equals("com.android.systemui")) return "system";
        if (pkg.equals("com.android.tv.settings") || pkg.equals("com.android.settings") || pkg.endsWith(".settings") || pkg.contains(".settings.")) return "the TV's settings";
        if (pkg.startsWith("sa.urtv.")) return "ours";
        if (pkg.startsWith("com.tcl.") || pkg.startsWith("com.mi.") || pkg.startsWith("com.xiaomi.") || pkg.startsWith("com.skyworth.") || pkg.startsWith("com.changhong."))
            return "a maker's app";
        return "another app";
    }
}
