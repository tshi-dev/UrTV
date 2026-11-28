package sa.urtv.screensaver;

/** For tests/cover_wait_test.py: each argument «live,waited,shownAt,now» printed as CoverWait.done's answer (1 or 0), one per line. */
public final class CoverWaitRun {
    public static void main(String[] args) {
        for (String a : args) {
            String[] f = a.split(",");
            System.out.println(CoverWait.done(f[0].equals("1"), Long.parseLong(f[1]), Long.parseLong(f[2]), Long.parseLong(f[3])) ? 1 : 0);
        }
    }
}
