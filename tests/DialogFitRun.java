package sa.urtv.screensaver;

/** For tests/dialog_fit_test.py: DialogFit.java (the settings' lists with «حفظ» on a TV screen) on a plain JVM. */
public final class DialogFitRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        // the TVs we see: 1080p at density 2 (960x540 dp, most Google TVs), 1080p at 1.5 and 1, 720p at 1 and tvdpi, 4K at 4, 540p at 2
        int[][] screens = {{1080, 200}, {1080, 150}, {1080, 100}, {720, 100}, {720, 133}, {2160, 400}, {540, 200}};
        for (int[] s : screens) {
            int h = s[0];
            float d = s[1] / 100f;
            int max = DialogFit.maxHeight(h, d);
            String at = h + "px @" + d;
            check(at + ": the list leaves room for the title and the buttons (" + DialogFit.RESERVED_DP + " dp), or keeps two rows",
                    max + Math.round(DialogFit.RESERVED_DP * d) <= h || max == Math.round(DialogFit.MIN_DP * d), "max " + max);
            check(at + ": the list takes at most " + DialogFit.MAX_SHARE + " of the screen", max <= Math.round(h * DialogFit.MAX_SHARE), "max " + max);
            check(at + ": the list keeps at least two rows (" + DialogFit.MIN_DP + " dp)", max >= Math.round(DialogFit.MIN_DP * d), "max " + max);
            // «الدوريات كاملة»: 16 rows of a 64 dp TV list row (the TV's own list row; 48 dp on a phone) are always taller than the screen allows
            int leagues = Math.round(16 * 64 * d);
            check(at + ": «الدوريات كاملة» (16 rows) is capped, so «حفظ» stays on the screen",
                    DialogFit.listHeight(leagues, h, d) == max && max < leagues, "height " + DialogFit.listHeight(leagues, h, d));
            int events = Math.round(3 * 64 * d);
            if (h / d >= 360)   // a screen of at least 360 dp (every TV but the 540p one at density 2, 270 dp, a projector's)
                check(at + ": a short list («رياضات أخرى», 3 rows) stays as tall as its rows (-2)", DialogFit.listHeight(events, h, d) == -2,
                        "height " + DialogFit.listHeight(events, h, d));
            check(at + ": a list exactly at the cap is not changed, one pixel more is capped",
                    DialogFit.listHeight(max, h, d) == -2 && DialogFit.listHeight(max + 1, h, d) == max, "");
        }
        check("a density of 0 (unknown) counts as 1", DialogFit.maxHeight(1080, 0) == DialogFit.maxHeight(1080, 1), "");
        check("DOWN on the last row goes to «حفظ»", DialogFit.downToSave(15, 16) && DialogFit.downToSave(0, 1), "");
        check("DOWN on any other row is the list's own move (the next row)", !DialogFit.downToSave(0, 16) && !DialogFit.downToSave(14, 16), "");
        check("no row selected (-1, touch mode) or no rows: the list's own handling",
                !DialogFit.downToSave(-1, 16) && !DialogFit.downToSave(-1, 0) && !DialogFit.downToSave(0, 0), "");
        if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    }
}
