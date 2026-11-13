package sa.urtv.home;

/**
 * A Play Protect test build (never merged, never on apk-latest or apk-next): Gradle's -PppTest=A or B (android/launcher/build.gradle) drops
 * from the manifest A: the two accessibility services (SleepService, TypeService); B: those and REQUEST_INSTALL_PACKAGES /
 * REQUEST_DELETE_PACKAGES, to learn which one a Google TV Streamer's Play Protect blocks. What needs them says LINE instead. A normal build: "".
 */
public final class PpTest {
    private PpTest() { }

    public static final String VARIANT = BuildConfig.PP_TEST;
    public static final String LINE = S.n("غير متوفر في هذه النسخة التجريبية");   // S.tv says it in «اللغة»

    /** no accessibility service in this build (A and B) */
    public static boolean noA11y() { return !VARIANT.isEmpty(); }

    /** no install or uninstall of other apps in this build (B) */
    public static boolean noInstall() { return "B".equals(VARIANT); }
}
