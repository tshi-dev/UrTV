package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The cryptography of the release file's group B (docs/GAMES-RELEASE.md; no Android, tests/GameEngineTest.java). A pass code never leaves the TV and is never stored; what the file holds
 * for it is three things made from the code with PBKDF2-HMAC-SHA256 and a salt of its own: a check hash (does this code belong to the file), a wrap key, and with that key the group key
 * (32 random bytes, the same for every code of the file) sealed with AES-256-GCM. A definition meant for group B only is sealed with the group key, so a TV that does not hold a code in the
 * file cannot read it, and a revoked code (its record removed) is of no use for a file sealed again with a new group key. Hex everywhere; a sealed value is «iv(12) + ciphertext + tag(16)».
 */
final class GameCrypt {
    private GameCrypt() {}

    static final int KEY_BYTES = 32, IV_BYTES = 12, TAG_BITS = 128, ROUNDS_MIN = 1000, ROUNDS_MAX = 100_000, SALT_MIN = 8, CODE_MIN = 12;

    /** what is typed, as the hashes see it: upper case, Arabic-Indic and Persian digits as Latin, only letters and digits (a dash or a space does not matter) */
    static String normalize(String typed) {
        if (typed == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : typed.toCharArray()) {
            if (c >= '٠' && c <= '٩') c = (char) ('0' + (c - '٠'));
            else if (c >= '۰' && c <= '۹') c = (char) ('0' + (c - '۰'));
            c = Character.toUpperCase(c);
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) b.append(c);
        }
        return b.toString();
    }

    static byte[] unhex(String s) { byte[] b = new byte[s.length() / 2]; for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16); return b; }
    static String hex(byte[] b) { StringBuilder s = new StringBuilder(); for (byte x : b) s.append(String.format("%02x", x & 0xff)); return s.toString(); }

    /** PBKDF2-HMAC-SHA256 of the normalised code; `purpose` ("check" or "wrap") is added to the salt so the hash that is public can never be the key */
    static byte[] derive(String code, byte[] salt, int rounds, String purpose) throws GeneralSecurityException {
        byte[] p = purpose.getBytes(StandardCharsets.UTF_8), s = Arrays.copyOf(salt, salt.length + p.length);
        System.arraycopy(p, 0, s, salt.length, p.length);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(normalize(code).toCharArray(), s, rounds, KEY_BYTES * 8)).getEncoded();
    }

    /** AES-256-GCM: «iv + ciphertext + tag» as hex, a fresh random iv unless one is given (a test) */
    static String seal(byte[] key, byte[] plain, byte[] iv) throws GeneralSecurityException {
        if (iv == null) { iv = new byte[IV_BYTES]; new SecureRandom().nextBytes(iv); }
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
        byte[] ct = c.doFinal(plain), out = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, out, 0, iv.length); System.arraycopy(ct, 0, out, iv.length, ct.length);
        return hex(out);
    }

    /** the plain bytes of a sealed value, or null when the key is wrong, the value was changed or it is not a sealed value (never an exception: a wrong code is silent) */
    static byte[] open(byte[] key, String sealed) {
        try {
            if (key == null || key.length != KEY_BYTES || sealed == null || sealed.length() < (IV_BYTES + 16) * 2 || sealed.length() % 2 != 0 || !sealed.matches("[0-9a-f]+")) return null;
            byte[] all = unhex(sealed);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            return c.doFinal(all, IV_BYTES, all.length - IV_BYTES);
        } catch (GeneralSecurityException | RuntimeException e) { return null; }
    }

    /** the record of a pass code for the file: «1:rounds:salt:check:wrapped» (the group key sealed under the code's wrap key) */
    static String record(String code, byte[] salt, int rounds, byte[] groupKey, byte[] iv) {
        try {
            return "1:" + rounds + ":" + hex(salt) + ":" + hex(derive(code, salt, rounds, "check")) + ":" + seal(derive(code, salt, rounds, "wrap"), groupKey, iv);
        } catch (GeneralSecurityException e) { return null; }
    }

    /** the group key a typed code opens in this record, or null (a wrong code, a short one, a record that is not one); the check hash is compared in constant time */
    static byte[] groupKey(String typed, String record) {
        try {
            String n = normalize(typed);
            String[] p = record.split(":");
            if (n.length() < CODE_MIN || p.length != 5 || !p[0].equals("1")) return null;
            int rounds = Integer.parseInt(p[1]);
            if (rounds < ROUNDS_MIN || rounds > ROUNDS_MAX || p[2].length() < SALT_MIN * 2) return null;
            byte[] salt = unhex(p[2]);
            byte[] check = derive(n, salt, rounds, "check");
            if (!java.security.MessageDigest.isEqual(hex(check).getBytes(StandardCharsets.UTF_8), p[3].getBytes(StandardCharsets.UTF_8))) return null;
            return open(derive(n, salt, rounds, "wrap"), p[4]);
        } catch (GeneralSecurityException | RuntimeException e) { return null; }
    }

    /** the shape of a record, without opening it: 1:rounds:salt hex:check hex (64):wrapped hex (the group key sealed: 60 bytes) */
    static boolean wellFormed(String r) {
        if (r == null) return false;
        String[] p = r.split(":");
        if (p.length != 5 || !p[0].equals("1") || !p[1].matches("\\d{4,6}") || !p[2].matches("[0-9a-f]{16,64}") || !p[3].matches("[0-9a-f]{64}") || !p[4].matches("[0-9a-f]{" + (IV_BYTES + KEY_BYTES + 16) * 2 + "}")) return false;
        int rounds = Integer.parseInt(p[1]);
        return rounds >= ROUNDS_MIN && rounds <= ROUNDS_MAX;
    }
}
