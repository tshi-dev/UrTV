package sa.urtv.home;

/**
 * The maintainer's recovery code for a forgotten master PIN: 5 to 8 digits typed where the PIN is asked open «رمز جديد» directly (the locks,
 * the allowed apps and kids mode stay; only the PIN is replaced), and it is never counted as a wrong try. The code itself is in no source
 * and no APK: the build is given only its salted PBKDF2 record (PinCrypto's form), as BuildConfig.PIN_RECOVERY, from a private source
 * (AGENTS.md, «PIN recovery»). A build without one has no recovery code at all, and the keypad then stops at four digits as it always did.
 * No Android in here (tests/pin_recovery_test.py runs it).
 */
final class PinRecovery {
    static final int SHORTEST = 5, LONGEST = 8;

    private PinRecovery() {}

    /** True when this build was given a recovery record. */
    static boolean on(String record) { return PinCrypto.wellFormed(record); }

    /** True when the digits are the recovery code of this record; false for anything when there is no record. */
    static boolean matches(String code, String record) {
        return code != null && code.length() >= SHORTEST && code.length() <= LONGEST && on(record) && PinCrypto.matches(code, record);
    }
}
