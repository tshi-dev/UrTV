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
            @Override public String exec(String command, java.io.InputStream data, long size) throws IOException { return s.adb.exec(command, data, size); }
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

    @Override public SetupInstall.Apks apks() { return new SetupApks(app); }

    @Override public String appVersion() { return sa.urtv.home.BuildConfig.MARKETING + " (build " + sa.urtv.home.BuildConfig.VERSION_CODE + ")"; }

    @Override public String asset(String name) {
        try (java.io.InputStream in = app.getAssets().open(name)) {
            return read(in, 4 * 1024 * 1024);
        } catch (IOException e) {
            return null;
        }
    }

    /** Only https, with timeouts and a size limit; null on any failure (the caller says what it skipped). */
    @Override public String fetch(String url, int maxBytes) {
        if (!url.startsWith("https://")) return null;
        java.net.HttpURLConnection con = null;
        try {
            con = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            con.setConnectTimeout(10_000);
            con.setReadTimeout(30_000);
            if (con.getResponseCode() != 200) return null;
            try (java.io.InputStream in = con.getInputStream()) { return read(in, maxBytes); }
        } catch (IOException | RuntimeException e) {
            return null;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String read(java.io.InputStream in, int max) throws IOException {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[16 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) {
            if (b.size() + n > max) throw new IOException("too big");
            b.write(buf, 0, n);
        }
        return b.toString("UTF-8");
    }
}
