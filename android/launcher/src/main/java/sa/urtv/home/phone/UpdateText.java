package sa.urtv.home.phone;

/** «تحديث تلفزيوني», every sentence the owner reads, in one place (the tests hold the exact words). No Android in here. */
public final class UpdateText {
    public static final String TITLE = "تحديث تلفزيوني";
    public static final String CHECK = "تحقق من التحديث";
    public static final String OFFLINE = "ما فيه اتصال بالإنترنت";
    public static final String REPO = "github.com/tshi-dev/UrTV";
    /** The three lines the owner is sent away with when the TV can't update itself; the address goes small under them. */
    public static final String AI_MESSAGE = "التحديث من التلفزيون ما يشتغل على هذا الجهاز.\nاطلب من مساعد الذكاء الاصطناعي على جهازك:\nحدّث تلفزيوني";
    public static final String NEED_PERMISSION = "لازم تسمح لتطبيق تلفزيونك بتثبيت التحديثات.";
    public static final String OPEN_PERMISSION = "فتح شاشة السماح";
    public static final String WORKING = "جارٍ التحقق…";
    public static final String DOWNLOADING = "جارٍ تنزيل التحديث والتحقق منه…";
    public static final String CONFIRM = "أكّد التثبيت على شاشة أندرويد بالريموت.";
    public static final String SHOW_CONFIRM = "اعرض شاشة التثبيت";
    public static final String NOT_DONE = "ما اكتمل التحديث";
    public static final String DECLINED = "ألغيت التثبيت.";

    private UpdateText() { }

    /** The version inside an Arabic line, isolated so its digits and dot keep together. */
    private static String isolate(String s) { return "⁧" + s + "⁩"; }

    /** The sentences with the version in them (and the failed step's): the Arabic templates, one %s each (Tr.format, as «اللغة» writes them). */
    public static final String INSTALLED = "الإصدار المثبّت %s", UP_TO_DATE = "تلفزيونك محدّث: %s", AVAILABLE = "تحديث جديد %s · اضغط للتحديث",
            DONE = "تم التحديث ✓ %s", FAILED_STEP = "الخطوة: %s";

    /**
     * How a screen says a sentence (docs/ENGLISH.md): the Arabic itself by default; UpdateActivity passes the home's S.tv, which with «اللغة»
     * English gives the table's words (i18n/en/home.tsv). Plain Java: Tr does the filling in, byte for byte the old concatenation.
     */
    public interface Say { String t(String ar, Object... args); }
    public static final Say ARABIC = (ar, args) -> sa.urtv.home.Tr.t(sa.urtv.home.Tr.AR, null, ar, args);

    public static String installed(String version) { return installed(version, ARABIC); }
    public static String installed(String version, Say say) { return say.t(INSTALLED, isolate(version)); }

    public static String upToDate(String version) { return upToDate(version, ARABIC); }
    public static String upToDate(String version, Say say) { return say.t(UP_TO_DATE, isolate(version)); }

    public static String available(String version) { return available(version, ARABIC); }
    public static String available(String version, Say say) { return say.t(AVAILABLE, isolate(version)); }

    public static String done(String version) { return done(version, ARABIC); }
    public static String done(String version, Say say) { return say.t(DONE, isolate(version)); }

    public static String failedStep(String stepLabel) { return failedStep(stepLabel, ARABIC); }
    public static String failedStep(String stepLabel, Say say) { return say.t(FAILED_STEP, stepLabel); }

    /** What a person reads: the release's name alone, «4.0» (the owner: «4.0, nothing else»). The build stays in the update status and the reports. */
    public static String versionOf(String marketing, int build) { return marketing; }
}
