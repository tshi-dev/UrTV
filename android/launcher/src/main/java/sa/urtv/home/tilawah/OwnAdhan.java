package sa.urtv.home.tilawah;

import android.content.Context;
import android.media.MediaMetadataRetriever;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * «أذان من ملفي»: the person's own adhan recording, one file in this app's private folder
 * (files/adhan-own.m4a). It only arrives over ADB (MusicReceiver ADHAN_OWN, optimize/music.sh adhan add) and stays on
 * this TV: nothing reads it out and nothing sends it anywhere. A new one replaces the old.
 *
 * It is checked before it is kept: an m4a or an Ogg, at most OwnAdhanText.MAX_BYTES, readable, at most 5 minutes.
 */
final class OwnAdhan {
    private OwnAdhan() {}

    static File file(Context c) { return new File(c.getFilesDir(), "adhan-own.m4a"); }

    /** 4.3 C16: the second recording, for Fajr only («أذاني للفجر»: optimize/music.sh adhan add --fajr FILE) */
    static File fajrFile(Context c) { return new File(c.getFilesDir(), "adhan-own-fajr.m4a"); }
    static boolean fajrExists(Context c) { return fajrFile(c).length() > 0; }
    /** the file a voice number plays: the first recording (Prefs.OWN), the Fajr one (Prefs.OWN_FAJR) */
    static File fileOf(Context c, int voice) { return voice == Prefs.OWN_FAJR ? fajrFile(c) : file(c); }
    static boolean isOwn(int voice) { return voice == Prefs.OWN || voice == Prefs.OWN_FAJR; }
    static boolean existsOf(Context c, int voice) { return voice == Prefs.OWN_FAJR ? fajrExists(c) : exists(c); }

    /** where OwnAdhanProvider lets the shell write the next one before MusicReceiver keeps it */
    static File incoming(Context c) { return new File(c.getFilesDir(), "adhan-own-in.tmp"); }

    static boolean exists(Context c) { return file(c).length() > 0; }

    static long bytes(Context c) { return file(c).length(); }

    /** "" when src is a recording we can keep, else why not */
    static String check(File src) {
        byte[] head = new byte[8];
        int n;
        try (InputStream in = new FileInputStream(src)) {
            n = in.read(head);
        } catch (IOException e) {
            return "cannot read it";
        }
        if (OwnAdhanText.container(head, n).isEmpty()) return "not an m4a (AAC) or Ogg (Opus) recording";
        long ms = 0;
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(src.getPath());
            String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            ms = d == null ? 0 : Long.parseLong(d);
        } catch (RuntimeException e) {
            ms = 0;
        } finally {
            try { r.release(); } catch (Exception ignored) { }   // release() declares IOException from API 29
        }
        return OwnAdhanText.refuse(src.length(), ms);
    }

    /**
     * Keeps src as the one recording, replacing the old. null when done, else why not; src is gone either way if it is in.tmp.
     * The old recording stays until the new one is whole and in place (SafeReplace): a refusal or a failure leaves it playing.
     */
    static synchronized String keep(Context c, File src) { return keep(c, src, false); }

    /** fajr: the second recording, for Fajr (4.3 C16); the first stays as it is. */
    static synchronized String keep(Context c, File src, boolean fajr) {
        String why = check(src);
        if (why.isEmpty()) why = SafeReplace.replace(src, fajr ? fajrFile(c) : file(c), src.equals(incoming(c)), SafeReplace.FILES);
        if (src.equals(incoming(c)) && src.exists()) src.delete();   // never leave a refused file to be taken for the next one
        return why.isEmpty() ? null : why;
    }

    static synchronized void clear(Context c) {
        file(c).delete();
        new File(c.getFilesDir(), "adhan-own.m4a.new").delete();
        new File(c.getFilesDir(), "adhan-own.m4a.old").delete();
        incoming(c).delete();
        clearFajr(c);
    }

    /** only the Fajr recording (4.3 C16) */
    static synchronized void clearFajr(Context c) {
        fajrFile(c).delete();
        new File(c.getFilesDir(), "adhan-own-fajr.m4a.new").delete();
        new File(c.getFilesDir(), "adhan-own-fajr.m4a.old").delete();
    }
}
