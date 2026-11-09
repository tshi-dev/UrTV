package sa.urtv.home.setup;

/** For tests/factory_test.py: BootGuard on a plain JVM. One line per scenario. */
public final class BootGuardRun {
    public static void main(String[] a) {
        BootGuard.State s = BootGuard.fresh(100);
        // first boot after switching on: the home counted as up, nothing counts
        BootGuard.Decision d = BootGuard.onBoot(s, 0, 200);
        System.out.println("b1 " + d.state.failed + " " + d.state.up + " " + d.restore);
        // that boot ended with no home and one crash: it fails (1), the next boot with the same: 2 = restore
        d = BootGuard.onBoot(d.state, 1, 300);
        System.out.println("b2 " + d.state.failed + " " + d.restore);
        d = BootGuard.onBoot(d.state, 2, 400);
        System.out.println("b3 " + d.state.failed + " " + d.restore);
        // a home that came up resets
        d = BootGuard.onBoot(d.state, 0, 500);
        BootGuard.State up = BootGuard.homeUp(d.state);
        d = BootGuard.onBoot(up, 5, 600);
        System.out.println("reset " + d.state.failed + " " + d.restore);
        // a boot with no home and no crash (the TV started in an HDMI input) does not count
        d = BootGuard.onBoot(new BootGuard.State(1, false, 1), 0, 700);
        System.out.println("hdmi " + d.state.failed + " " + d.restore);
        // an Android that cannot say crashes (-1): a boot with no home counts
        d = BootGuard.onBoot(new BootGuard.State(1, false, 1), -1, 800);
        System.out.println("unknown " + d.state.failed + " " + d.restore);
        // the file: round trip, and a damaged one is clean
        BootGuard.State r = BootGuard.State.parse(new BootGuard.State(1, false, 123).text());
        System.out.println("file " + r.failed + " " + r.up + " " + r.bootAt);
        BootGuard.State bad = BootGuard.State.parse("failed=x\nup=0\n");
        System.out.println("damaged " + bad.failed + " " + bad.up);
        System.out.println("none " + BootGuard.State.parse(null).up);
    }
}
