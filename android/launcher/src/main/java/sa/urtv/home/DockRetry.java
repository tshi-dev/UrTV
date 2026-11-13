package sa.urtv.home;

/**
 * Hotfix 4.3 (two users' reports, 4.2 and 4.3: after the TV is turned off and on the favourites row shows only FIVE apps, though all were set; it comes back after the settings are opened): right
 * after a cold start the system's list of launchable apps may not hold every app yet (the package scan is still going, an app on another volume is not mounted), so showApps() finds fewer of the
 * chosen favourites than were saved and tops the row up to the minimum (five). The saved list is never changed; the row is built again a few seconds later, and again, while apps are missing: 3 s,
 * 10 s, 30 s, 60 s, then it stops (an app that was really uninstalled never comes back). No Android in here: tests/dock_retry_test.py runs it.
 */
final class DockRetry {
    private DockRetry() { }

    static final long[] WAITS = {3_000, 10_000, 30_000, 60_000};

    /** the wait before the next rebuild, or -1: attempt = rebuilds already made since the start; wanted = favourites saved; found = of them in the list of launchable apps now */
    static long next(int attempt, int wanted, int found) { return found >= wanted || attempt >= WAITS.length ? -1 : WAITS[Math.max(0, attempt)]; }
}
