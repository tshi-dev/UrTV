package sa.urtv.home;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Which copy of the page the home card opens. The launcher carries the page of its own build (assets/web), and only an app update ever
 * changed it, while the screensaver downloads the newest page every few hours. So a fix to the page (the markets' requests among them)
 * reached the screensaver and never the home: a stocks card on the home stayed at its last good prices for good. Now the home asks the
 * screensaver for its page (ConfigProvider .../page/tv.html, given only when it is newer than the screensaver's own and its automatic
 * update is on), keeps a copy named by its build stamp, and opens it when its stamp is newer than the launcher's own page; otherwise, and
 * on any failure, its own page exactly as before.
 */
final class HomePage {
    private static final String PAGE = "content://sa.urtv.screensaver.config/page/";

    private HomePage() {}

    /** The url to open: {@code url} (which starts with one of HomeCardChoice's bases) on the freshest page this TV has. */
    static String fresh(Context c, String url) {
        if (url == null) return null;
        String base = url.startsWith(HomeCardChoice.BASE_EN) ? HomeCardChoice.BASE_EN : url.startsWith(HomeCardChoice.BASE) ? HomeCardChoice.BASE : null;
        if (base == null) return url;
        String name = base == HomeCardChoice.BASE_EN ? "tv-en.html" : "tv.html";
        try {
            File copy = copy(c, name);
            if (copy == null) return url;
            return Uri.fromFile(copy) + "#" + url.substring(base.length());
        } catch (RuntimeException e) {
            return url;
        }
    }

    /** Our copy of the screensaver's page when it is newer than ours, made or brought up to date now; null for none. */
    private static File copy(Context c, String name) {
        String own;
        try { own = PageStamp.read(c.getAssets().open("web/" + name)); } catch (Exception e) { own = ""; }
        if (!providerIsOurs(c)) return null;   // S1: a page is taken only from the screensaver app that carries our signature, never from another app that claimed the authority
        Uri uri = Uri.parse(PAGE + name);
        String theirs;
        try { theirs = PageStamp.read(c.getContentResolver().openInputStream(uri)); }
        catch (Exception e) { theirs = ""; }   // the screensaver is missing, older, or has nothing newer: our own page
        File dir = new File(c.getFilesDir(), "web");
        String prefix = name.replace(".html", "") + "_";   // tv_STAMP.html, tv-en_STAMP.html: neither prefix is the start of the other
        if (!PageStamp.newer(theirs, own)) { clean(dir, prefix, null); return null; }
        File want = new File(dir, prefix + theirs + ".html");
        if (!want.isFile() || !theirs.equals(fileStamp(want))) {
            if (!dir.isDirectory() && !dir.mkdirs()) return null;
            File tmp = new File(dir, prefix + "tmp");
            try (InputStream in = c.getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(tmp)) {
                if (in == null) return null;
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            } catch (Exception e) {
                tmp.delete();
                return null;
            }
            if (!theirs.equals(fileStamp(tmp)) || !tmp.renameTo(want)) { tmp.delete(); return null; }   // changed while read: next time
        }
        clean(dir, prefix, want);
        return want;
    }

    /** the app that answers sa.urtv.screensaver.config is sa.urtv.screensaver and is signed with the key this home is signed with */
    static boolean providerIsOurs(Context c) {
        try {
            android.content.pm.ProviderInfo pi = c.getPackageManager().resolveContentProvider("sa.urtv.screensaver.config", 0);
            return pi != null && "sa.urtv.screensaver".equals(pi.packageName)
                    && c.getPackageManager().checkSignatures(c.getPackageName(), pi.packageName) == android.content.pm.PackageManager.SIGNATURE_MATCH;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String fileStamp(File f) {
        try { return PageStamp.read(new java.io.FileInputStream(f)); } catch (Exception e) { return ""; }
    }

    /** Older copies go (one page is about 0.7 MB). */
    private static void clean(File dir, String prefix, File keep) {
        File[] all = dir.listFiles();
        if (all == null) return;
        for (File f : all) if (f.getName().startsWith(prefix) && !f.equals(keep)) f.delete();
    }
}
