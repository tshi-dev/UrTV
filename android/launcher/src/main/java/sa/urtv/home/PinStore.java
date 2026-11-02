package sa.urtv.home;

/**
 * The master PIN and its wrong tries, over any small key-value store (Parental gives it the SharedPreferences; the test a map). Only the
 * PBKDF2 record is kept (PinCrypto). Five wrong tries in a row lock it for 1 minute, the next five for 5, the next five and every five
 * after for 15; a right PIN starts over. While it is locked a try is turned away without being looked at, so guessing during the wait
 * learns nothing. The wait is measured on the TV's clock: a clock set back is capped at the longest wait. No Android in here.
 */
public final class PinStore {
    public interface Backing {
        String get(String key);
        void put(String key, String value);
        void remove(String key);
    }

    public static final int TRIES = 5;
    static final long[] WAIT_MS = {60_000L, 5 * 60_000L, 15 * 60_000L};
    private static final String RECORD = "pin", LOCK = "pinlock";   // LOCK is "wrong tries in a row,unlock time" in one value

    public static final int OK = 0, WRONG = 1, LOCKED = 2, NOT_SET = 3;

    /** kind: one of the four above; waitMs: how long until the next try (LOCKED, or WRONG on the try that locked it); triesLeft: before the lock (WRONG) */
    public static final class Result {
        public final int kind;
        public final long waitMs;
        public final int triesLeft;
        Result(int kind, long waitMs, int triesLeft) { this.kind = kind; this.waitMs = waitMs; this.triesLeft = triesLeft; }
    }

    private final Backing store;

    public PinStore(Backing store) { this.store = store; }

    public synchronized boolean isSet() { return store.get(RECORD) != null; }

    /** A new PIN; the tries start over. */
    public synchronized void set(String pin) {
        String record = PinCrypto.create(pin);
        store.put(RECORD, record);
        store.remove(LOCK);
    }

    /** No PIN, no tries. */
    public synchronized void clear() {
        store.remove(RECORD);
        store.remove(LOCK);
    }

    /** How long until a PIN is looked at again, 0 when it is now. */
    public synchronized long waitLeft(long now) {
        long until = state()[1];
        if (until <= now) return 0;
        return Math.min(until - now, WAIT_MS[WAIT_MS.length - 1]);
    }

    /** Wrong tries in a row so far. */
    public synchronized int fails() { return (int) state()[0]; }

    public synchronized Result verify(String pin, long now) {
        String record = store.get(RECORD);
        if (record == null) return new Result(NOT_SET, 0, 0);
        long wait = waitLeft(now);
        if (wait > 0) return new Result(LOCKED, wait, 0);
        return outcome(PinCrypto.verify(pin, record), now);
    }

    /**
     * Whether these digits are the PIN, with nothing counted or started over, and false while locked: the keypad's look at the first four
     * digits when a longer code may follow (PinRecovery). Its answer is then counted once, by {@link #record}, when the keypad gives up
     * those four digits, so a look never makes a free try.
     */
    public synchronized boolean matches(String pin, long now) {
        String record = store.get(RECORD);
        return record != null && waitLeft(now) == 0 && PinCrypto.verify(pin, record);
    }

    /** A try already looked at by {@link #matches}, counted now as verify would (and turned away the same way if it has locked since). */
    public synchronized Result record(boolean right, long now) {
        if (store.get(RECORD) == null) return new Result(NOT_SET, 0, 0);
        long wait = waitLeft(now);
        if (wait > 0) return new Result(LOCKED, wait, 0);
        return outcome(right, now);
    }

    private Result outcome(boolean right, long now) {
        if (right) {
            store.remove(LOCK);
            return new Result(OK, 0, TRIES);
        }
        int fails = (int) state()[0] + 1;
        long lock = waitFor(fails);
        store.put(LOCK, fails + "," + (lock > 0 ? now + lock : 0));
        return new Result(WRONG, lock, lock > 0 ? 0 : TRIES - fails % TRIES);
    }

    /** The wait the nth wrong try in a row brings: none until the 5th, then 1, 5 and 15 minutes, 15 from then on. */
    static long waitFor(int fails) {
        if (fails <= 0 || fails % TRIES != 0) return 0;
        return WAIT_MS[Math.min(fails / TRIES, WAIT_MS.length) - 1];
    }

    private long[] state() {
        String s = store.get(LOCK);
        if (s == null) return new long[] {0, 0};
        try {
            String[] p = s.split(",");
            return new long[] {Math.max(0, Long.parseLong(p[0])), Math.max(0, Long.parseLong(p[1]))};
        } catch (RuntimeException e) {
            return new long[] {0, 0};
        }
    }
}
