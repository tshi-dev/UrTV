package sa.urtv.home.tilawah;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A station read by LiveFeed, served to the player as an ordinary http stream on this device only (127.0.0.1, a port the system picks): MediaPlayer reads it through the same
 * HTTP path as the stations it plays today. (A MediaDataSource of unknown size stalled the TV's vendor sniffer, tester step 33.) Answers every GET with audio/mpeg and no length,
 * from the first byte while the feed still has it, else from where the live stream is. Plain Java: tests/live_feed_test.py.
 */
final class LiveProxy {
    private final ServerSocket server;
    private final LiveFeed feed;
    private volatile boolean closed;

    private LiveProxy(ServerSocket server, LiveFeed feed) { this.server = server; this.feed = feed; }

    static LiveProxy start(LiveFeed feed) throws IOException {
        LiveProxy p = new LiveProxy(new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1")), feed);
        Thread t = new Thread(p::accept, "live-proxy");
        t.setDaemon(true);
        t.start();
        return p;
    }

    String url() { return "http://127.0.0.1:" + server.getLocalPort() + "/live.mp3"; }

    private void accept() {
        while (!closed) {
            try {
                final Socket s = server.accept();
                Thread t = new Thread(() -> serve(s), "live-proxy-client");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                return;   // closed
            }
        }
    }

    private void serve(Socket s) {
        try (Socket c = s) {
            c.setSoTimeout(10_000);
            InputStream in = c.getInputStream();
            StringBuilder head = new StringBuilder();
            int b, last4 = 0;
            while (head.length() < 8192 && (b = in.read()) >= 0) {   // the request's lines, up to the blank one
                head.append((char) b);
                last4 = (last4 << 8 | b) & 0xFFFFFFFF;
                if (last4 == 0x0D0A0D0A) break;
            }
            OutputStream out = c.getOutputStream();
            out.write(("HTTP/1.0 200 OK\r\nContent-Type: audio/mpeg\r\nCache-Control: no-cache\r\nicy-name: radio\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));
            out.flush();
            if (head.toString().startsWith("HEAD")) return;
            long pos = feed.startPosition();
            byte[] buf = new byte[8192];
            int n;
            while (!closed && (n = feed.readAt(pos, buf, 0, buf.length, 15_000)) > 0) {
                out.write(buf, 0, n);
                out.flush();
                pos += n;
            }
        } catch (IOException ignored) {
            // the player went away, or the feed failed: the connection ends, the player reports it
        }
    }

    void close() {
        closed = true;
        try { server.close(); } catch (IOException ignored) { }
        feed.close();
    }
}
