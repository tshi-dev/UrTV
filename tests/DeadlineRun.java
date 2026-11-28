package sa.urtv.home.pair;

import java.io.IOException;

/** For tests/pair_deadline_test.py: Deadline's rules, one "name<TAB>got" line per check. */
public final class DeadlineRun {
    public static void main(String[] a) throws Exception {
        System.out.println("fast\t" + Deadline.call(() -> "ok", 2000, null));
        boolean[] late = {false};
        long t0 = System.currentTimeMillis();
        String got;
        try { Deadline.call(() -> { Thread.sleep(60_000); return "never"; }, 200, () -> late[0] = true); got = "returned"; }
        catch (IOException e) { got = e.getMessage(); }
        System.out.println("stalled\t" + got + " " + late[0] + " " + (System.currentTimeMillis() - t0 < 5000));
        try { Deadline.call(() -> { throw new IOException("closed"); }, 2000, null); got = "none"; }
        catch (IOException e) { got = e.getMessage(); }
        System.out.println("io error passes\t" + got);
    }
}
