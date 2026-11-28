package sa.urtv.screensaver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 4.4 beta 4 (a user: a photo sent from the phone arrived twice): is a photo arriving one «صوري» already keeps? The same bytes (SHA-256) as a kept
 * photo is the same photo: the phone sending it again (a retry over a weak Wi-Fi, a second tap) or photos.sh run twice. Only a kept photo of
 * the same length is read, so an add costs nothing when there is no twin. No Android in it (tests/PhotoTwinRun.java runs it).
 */
final class PhotoTwin {
    private PhotoTwin() {}

    /** the number (from 1) of the kept photo with src's bytes, 0 when none (or src cannot be read: the add then refuses it as before) */
    static int find(File src, File[] kept) {
        long len = src.length();
        byte[] mine = null;
        for (int i = 0; i < kept.length; i++) {
            if (kept[i] == null || !kept[i].isFile() || kept[i].length() != len) continue;
            if (mine == null && (mine = sha256(src)) == null) return 0;
            byte[] theirs = sha256(kept[i]);
            if (theirs != null && Arrays.equals(mine, theirs)) return i + 1;
        }
        return 0;
    }

    static byte[] sha256(File f) {
        try (InputStream in = new FileInputStream(f)) {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) d.update(buf, 0, n);
            return d.digest();
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }
}
