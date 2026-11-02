package sa.urtv.home;

/**
 * One entry from the keypad, decided: the recovery code (PinRecovery, 5 to 8 digits) is RECOVER and counts as nothing; four digits a
 * quiet look already decided are counted once by PinStore.record; anything else is PinStore.verify, so a wrong longer entry is a wrong
 * try like any other. No Android in here.
 */
final class PinGate {
    static final int RECOVER = 4;   // beside PinStore's OK, WRONG, LOCKED, NOT_SET: open «رمز جديد»

    private PinGate() {}

    /** probed / probedRight: the four digits the keypad looked at (null when none) and the answer the look had. */
    static PinStore.Result check(PinStore store, String code, String recovery, String probed, boolean probedRight, long now) {
        if (code.length() > 4 && PinRecovery.matches(code, recovery)) return new PinStore.Result(RECOVER, 0, PinStore.TRIES);
        if (probed != null && probed.equals(code)) return store.record(probedRight, now);
        return store.verify(code, now);
    }
}
