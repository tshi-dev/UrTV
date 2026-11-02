package sa.urtv.home.tilawah;

/** For tests/adhan_options_test.py: AdhanPlan (what the adhan's alarm does) and the adhan's length. One line per check: name TAB ok (or FAIL). */
public final class AdhanOptionsRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] args) {
        // ---- what is in front
        check("front: our home wins", AdhanPlan.front(true, 0) == AdhanPlan.HOME && AdhanPlan.front(true, 1) == AdhanPlan.HOME);
        check("front: the screensaver showing", AdhanPlan.front(false, 1) == AdhanPlan.DREAM);
        check("front: neither is another app or an input", AdhanPlan.front(false, 0) == AdhanPlan.OTHER);
        check("front: no answer from the screensaver app is not known", AdhanPlan.front(false, -1) == AdhanPlan.UNKNOWN);

        // ---- «الأذان على شاشة التوقف فقط»
        for (int f : new int[]{AdhanPlan.HOME, AdhanPlan.DREAM, AdhanPlan.OTHER, AdhanPlan.UNKNOWN})
            check("saver-only off: the adhan plays whatever is in front (" + f + ")", AdhanPlan.mayPlay(false, f));
        check("saver-only on: plays over our home", AdhanPlan.mayPlay(true, AdhanPlan.HOME));
        check("saver-only on: plays over the screensaver", AdhanPlan.mayPlay(true, AdhanPlan.DREAM));
        check("saver-only on: another app or an input in front gets none", !AdhanPlan.mayPlay(true, AdhanPlan.OTHER));
        check("note: says what the last alarm did", AdhanPlan.note(AdhanPlan.OTHER, false, true, false).equals("front=other play=0 notice=1 saver=0")
                && AdhanPlan.note(AdhanPlan.HOME, true, false, true).equals("front=home play=1 notice=0 saver=1")
                && AdhanPlan.note(AdhanPlan.UNKNOWN, true, false, false).equals("front=unknown play=1 notice=0 saver=0") && AdhanPlan.note(AdhanPlan.DREAM, true, true, false).equals("front=dream play=1 notice=1 saver=0"));

        // ---- «تكبير فقط»
        check("length: none is the whole adhan", AdhanVoices.length(false, false) == AdhanVoices.FULL && AdhanVoices.cutMs(AdhanVoices.FULL, 1) == 0);
        check("length: short is its first minute", AdhanVoices.length(false, true) == AdhanVoices.SHORT && AdhanVoices.cutMs(AdhanVoices.SHORT, 1) == 60_000 && AdhanVoices.fadeMs(AdhanVoices.SHORT) == 3_000);
        check("length: takbir wins over short", AdhanVoices.length(true, true) == AdhanVoices.TAKBIR && AdhanVoices.length(true, false) == AdhanVoices.TAKBIR);
        boolean same = true;
        for (int v = 0; v <= AdhanVoices.COUNT; v++) same &= AdhanVoices.cutMs(AdhanVoices.TAKBIR, v) == 14_000;
        check("takbir: cut at 14 s for every recording and the person's own, with a 2 s fade", same && AdhanVoices.fadeMs(AdhanVoices.TAKBIR) == 2_000);
        check("takbir: inside the 12 to 15 seconds asked, and the fade shorter than the cut", AdhanVoices.TAKBIR_MS >= 12_000 && AdhanVoices.TAKBIR_MS <= 15_000 && AdhanVoices.TAKBIR_FADE_MS < AdhanVoices.TAKBIR_MS);
        check("takbir: full volume for 12 seconds, half at 13, nothing at 14", AdhanVoices.gain(0, 14_000, 2_000) == 1f && AdhanVoices.gain(12_000, 14_000, 2_000) == 1f
                && Math.abs(AdhanVoices.gain(13_000, 14_000, 2_000) - 0.5f) < 0.001f && AdhanVoices.gain(14_000, 14_000, 2_000) == 0f && AdhanVoices.gain(20_000, 14_000, 2_000) == 0f);
        boolean falling = true; float last = 1f;
        for (long t = 0; t <= 15_000; t += 250) { float x = AdhanVoices.gain(t, 14_000, 2_000); if (x > last) falling = false; last = x; }
        check("takbir: the volume only goes down", falling);
        check("short is the same as before (shortGain is gain at 60 s / 3 s)", AdhanVoices.shortGain(58_500) == AdhanVoices.gain(58_500, 60_000, 3_000) && AdhanVoices.shortGain(57_000) == 1f && AdhanVoices.shortGain(60_000) == 0f);

        // ---- «شاشة التوقف وقت الأذان»
        check("saver-at off: never starts, whatever is in front", !AdhanPlan.startSaver(false, AdhanPlan.HOME) && !AdhanPlan.startSaver(false, AdhanPlan.OTHER));
        check("saver-at on: starts when our home is in front", AdhanPlan.startSaver(true, AdhanPlan.HOME));
        check("saver-at on: never over another app or an input", !AdhanPlan.startSaver(true, AdhanPlan.OTHER));
        check("saver-at on: not when it is showing already", !AdhanPlan.startSaver(true, AdhanPlan.DREAM));
        check("saver-at on: not when it cannot be known what is in front", !AdhanPlan.startSaver(true, AdhanPlan.UNKNOWN));

        // ---- «تنبيه عند الأذان»
        check("notice: the words name the prayer", AdhanNoticeText.text(3).equals("حان وقت صلاة العصر") && AdhanNoticeText.text(0).equals("حان وقت صلاة الفجر")
                && AdhanNoticeText.text(2).equals("حان وقت صلاة الظهر") && AdhanNoticeText.text(4).equals("حان وقت صلاة المغرب") && AdhanNoticeText.text(5).equals("حان وقت صلاة العشاء"));
        check("notice: no prayer, or the sunrise, is just «الصلاة»", AdhanNoticeText.text(-1).equals("حان وقت الصلاة") && AdhanNoticeText.text(1).equals("حان وقت الصلاة") && AdhanNoticeText.text(9).equals("حان وقت الصلاة"));
        check("notice: 5 seconds, the first toast shorter than that", AdhanNoticeText.SHOW_MS == 5_000 && AdhanNoticeText.FIRST_MS > 0 && AdhanNoticeText.FIRST_MS < AdhanNoticeText.SHOW_MS);
        check("saver-only on: when it cannot be known, it plays (a missed adhan is worse)", AdhanPlan.mayPlay(true, AdhanPlan.UNKNOWN));
    }
}
