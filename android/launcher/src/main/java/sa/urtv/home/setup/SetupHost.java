package sa.urtv.home.setup;

import android.content.Context;

import java.io.IOException;

/**
 * The runner on the TV: its connection is the TV's own adb (SetupSession), its files are the app's private setup folder (FileStore), and its thread
 * runs inside a foreground service (SetupService) so that closing the setup screen does not end it. After every end the ADB key is deleted when
 * nothing is pending (no home switch waiting for its safety switch, no full run waiting for the TV to come back from a restart).
 */
final class SetupHost implements SetupRunner.Host {
    private final Context app;

    SetupHost(Context c) { app = c.getApplicationContext(); }

    @Override public SetupRunner.Session open(int approvalMs, AdbClient.Approval approval, SetupEngine.Out out) throws IOException {
        final SetupSession s = SetupSession.open(app, approvalMs, approval, out);
        return new SetupRunner.Session() {
            @Override public SetupEngine engine() { return s.engine; }
            @Override public SetupEngine.Shell shell() { return s.adb::run; }
            @Override public void close() { s.close(); }
        };
    }

    @Override public SetupEngine.Store store() { return new FileStore(app); }

    @Override public void spawn(Runnable job) { SetupService.run(app, job); }

    @Override public void finished() { SetupSession.deleteKeyIfFinished(app); }

    @Override public void sleep(int ms) throws IOException {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("interrupted"); }
    }

    @Override public long now() { return System.currentTimeMillis(); }
}
