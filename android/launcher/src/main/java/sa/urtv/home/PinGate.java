package sa.urtv.home;

/**
 * One entry from the keypad, settled exactly once. The keypad's look at the first four digits (a build with a recovery code) is counted
 * when it is made (PinStore.charge), so Back, a dismissed dialog or a look answered after the keypad closed never makes a free try. When the
 * entry is given up: the recovery code (PinRecovery, 5 to 8 digits) is RECOVER and the look is refunded, so it counts as nothing; the four
 * digits the look counted are that try, not counted again; anything else (a longer entry) refunds the look and is PinStore.verify, so a
 * wrong longer entry is one wrong try like any other. No Android in here.
 */
final class PinGate {
    static final int RECOVER = 4;   // beside PinStore's OK, WRONG, LOCKED, NOT_SET: open «رمز جديد»

    private final PinStore store;
    private final String recovery;
    private PinStore.Charge looked;   // the last look of this keypad (each new four digits is a new try)
    private boolean settled;

    /** One per keypad: the looks it makes and the entry it gives up. */
    PinGate(PinStore store, String recovery) {
        this.store = store;
        this.recovery = recovery;
    }

    /** The look at four digits (on the worker thread): counted now; true when they are the PIN. */
    synchronized boolean look(String four, long now) {
        if (settled) return false;   // a keypad already given up asks nothing more
        looked = store.charge(four, now);
        return looked.right();
    }

    /** The entry the keypad gave up; a second call (never made) is turned away without counting. */
    synchronized PinStore.Result submit(String code, long now) {
        if (settled) return new PinStore.Result(PinStore.LOCKED, 0, 0);
        settled = true;
        return check(store, code, recovery, looked, now);
    }

    /** looked: the keypad's last look (already counted), or null when it made none. */
    static PinStore.Result check(PinStore store, String code, String recovery, PinStore.Charge looked, long now) {
        if (code.length() > 4 && PinRecovery.matches(code, recovery)) {
            store.refund(looked);
            return new PinStore.Result(RECOVER, 0, PinStore.TRIES);
        }
        if (looked != null && looked.four.equals(code)) return looked.result;
        store.refund(looked);
        return store.verify(code, now);
    }
}
