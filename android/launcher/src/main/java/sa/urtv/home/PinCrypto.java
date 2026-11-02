package sa.urtv.home;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * The master PIN as a salted, deliberately slow PBKDF2 record «1:rounds:salt:hash»: the digits themselves are never kept or printed.
 * No Android in here (tests/PinStoreRun.java runs it). The idea and the record format are from AbdullahCg's PR #40.
 */
final class PinCrypto {
    static final int ROUNDS = 120000;
    private static final int MIN_ROUNDS = 10000, MAX_ROUNDS = 1000000;

    private PinCrypto() {}

    static boolean valid(String pin) { return pin != null && pin.matches("[0-9]{4}"); }

    static String create(String pin) {
        if (!valid(pin)) throw new IllegalArgumentException("a PIN is 4 digits");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "1:" + ROUNDS + ":" + hex(salt) + ":" + hex(derive(pin, salt, ROUNDS));
    }

    static boolean verify(String pin, String record) { return valid(pin) && matches(pin, record); }

    /** True when the record is one this class reads (its form only; nothing is derived). */
    static boolean wellFormed(String record) {
        if (record == null) return false;
        String[] p = record.split(":", -1);
        if (p.length != 4 || !"1".equals(p[0]) || !p[2].matches("[0-9a-f]{32}") || !p[3].matches("[0-9a-f]{64}") || !p[1].matches("[0-9]{1,7}")) return false;
        int rounds = Integer.parseInt(p[1]);
        return rounds >= MIN_ROUNDS && rounds <= MAX_ROUNDS;
    }

    /** Any digits against a record (PinRecovery's longer code uses the same record form); constant-time compare. */
    static boolean matches(String digits, String record) {
        if (digits == null || !digits.matches("[0-9]{1,16}") || !wellFormed(record)) return false;
        try {
            String[] p = record.split(":");
            return MessageDigest.isEqual(unhex(p[3]), derive(digits, unhex(p[2]), Integer.parseInt(p[1])));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] derive(String pin, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, rounds, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("the PIN check is not available", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format(Locale.ROOT, "%02x", x & 255));
        return s.toString();
    }

    private static byte[] unhex(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return b;
    }
}
