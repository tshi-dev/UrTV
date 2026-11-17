package sa.urtv.home;

/** For tests/beta_notes_test.py: the rule of BetaNotesRule.what on a plain JVM. */
public final class BetaNotesRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        check("off: never anything, even after a crash", BetaNotesRule.what(false, "x.Y", 5, 9).isEmpty() && BetaNotesRule.what(false, null, 5, 9).isEmpty());
        check("beta, the first time on it (nothing seen): no question", BetaNotesRule.what(true, null, 0, 9).isEmpty());
        check("beta, the same build: no question", BetaNotesRule.what(true, null, 9, 9).isEmpty() && BetaNotesRule.what(true, "", 9, 9).isEmpty());
        check("beta, a newer build than the one seen: the one-time line", "update".equals(BetaNotesRule.what(true, null, 5, 9)));
        check("beta, never for an older build (no downgrade is shown as an update)", BetaNotesRule.what(true, null, 9, 5).isEmpty());
        check("beta, a crash: the crash question, even on the first time and even with a newer build", "crash".equals(BetaNotesRule.what(true, "java.lang.Boom", 0, 9)) && "crash".equals(BetaNotesRule.what(true, "x", 5, 9)));
        check("a crash name is cleaned to letters, digits, dots and underscores", Report.name("a b\nTV: me").equals("abTVme") && Report.name("java.lang.IllegalState_1").equals("java.lang.IllegalState_1"));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
