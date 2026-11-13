package sa.urtv.home;

/** For tests/dock_retry_test.py: DockRetry's schedule. One line per check: name TAB ok|FAIL. */
public final class DockRetryRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] a) {
        check("all the chosen favourites are found: no rebuild", DockRetry.next(0, 8, 8) == -1 && DockRetry.next(2, 7, 9) == -1 && DockRetry.next(0, 0, 0) == -1);
        check("fewer found than saved (a cold start): rebuilt after 3 s, then 10 s, 30 s and 60 s", DockRetry.next(0, 8, 5) == 3000 && DockRetry.next(1, 8, 5) == 10000 && DockRetry.next(2, 8, 6) == 30000 && DockRetry.next(3, 8, 7) == 60000);
        check("then it stops (an app really uninstalled never comes back)", DockRetry.next(4, 8, 5) == -1 && DockRetry.next(99, 8, 1) == -1);
        check("a negative attempt is the first", DockRetry.next(-1, 8, 5) == 3000);
        System.exit(0);
    }
}
