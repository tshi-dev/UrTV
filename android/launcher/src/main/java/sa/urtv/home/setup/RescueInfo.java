package sa.urtv.home.setup;

/**
 * 4.4 «قبل ما نبدأ: لو صار شيء»: what the setup screen shows before «جهّز كل شي», so a person with no computer and no AI helper can always get the TV back: the
 * rescue app «إنقاذ تلفزيونك» through Downloader (reached with the remote's Settings key even with no home), its Downloader code, and docs/RESCUE.md (a QR to it:
 * the same steps, and a phone-only way). The person is asked to take a photo of it. No Android in here.
 */
final class RescueInfo {
    private RescueInfo() { }

    static final String DOC_URL = "https://github.com/tshi-dev/UrTV/blob/master/docs/RESCUE.md";
    /** Downloader's short code for urtv-rescue.apk (the owner makes it; empty until then: the screen points to the guide instead) */
    static final String DOWNLOADER_CODE = "";

    static final String TITLE = "قبل ما نبدأ: لو صار شيء";
    static final String STEP1 = "1. اضغط زر الإعدادات في الريموت ← التطبيقات ← Downloader ← فتح.";
    static final String STEP2_CODE = "2. اكتب الكود %s وثبّت «إنقاذ تلفزيونك».";
    static final String STEP2_DOC = "2. ثبّت «إنقاذ تلفزيونك» برابطه في الدليل (امسح الرمز).";
    static final String STEP3 = "3. افتحه واضغط «رجّع التلفزيون كما كان».";
    static final String PHOTO = "صوّر هذه الشاشة بجوالك الحين، عشان تكون عندك لو احتجتها.";

    /** step 2 with the code when there is one */
    static boolean hasCode() { return DOWNLOADER_CODE.matches("[0-9]{4,10}"); }
}
