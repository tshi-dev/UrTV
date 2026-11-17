package sa.urtv.home;

/** For tests/key_taps_test.py: KeyApps.Taps (1×, 2×, 3× per remote button, 4.5). Prints PASS/FAIL lines. */
public final class KeyTapsRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        String yt = "com.google.android.youtube.tv", tvbro = "com.phlox.tvwebbrowser";
        KeyApps.Taps t = new KeyApps.Taps();
        check("only 1× set: opens at once, no wait (as before 4.5)", t.press("youtube", new String[]{yt, "", ""}, 1000) == KeyApps.OPEN && yt.equals(t.target) && !t.pending());
        check("nothing set: the button is the TV's", t.press("youtube", new String[]{"", "", ""}, 1000) == KeyApps.PASS);
        String[] two = {yt, KeyApps.SWITCHER, ""};
        check("1× and 2× set, one press: waits, then 1× when the window passes", t.press("youtube", two, 2000) == KeyApps.WAIT && t.deadline == 2000 + KeyApps.TAP_WINDOW_MS
            && t.timeout(2299) == KeyApps.PASS && t.timeout(2300) == KeyApps.OPEN && yt.equals(t.target) && !t.pending());
        check("two presses within the window: 2× at once (its highest), no wait for a third", t.press("youtube", two, 3000) == KeyApps.WAIT && t.press("youtube", two, 3200) == KeyApps.OPEN
            && KeyApps.SWITCHER.equals(t.target) && !t.pending());
        String[] three = {yt, "", tvbro};
        check("3× set but 2× empty: two presses fall back to 1×", t.press("star", three, 4000) == KeyApps.WAIT && t.press("star", three, 4200) == KeyApps.WAIT
            && t.timeout(4500) == KeyApps.OPEN && yt.equals(t.target));
        check("three presses: 3×", t.press("star", three, 5000) == KeyApps.WAIT && t.press("star", three, 5100) == KeyApps.WAIT && t.press("star", three, 5200) == KeyApps.OPEN && tvbro.equals(t.target));
        check("a second press after the window is a new first press", t.press("star", three, 6000) == KeyApps.WAIT && t.press("star", three, 6400) == KeyApps.WAIT
            && t.timeout(6700) == KeyApps.OPEN && yt.equals(t.target));
        String[] only2 = {"", KeyApps.CENTER, ""};
        check("only 2× set: one press does nothing but is eaten (not half a key to the app), two open the control centre",
            t.press("netflix", only2, 7000) == KeyApps.WAIT && t.timeout(7300) == KeyApps.EAT && t.press("netflix", only2, 8000) == KeyApps.WAIT && t.press("netflix", only2, 8100) == KeyApps.OPEN && KeyApps.CENTER.equals(t.target));
        check("targets: an app, @switcher, @center or empty; nothing else", KeyApps.validTarget(yt) && KeyApps.validTarget("@switcher") && KeyApps.validTarget("@center") && KeyApps.validTarget("")
            && !KeyApps.validTarget("@shell") && !KeyApps.validTarget("rm -rf"));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
