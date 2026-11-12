package sa.urtv.home.setup.pair;

/**
 * Where the pairing stands, as plain rules (no Android: tests/PairRun.java runs it). The TV's own page ARMS it (the person pressed «ابدأ الإقران», behind the family PIN); only then does the phone
 * page show «رمز الإقران» and only then is a code taken, once at a time. «armed» ends by itself after {@link #ARMED_MS}. The result is one of "paired" or "failed" with a short reason.
 */
public final class PairState {
    public static final long ARMED_MS = 10 * 60_000L;
    public static final String OFF = "off", ARMED = "armed", PAIRING = "pairing", PAIRED = "paired", FAILED = "failed";

    private long armedUntil;
    private String state = OFF, why = "";

    public synchronized void arm(long now) { armedUntil = now + ARMED_MS; state = ARMED; why = ""; }

    public synchronized void disarm() { armedUntil = 0; state = OFF; why = ""; }

    /** the state now: an armed state that ran out is off */
    public synchronized String state(long now) {
        if (ARMED.equals(state) && now >= armedUntil) { state = OFF; armedUntil = 0; }
        return state;
    }

    public synchronized String why() { return why; }

    /** a code arrives: only while armed (and not already pairing); false otherwise */
    public synchronized boolean begin(long now) {
        if (!ARMED.equals(state(now))) return false;
        state = PAIRING;
        return true;
    }

    public synchronized void finish(boolean ok, String reason) {
        state = ok ? PAIRED : FAILED;
        why = ok ? "" : reason == null ? "failed" : reason;
        if (!ok) armedUntil = 0;
    }

    /** after a failure the person may try again: back to armed for the rest of the time */
    public synchronized void retry(long now) { if (FAILED.equals(state)) { state = ARMED; armedUntil = now + ARMED_MS; why = ""; } }
}
