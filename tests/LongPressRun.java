package sa.urtv.home;

/** For tests/long_press_test.py: LongPress (a long press by its repeats or by its hold time on the release). Prints PASS/FAIL lines. */
public final class LongPressRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("no repeats, held 500 ms or more: the release is the long press", LongPress.onRelease(10_600, 10_000, false) && LongPress.onRelease(10_500, 10_000, false));
        check("a short press stays a press", !LongPress.onRelease(10_300, 10_000, false));
        check("a long press a repeat already fired does not act again on its release", !LongPress.onRelease(11_000, 10_000, true));
        check("the same length as the home's other long presses", LongPress.LONG_MS == 500);
        System.out.println(failed == 0 ? "ALL PASS" : failed + " FAILED");
    }
}
