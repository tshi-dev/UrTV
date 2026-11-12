package sa.urtv.home.setup.pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbInputStream;
import io.github.muntashirakon.adb.AdbStream;

/** One shell command over the paired (TLS) connection, answered as the engine expects: the output and the exit code (the same `echo URTV_RC=$?` marker as AdbClient.run). */
public final class PairShell {
    private final AbsAdbConnectionManager manager;

    PairShell(AbsAdbConnectionManager manager) { this.manager = manager; }

    public String run(String command) throws IOException {
        try (AdbStream s = manager.openStream("shell:" + command + "\necho URTV_RC=$?")) {
            AdbInputStream in = s.openInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            try {
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    if (out.size() > 2 * 1024 * 1024) throw new IOException("the command printed too much");
                }
            } catch (IOException e) {
                if (!String.valueOf(e.getMessage()).startsWith("Stream closed")) throw e;   // the TV closed it after its last word: the end
            }
            return out.toString("UTF-8");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        }
    }
}
