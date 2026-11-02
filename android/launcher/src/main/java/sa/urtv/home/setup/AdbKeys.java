/*
 * Adapted from Free My TV, app/src/io/freemytv/Adb.java (https://github.com/simbasjungles/free-my-tv), MIT License,
 * Copyright (c) 2026 Free My TV contributors (see THIRD_PARTY_NOTICES.md): the app's own ADB key, kept in private storage, never in a log.
 */
package sa.urtv.home.setup;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/** The key this app presents to the TV's own adb. The TV asks "Allow USB debugging?" for it once; "Always allow" saves it on the TV. */
final class AdbKeys {
    private AdbKeys() { }

    static synchronized KeyPair load(Context c) throws IOException {
        File dir = c.getFilesDir();
        File privateFile = new File(dir, "adbkey"), publicFile = new File(dir, "adbkey.pub");
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            if (privateFile.exists() && publicFile.exists()) {
                try {
                    return new KeyPair(factory.generatePublic(new X509EncodedKeySpec(readFile(publicFile))),
                            factory.generatePrivate(new PKCS8EncodedKeySpec(readFile(privateFile))));
                } catch (GeneralSecurityException | IOException e) {
                    // a file cut by a power loss: a new key; the TV asks again
                }
            }
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            writeFile(privateFile, pair.getPrivate().getEncoded());
            writeFile(publicFile, pair.getPublic().getEncoded());
            return pair;
        } catch (GeneralSecurityException e) {
            throw new IOException("ADB key: " + e.getMessage());
        }
    }

    /**
     * The key is a standing credential once the person has accepted «السماح دائماً» on the TV: it is deleted when the setup (or the undo) is finished
     * and no safety switch is pending. The next run makes a new key and the TV asks again; the old authorization can be revoked in the TV's
     * developer options («إلغاء تراخيص تصحيح USB»).
     */
    static synchronized void delete(Context c) {
        File dir = c.getFilesDir();
        new File(dir, "adbkey").delete();
        new File(dir, "adbkey.pub").delete();
        new File(dir, "adbkey.tmp").delete();
        new File(dir, "adbkey.pub.tmp").delete();
    }

    /** For the log only: is a key on disk now. */
    static synchronized boolean exists(Context c) { return new File(c.getFilesDir(), "adbkey").exists() || new File(c.getFilesDir(), "adbkey.pub").exists(); }

    private static byte[] readFile(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            return out.toByteArray();
        }
    }

    /** All or nothing: to a file alongside, synced to disk, and only then renamed to the real name. */
    private static void writeFile(File f, byte[] data) throws IOException {
        File tmp = new File(f.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(data);
            out.getFD().sync();
        }
        if (!tmp.renameTo(f)) throw new IOException("could not save " + f.getName());
    }
}
