package sa.urtv.home.setup;

import android.content.Context;
import android.os.Build;
import android.util.Base64;

import java.io.Closeable;
import java.io.IOException;

/** One connection to the TV's own adb with a {@link SetupEngine} on it: what the screen and the safety-switch receiver both use. */
final class SetupSession implements Closeable {
    /** null over the paired route (4.4 beta 3, wireless debugging through the add-on): {@link #run} and {@link #exec} are what every caller uses */
    final AdbClient adb;
    final SetupEngine engine;
    private final SetupEngine.Shell shell;
    private final Exec exec;
    private final Closeable other;

    /** a command fed the bytes of a file (an APK for `cmd package install -S`) */
    interface Exec { String exec(String command, java.io.InputStream data, long size) throws IOException; }

    private SetupSession(AdbClient adb, SetupEngine.Shell shell, Exec exec, Closeable other, SetupEngine engine) {
        this.adb = adb; this.shell = shell; this.exec = exec; this.other = other; this.engine = engine;
    }

    /** one command over this session (SetupHook.enableStock) */
    AdbClient.Result run(String command) throws IOException { return shell.run(command); }

    String exec(String command, java.io.InputStream data, long size) throws IOException { return exec.exec(command, data, size); }

    private static SetupEngine engineOn(Context c, SetupEngine.Shell shell, SetupEngine.Out out) {
        return new SetupEngine(shell, new FileStore(c), out, new SetupDeadman(c), ms -> {
            try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("interrupted"); }
        }, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date()));
    }

    /** an engine over the paired route (PairRoute: the add-on's TLS connection); {@code closer} ends it */
    static SetupSession wrap(Context c, SetupEngine.Shell shell, Exec exec, Closeable closer, SetupEngine.Out out) {
        return new SetupSession(null, shell, exec, closer, engineOn(c, shell, out));
    }

    /**
     * 4.4 beta 3 (the owner: every Google TV and Chromecast that has only «تصحيح الأخطاء اللاسلكي»): the TV's own adb on 5555 first, as always; ONLY when it does not
     * answer (closed, refused, or the prompt never accepted) and this TV was paired through the add-on, the connection over that. Fails closed: when neither answers,
     * the 5555 failure is thrown as it was, with the paired route's reason added («paired route: no_addon / not_paired / no_port / …»), so the screen says why.
     */
    static SetupSession openAny(Context c, int approvalMs, AdbClient.Approval approval, SetupEngine.Out out) throws IOException {
        try {
            return open(c, approvalMs, approval, out);
        } catch (IOException first) {
            if (String.valueOf(first.getMessage()).equals("interrupted")) throw first;
            String why = PairRoute.why(c);
            if (!"paired".equals(why)) throw new IOException(first.getMessage() + "; paired route: " + why);
            android.util.Log.i("UrTVFactory", "5555 did not answer; the paired route");
            try {
                return PairRoute.open(c, out);
            } catch (IOException second) {
                throw new IOException(first.getMessage() + "; paired route: " + second.getMessage());
            }
        }
    }

    /** approvalMs: how long to wait for the person to accept the TV's prompt (0 for the receiver: the key is known by then). */
    static SetupSession open(Context c, int approvalMs, AdbClient.Approval approval, SetupEngine.Out out) throws IOException { return open(c, "127.0.0.1", approvalMs, approval, out); }

    /** the same to another address of this TV (its own address on the network: {@link LanAddress}), 5555 as always */
    static SetupSession open(Context c, String host, int approvalMs, AdbClient.Approval approval, SetupEngine.Out out) throws IOException {
        android.util.Log.i("UrTVFactory", "session: key exists=" + AdbKeys.exists(c) + " sdk=" + Build.VERSION.SDK_INT);
        AdbClient.trace = step -> android.util.Log.i("UrTVFactory", step);   // each step of the connection, for whoever reads why it failed
        long began = System.currentTimeMillis();
        AdbClient adb;
        try {
            adb = AdbClient.connect(host, 5555, AdbKeys.load(c), Build.VERSION.SDK_INT, approvalMs, approval, b -> Base64.encodeToString(b, Base64.NO_WRAP));
        } catch (IOException e) {
            android.util.Log.w("UrTVFactory", "session failed after " + (System.currentTimeMillis() - began) + " ms: " + e.getMessage());
            throw e;
        } finally {
            AdbClient.trace = null;
        }
        android.util.Log.i("UrTVFactory", "session open after " + (System.currentTimeMillis() - began) + " ms");
        try {
            adb.setReadTimeout(60_000);   // `pm` on a slow TV
        } catch (IOException e) {
            adb.close();
            throw e;
        }
        return new SetupSession(adb, adb::run, adb::exec, null, engineOn(c, adb::run, out));
    }

    @Override public void close() {
        if (adb != null) adb.close();
        if (other != null) { try { other.close(); } catch (IOException ignored) { } }
    }

    /**
     * 4.4 (the owner's TV, step 86 B): ONE ADB key between the steps of a run. Until 4.4 it was deleted after every finished run, so every run made a new
     * key, the TV asked «السماح بتصحيح USB؟» again, and its saved list filled with urtv-home@tv keys. So after a normal end it is kept (this only says so);
     * it goes at «تراجع» and when the factory interface is given back ({@link #dropKey}), nowhere else.
     */
    static void keepKey(Context c) {
        android.util.Log.i("UrTVSetup", "adb key: kept between the steps exists=" + AdbKeys.exists(c));
    }

    /**
     * 4.4 review F20 (the owner's decision): «تراجع» finished, or the factory interface given back: the key goes (the TV trusts nothing of ours any more).
     * Not while something still needs it: a home switch waiting for its safety switch, or a full run waiting for the TV's restart or the person's «كمّل».
     */
    static void dropKey(Context c) {
        if (SetupRecovery.pending(c) || SetupRunner.awaiting(new FileStore(c))) {
            android.util.Log.i("UrTVSetup", "adb key: kept (a switch or a run still waits for it)");
            return;
        }
        AdbKeys.delete(c);
        PairRoute.forget(c);   // 4.4 beta 3: the add-on's pairing key goes too (off this thread's caller: dropKey runs on the setup's thread)
        android.util.Log.i("UrTVSetup", "adb key: deleted exists=" + AdbKeys.exists(c));
    }
}
