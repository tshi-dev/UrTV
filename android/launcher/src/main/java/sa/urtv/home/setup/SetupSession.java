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
    static SetupSession open(Context c, int approvalMs, AdbClient.Approval approval, SetupEngine.Out out) throws IOException {
        AdbClient adb = AdbClient.connect("127.0.0.1", 5555, AdbKeys.load(c), Build.VERSION.SDK_INT, approvalMs, approval, b -> Base64.encodeToString(b, Base64.NO_WRAP));
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
     * safety switch, no full run waiting for the TV to come back from a restart). The next run makes a new key and the TV asks again.
     */
    static void deleteKeyIfFinished(Context c) {
        if (!SetupRecovery.pending(c) && !SetupRunner.awaitingRestart(new FileStore(c))) AdbKeys.delete(c);
    }
}
