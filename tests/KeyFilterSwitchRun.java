package sa.urtv.home;

/** For tests/key_filter_switch_test.py (R-408). */
public final class KeyFilterSwitchRun {
    public static void main(String[] args) {
        int failed = 0;
        boolean[][] cases = {{true, true, true}, {true, false, false}, {false, true, false}, {false, false, false}};
        for (boolean[] c : cases) {
            boolean ok = KeyFilterSwitch.want(c[0], c[1]) == c[2];
            System.out.println((ok ? "PASS " : "FAIL ") + "switch " + (c[0] ? "on" : "off") + ", ours wants it " + c[1] + ": " + c[2]);
            if (!ok) failed++;
        }
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
