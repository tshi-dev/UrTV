package sa.urtv.home.tilawah;

/**
 * The last good place and the rule for taking a new one. Only a real answer of the screensaver may become the remembered place, and only if
 * it was asked later than the newest answer already taken: a read that started earlier on another thread (the prayer times fetch) and ends
 * after a newer one must not put the older city back. A failed read, or an answer older than one already taken, gets the remembered place.
 * No Android in here (the preferences come through {@link Store}): tests/PlaceMemoryRun.java.
 */
final class PlaceMemory {
    interface Store { String load(); void save(String fragment); }

    private String lastGood;
    private boolean loaded;
    private long accepted = Long.MIN_VALUE;
    private boolean failing;

    /** @param fresh the screensaver's fragment, or null when it did not answer; @param ticket larger for a read started later */
    synchronized PlaceChoice.Pick resolve(Store store, String fresh, long ticket) {
        if (!loaded) { lastGood = store.load(); loaded = true; }
        if (PlaceFragment.parse(fresh) != null && ticket >= accepted) {
            accepted = ticket;
            if (!fresh.equals(lastGood)) { lastGood = fresh; store.save(fresh); }
            return new PlaceChoice.Pick(fresh, false);
        }
        return PlaceChoice.pick(null, lastGood);
    }

    /** True once at the start of a streak of failed reads that were answered with the remembered place (for the log). */
    synchronized boolean streakStarted(boolean readFailed, boolean usedRemembered) {
        boolean start = readFailed && usedRemembered && !failing;
        failing = readFailed && usedRemembered;
        return start;
    }
}
