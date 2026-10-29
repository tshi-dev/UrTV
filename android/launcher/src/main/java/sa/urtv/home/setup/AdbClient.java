/*
 * Adapted from Free My TV, app/src/io/freemytv/Adb.java (https://github.com/simbasjungles/free-my-tv), MIT License,
 * Copyright (c) 2026 Free My TV contributors. The licence text is in THIRD_PARTY_NOTICES.md at the root of this repository.
 * What UrTV changed: no Android classes (the key, the Base64 coder and the SDK number are passed in, so tests/AdbClientRun.java runs it on a
 * plain JVM against a fake adbd), a host and port that can be given, an exit code for every command (shell protocol v1 does not carry one),
 * and plain English errors for the screen to map.
 */
package sa.urtv.home.setup;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;

/**
 * An ADB client that talks to the TV's OWN adb daemon at 127.0.0.1:5555. Be clear about what that port is: while USB debugging is on, adbd
 * listens on TCP 5555 on the whole LAN, not only on the TV itself, so another device on the Wi-Fi can reach it and make the TV show the
 * "Allow USB debugging?" prompt; it cannot get in unless the person accepts. Once "Always allow" is accepted, the key this app presents is a
 * standing credential (see {@link SetupSession}: it is deleted after a finished setup or undo). Only the setup screen uses this class, and
 * only with fixed commands built by {@link SetupEngine} (no text from outside). Package-private on purpose.
 * Protocol: https://android.googlesource.com/platform/packages/modules/adb/+/refs/heads/main/protocol.txt
 */
final class AdbClient implements Closeable {
    static final int CNXN = 0x4e584e43, AUTH = 0x48545541, OPEN = 0x4e45504f, OKAY = 0x59414b4f,
            CLSE = 0x45534c43, WRTE = 0x45545257, STLS = 0x534c5453;
    static final int VERSION = 0x01000001, MAX_DATA = 256 * 1024;
    static final int AUTH_TOKEN = 1, AUTH_SIGNATURE = 2, AUTH_RSAPUBLICKEY = 3;
    /** Start of the PKCS#1 block of a SHA-1 signature (adbd treats the token as if it were the digest). */
    static final byte[] SHA1_PREFIX = {0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};
    static final String KEY_NAME = "urtv-home@tv";
    /** A command that prints more than this is cut off with an error (a runaway `logcat` or `dumpsys` must not fill the TV's memory). */
    static final int MAX_OUTPUT = 2 * 1024 * 1024;

    /** Notified when the TV is showing the permission prompt (so the screen can explain what to do). */
    interface Approval { void waiting(); }

    /** android.util.Base64 on the TV, java.util.Base64 in the tests. */
    interface Base64Coder { String encode(byte[] bytes); }

    /** The result of one command: its output (stdout and stderr together) and its exit code (-1 when the TV did not say). */
    static final class Result {
        final String output;
        final int code;
        Result(String output, int code) { this.output = output; this.code = code; }
    }

    static final class Message {
        int command, arg0, arg1;
        byte[] data;
    }

    private final Socket socket;
    private final DataInputStream in;
    private final OutputStream out;
    private int nextLocalId = 1;
    /** The most data one message may carry, as the TV said in its CNXN (older adbd: 4096); a stream is cut in pieces of this size. */
    private int maxPayload = 4096;

    private AdbClient(Socket socket) throws IOException {
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = socket.getOutputStream();
    }

    /**
     * Connects and authenticates. If the TV does not know our key yet, waits up to {@code approvalMs} for the person to accept.
     * {@code sdk}: Android 8 to 10 hand the answer to the first connection that offered a key, so they are handled by polling (#221 of Free My TV).
     */
    static AdbClient connect(String host, int port, KeyPair keys, int sdk, int approvalMs, Approval approval, Base64Coder b64) throws IOException {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, port), 3000);
        } catch (IOException e) {
            s.close();
            throw new IOException("debugging is off on the TV (port " + port + " closed)");
        }
        AdbClient adb;
        try {
            s.setSoTimeout(10_000);
            adb = new AdbClient(s);
        } catch (IOException | RuntimeException e) {
            s.close();
            throw e;
        }
        try {
            adb.send(CNXN, VERSION, MAX_DATA, "host::\0".getBytes(StandardCharsets.US_ASCII));
            boolean signed = false, offeredKey = false;
            while (true) {
                Message m;
                try {
                    m = adb.read();
                } catch (EOFException | SocketTimeoutException e) {
                    throw new IOException(offeredKey ? "debugging permission was not accepted on the TV" : "the TV's ADB did not respond");
                }
                if (m.command == CNXN) {
                    s.setSoTimeout(20_000);
                    adb.payload(m.arg1);
                    return adb;
                }
                if (m.command == STLS) throw new IOException("the TV asked for TLS (wireless debugging); use USB debugging");
                if (m.command != AUTH || m.arg0 != AUTH_TOKEN) throw new IOException("unexpected reply from ADB");
                if (!signed) {
                    adb.send(AUTH, AUTH_SIGNATURE, 0, sign(keys.getPrivate(), m.data));
                    signed = true;
                } else if (!offeredKey) {
                    // Unknown key: send the public one; the TV shows "Allow USB debugging?".
                    adb.send(AUTH, AUTH_RSAPUBLICKEY, 0, publicKeyBlob(keys.getPublic(), b64));
                    offeredKey = true;
                    if (approval != null) approval.waiting();
                    if (sdk >= 30) {
                        s.setSoTimeout(approvalMs);
                    } else {
                        // Android 8 to 10 hand the answer to the first connection that offered a key while it stays open (often the
                        // computer's adb). "Always allow" saves our key, so a new connection that only signs gets in.
                        long until = System.currentTimeMillis() + approvalMs;
                        while (adb.in.available() == 0) {
                            if (System.currentTimeMillis() > until) throw new IOException("debugging permission was not accepted on the TV");
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IOException("interrupted");
                            }
                            AdbClient fresh = signedIn(host, port, keys);
                            if (fresh != null) {
                                adb.close();
                                return fresh;
                            }
                        }
                    }
                } else {
                    throw new IOException("the TV rejected the app's key");
                }
            }
        } catch (IOException | RuntimeException e) {
            adb.close();
            throw e;
        }
    }

    /** A new connection that only signs (it never offers the key, so no second prompt); null while the TV does not know it. */
    private static AdbClient signedIn(String host, int port, KeyPair keys) {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, port), 3000);
            s.setSoTimeout(5000);
            AdbClient adb = new AdbClient(s);
            adb.send(CNXN, VERSION, MAX_DATA, "host::\0".getBytes(StandardCharsets.US_ASCII));
            Message m = adb.read();
            if (m.command == AUTH && m.arg0 == AUTH_TOKEN) {
                adb.send(AUTH, AUTH_SIGNATURE, 0, sign(keys.getPrivate(), m.data));
                m = adb.read();
            }
            if (m.command == CNXN) {
                s.setSoTimeout(20_000);
                adb.payload(m.arg1);
                return adb;
            }
        } catch (IOException | RuntimeException ignored) {
        }
        try { s.close(); } catch (IOException ignored) { }
        return null;
    }

    private void payload(int theirs) { maxPayload = theirs <= 0 ? 4096 : Math.min(theirs, MAX_DATA); }

    int maxPayload() { return maxPayload; }

    /**
     * Runs a command through the `exec:` service with {@code size} bytes of {@code data} as its standard input, and returns what it printed: how
     * `adb install` hands an APK to `cmd package install -S <size>` without a file on the TV. The bytes go in pieces of the TV's message size, each
     * one only after the TV acknowledged the last (ADB's flow control); a stream that ends early, or a TV that closes it, is an error.
     */
    String exec(String command, java.io.InputStream data, long size) throws IOException {
        int local = nextLocalId++;
        send(OPEN, local, 0, ("exec:" + command + "\0").getBytes(StandardCharsets.UTF_8));
        int remote = 0;
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        while (remote == 0) {
            Message m = read();
            if (m.arg1 != local) continue;
            if (m.command == OKAY) remote = m.arg0;
            else if (m.command == CLSE) throw new IOException("ADB refused the command");
        }
        byte[] buf = new byte[maxPayload];
        long sent = 0;
        while (sent < size) {
            int want = (int) Math.min(buf.length, size - sent), got = 0;
            while (got < want) {
                int n = data.read(buf, got, want - got);
                if (n < 0) throw new IOException("the file ended before its size");
                got += n;
            }
            send(WRTE, local, remote, got == buf.length ? buf : java.util.Arrays.copyOf(buf, got));
            sent += got;
            boolean acked = false;
            while (!acked) {   // the TV's OKAY for this piece; it may also say something meanwhile
                Message m = read();
                if (m.arg1 != local) continue;
                if (m.command == OKAY) acked = true;
                else if (m.command == WRTE) { collect(result, m); send(OKAY, local, m.arg0, null); }
                else if (m.command == CLSE) { send(CLSE, local, m.arg0, null); throw new IOException("the TV closed the stream after " + sent + " bytes: " + result.toString("UTF-8").trim()); }
            }
        }
        while (true) {   // its answer, until it closes
            Message m = read();
            if (m.arg1 != local) continue;
            if (m.command == WRTE) { collect(result, m); send(OKAY, local, m.arg0, null); }
            else if (m.command == CLSE) { send(CLSE, local, m.arg0, null); return result.toString("UTF-8"); }
        }
    }

    private static void collect(ByteArrayOutputStream result, Message m) throws IOException {
        result.write(m.data);
        if (result.size() > MAX_OUTPUT) throw new IOException("the command printed too much");
    }

    /** Sets how long one read may wait for the TV (a slow `pm` command). */
    void setReadTimeout(int ms) throws IOException { socket.setSoTimeout(ms); }

    /** Runs a command in the TV's shell and returns its output (stdout and stderr together). */
    String shell(String command) throws IOException {
        int local = nextLocalId++;
        send(OPEN, local, 0, ("shell:" + command + "\0").getBytes(StandardCharsets.UTF_8));
        int remote = 0;
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        while (true) {
            Message m = read();
            if (m.arg1 != local) continue;
            if (m.command == OKAY) {
                remote = m.arg0;
            } else if (m.command == WRTE) {
                result.write(m.data);
                if (result.size() > MAX_OUTPUT) throw new IOException("the command printed too much");
                send(OKAY, local, m.arg0, null);
            } else if (m.command == CLSE) {
                if (remote == 0 && m.arg0 == 0) throw new IOException("ADB refused the command");
                send(CLSE, local, m.arg0, null);
                return result.toString("UTF-8");
            }
        }
    }

    /**
     * Like {@link #shell}, plus the command's exit code: shell protocol v1 does not carry one, so the command is followed by an echo of {@code $?}.
     * A missing or garbled marker is code -1 (the TV did not say), never 0.
     */
    Result run(String command) throws IOException {
        String text = shell(command + "\necho URTV_RC=$?");
        String marker = "URTV_RC=";
        int i = text.lastIndexOf(marker);
        if (i < 0) return new Result(text, -1);
        String rest = text.substring(i + marker.length()).trim();
        int code;
        try {
            code = Integer.parseInt(rest);
        } catch (NumberFormatException e) {
            return new Result(text.substring(0, i), -1);
        }
        return new Result(text.substring(0, i), code);
    }

    @Override public void close() {
        try { socket.close(); } catch (IOException ignored) { }
    }

    private Message read() throws IOException {
        byte[] header = new byte[24];
        in.readFully(header);
        ByteBuffer b = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        Message m = new Message();
        m.command = b.getInt();
        m.arg0 = b.getInt();
        m.arg1 = b.getInt();
        int length = b.getInt();
        b.getInt(); // checksum (newer adbd does not check it)
        int magic = b.getInt();
        if ((m.command ^ 0xffffffff) != magic) throw new IOException("invalid ADB message");
        if (length < 0 || length > 1024 * 1024) throw new IOException("ADB message too large");
        m.data = new byte[length];
        in.readFully(m.data);
        return m;
    }

    private void send(int command, int arg0, int arg1, byte[] data) throws IOException {
        out.write(frame(command, arg0, arg1, data));
        out.flush();
    }

    /** One ADB message: a 24-byte little-endian header (command, arg0, arg1, length, checksum, command XOR 0xffffffff), then the data. */
    static byte[] frame(int command, int arg0, int arg1, byte[] data) {
        int length = data == null ? 0 : data.length;
        int sum = 0;
        if (data != null) for (byte x : data) sum += x & 0xff;
        ByteBuffer b = ByteBuffer.allocate(24 + length).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(command).putInt(arg0).putInt(arg1).putInt(length).putInt(sum).putInt(command ^ 0xffffffff);
        if (data != null) b.put(data);
        return b.array();
    }

    /** The token padded as a PKCS#1 v1.5 block with the SHA-1 prefix, signed with the private key (RSA, no further padding). */
    static byte[] sign(PrivateKey key, byte[] token) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(paddedToken(token));
        } catch (GeneralSecurityException e) {
            throw new IOException("ADB signature: " + e.getMessage());
        }
    }

    static byte[] paddedToken(byte[] token) {
        byte[] block = new byte[256];
        block[1] = 1;
        int separator = block.length - SHA1_PREFIX.length - token.length - 1;
        for (int i = 2; i < separator; i++) block[i] = (byte) 0xff;
        System.arraycopy(SHA1_PREFIX, 0, block, separator + 1, SHA1_PREFIX.length);
        System.arraycopy(token, 0, block, separator + 1 + SHA1_PREFIX.length, token.length);
        return block;
    }

    /** Public key in adbd's format (the RSAPublicKey struct in base64, plus a name, NUL-terminated). */
    static byte[] publicKeyBlob(PublicKey key, Base64Coder b64) {
        RSAPublicKey rsa = (RSAPublicKey) key;
        BigInteger n = rsa.getModulus();
        BigInteger r32 = BigInteger.ONE.shiftLeft(32);
        BigInteger n0inv = r32.subtract(n.mod(r32).modInverse(r32));
        BigInteger rr = BigInteger.ONE.shiftLeft(4096).mod(n);
        ByteBuffer b = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(64);
        b.putInt(n0inv.intValue());
        b.put(littleEndian(n, 256));
        b.put(littleEndian(rr, 256));
        b.putInt(rsa.getPublicExponent().intValue());
        String text = b64.encode(b.array()) + " " + KEY_NAME + "\0";
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] littleEndian(BigInteger value, int size) {
        byte[] big = value.toByteArray();
        byte[] little = new byte[size];
        for (int i = 0; i < size && i < big.length; i++) little[i] = big[big.length - 1 - i];
        return little;
    }
}
