package sa.urtv.home;

/**
 * 4.3.5 review F2: which HOME activity is the TV's own home for «افتح الشاشة الأصلية»: never ours, never the setup wizard (setupwraith: it would
 * start the TV's setup again), never a Settings stand-in (com.android.settings, com.android.tv.settings, any FallbackHome). The same exclusions as
 * SetupEngine.otherHome. No Android in here (tests/crash_loop_test.py runs it).
 */
final class StockHome {
    private StockHome() { }

    static final String WRAITH = "com.google.android.tungsten.setupwraith";

    static boolean real(String pkg, String cls) {
        if (pkg == null || pkg.isEmpty() || pkg.startsWith("sa.urtv.")) return false;
        if (pkg.equals(WRAITH) || pkg.equals("com.android.settings") || pkg.equals("com.android.tv.settings")) return false;
        return cls == null || !cls.matches("([A-Za-z0-9_.]*\\.)?FallbackHome");
    }
}
