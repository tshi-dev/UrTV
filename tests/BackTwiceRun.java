package sa.urtv.home;

/** tests/back_twice_test.py: HomeBack.twice on a JVM; one line per case, "ok" or "bad" */
public class BackTwiceRun {
    static void c(String name, boolean got, boolean want) { System.out.println((got == want ? "ok " : "bad ") + name); }
    public static void main(String[] a) {
        c("second Back within a second", HomeBack.twice(true, 1500, 800, true, false, false, true), true);
        c("exactly a second", HomeBack.twice(true, 1800, 800, true, false, false, true), true);
        c("too slow", HomeBack.twice(true, 1801, 800, true, false, false, true), false);
        c("a single Back (no earlier one)", HomeBack.twice(true, 1500, 0, true, false, false, true), false);
        c("the setting off", HomeBack.twice(false, 1500, 800, true, false, false, true), false);
        c("kids mode", HomeBack.twice(true, 1500, 800, true, false, true, true), false);
        c("not the default home", HomeBack.twice(true, 1500, 800, false, false, false, true), false);
        c("the grid scrolled", HomeBack.twice(true, 1500, 800, true, true, false, true), false);
        c("no screensaver app", HomeBack.twice(true, 1500, 800, true, false, false, false), false);
        c("a clock that went back", HomeBack.twice(true, 700, 800, true, false, false, true), false);
    }
}
