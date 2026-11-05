package sa.urtv.home.setup;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

import sa.urtv.home.phone.UpdateRules;

/**
 * The APKs of the setup on the TV: downloaded into the app's own folder (files/setup-apk/, emptied after each install), only from our release
 * (UpdateRules.isReleaseUrl) or the GitHub releases of TV Bro and SmartTube, at most UpdateRules.MAX_APK_BYTES, over https; read with the package
 * manager (package, version, signers as SHA-256 of the certificates, the way «تحديث تلفزيونك» reads them).
 */
final class SetupApks implements SetupInstall.Apks {
    private final Context app;

    SetupApks(Context c) { app = c.getApplicationContext(); }

    static boolean allowed(String url) {
        if (UpdateRules.isReleaseUrl(url)) return true;
        for (String[] x : SetupInstall.EXTRAS) if (url.startsWith("https://github.com/" + x[0] + "/releases/download/") && url.endsWith(".apk") && !url.contains("..")) return true;
        return false;
    }

    @Override public SetupInstall.Apk download(String url) throws IOException {
        if (!allowed(url)) return null;
        File dir = new File(app.getFilesDir(), "setup-apk");
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        File[] old = dir.listFiles();
        if (old != null) for (File f : old) f.delete();
        final File f = new File(dir, "download.apk");
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(10_000);
        con.setReadTimeout(30_000);
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK || !con.getURL().getProtocol().equals("https")) return null;
            if (con.getContentLength() > UpdateRules.MAX_APK_BYTES) return null;
            long total = 0;
            try (InputStream in = con.getInputStream(); OutputStream o = new FileOutputStream(f)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > UpdateRules.MAX_APK_BYTES) { f.delete(); return null; }
                    o.write(buf, 0, n);
                }
            }
            if (total == 0) { f.delete(); return null; }
        } catch (IOException e) {
            f.delete();
            return null;
        } finally {
            con.disconnect();
        }
        PackageInfo info = app.getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), flags());
        if (info == null || info.packageName == null) { f.delete(); return null; }
        Set<String> signers;
        try { signers = signers(info); } catch (Exception e) { signers = new HashSet<>(); }
        return new SetupInstall.Apk(info.packageName, code(info), signers, f.length(), () -> new FileInputStream(f), f::delete);
    }

    @Override public Set<String> selfSigners() { return signersOf(app.getPackageName()); }

    @Override public long installedCode(String pkg) {
        try { return code(app.getPackageManager().getPackageInfo(pkg, 0)); } catch (PackageManager.NameNotFoundException e) { return -1; }
    }

    @Override public Set<String> installedSigners(String pkg) { return signersOf(pkg); }

    private Set<String> signersOf(String pkg) {
        try { return signers(app.getPackageManager().getPackageInfo(pkg, flags())); } catch (Exception e) { return new HashSet<>(); }
    }

    private static int flags() {
        return Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_SIGNATURES : PackageManager.GET_SIGNATURES;
    }

    private static long code(PackageInfo i) { return Build.VERSION.SDK_INT >= 28 ? i.getLongVersionCode() : i.versionCode; }

    private static Set<String> signers(PackageInfo i) throws Exception {
        Signature[] sigs = null;
        if (Build.VERSION.SDK_INT >= 28 && i.signingInfo != null) sigs = i.signingInfo.getApkContentsSigners();
        if ((sigs == null || sigs.length == 0) && i.signatures != null) sigs = i.signatures;
        Set<String> out = new HashSet<>();
        if (sigs != null) for (Signature s : sigs) out.add(UpdateRules.hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())));
        return out;
    }
}
