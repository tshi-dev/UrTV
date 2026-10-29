package sa.urtv.home.tilawah;

/**
 * What the adhan alarm was last set from. AdhanScheduler.schedule writes it, for every schedule, wherever it comes from (a prayer that
 * rang, a boot, an update, the home screen's check, the shell), so the home's per-minute check compares with what was REALLY scheduled:
 * an alarm set from the bundled place, while the screensaver could not be read, is recorded as "" and the first good read of the real
 * place differs from it and sets the alarm again. No Android in here: tests/PlaceMemoryRun.java.
 */
final class SchedIdentity {
    private SchedIdentity() { }

    static String of(boolean bundledPlace, String identity) { return bundledPlace ? "" : identity; }

    /** Must the alarm be set again for this (real) identity? */
    static boolean due(String recorded, String identity) { return !identity.equals(recorded); }
}
