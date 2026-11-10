package sa.urtv.home;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

import sa.urtv.home.phone.UpdateRules;

/**
 * The one install path of «تطبيقات مقترحة» (SuggestedAppsActivity's tiles) and of the Player's own «تحديث» (InstallPlayerActivity): download →
 * SHA-256 = the published one → the package name and, for the Player, exactly its pinned certificate → Android's installer (PackageInstaller,
 * MODE_FULL_INSTALL: over an installed app of the same key it is an update and keeps its data; nothing is ever uninstalled). Also the Player's
 * update cache (PlayerUpdate), refreshed in the background through Net.
 */
final class MirrorInstall {
    private MirrorInstall() { }

    private static final String TAG = "UrTVSuggested", PREFS = "suggested", UPDATE_KEY = "player_update";
    private static volatile long lastTry;   // a failed refresh is not tried again for 10 minutes (a provider may be asked often)

    /** the SHA-256 of each signing certificate of an installed package; empty when it is not there */
    static Set<String> signers(PackageManager pm, String pkg) {
        Set<String> out = new HashSet<>();
        if (pkg == null) return out;
        try {
            int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
            PackageInfo info = pm.getPackageInfo(pkg, flags);
            Signature[] sigs = Build.VERSION.SDK_INT >= 28 && info.signingInfo != null
                ? (info.signingInfo.hasMultipleSigners() ? info.signingInfo.getApkContentsSigners() : info.signingInfo.getSigningCertificateHistory()) : info.signatures;
            if (sigs != null) for (Signature s : sigs) out.add(SuggestedApps.hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())));
        } catch (Exception ignored) { }
        return out;
    }

    /** the installed Player's versionCode, -1 when it is not installed */
    static long installedCode(Context c) {
        try {
            PackageInfo i = c.getPackageManager().getPackageInfo(SuggestedApps.PLAYER, 0);
            return Build.VERSION.SDK_INT >= 28 ? i.getLongVersionCode() : i.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    static String cache(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(UPDATE_KEY, ""); }

    /** the update known for the installed Player, or null (PlayerUpdate.row on the cache) */
    static PlayerUpdate.Version known(Context c) { return PlayerUpdate.row(cache(c), installedCode(c)); }

    /** Asks GitHub again in the background when the cache is due (PlayerUpdate.REFRESH_MS); never waits for it. then: on the UI's own terms, may be null. */
    static void refreshAsync(final Context c, final Runnable then) {
        final long now = System.currentTimeMillis();
        if (!PlayerUpdate.refreshDue(cache(c), now) || now - lastTry < 10 * 60_000L && now >= lastTry) return;
        lastTry = now;
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            try {
                if (refresh(app, now) && then != null) then.run();
            } catch (Exception e) {
                Log.w(TAG, "player update: " + e.getClass().getSimpleName());
            }
        }, "player-update").start();
    }

    /** One look at the releases: the mirror answer (the tile) and the version (the cache). False when GitHub did not answer (nothing kept). */
    static boolean refresh(Context c, long now) throws Exception {
        SuggestedApps.App a = SuggestedApps.PLAYER_APP;
        String json = get(a.address(), 2 * 1024 * 1024);
        if (json == null) return false;
        boolean there = SuggestedApps.pickMirror(a, json) != null;
        PlayerUpdate.Version v = null;
        String vu = there ? PlayerUpdate.versionUrl(a, json) : null;
        if (vu != null) v = PlayerUpdate.parse(get(vu, PlayerUpdate.VERSION_MAX_BYTES));
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("mirror_" + a.id, (there ? "1|" : "0|") + now)
            .putString(UPDATE_KEY, PlayerUpdate.encode(now, v)).apply();
        return true;
    }

    /**
     * A small text file through Net (IPv4 first, a deadline); null on anything but a 200. Net follows no redirect: a release file's address
     * answers 302 to GitHub's file host, followed here once, and only to an https address of githubusercontent.com (PlayerUpdate.redirectOk).
     */
    static String get(String url, int max) throws Exception {
        java.util.Map<String, String> h = new java.util.HashMap<>();
        h.put("Accept", "application/vnd.github+json");
        Net.Reply r = Net.get(url, h, 15_000, 25_000, max);
        if (r.status / 100 == 3 && PlayerUpdate.redirectOk(r.headers.get("location"))) r = Net.get(r.headers.get("location"), null, 15_000, 25_000, max);
        return r.status == 200 ? r.text() : null;
    }

    /**
     * A MIRROR app (UrTV Player): the newest release of its own repository (SuggestedApps.pickMirror), its published SHA-256, then the file; handed to the installer only when the file
     * has that SHA-256, is that package and is signed by exactly the pinned certificate (SuggestedApps.mirrorFileOk). expectCode > 0 (the
     * Player's own «تحديث»): the release's urtv-player.version must say that version, newer than the installed one (PlayerUpdate.installOk).
     * null when Android's installer was given the file; else the words to show.
     */
    static String mirror(Context c, SuggestedApps.App a, File f, String resultAction, int expectCode) throws Exception {
        if (!SuggestedApps.mirrorReady(a)) return S.t("ما قدرنا نثبّته. جرّب مرة ثانية");
        String json = fetch(a.address(), 2 * 1024 * 1024);
        String[] urls = json == null ? null : SuggestedApps.pickMirror(a, json);
        if (urls == null || !SuggestedApps.mirrorOk(a, urls[0]) || !SuggestedApps.mirrorOk(a, urls[1])) return S.t("ما لقينا نسخة تناسب هذا التلفزيون");
        if (expectCode > 0) {
            String vu = PlayerUpdate.versionUrl(a, json);
            PlayerUpdate.Version v = vu == null ? null : PlayerUpdate.parse(fetch(vu, PlayerUpdate.VERSION_MAX_BYTES));
            if (!PlayerUpdate.installOk(expectCode, v, installedCode(c))) return S.t("ما لقينا نسخة تناسب هذا التلفزيون");
        }
        String published = SuggestedApps.parseSha(fetch(urls[1], 4096));
        if (published == null) return S.t("ما قدرنا ننزّله. جرّب مرة ثانية");
        String sha = download(urls[0], f);
        if (sha == null) return S.t("ما قدرنا ننزّله. جرّب مرة ثانية");
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_SIGNATURES : PackageManager.GET_SIGNATURES;
        PackageInfo info = c.getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), flags);
        Set<String> signers = new HashSet<>();
        if (info != null) {
            Signature[] sigs = null;
            if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) sigs = info.signingInfo.getApkContentsSigners();
            if ((sigs == null || sigs.length == 0) && info.signatures != null) sigs = info.signatures;
            if (sigs != null) for (Signature s : sigs) signers.add(SuggestedApps.hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())));
        }
        if (!SuggestedApps.mirrorFileOk(a, sha, published, info == null ? null : info.packageName, signers)) {
            f.delete();
            return S.t("الملف ليس هذا التطبيق، فما ثبّتناه");
        }
        commit(c, f, a.pkg, resultAction);
        return null;
    }

    /** Android's installer gets the file (MODE_FULL_INSTALL: an update over an installed app of the same key keeps its data); its answer comes back as resultAction */
    static void commit(Context c, File f, String pkg, String resultAction) throws Exception {
        PackageInstaller installer = c.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(pkg);
        int id = installer.createSession(params);
        try (PackageInstaller.Session session = installer.openSession(id)) {
            try (OutputStream out = session.openWrite("base.apk", 0, f.length()); InputStream in = new FileInputStream(f)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                session.fsync(out);
            }
            Intent back = new Intent(resultAction).setPackage(c.getPackageName());
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            session.commit(PendingIntent.getBroadcast(c, 9300, back, flags).getIntentSender());
        } catch (Exception e) {
            try { installer.abandonSession(id); } catch (Exception ignored) { }
            throw e;
        } finally {
            f.delete();
        }
    }

    /** The file into f: https only, from an allowed place (SuggestedApps), at most UpdateRules.MAX_APK_BYTES. Its SHA-256, or null. */
    static String download(String url, File f) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(10_000);
        con.setReadTimeout(30_000);
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK || !"https".equals(con.getURL().getProtocol())) return null;
            if (con.getContentLength() > UpdateRules.MAX_APK_BYTES) return null;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            long total = 0;
            try (InputStream in = con.getInputStream(); OutputStream o = new FileOutputStream(f)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > UpdateRules.MAX_APK_BYTES) { f.delete(); return null; }
                    md.update(buf, 0, n);
                    o.write(buf, 0, n);
                }
            }
            if (total == 0) { f.delete(); return null; }
            return SuggestedApps.hex(md.digest());
        } finally {
            con.disconnect();
        }
    }

    static String fetch(String url, int max) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(10_000);
        con.setReadTimeout(20_000);
        con.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            try (InputStream in = con.getInputStream()) {
                java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) { if (b.size() + n > max) return null; b.write(buf, 0, n); }
                return b.toString("UTF-8");
            }
        } finally {
            con.disconnect();
        }
    }
}
