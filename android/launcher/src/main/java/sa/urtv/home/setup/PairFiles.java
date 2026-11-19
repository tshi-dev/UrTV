package sa.urtv.home.setup;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 4.4 beta 3, the paired route's installs: the add-on streams an APK to the TV's adbd over its TLS connection, and reads it from here. Each file is put here for ONE
 * install under a random token, opened read-only, and deleted right after (PairRoute.open's exec). Guarded by the add-on's signature permission
 * (sa.urtv.permission.EXTRAS): only our add-on reads it. No rows, no listing, nothing else served.
 */
public class PairFiles extends ContentProvider {
    static final String AUTHORITY = "sa.urtv.home.pairfiles";
    private static final Map<String, File> FILES = new HashMap<>();

    static Uri uri(String token) { return Uri.parse("content://" + AUTHORITY + "/" + token); }

    /** copies {@code size} bytes into the app's cache under a new token (the caller's thread: the setup's) */
    static String put(Context c, InputStream data, long size) throws IOException {
        File dir = new File(c.getCacheDir(), "pair");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("no room for the file");
        String token = UUID.randomUUID().toString();
        File f = new File(dir, token + ".bin");
        try (FileOutputStream out = new FileOutputStream(f)) {
            byte[] buf = new byte[64 * 1024];
            long left = size;
            while (left > 0) {
                int n = data.read(buf, 0, (int) Math.min(buf.length, left));
                if (n < 0) throw new IOException("the file ended before its size");
                out.write(buf, 0, n);
                left -= n;
            }
        } catch (IOException e) {
            f.delete();
            throw e;
        }
        synchronized (FILES) { FILES.put(token, f); }
        return token;
    }

    static void drop(Context c, String token) {
        File f;
        synchronized (FILES) { f = FILES.remove(token); }
        if (f != null) f.delete();
    }

    @Override public boolean onCreate() { return true; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("read only");
        String token = uri == null ? null : uri.getLastPathSegment();
        File f;
        synchronized (FILES) { f = token == null ? null : FILES.get(token); }
        if (f == null) throw new FileNotFoundException("no such file");
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
