package sa.urtv.screensaver;

/** tests/saver_kids_keep_test.py: KeptAnswer on a plain JVM (4.4 review F3). */
public class KeptAnswerRun {
    static int failed = 0;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        check("no answer: nothing is written, whatever was kept (kids on stays on)", KeptAnswer.toStore(null, true) == null && KeptAnswer.toStore(null, false) == null);
        check("the same answer as kept: nothing is written (no broadcast)", KeptAnswer.toStore(true, true) == null && KeptAnswer.toStore(false, false) == null);
        check("a real answer that differs replaces it, both ways", Boolean.TRUE.equals(KeptAnswer.toStore(true, false)) && Boolean.FALSE.equals(KeptAnswer.toStore(false, true)));
        System.exit(failed == 0 ? 0 : 1);
    }
}
