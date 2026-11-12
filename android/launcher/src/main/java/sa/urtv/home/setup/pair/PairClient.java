package sa.urtv.home.setup.pair;

import android.content.Context;
import android.os.Build;

import java.io.Closeable;
import java.io.IOException;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;

/**
 * Wireless-debugging PAIRING for TVs whose loopback adb never asks «السماح بتصحيح USB؟» (Android 11 and later): the person turns on «تصحيح الأخطاء اللاسلكي» and opens «إقران الجهاز
 * برمز الإقران»; this finds the pairing service (network service discovery), pairs with the six digits (SPAKE2 and TLS, libadb-android: Apache-2.0 side of its dual licence, with spake2-android under
 * LGPL-3.0: THIRD_PARTY_NOTICES.md) and then connects over {@code _adb-tls-connect._tcp}. This package is the whole feature: the one entry point is {@code PairClient}, called from
 * setup.PairRoute; take both away (and the gradle lines) and the app is as it was. Every stage is told to the log lines the caller gives; the code is never one of them.
 */
public final class PairClient {
    private PairClient() { }

    public interface Log { void line(String text); }

    /** an open connection to this TV's adbd over TLS */
    public static final class Connection implements Closeable {
        private final AbsAdbConnectionManager manager;
        private final PairShell shell;
        private Connection(AbsAdbConnectionManager m) { manager = m; shell = new PairShell(m); }
        public String run(String command) throws IOException { return shell.run(command); }
        @Override public void close() { try { manager.close(); } catch (IOException ignored) { } }
    }

    /** wireless debugging exists from Android 11 */
    public static boolean available() { return Build.VERSION.SDK_INT >= 30; }

    /** the pairing worked before with the key that is still here: a connection may be tried without a code */
    public static boolean paired(Context c) { return available() && PairKeys.paired(c); }

    /** the key and its mark go (with the rest of the setup's keys, when nothing is pending); the TV's own record goes with «إلغاء تراخيص تصحيح USB» */
    public static void forget(Context c) { PairKeys.delete(c); }

    /**
     * Pairs with the six digits. {@code port} is the pairing port when the person typed it (the dialog shows «address:port»), else 0: the service is found by name.
     * Throws IOException with a short reason (never the code); returns when the TV has accepted the pairing.
     */
    public static void pair(Context c, String digits, int port, Log log) throws IOException {
        if (PairCode.clean(digits) == null) throw new IOException("the code is not six digits");
        PairDiscovery.Found where;
        if (port > 0) {
            log.line("pairing: the port was typed");
            where = new PairDiscovery.Found(java.net.InetAddress.getLoopbackAddress(), port);
        } else {
            log.line("pairing: looking for the pairing service");
            where = PairDiscovery.find(c, PairDiscovery.PAIRING, 20_000, new PairDiscovery.Log() { @Override public void line(String t) { log.line("pairing: " + t); } });
        }
        PairManager m = new PairManager(PairKeys.load(c));
        try {
            log.line("pairing: SPAKE2 and TLS, port " + where.port);
            boolean ok = m.pair(where.host.getHostAddress(), where.port, digits);
            log.line("pairing: " + (ok ? "accepted" : "not accepted"));
            if (!ok) throw new IOException("the TV did not accept the pairing");
            PairKeys.markPaired(c);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.line("pairing failed: " + e.getClass().getSimpleName());
            throw new IOException("pairing failed (a wrong code or the dialog was closed)");
        } finally {
            try { m.close(); } catch (IOException ignored) { }
        }
    }

    /** connects over TLS to this TV's {@code _adb-tls-connect._tcp} service (the port changes each time wireless debugging is turned on) */
    public static Connection connect(Context c, Log log) throws IOException {
        if (!PairKeys.paired(c)) throw new IOException("this TV was not paired");
        PairDiscovery.Found where = PairDiscovery.find(c, PairDiscovery.CONNECT, 15_000, new PairDiscovery.Log() { @Override public void line(String t) { log.line("connect: " + t); } });
        PairManager m = new PairManager(PairKeys.load(c));
        try {
            log.line("connect: TLS to port " + where.port);
            boolean ok = m.connect(where.host.getHostAddress(), where.port);
            if (!ok && !m.isConnected()) throw new IOException("the TV did not accept the connection");
            log.line("connect: connected");
            return new Connection(m);
        } catch (IOException e) {
            try { m.close(); } catch (IOException ignored) { }
            throw e;
        } catch (Exception e) {
            try { m.close(); } catch (IOException ignored) { }
            log.line("connect failed: " + e.getClass().getSimpleName());
            throw new IOException("the connection over wireless debugging failed");
        }
    }
}
