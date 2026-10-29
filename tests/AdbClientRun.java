package sa.urtv.home.setup;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Runs AdbClient (no Android) against a fake adbd that speaks the real framing and checks the real RSA signature. Prints one line per check. */
public class AdbClientRun {
    static final AdbClient.Base64Coder B64 = b -> Base64.getEncoder().encodeToString(b);

    /** A fake adbd. scenario: "accept" (the person accepts the prompt), "deny" (the connection is closed), "legacy" (Android 8 to 10: the first connection is left
     * hanging, the key is learned a moment later, only a new connection that signs gets in). */
    static final class FakeAdbd implements Runnable {
        final ServerSocket server;
        final String scenario;
        final List<BigInteger[]> known = new CopyOnWriteArrayList<>();   // {modulus, exponent}
        final AtomicInteger prompts = new AtomicInteger(), connections = new AtomicInteger();
        final List<String> commands = new CopyOnWriteArrayList<>();
        volatile boolean refuseShell, closeEarly;
        volatile int maxData = AdbClient.MAX_DATA, biggestPiece;
        final java.io.ByteArrayOutputStream received = new java.io.ByteArrayOutputStream();

        FakeAdbd(String scenario) throws IOException {
            this.scenario = scenario;
            server = new ServerSocket(0, 5, java.net.InetAddress.getLoopbackAddress());
            Thread t = new Thread(this, "fake-adbd");
            t.setDaemon(true);
            t.start();
        }
        int port() { return server.getLocalPort(); }

        public void run() {
            while (true) {
                try {
                    final Socket s = server.accept();
                    connections.incrementAndGet();
                    Thread t = new Thread(() -> { try { serve(s); } catch (Exception ignored) { } });
                    t.setDaemon(true);
                    t.start();
                } catch (IOException e) {
                    return;
                }
            }
        }

        static byte[] frame(int cmd, int a0, int a1, byte[] data) { return AdbClient.frame(cmd, a0, a1, data); }

        AdbClient.Message read(DataInputStream in) throws IOException {
            byte[] h = new byte[24];
            in.readFully(h);
            ByteBuffer b = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN);
            AdbClient.Message m = new AdbClient.Message();
            m.command = b.getInt(); m.arg0 = b.getInt(); m.arg1 = b.getInt();
            int len = b.getInt(); b.getInt(); int magic = b.getInt();
            if ((m.command ^ 0xffffffff) != magic) throw new IOException("bad magic");
            m.data = new byte[len];
            in.readFully(m.data);
            return m;
        }

        boolean verifies(byte[] sig, byte[] token) {
            for (BigInteger[] k : known) {
                byte[] plain = new BigInteger(1, sig).modPow(k[1], k[0]).toByteArray();
                byte[] want = AdbClient.paddedToken(token);
                byte[] got = new byte[256];
                for (int i = 0; i < plain.length && i < 256; i++) got[255 - i] = plain[plain.length - 1 - i];
                if (java.util.Arrays.equals(got, want)) return true;
            }
            return false;
        }

        BigInteger[] parseKey(byte[] blob) {
            String text = new String(blob, StandardCharsets.US_ASCII);
            String b64 = text.substring(0, text.indexOf(' '));
            ByteBuffer b = ByteBuffer.wrap(Base64.getDecoder().decode(b64)).order(ByteOrder.LITTLE_ENDIAN);
            b.getInt(); b.getInt();
            byte[] n = new byte[256];
            b.get(n);
            byte[] bigEndian = new byte[256];
            for (int i = 0; i < 256; i++) bigEndian[255 - i] = n[i];
            b.position(8 + 256 + 256);
            return new BigInteger[] {new BigInteger(1, bigEndian), BigInteger.valueOf(b.getInt() & 0xffffffffL)};
        }

        void serve(Socket s) throws Exception {
            DataInputStream in = new DataInputStream(s.getInputStream());
            OutputStream out = s.getOutputStream();
            AdbClient.Message m = read(in);
            if (m.command != AdbClient.CNXN) { s.close(); return; }
            byte[] token = new byte[20];
            new SecureRandom().nextBytes(token);
            out.write(frame(AdbClient.AUTH, AdbClient.AUTH_TOKEN, 0, token)); out.flush();
            boolean in_ = false;
            while (!in_) {
                m = read(in);
                if (m.command != AdbClient.AUTH) { s.close(); return; }
                if (m.arg0 == AdbClient.AUTH_SIGNATURE) {
                    if (verifies(m.data, token)) { in_ = true; break; }
                    byte[] again = new byte[20];
                    new SecureRandom().nextBytes(again);
                    token = again;
                    out.write(frame(AdbClient.AUTH, AdbClient.AUTH_TOKEN, 0, again)); out.flush();
                } else if (m.arg0 == AdbClient.AUTH_RSAPUBLICKEY) {
                    prompts.incrementAndGet();
                    BigInteger[] k = parseKey(m.data);
                    if ("deny".equals(scenario)) { s.close(); return; }
                    if ("legacy".equals(scenario)) {
                        Thread.sleep(300);
                        known.add(k);      // "Always allow": saved, but the answer is not delivered to this connection
                        Thread.sleep(60_000);
                        return;
                    }
                    known.add(k);
                    in_ = true;
                }
            }
            out.write(frame(AdbClient.CNXN, AdbClient.VERSION, maxData, "device::\0".getBytes(StandardCharsets.US_ASCII))); out.flush();
            while (true) {
                m = read(in);
                if (m.command == AdbClient.OPEN) {
                    String cmd = new String(m.data, StandardCharsets.UTF_8);
                    if (cmd.startsWith("exec:")) { exec(cmd.substring(5, cmd.indexOf('\0')), m.arg0, in, out); continue; }
                    cmd = cmd.substring("shell:".length(), cmd.indexOf('\0'));
                    commands.add(cmd);
                    int remote = 77;
                    if (refuseShell) { out.write(frame(AdbClient.CLSE, 0, m.arg0, null)); out.flush(); continue; }
                    out.write(frame(AdbClient.OKAY, remote, m.arg0, null));
                    String text = answer(cmd);
                    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                    int chunk = Math.max(1, Math.min(60_000, bytes.length / 2));   // at least two messages: the client reassembles them
                    for (int off = 0; off < bytes.length; off += chunk) {
                        out.write(frame(AdbClient.WRTE, remote, m.arg0, java.util.Arrays.copyOfRange(bytes, off, Math.min(bytes.length, off + chunk))));
                        out.flush();
                        AdbClient.Message ok = read(in);   // the client acknowledges
                        if (ok.command == AdbClient.CLSE) break;   // it gave up (too much output)
                        if (ok.command != AdbClient.OKAY) throw new IOException("no OKAY");
                    }
                    try { out.write(frame(AdbClient.CLSE, remote, m.arg0, null)); out.flush(); read(in); } catch (IOException gone) { return; }   // the client's CLSE
                } else if (m.command == AdbClient.CLSE || m.command == AdbClient.OKAY) {
                    continue;
                }
            }
        }

        /** `exec:cmd package install -S <n>`: reads n bytes from the stream (each piece acknowledged), then says Success and closes. */
        void exec(String cmd, int local, DataInputStream in, OutputStream out) throws IOException {
            commands.add("exec:" + cmd);
            int remote = 91;
            java.util.regex.Matcher sm = java.util.regex.Pattern.compile("-S (\\d+)").matcher(cmd);
            long size = sm.find() ? Long.parseLong(sm.group(1)) : 0;
            out.write(frame(AdbClient.OKAY, remote, local, null)); out.flush();
            long got = 0;
            while (got < size) {
                AdbClient.Message w = read(in);
                if (w.command != AdbClient.WRTE) continue;
                biggestPiece = Math.max(biggestPiece, w.data.length);
                received.write(w.data);
                got += w.data.length;
                if (closeEarly && got >= size / 2) {
                    out.write(frame(AdbClient.WRTE, remote, local, "Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]\n".getBytes(StandardCharsets.UTF_8)));
                    out.write(frame(AdbClient.CLSE, remote, local, null)); out.flush();
                    return;
                }
                out.write(frame(AdbClient.OKAY, remote, local, null)); out.flush();
            }
            out.write(frame(AdbClient.WRTE, remote, local, ("Success\n").getBytes(StandardCharsets.UTF_8))); out.flush();
            read(in);   // the client's OKAY
            out.write(frame(AdbClient.CLSE, remote, local, null)); out.flush();
            read(in);   // its CLSE
        }

        /** The text of a command: «echo hello» and friends; a trailing «echo URTV_RC=$?» gets the code of the line before it. */
        String answer(String cmd) {
            String rc = "";
            int i = cmd.lastIndexOf("\necho URTV_RC=$?");
            String body = cmd;
            if (i >= 0) { body = cmd.substring(0, i); rc = "SILENT".equals(body) ? "" : (body.startsWith("fail") ? "URTV_RC=3\n" : "URTV_RC=0\n"); }
            if ("SILENT".equals(body)) return "no marker here\n";
            if (body.equals("garbled")) return "x\nURTV_RC=abc\n";
            if (body.equals("huge")) { StringBuilder b = new StringBuilder(); for (int k = 0; k < 70_000; k++) b.append("0123456789012345678901234567890\n"); return b.toString(); }
            return "out(" + body + ")\r\n" + rc;
        }
    }

    static KeyPair newKey() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    static void line(String s) { System.out.println(s); }

    public static void main(String[] a) throws Exception {
        KeyPair keys = newKey();
        // 1 framing
        byte[] f = AdbClient.frame(AdbClient.OPEN, 1, 0, "shell:ls\0".getBytes(StandardCharsets.US_ASCII));
        ByteBuffer b = ByteBuffer.wrap(f).order(ByteOrder.LITTLE_ENDIAN);
        int cmd = b.getInt(), a0 = b.getInt(), a1 = b.getInt(), len = b.getInt(), sum = b.getInt(), magic = b.getInt();
        int want = 0; for (byte x : "shell:ls\0".getBytes(StandardCharsets.US_ASCII)) want += x & 0xff;
        line("frame " + (cmd == 0x4e45504f) + " " + a0 + " " + a1 + " " + len + " " + (sum == want) + " " + (magic == (cmd ^ 0xffffffff)) + " " + f.length);
        // 2 the signed block
        byte[] token = new byte[20]; for (int i = 0; i < 20; i++) token[i] = (byte) (i + 1);
        byte[] pad = AdbClient.paddedToken(token);
        boolean padOk = pad.length == 256 && pad[0] == 0 && pad[1] == 1 && pad[2] == (byte) 0xff;
        int sep = 256 - AdbClient.SHA1_PREFIX.length - 20 - 1;
        padOk &= pad[sep] == 0 && java.util.Arrays.equals(java.util.Arrays.copyOfRange(pad, sep + 1, sep + 1 + 15), AdbClient.SHA1_PREFIX)
            && java.util.Arrays.equals(java.util.Arrays.copyOfRange(pad, 256 - 20, 256), token);
        line("padded " + padOk);
        // 3 the public key blob
        byte[] blob = AdbClient.publicKeyBlob(keys.getPublic(), B64);
        String text = new String(blob, StandardCharsets.US_ASCII);
        boolean named = text.endsWith(" urtv-home@tv\0");
        ByteBuffer kb = ByteBuffer.wrap(Base64.getDecoder().decode(text.substring(0, text.indexOf(' ')))).order(ByteOrder.LITTLE_ENDIAN);
        int words = kb.getInt(), n0inv = kb.getInt();
        byte[] nLittle = new byte[256]; kb.get(nLittle);
        byte[] rrLittle = new byte[256]; kb.get(rrLittle);
        int exp = kb.getInt();
        byte[] nBig = new byte[256], rrBig = new byte[256];
        for (int i = 0; i < 256; i++) { nBig[255 - i] = nLittle[i]; rrBig[255 - i] = rrLittle[i]; }
        BigInteger n = new BigInteger(1, nBig);
        boolean keyOk = words == 64 && exp == 65537 && n.equals(((java.security.interfaces.RSAPublicKey) keys.getPublic()).getModulus())
            && BigInteger.valueOf(n0inv & 0xffffffffL).multiply(n).add(BigInteger.ONE).mod(BigInteger.ONE.shiftLeft(32)).signum() == 0
            && new BigInteger(1, rrBig).equals(BigInteger.ONE.shiftLeft(4096).mod(n));
        line("blob " + named + " " + keyOk + " " + (blob.length == text.length()));
        // 4 an unknown key, accepted on the TV: the prompt is announced, the command runs, the code is read
        FakeAdbd d = new FakeAdbd("accept");
        final boolean[] waited = {false};
        AdbClient c = AdbClient.connect("127.0.0.1", d.port(), keys, 34, 5000, () -> waited[0] = true, B64);
        AdbClient.Result r = c.run("echo hello");
        AdbClient.Result bad = c.run("fail now");
        String plain = c.shell("echo plain");
        line("accept " + waited[0] + " " + d.prompts.get() + " " + r.code + " [" + r.output.trim() + "] " + bad.code + " [" + plain.trim() + "]");
        AdbClient.Result silent = c.run("SILENT");
        AdbClient.Result garbled = c.run("garbled");
        line("noMarker " + silent.code + " " + garbled.code);
        try {
            c.run("huge");
            line("huge answered");
        } catch (IOException e) {
            line("huge " + e.getMessage());
        }
        c.close();
        // 5 the key is known now: a second connection is not asked again
        waited[0] = false;
        AdbClient c2 = AdbClient.connect("127.0.0.1", d.port(), keys, 34, 5000, () -> waited[0] = true, B64);
        c2.run("echo again");
        line("known " + waited[0] + " " + d.prompts.get() + " " + d.commands.contains("echo again\necho URTV_RC=$?"));
        c2.close();
        // 6 the person says no
        FakeAdbd deny = new FakeAdbd("deny");
        try {
            AdbClient.connect("127.0.0.1", deny.port(), keys, 34, 2000, null, B64);
            line("deny connected");
        } catch (IOException e) {
            line("deny " + e.getMessage());
        }
        // 7 debugging is off: nothing listens
        ServerSocket closed = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress());
        int port = closed.getLocalPort();
        closed.close();
        try {
            AdbClient.connect("127.0.0.1", port, keys, 34, 1000, null, B64);
            line("closed connected");
        } catch (IOException e) {
            line("closed " + e.getMessage().startsWith("debugging is off on the TV"));
        }
        // 8 a refused command is an error, not an empty answer
        d.refuseShell = true;
        AdbClient c3 = AdbClient.connect("127.0.0.1", d.port(), keys, 34, 5000, null, B64);
        try {
            c3.run("echo x");
            line("refused answered");
        } catch (IOException e) {
            line("refused " + e.getMessage());
        }
        c3.close();
        // 8b exec: an APK's bytes streamed to `cmd package install -S`, in pieces no bigger than the TV's message size, each acknowledged
        FakeAdbd small = new FakeAdbd("accept");
        small.maxData = 4096;
        AdbClient c5 = AdbClient.connect("127.0.0.1", small.port(), keys, 34, 5000, null, B64);
        byte[] apk = new byte[100_003];
        new java.util.Random(7).nextBytes(apk);
        String said = c5.exec("cmd package install -r -S " + apk.length, new java.io.ByteArrayInputStream(apk), apk.length);
        AdbClient.Result after = c5.run("echo after");
        line("exec " + said.trim() + " " + java.util.Arrays.equals(small.received.toByteArray(), apk) + " " + small.biggestPiece + " " + c5.maxPayload() + " "
            + small.commands.contains("exec:cmd package install -r -S 100003") + " " + after.code);
        c5.close();
        // 8c the TV closes the stream half way (it ran out of room): an error that carries what it said, not a success
        FakeAdbd early = new FakeAdbd("accept");
        early.maxData = 4096; early.closeEarly = true;
        AdbClient c6 = AdbClient.connect("127.0.0.1", early.port(), keys, 34, 5000, null, B64);
        try {
            c6.exec("cmd package install -r -S " + apk.length, new java.io.ByteArrayInputStream(apk), apk.length);
            line("execEarly answered");
        } catch (IOException e) {
            line("execEarly " + e.getMessage().contains("INSUFFICIENT_STORAGE"));
        }
        c6.close();
        // 8d a file shorter than the size it claimed: an error, never a half install called done
        AdbClient c7 = AdbClient.connect("127.0.0.1", d.port(), keys, 34, 5000, null, B64);
        try {
            c7.exec("cmd package install -r -S 5000", new java.io.ByteArrayInputStream(new byte[1000]), 5000);
            line("execShort answered");
        } catch (IOException e) {
            line("execShort " + e.getMessage());
        }
        c7.close();
        // 9 Android 8 to 10: the first connection hangs, a new one that only signs gets in
        FakeAdbd legacy = new FakeAdbd("legacy");
        long t0 = System.currentTimeMillis();
        final boolean[] w2 = {false};
        AdbClient c4 = AdbClient.connect("127.0.0.1", legacy.port(), keys, 29, 20_000, () -> w2[0] = true, B64);
        AdbClient.Result r4 = c4.run("echo legacy");
        line("legacy " + w2[0] + " " + (legacy.connections.get() >= 2) + " " + r4.code + " " + ((System.currentTimeMillis() - t0) < 15_000));
        c4.close();
    }
}
