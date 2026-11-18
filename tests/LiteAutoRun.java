package sa.urtv.home;

/** For tests/lite_auto_test.py (4.4 beta 3): LiteAuto's rule on a fake clock. One line per case. */
public final class LiteAutoRun {
    public static void main(String[] a) {
        long D = LiteAuto.DAY, t0 = 100 * D;
        String s = "";
        System.out.println("NONE " + LiteAuto.on(s, t0) + " " + LiteAuto.since(s, t0) + " [" + LiteAuto.reasonKind(s, t0) + "]");
        s = LiteAuto.add(s, LiteAuto.TRIM, t0);
        System.out.println("ONE " + LiteAuto.on(s, t0 + 1000));
        s = LiteAuto.add(s, LiteAuto.TRIM, t0 + 3 * D);
        System.out.println("TWO " + LiteAuto.on(s, t0 + 3 * D) + " since=" + (LiteAuto.since(s, t0 + 3 * D) == t0 + 3 * D) + " kind=" + LiteAuto.reasonKind(s, t0 + 3 * D)
            + " report=" + LiteAuto.report(s, t0 + 3 * D));
        System.out.println("HELD " + LiteAuto.on(s, t0 + 3 * D + 13 * D) + " " + LiteAuto.on(s, t0 + 3 * D + 15 * D));
        String far = LiteAuto.add(LiteAuto.add("", LiteAuto.SLOW, t0), LiteAuto.SLOW, t0 + 8 * D);
        System.out.println("FAR " + LiteAuto.on(far, t0 + 8 * D));
        String oom = LiteAuto.add(LiteAuto.add(LiteAuto.add("", LiteAuto.OOM, t0), LiteAuto.OOM, t0 + D), LiteAuto.TRIM, t0 + 2 * D);
        System.out.println("KIND " + LiteAuto.reasonKind(oom, t0 + 2 * D));
        String future = LiteAuto.add("", LiteAuto.TRIM, t0) + "trim " + (t0 + 50 * D) + "\n";
        System.out.println("FUTURE " + LiteAuto.on(future, t0 + D));
        String junk = "trim x\nhack 5\n\n" + "slow " + t0 + "\nslow " + (t0 + D) + "\n";
        System.out.println("JUNK " + LiteAuto.parse(junk).size() + " " + LiteAuto.on(junk, t0 + D));
        String many = "";
        for (int i = 0; i < 40; i++) many = LiteAuto.add(many, LiteAuto.TRIM, t0 + i * 1000);
        System.out.println("KEEP " + LiteAuto.parse(many).size());
        String old = LiteAuto.add("trim " + (t0 - 30 * D) + "\n", LiteAuto.SLOW, t0);
        System.out.println("OLD " + LiteAuto.parse(old).size());
    }
}
