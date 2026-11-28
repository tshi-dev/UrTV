package sa.urtv.home.setup;

public final class AsyncCountRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        final int[] ends = {0};
        Runnable end = new Runnable() { public void run() { ends[0]++; } };
        AsyncCount one = new AsyncCount(end);
        one.release();
        check("no task: onReceive's own release ends the broadcast, once", ends[0] == 1);
        one.release(); one.release();
        check("a release too many ends nothing again", ends[0] == 1);
        ends[0] = 0;
        AsyncCount two = new AsyncCount(end);
        two.hold(); two.hold();   // the factory restore and the recovery of one boot
        two.release();            // onReceive has started them
        check("two tasks still running: not ended", ends[0] == 0);
        two.release();
        check("one of two tasks done: not ended", ends[0] == 0);
        two.release();
        check("the last task ends the broadcast, once (F133: not three goAsync calls, one)", ends[0] == 1);
        ends[0] = 0;
        AsyncCount early = new AsyncCount(end);
        early.hold(); early.release(); early.release();   // the task ended before onReceive let go
        check("a task that ends before onReceive does: ended once, by onReceive's release", ends[0] == 1);
        AsyncCount none = new AsyncCount(null);
        none.hold(); none.release(); none.release();
        check("no PendingResult (goAsync gave none): nothing to finish, no crash", true);
        AsyncCount late = new AsyncCount(end);
        late.release();
        late.hold();
        late.release();
        check("a hold after the end is ignored (nothing ends twice)", ends[0] == 2 || ends[0] == 1);
        AsyncCount boom = new AsyncCount(new Runnable() { public void run() { throw new IllegalStateException("x"); } });
        boom.release();
        check("a finish that throws does not escape", true);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
