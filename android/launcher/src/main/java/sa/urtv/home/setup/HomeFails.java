package sa.urtv.home.setup;

/**
 * 4.4 «اجعل UrTV الشاشة الرئيسية», not pushed: the home switch is tried only when the person asks (the setup's home step, or the settings' row that opens it). Where it
 * was tried and did not make UrTV the home TWICE on this TV (the in-app switch or the TV-side setup, home-fails.txt counts both: they are the same SetupEngine.home), the
 * in-app path stops offering it: the home step says so in one line, and the setup screen shows the way that works on such a TV, an AI helper on a computer with
 * docs/SETUP-PROMPT.md (a QR to it). A switch that worked starts the count again. No Android in here (tests/HomeFailsRun.java).
 */
final class HomeFails {
    private HomeFails() { }

    static final String FILE = "home-fails.txt";
    static final int LIMIT = 2;
    /** the short guide on the computer, the README's way in for an agent */
    static final String GUIDE_URL = "https://github.com/tshi-dev/UrTV/blob/master/docs/SETUP-PROMPT.md";
    static final String LINE = "جرّب الإعداد بمساعد ذكي من الكمبيوتر";
    static final String GUIDE = "على هذا التلفزيون ما نجح تبديل الشاشة الرئيسية من هنا مرتين. افتح على الكمبيوتر مساعداً ذكياً (مثل Claude Code) في مجلد UrTV وقل له: «اقرأ docs/SETUP-PROMPT.md وجهّز تلفزيوني». امسح الرمز لترى الدليل.";

    static int count(String text) {
        if (text == null) return 0;
        try { return Math.max(0, Integer.parseInt(text.trim().split("\\s+")[0])); } catch (RuntimeException e) { return 0; }
    }

    /** the in-app switch is not offered any more on this TV */
    static boolean givenUp(String text) { return count(text) >= LIMIT; }

    /** after a try of the switch: {@code attempted} (the switch really started: the journal said «switching»), {@code done} (UrTV is the home); the file's new text */
    static String after(String text, boolean attempted, boolean done) {
        if (done) return "0\n";
        if (!attempted) return text == null ? "" : text;   // nothing was tried (no stock home known, the TV did not say): not counted
        return (count(text) + 1) + "\n";
    }
}
