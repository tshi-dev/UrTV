package sa.urtv.home.phone;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sa.urtv.home.BuildConfig;

/**
 * «تحديث تلفزيوني», the one engine: the TV's own screen (UpdateActivity) and the phone page's button call the same code. check: reads
 * version.txt of the apk-latest release (the one optimize/install.sh uses; the apk-next test release when an agent set `saver.sh updatechannel next`, see UpdateChannel) and decides (UpdateFlow); when there is a newer version and the
 * permission is given, it downloads the APKs and refuses any that is not the same app, not newer, or signed with another certificate than the
 * installed one (UpdateRules.verify), so «available» means every check passed. install: hands the files, the screensaver app first and the
 * launcher last (UpdateOrder), one after the other to Android's own installer (PackageInstaller sessions, their status callbacks): its confirm
 * screen opens on the TV and the owner confirms with the remote. Before the launcher is replaced (this process dies with it) the «update in
 * progress» record is written (UpdateRecords), and the next start of the launcher reads it. Nothing is installed silently, nothing is
 * uninstalled. The approach (download, check the signer, PackageInstaller) is from itsAbdulelah's pull request #10.
 */
public final class PhoneUpdater {
    public static final String ACTION_RESULT = "sa.urtv.home.PHONE_INSTALL_RESULT";
    private static final String TAG = "UrTVUpdate";

    private static final long STUCK_MS = 10 * 60_000L;   // an install screen nobody answers for ten minutes no longer blocks a new try
    private static String state = "idle", error = "", detail = "", why = "";
    private static int build;
    private static long since;
    private static final List<File> ready = new ArrayList<>();           // downloaded and checked, in install order
    private static final List<String> readyPackages = new ArrayList<>();
    private static final List<File> queue = new ArrayList<>();           // still to hand to the installer
    private static final List<String> queuePackages = new ArrayList<>();
    private static String installing = "";                              // the package whose session is open now
    private static Intent confirm;                                        // the installer's confirm screen, until its answer
    private static boolean confirmShown, visible;
    private static Runnable listener;

    private PhoneUpdater() { }

    public static synchronized Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        String[] shown = UpdateFlow.shown(state, error, busy(), confirm != null);   // a «waiting» nobody is answering is a failure, not a dead end
        m.put("status", shown[0]);
        if (!shown[1].isEmpty()) m.put("reason", shown[1]);
        if (!detail.isEmpty()) m.put("detail", detail);
        if (!why.isEmpty()) m.put("why", why);
        if (build > 0) { m.put("build", (long) build); m.put("version", UpdateText.versionOf(BuildConfig.MARKETING, build)); }
        return m;
    }

    /** The screen showing the update wants to hear about changes (any thread); null to stop. */
    public static synchronized void setListener(Runnable r) { listener = r; }

    /** The update screen is in front: the installer's confirm screen may be opened from it. */
    public static synchronized void setVisible(boolean v) { visible = v; }

    private static void set(String s, String reason, String d) { set(s, reason, d, ""); }

    /** w: the installer's or the check's word for a failure (storage, installer, blocked…), kept for the phone page and the log. */
    private static void set(String s, String reason, String d, String w) {
        Runnable r;
        synchronized (PhoneUpdater.class) {
            state = s;
            error = reason;
            detail = d;
            why = w;
            since = System.currentTimeMillis();
            r = listener;
        }
        if (r != null) r.run();
    }

    private static boolean busy() {
        return (state.equals("checking") || state.equals("downloading") || state.equals("waiting_owner")) && System.currentTimeMillis() - since < STUCK_MS;
    }

    /** The word for the state the screen shows. */
    public static synchronized boolean isBusy() { return busy(); }

    // ---- check

    /** The TV's own screen: reads version.txt, and when there is a newer version downloads and checks the files. */
    public static Map<String, Object> check(final Context c) { return check(c, true); }

    /** The phone page's «افحص التحديث»: version.txt only, nothing is downloaded until «ثبّت» (install). */
    public static Map<String, Object> checkVersion(final Context c) { return check(c, false); }

    private static Map<String, Object> check(final Context c, final boolean download) {
        synchronized (PhoneUpdater.class) {
            if (busy()) return status();
            set("checking", "", "");
        }
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    prepare(app, download);
                } catch (Exception e) {
                    Log.w(TAG, "check failed: " + e);
                    cannot(UpdateFlow.crashed().reason);   // no network was already told by version.txt; this is something else
                }
            }
        }, "tv-update-check").start();
        return status();
    }

    /**
     * The release the files come from: the latest, unless an agent set the test channel on the screensaver (UpdateChannel). Read ONCE per check
     * and kept, so the version, the sizes and the files of one run (the check, then the install that follows it) come from the same release even if the
     * channel is changed meanwhile.
     */
    private static volatile String releaseBase = UpdateRules.BASE;

    private static void prepare(Context c, boolean download) throws Exception {
        clearReady();
        final String base = UpdateRules.baseFor(UpdateChannel.read(c));
        releaseBase = base;
        Version ver = readVersion(base + "version.txt");
        int newest = UpdateRules.buildOf(ver.text);
        synchronized (PhoneUpdater.class) { if (newest > 0) build = newest; }
        UpdateFlow.Verdict v = UpdateFlow.versionRead(ver.reached, ver.code, newest);
        if (v.result == UpdateFlow.Result.OFFLINE) { set("error", "offline", ""); return; }
        if (v.result == UpdateFlow.Result.CANNOT) { cannot(v.reason); return; }
        v = UpdateFlow.first(newest, lowestInstalled(c), canInstall(c));
        switch (v.result) {
            case OFFLINE: set("error", "offline", ""); return;
            case UP_TO_DATE: set("uptodate", "", ""); return;
            case NEED_PERMISSION: set("error", "need_permission", ""); return;
            default: break;
        }
        if (UpdateFlow.plan(v, download).result == UpdateFlow.Result.FOUND) { set("found", "", ""); return; }
        downloadAndVerify(c, false);
    }

    /**
     * Newer, and allowed: what is installed here, how big the files are, is there room, then the files themselves and their checks. thenInstall:
     * the owner already pressed «ثبّت», so the queue starts (the state goes to «waiting_owner») instead of stopping at «available». False when
     * it stopped on an error (already set).
     */
    private static boolean downloadAndVerify(Context c, boolean thenInstall) throws Exception {
        final String base = releaseBase;
        List<String> have = new ArrayList<>();
        for (String[] apk : UpdateRules.APKS) if (installedCode(c, apk[0]) >= 0) have.add(apk[0]);
        List<String> order = UpdateOrder.order(have);
        long[] sizes = new long[order.size()];
        for (int i = 0; i < sizes.length; i++) {
            long n = sizeOf(base + fileOf(order.get(i)));
            sizes[i] = n > 0 ? n : UpdateRules.MAX_APK_BYTES;   // unknown: the most a file may be
        }
        File dir = new File(c.getFilesDir(), "phone-update");
        if (!dir.isDirectory() && !dir.mkdirs()) { cannot("storage"); return false; }
        clean(dir);
        UpdateFlow.Verdict v = UpdateFlow.room(wifiOrEthernet(c), dir.getUsableSpace(), sizes);
        if (v.result == UpdateFlow.Result.CANNOT) { cannot(v.reason); return false; }

        set("downloading", "", "");
        Map<String, String> verdicts = new LinkedHashMap<>();
        List<File> files = new ArrayList<>();
        List<String> pkgs = new ArrayList<>();
        for (String pkg : order) {
            File f = new File(dir, fileOf(pkg));
            boolean got;
            try {
                got = fetch(base + fileOf(pkg), f);
            } catch (java.io.IOException e) {
                Log.w(TAG, "download failed: " + e);
                got = false;
            }
            if (!got) { clean(dir); cannot(UpdateFlow.fetchFailed().reason); return false; }   // version.txt was read: this is not «no connection»
            String why = verify(c, pkg, f);
            verdicts.put(pkg, why);
            if (why == null) { files.add(f); pkgs.add(pkg); }
        }
        v = UpdateFlow.files(verdicts);
        if (v.result != UpdateFlow.Result.AVAILABLE) { clean(dir); cannot(v.reason); return false; }
        synchronized (PhoneUpdater.class) {
            ready.clear();
            ready.addAll(files);
            readyPackages.clear();
            readyPackages.addAll(pkgs);
            if (thenInstall) {
                if (!canInstall(c)) { set("error", "need_permission", ""); return false; }
                startQueue();
                return true;
            }
        }
        set("available", "", "");
        return true;
    }

    /** The TV can't update itself: the screen shows the message about the AI assistant. The reason goes to the log. */
    private static void cannot(String why) {
        Log.w(TAG, "cannot update: " + why);
        set("error", "cannot", why);
    }

    private static String fileOf(String pkg) {
        for (String[] apk : UpdateRules.APKS) if (apk[0].equals(pkg)) return apk[1];
        return "";
    }

    private static synchronized void clearReady() {
        ready.clear();
        readyPackages.clear();
    }

    /** What reading version.txt gave: reached is false only when the connection itself failed (no network). */
    private static final class Version {
        boolean reached;
        int code;
        String text = "";
    }

    private static Version readVersion(String url) {
        Version v = new Version();
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            v.code = con.getResponseCode();
            v.reached = true;
            if (v.code != HttpURLConnection.HTTP_OK) return v;
            InputStream in = con.getInputStream();
            try {
                byte[] buf = new byte[64];   // "3.51\n": anything longer is not it
                int n = 0, r;
                while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
                v.text = new String(buf, 0, n, "UTF-8");
            } finally {
                in.close();
            }
        } catch (java.io.IOException e) {
            Log.w(TAG, "version.txt: " + e);   // reached stays as it was: false before any answer, true after
        } finally {
            if (con != null) con.disconnect();
        }
        return v;
    }

    /** The size of a file without downloading it: the first byte only, and the total from Content-Range. -1 when the server doesn't say. */
    private static long sizeOf(String url) {
        if (!UpdateRules.isReleaseUrl(url)) return -1;
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setRequestProperty("Range", "bytes=0-0");
            try {
                int code = con.getResponseCode();
                if (code == HttpURLConnection.HTTP_PARTIAL) {
                    String range = con.getHeaderField("Content-Range");   // "bytes 0-0/12345678"
                    if (range != null && range.matches("bytes 0-0/[0-9]{1,12}")) return Long.parseLong(range.substring(range.indexOf('/') + 1));
                    return -1;
                }
                return code == HttpURLConnection.HTTP_OK ? con.getContentLengthLong() : -1;
            } finally {
                con.disconnect();
            }
        } catch (Exception e) {
            return -1;
        }
    }

    // ---- install

    /** From the TV's own screen or from the phone page (fromPhone: the TV is brought to the update screen, where the owner confirms). */
    public static Map<String, Object> install(final Context c, boolean fromPhone) {
        final Context app = c.getApplicationContext();
        final boolean found;
        synchronized (PhoneUpdater.class) {
            if (busy()) return status();
            found = state.equals("found");   // the phone page only read version.txt: the files come now, with their checks
            if (!found && (!state.equals("available") || ready.isEmpty())) return status();   // the checks come first: nothing is installed that wasn't checked
            if (!canInstall(app)) {
                set("error", "need_permission", "");
                return status();
            }
            if (found) set("downloading", "", "");
            else startQueue();
        }
        if (fromPhone) {
            try {
                app.startActivity(new Intent().setClassName(app.getPackageName(), "sa.urtv.home.UpdateActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (RuntimeException ignored) { }
        }
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    if (found && !downloadAndVerify(app, true)) return;   // stopped on an error, which is already on the screen
                    installNext(app);
                } catch (Exception e) {
                    Log.w(TAG, "install failed: " + e);
                    cannot(UpdateFlow.crashed().reason);
                }
            }
        }, "tv-update").start();
        return status();
    }

    /** The checked files become the queue for the installer, and the state «waiting_owner». Called with the lock held. */
    private static void startQueue() {
        queue.clear();
        queuePackages.clear();
        for (String pkg : UpdateOrder.order(readyPackages)) {
            queue.add(ready.get(readyPackages.indexOf(pkg)));
            queuePackages.add(pkg);
        }
        confirm = null;
        confirmShown = false;
        set("waiting_owner", "", "");
    }

    /** The TV's screen where the owner allows this app to install updates; false when it can't be opened. */
    public static boolean openPermissionScreen(Context c) {
        try {
            c.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + c.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean canInstall(Context c) {
        return Build.VERSION.SDK_INT < 26 || c.getPackageManager().canRequestPackageInstalls();
    }

    /** The permission screen could not be opened: the same message as any other way the TV can't update itself. */
    public static void permissionScreenFailed() { cannot("permission_screen"); }

    /** Wi-Fi or Ethernet with internet, not mobile data and not nothing. */
    private static boolean wifiOrEthernet(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            for (Network n : cm.getAllNetworks()) {
                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                if (nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) return true;
            }
        } catch (RuntimeException ignored) { }
        return false;
    }

    private static void clean(File dir) {
        File[] all = dir.listFiles();
        if (all != null) for (File f : all) f.delete();
    }

    private static boolean fetch(String url, File out) throws Exception {
        if (!UpdateRules.isReleaseUrl(url)) return false;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(10000);
        con.setReadTimeout(30000);
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) return false;
            long len = con.getContentLength();
            if (len > UpdateRules.MAX_APK_BYTES) return false;
            long total = 0;
            InputStream in = con.getInputStream();
            OutputStream o = new FileOutputStream(out);
            try {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > UpdateRules.MAX_APK_BYTES) return false;
                    o.write(buf, 0, n);
                }
            } finally {
                o.close();
                in.close();
            }
            return total > 0;
        } finally {
            con.disconnect();
        }
    }

    // ---- the check of the file against the installed app

    private static int flags() {
        return Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_SIGNATURES : PackageManager.GET_SIGNATURES;
    }

    private static long code(PackageInfo i) {
        return Build.VERSION.SDK_INT >= 28 ? i.getLongVersionCode() : i.versionCode;
    }

    /** The installed version code of a package, or -1 when it isn't installed. */
    public static long installedCode(Context c, String pkg) {
        try {
            return code(c.getPackageManager().getPackageInfo(pkg, 0));
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    /** The version of this app as the screen and the phone page name it: «4.0 (بناء 151)» (the build is the versionCode, the name the VERSION file's). */
    public static String installedName(Context c) { return UpdateText.versionOf(BuildConfig.MARKETING, BuildConfig.VERSION_CODE); }

    /** The lowest build among our apps that are installed: the TV is up to date only when all of them are. */
    private static long lowestInstalled(Context c) {
        long low = Long.MAX_VALUE;
        for (String[] apk : UpdateRules.APKS) {
            long k = installedCode(c, apk[0]);
            if (k >= 0 && k < low) low = k;
        }
        return low == Long.MAX_VALUE ? 0 : low;
    }

    private static Set<String> signers(PackageInfo i) throws Exception {
        Signature[] sigs = null;
        if (Build.VERSION.SDK_INT >= 28 && i.signingInfo != null) sigs = i.signingInfo.getApkContentsSigners();
        if ((sigs == null || sigs.length == 0) && i.signatures != null) sigs = i.signatures;
        Set<String> out = new HashSet<>();
        if (sigs != null) for (Signature s : sigs) out.add(UpdateRules.hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())));
        return out;
    }

    /** The version code the file itself says (what the installer will make of it), or -1 when it can't be read. */
    private static long archiveCode(Context c, File f) {
        try {
            PackageInfo archive = c.getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), 0);
            return archive == null ? -1 : code(archive);
        } catch (Exception e) {
            return -1;
        }
    }

    /** Null when the file may be installed; else a word from UpdateRules.verify, or "unreadable". */
    private static String verify(Context c, String pkg, File f) {
        try {
            PackageManager pm = c.getPackageManager();
            PackageInfo archive = pm.getPackageArchiveInfo(f.getAbsolutePath(), flags());
            if (archive == null || archive.packageName == null) return "unreadable";
            PackageInfo installed = pm.getPackageInfo(pkg, flags());
            return UpdateRules.verify(pkg, archive.packageName, code(installed), code(archive), signers(installed), signers(archive));
        } catch (Exception e) {
            return "unreadable";
        }
    }

    // ---- Android's installer, one app at a time

    private static void installNext(Context c) {
        File file;
        String pkg;
        synchronized (PhoneUpdater.class) {
            if (queue.isEmpty()) {
                if (installing.isEmpty()) return;   // a late answer in a restarted process: the launcher's start reads the record and tells the owner
                installing = "";
                set("done", "", "");
                return;
            }
            file = queue.remove(0);
            pkg = queuePackages.remove(0);
            installing = pkg;
            confirm = null;
            confirmShown = false;
            set("waiting_owner", "", pkg);   // the TV shows Android's install screen: the owner confirms with the remote
        }
        // the file is checked again right before its turn: it is what gets installed
        String why = verify(c, pkg, file);
        if (why != null) { fail(c, "failed", pkg, why); return; }
        UpdateStore store = new UpdateStore(c);
        if (UpdateOrder.replacesUs(pkg)) {
            // this process dies with the launcher: the next start of the launcher reads this and tells the owner how it went
            // the target is what the file says, not what version.txt said: the screen compares it with the build that is installed then
            long target = archiveCode(c, file);
            if (target < 0 || !UpdateRecords.begin(store, (int) Math.min(target, 999_999_999L), pkg)) { fail(c, "failed", pkg, "record"); return; }
        }
        PackageInstaller installer = null;
        int id = -1;
        try {
            installer = c.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(pkg);
            id = installer.createSession(params);
            PackageInstaller.Session session = installer.openSession(id);
            try {
                OutputStream out = session.openWrite("base.apk", 0, file.length());
                InputStream in = new FileInputStream(file);
                try {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    session.fsync(out);
                } finally {
                    in.close();
                    out.close();
                }
                Intent result = new Intent(c, PhoneInstallReceiver.class).setAction(ACTION_RESULT).putExtra("pkg", pkg);
                int pi = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
                session.commit(PendingIntent.getBroadcast(c, id, result, pi).getIntentSender());
            } finally {
                session.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "session failed: " + e);
            if (installer != null && id >= 0) {
                try { installer.abandonSession(id); } catch (Exception ignored) { }   // the session is not left open
            }
            fail(c, "failed", pkg, "installer");
        }
    }

    /** A failure ends the update: nothing more is handed to the installer, and the record (if one was written) is removed, since the screen says it now. */
    private static void fail(Context c, String reason, String pkg, String why) {
        new UpdateStore(c).clear();
        Log.w(TAG, "update stopped at " + pkg + ": " + why);
        synchronized (PhoneUpdater.class) {
            queue.clear();
            queuePackages.clear();
            installing = "";
            confirm = null;
        }
        set("error", reason, pkg, why);
    }

    /** The installer's answer (PhoneInstallReceiver): the confirm screen is needed, it went through, or it failed. */
    static void onResult(Context c, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Integer.MIN_VALUE);
        String pkg = intent.getStringExtra("pkg");
        Log.i(TAG, "installer answered: status=" + status + " (-1 = the confirm screen is needed, 0 = installed without one)");   // a number only: tells a silent step from a confirmed one
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent screen = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (screen == null) { fail(c, "failed", pkg == null ? "" : pkg, "installer"); return; }
            boolean front;
            synchronized (PhoneUpdater.class) {
                confirm = screen;
                confirmShown = false;
                front = visible;
            }
            if (front) {
                set("waiting_owner", "", pkg == null ? "" : pkg);   // the update screen opens it (UpdateActivity), from the front
            } else {
                showConfirm(c);
            }
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            // the next install (an APK parse, a signature check, a copy of up to 80 MB) never runs inside the receiver's answer: it finishes first
            final Context app = c;
            new Thread(new Runnable() { @Override public void run() { installNext(app); } }, "tv-update-next").start();
        } else {
            String why = UpdateFlow.failure(status);
            if (UpdateFlow.isDeclined(why)) {
                new UpdateStore(c).clear();
                synchronized (PhoneUpdater.class) { queue.clear(); queuePackages.clear(); installing = ""; confirm = null; }
                set("error", "declined", pkg == null ? "" : pkg);
            } else {
                fail(c, "failed", pkg == null ? "" : pkg, why == null ? "failed" : why);
            }
        }
    }

    /** Opens the installer's confirm screen (from the screen in front, or from the background where Android allows it). */
    public static synchronized boolean showConfirm(Context c) {
        if (confirm == null) return false;
        try {
            c.startActivity(new Intent(confirm).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            confirmShown = true;
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "confirm screen: " + e);
            return false;
        }
    }

    /** True when the installer's confirm screen is waiting and nobody has opened it yet. */
    public static synchronized boolean confirmWaiting() { return confirm != null && !confirmShown; }

    /** True when there is a confirm screen to open again (the owner left it, or Android didn't show it). */
    public static synchronized boolean confirmKnown() { return confirm != null; }

    /** Which app the installer is working on now ("" when none). */
    public static synchronized String installingNow() { return installing; }

    /** Back to the first screen after a result has been read. */
    public static synchronized void reset() {
        if (busy()) return;
        state = "idle";
        error = "";
        detail = "";
        why = "";
    }
}
