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

    /** A Latin version inside an Arabic line keeps its own direction. */
    private static String ltr(String s) { return "⁦" + s + "⁩"; }

    public static String installed(String version) { return "الإصدار المثبّت " + ltr(version); }

    public static String upToDate(String version) { return "تلفزيونك محدّث (" + ltr(version) + ")"; }

    public static String available(String version) { return "تحديث جديد " + ltr(version) + " · اضغط للتحديث"; }

    public static String done(String version) { return "تم التحديث ✓ " + ltr(version); }

    public static String failedStep(String stepLabel) { return "الخطوة: " + stepLabel; }

    /** «4.0 (بناء 151)»: the marketing name (the VERSION file) and the build number, the way the apps name their versions. */
    public static String versionOf(String marketing, int build) { return marketing + " (بناء " + build + ")"; }
}
