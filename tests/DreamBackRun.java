package sa.urtv.home.tilawah;

/** For tests/dream_back_test.py: each argument «lostInDream,waiting,wanted,sinceLossMs» printed as FocusRule.afterDream (1 or 0), one per line. */
public final class DreamBackRun {
    public static void main(String[] args) {
        for (String a : args) {
            String[] f = a.split(",");
            System.out.println(FocusRule.afterDream(f[0].equals("1"), f[1].equals("1"), f[2].equals("1"), Long.parseLong(f[3])) ? 1 : 0);
        }
    }
}
