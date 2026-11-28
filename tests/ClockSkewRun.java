package sa.urtv.screensaver;

/** For tests/clock_skew_test.py: each argument «tvMs|Date header» printed as «parsed judge», one per line. */
public final class ClockSkewRun {
    public static void main(String[] args) {
        for (String a : args) {
            int bar = a.indexOf('|');
            long tv = Long.parseLong(a.substring(0, bar)), net = ClockSkew.parse(a.substring(bar + 1));
            System.out.println(net + " " + ClockSkew.judge(tv, net));
        }
    }
}
