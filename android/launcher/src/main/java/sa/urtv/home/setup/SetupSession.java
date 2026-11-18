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
     * 4.4 (the owner's TV, step 86 B): the ADB key is kept for good, ONE key. Until 4.4 it was deleted after every finished run, so every run made a new
     * key, the TV asked «السماح بتصحيح USB؟» again, and its saved list filled with urtv-home@tv keys; the safety switch, the home's own re-apply of the
     * HOME role and «تراجع» all need it later. It goes only when the person revokes it on the TV («إلغاء تراخيص تصحيح USB»: the TV then asks again for
     * the same key). Called after every end; it never deletes, it only says the key is there.
     */
    static void keepKey(Context c) {
        android.util.Log.i("UrTVSetup", "adb key: kept (one key for good) exists=" + AdbKeys.exists(c));
    }
}
