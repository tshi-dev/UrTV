package sa.urtv.home.setup;

/**
 * The automatic restore of «تخلّص من واجهة المصنع»: while the stock interface is off, two failed boots of OUR home in a row give the factory interface back (the same one-press restore,
 * SetupEngine.restoreFactory). A boot FAILED when our home did not come up in it (it never reached the screen: BootGuard.homeUp is written by MainActivity) and our process crashed in it
 * at least once (ApplicationExitInfo, Android 11 and newer; older TVs cannot say, and a boot with no home counts). A boot where the home came up, or where the TV started in an HDMI
 * input with no crash, does not count and resets the count. The rules are plain Java (tests/BootGuardRun.java); the file and the receiver are SetupBootReceiver and FileStore.
 */
final class BootGuard {
    private BootGuard() { }

    /** failed boots in a row before the restore */
    static final int LIMIT = 2;
    static final String FILE = "boot-guard.txt";

    /** what the file keeps: failed boots in a row, whether our home came up since the last boot, and when that boot was (epoch ms) */
    static final class State {
        final int failed;
        final boolean up;
        final long bootAt;
        State(int failed, boolean up, long bootAt) { this.failed = failed; this.up = up; this.bootAt = bootAt; }

        String text() { return "failed=" + failed + "\nup=" + (up ? 1 : 0) + "\nat=" + bootAt + "\n"; }

        /** a missing or damaged file is a clean state (nothing counted, the home counted as up: no false alarm after switching the step on) */
        static State parse(String t) {
            if (t == null) return new State(0, true, 0);
            int f = 0;
            boolean up = true;
            long at = 0;
            try {
                for (String l : t.split("\n")) {
                    if (l.startsWith("failed=")) f = Math.max(0, Integer.parseInt(l.substring(7).trim()));
                    else if (l.startsWith("up=")) up = !l.substring(3).trim().equals("0");
                    else if (l.startsWith("at=")) at = Long.parseLong(l.substring(3).trim());
                }
            } catch (NumberFormatException e) {
                return new State(0, true, 0);
            }
            return new State(f, up, at);
        }
    }

    static final class Decision {
        final State state;
        final boolean restore;
        Decision(State state, boolean restore) { this.state = state; this.restore = restore; }
    }

    /** the state when the step is switched on: nothing counted, the home counts as up */
    static State fresh(long now) { return new State(0, true, now); }

    /**
     * The TV has started. crashes: how many times our process crashed since the previous boot (-1: this Android cannot say). The boot that just ended failed when our home never came up in
     * it and (a crash was recorded, or none can be told). Two failures in a row restore; anything else resets the count. The new state says the home is not up yet in THIS boot.
     */
    static Decision onBoot(State prev, int crashes, long now) {
        boolean failedBoot = !prev.up && (crashes < 0 || crashes >= 1);
        int failed = failedBoot ? prev.failed + 1 : 0;
        return new Decision(new State(failed, false, now), failed >= LIMIT);
    }

    /** our home reached the screen: this boot is fine and the count starts again */
    static State homeUp(State s) { return new State(0, true, s.bootAt); }
}
