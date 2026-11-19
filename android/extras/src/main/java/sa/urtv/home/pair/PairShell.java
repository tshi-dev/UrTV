package sa.urtv.home.pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbInputStream;
import io.github.muntashirakon.adb.AdbStream;

/** One shell command over the paired (TLS) connection, answered as the engine expects: the output and the exit code (the same `echo URTV_RC=$?` marker as AdbClient.run). */
public final class PairShell {
    private final AbsAdbConnectionManager manager;

    PairShell(AbsAdbConnectionManager manager) { this.manager = manager; }

    /**
     * 4.4 beta 3: a command that reads {@code size} bytes of {@code data} (an APK for `cmd package install -S`), as AdbClient.exec does over the loopback:
     * the bytes go in, then what the TV printed comes back. Over the TLS connection of the paired route.
     */
    public String exec(String command, java.io.InputStream data, long size) throws IOException {
        try (AdbStream s = manager.openStream("exec:" + command)) {
            java.io.OutputStream out = s.openOutputStream();
            byte[] buf = new byte[64 * 1024];
            long sent = 0;
            while (sent < size) {
                int n = data.read(buf, 0, (int) Math.min(buf.length, size - sent));
                if (n < 0) throw new IOException("the file ended before its size");
                out.write(buf, 0, n);
                sent += n;
            }
            out.flush();
            AdbInputStream in = s.openInputStream();
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try {
                int n;
                while ((n = in.read(buf)) != -1) {
                    result.write(buf, 0, n);
                    if (result.size() > 64 * 1024) break;   // an install says a line or two
                }
            } catch (IOException e) {
                if (!String.valueOf(e.getMessage()).startsWith("Stream closed")) throw e;
            }
            return result.toString("UTF-8");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        }
    }

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
