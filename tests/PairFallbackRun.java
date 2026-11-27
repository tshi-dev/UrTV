package sa.urtv.home.setup;

/** For tests/setup_cancel_test.py: the paired-route fallback keeps a Cancel as it was (F87). One line a check: name TAB ok|FAIL. */
public final class PairFallbackRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] a) {
        String cancel = AdbClient.CANCELLED;
        check("a Cancel is an answer: the paired route is not tried", PairFallback.answered(cancel) && PairFallback.answered("interrupted") && !PairFallback.answered("connection refused"));
        check("a Cancel's message is thrown exactly as it was, whatever the paired route said", PairFallback.message(cancel, "no_addon").equals(cancel) && PairFallback.message(cancel, "paired").equals(cancel));
        check("another failure gets the paired route's reason", PairFallback.message("connection refused", "not_paired").equals("connection refused; paired route: not_paired"));
        check("friendly() of a Cancel is the Cancel's own text (the run says «ضغطت إلغاء», with its retry)", SetupRunner.friendly(PairFallback.message(cancel, "no_addon")).equals(SetupRunner.CANCELLED_TEXT));
        check("even with a word added after it (an older message) friendly() still says it is a Cancel", SetupRunner.friendly(cancel + "; paired route: no_addon").equals(SetupRunner.CANCELLED_TEXT));
        check("the retry follows: the End of a Cancel is the one with retry", new SetupRunner.End(SetupRunner.friendly(PairFallback.message(cancel, "no_port")), true).retry);
        check("a prompt left unanswered is still its own text with the paired word added", SetupRunner.friendly(PairFallback.message("the permission was not accepted", "not_paired")).equals(SetupRunner.NOT_ACCEPTED_TEXT));
        check("a TV with debugging off still says so with the paired word added", SetupRunner.friendly(PairFallback.message("debugging is off on this TV", "no_addon")).startsWith("تصحيح USB مطفأ"));
    }
}
