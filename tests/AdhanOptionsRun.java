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
        check("note: says what the last alarm did", AdhanPlan.note(AdhanPlan.OTHER, false, true).equals("front=other play=0 notice=1")
                && AdhanPlan.note(AdhanPlan.HOME, true, false).equals("front=home play=1 notice=0")
                && AdhanPlan.note(AdhanPlan.UNKNOWN, true, false).equals("front=unknown play=1 notice=0") && AdhanPlan.note(AdhanPlan.DREAM, true, true).equals("front=dream play=1 notice=1"));

        // ---- «تنبيه عند الأذان»
        check("notice: the words name the prayer", AdhanNoticeText.text(3).equals("حان وقت صلاة العصر") && AdhanNoticeText.text(0).equals("حان وقت صلاة الفجر")
                && AdhanNoticeText.text(2).equals("حان وقت صلاة الظهر") && AdhanNoticeText.text(4).equals("حان وقت صلاة المغرب") && AdhanNoticeText.text(5).equals("حان وقت صلاة العشاء"));
        check("notice: no prayer, or the sunrise, is just «الصلاة»", AdhanNoticeText.text(-1).equals("حان وقت الصلاة") && AdhanNoticeText.text(1).equals("حان وقت الصلاة") && AdhanNoticeText.text(9).equals("حان وقت الصلاة"));
        check("notice: 5 seconds, the first toast shorter than that", AdhanNoticeText.SHOW_MS == 5_000 && AdhanNoticeText.FIRST_MS > 0 && AdhanNoticeText.FIRST_MS < AdhanNoticeText.SHOW_MS);
        check("saver-only on: when it cannot be known, it plays (a missed adhan is worse)", AdhanPlan.mayPlay(true, AdhanPlan.UNKNOWN));
    }
}
