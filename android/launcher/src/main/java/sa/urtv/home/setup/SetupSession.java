package sa.urtv.home.setup;

import android.content.Context;
import android.os.Build;
import android.util.Base64;

import java.io.Closeable;
import java.io.IOException;

/** One connection to the TV's own adb with a {@link SetupEngine} on it: what the screen and the safety-switch receiver both use. */
final class SetupSession implements Closeable {
    final AdbClient adb;
    final SetupEngine engine;

    private SetupSession(AdbClient adb, SetupEngine engine) { this.adb = adb; this.engine = engine; }

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
        SetupEngine engine = new SetupEngine(adb::run, new FileStore(c), out, new SetupDeadman(c), ms -> {
            try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("interrupted"); }
        }, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date()));
        return new SetupSession(adb, engine);
    }

    @Override public void close() { adb.close(); }

    /**
     * The ADB key is a standing credential once «السماح دائماً» was accepted: it is deleted when nothing is pending (no home switch waiting for its
     * safety switch, no full run waiting for the TV to come back from a restart or for the person's «كمّل» after the Wi-Fi). The next run makes a new
     * key and the TV asks again.
     */
    static void deleteKeyIfFinished(Context c) {
        String factory = new FileStore(c).get(SetupEngine.FACTORY);
        boolean pending = SetupRecovery.pending(c) || (factory != null && factory.startsWith("on")), awaiting = SetupRunner.awaiting(new FileStore(c));   // the factory step keeps the key: its restore needs it
        if (!pending && !awaiting) AdbKeys.delete(c);
        android.util.Log.i("UrTVSetup", "adb key: " + (!pending && !awaiting ? "deleted" : "kept (" + (pending ? "home switch or factory step pending" : "run waiting") + ")")
            + " exists=" + AdbKeys.exists(c));
    }
}
