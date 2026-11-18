package sa.urtv.screensaver;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The games' content as the TV holds it (no Android: tests/GameBundleTest.java): ONE compressed, lightly encrypted file per bank or pack, never the editable
 * source (docs/LICENSE-CONTENT.md). Layout: "GB1" + one byte format + 16 bytes IV + AES-256-CBC(deflate(JSON)) + 32 bytes HMAC-SHA256 of everything before it
 * (encrypt then check). The two keys come from the bundle's own name through PBKDF2 and pieces kept in the code: it stops casual copying (opening the file in a
 * text editor, a grep over the data), not a person who reads the app's code. games/tools/pack_bundle.py makes the same file. A wrong key, a changed byte or a
 * file that is not a bundle gives null, never an exception to the game.
 */
final class GameBundle {
    private GameBundle() {}

    static final int MAX_PLAIN = 8_000_000, ROUNDS = 20000;
    private static final byte[] MAGIC = {'G', 'B', '1'};
    /** the pieces of the pass phrase, apart in the code (docs/LICENSE-CONTENT.md says what this is and is not) */
    private static final String[] PIECES = {"urtv", "games", "ٱلْمُحْتَوَى", "2026"};

    static byte[][] keys(String name) throws GeneralSecurityException {
        StringBuilder p = new StringBuilder();
        for (String s : PIECES) p.append(s).append('|');
        p.append(name);
        byte[] salt = MessageDigest.getInstance("SHA-256").digest(("urtv-salt|" + name).getBytes(StandardCharsets.UTF_8));
        byte[] k = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(p.toString().toCharArray(), salt, ROUNDS, 512)).getEncoded();
        byte[] enc = new byte[32], mac = new byte[32];
        System.arraycopy(k, 0, enc, 0, 32); System.arraycopy(k, 32, mac, 0, 32);
        return new byte[][] {enc, mac};
    }

    /** the JSON text inside a bundle named `name`, or null when it is not a good bundle of that name */
    static byte[] open(String name, byte[] file) {
        try {
            if (file == null || file.length < 3 + 1 + 16 + 16 + 32) return null;
            for (int i = 0; i < 3; i++) if (file[i] != MAGIC[i]) return null;
            byte[][] k = keys(name);
            Mac m = Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(k[1], "HmacSHA256"));
            m.update(file, 0, file.length - 32);
            byte[] want = m.doFinal(), got = new byte[32];
            System.arraycopy(file, file.length - 32, got, 0, 32);
            if (!MessageDigest.isEqual(want, got)) return null;
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(k[0], "AES"), new IvParameterSpec(file, 4, 16));
            byte[] z = c.doFinal(file, 20, file.length - 20 - 32);
            Inflater inf = new Inflater();
            inf.setInput(z);
            ByteArrayOutputStream out = new ByteArrayOutputStream(z.length * 4);
            byte[] buf = new byte[16384];
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break;
                out.write(buf, 0, n);
                if (out.size() > MAX_PLAIN) { inf.end(); return null; }
            }
            inf.end();
            return out.toByteArray();
        } catch (GeneralSecurityException | DataFormatException | RuntimeException e) { return null; }
    }
}
