package sa.urtv.home;

/**
 * 4.3.5 review F2: which HOME activity is the TV's own home for «افتح الشاشة الأصلية»: never ours, never the setup wizard (setupwraith: it would
 * start the TV's setup again), never a Settings stand-in (com.android.settings, com.android.tv.settings, any FallbackHome). The same exclusions as
 * SetupEngine.otherHome. No Android in here (tests/crash_loop_test.py runs it).
 */
public final class StockHome {
    private StockHome() { }

    static final String WRAITH = "com.google.android.tungsten.setupwraith";

    static boolean real(String pkg, String cls) {
        if (pkg == null || pkg.isEmpty() || pkg.startsWith("sa.urtv.")) return false;
        if (pkg.equals(WRAITH) || pkg.equals("com.android.settings") || pkg.equals("com.android.tv.settings")) return false;
        return cls == null || !cls.matches("([A-Za-z0-9_.]*\\.)?FallbackHome");
    }

    /**
     * 4.4 (step 100, the owner's TCL G08 on Android 14): «افتح الشاشة الأصلية» did nothing where an earlier setup had turned Google TV's home off. The known stock
     * homes, Google's first: the package names are the facts of tvOS Launcher's list (github.com/techgenius1/tvOS-Launcher, Perms.java, Apache-2.0). Never the setup wizard.
     */
    static final String[] KNOWN = {
        "com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher", "com.google.android.leanbacklauncher",
        "org.droidtv.ui.home", "org.droidtv.home", "com.tpvision.home", "com.tcl.cyberui", "com.tcl.tv.launcher", "com.tcl.launcher", "com.falcon.launcher",
        "com.ffalcon.launcher", "com.hisense.tv.launcher", "com.mi.tv.launcher", "com.mitv.tvhome", "com.sony.dtv.homemenu", "com.skyworth.tvos.launcher",
        "com.coocaa.tv.launcher", "com.haier.tv.launcher", "com.homwee.launcher", "com.zeasn.whaleos.launcher", "com.sdmc.launcher", "com.amazon.tv.launcher",
        "com.amazon.firehomestarter",
    };

    /** the first known stock home that is installed but turned off (in KNOWN's order: Google's first); null when none */
    public static String disabledKnown(java.util.Set<String> installedButOff) {
        if (installedButOff == null) return null;
        for (String k : KNOWN) if (installedButOff.contains(k) && real(k, null)) return k;
        return null;
    }

    /** what the button does: open an enabled stock home; turn a known one back on first (our adb key); say the way by hand; nothing to offer */
    public static final int OPEN = 0, ENABLE = 1, MANUAL = 2, NONE = 3;

    /** a known stock home that is off comes first (another «home» answering may be a stand-in that opens nothing: step 100) */
    public static int decide(String disabledKnown, boolean enabledHome, boolean adbKey) {
        if (disabledKnown != null) return adbKey ? ENABLE : MANUAL;
        return enabledHome ? OPEN : NONE;
    }

    /** for the setup package (SetupHook.enableStock): a copy of KNOWN */
    public static String[] known() { return KNOWN.clone(); }
}
