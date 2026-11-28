package sa.urtv.screensaver;

/** For tests/dream_hold_test.py: DreamHold.java on a plain JVM. Prints PASS/FAIL lines. */
public final class DreamHoldRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }
    public static void main(String[] a) {
        check("«not held» answered: the page starts", DreamHold.decide(true, false, true, false) == DreamHold.START);
        check("«held» answered before the start: the dream ends, nothing shown", DreamHold.decide(true, false, true, true) == DreamHold.END);
        check("no answer in time: the page starts anyway (the screensaver as always)", DreamHold.decide(true, false, false, false) == DreamHold.START);
        check("«held» answered late, after the start: the dream still ends (the debugging prompt is never covered)", DreamHold.decide(true, true, true, true) == DreamHold.END);
        check("«not held» answered after the wait started the page: nothing more (one start)", DreamHold.decide(true, true, true, false) == DreamHold.NOTHING);
        check("the dream is gone: nothing", DreamHold.decide(false, false, true, true) == DreamHold.NOTHING && DreamHold.decide(false, false, false, false) == DreamHold.NOTHING);
        check("the wait is short (at most 2 s on the dark ground)", DreamHold.WAIT_MS > 0 && DreamHold.WAIT_MS <= 2000);
        System.exit(failed == 0 ? 0 : 1);
    }
}
