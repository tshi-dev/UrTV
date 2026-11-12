package sa.urtv.home.setup.pair;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Finds this TV's own wireless-debugging services on the local network (NsdManager): {@code _adb-tls-pairing._tcp} while the pairing dialog is open and
 * {@code _adb-tls-connect._tcp} once debugging is on. Only a service whose address is one of THIS device's own addresses is taken: the code is for this TV, never for another
 * device that happens to be on the network. The address is never logged.
 */
final class PairDiscovery {
    private PairDiscovery() { }

    static final String PAIRING = "_adb-tls-pairing._tcp", CONNECT = "_adb-tls-connect._tcp";

    static final class Found {
        final InetAddress host;
        final int port;
        Found(InetAddress host, int port) { this.host = host; this.port = port; }
    }

    interface Log { void line(String text); }

    static Found find(Context c, String type, long timeoutMs, final Log log) throws IOException {
        final NsdManager nsd = (NsdManager) c.getSystemService(Context.NSD_SERVICE);
        if (nsd == null) throw new IOException("no service discovery on this TV");
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<Found> got = new AtomicReference<>();
        final ArrayDeque<NsdServiceInfo> queue = new ArrayDeque<>();
        final boolean[] resolving = {false};
        final Object lock = new Object();
        final NsdManager.ResolveListener resolver = new NsdManager.ResolveListener() {
            @Override public void onResolveFailed(NsdServiceInfo info, int err) {
                log.line("resolve failed (" + err + ")");
                next();
            }
            @Override public void onServiceResolved(NsdServiceInfo info) {
                InetAddress h = info.getHost();
                boolean own = h != null && isOwn(h);
                log.line("service found: " + (own ? "on this TV" : "on another device, ignored") + ", port " + info.getPort());
                if (own) { got.set(new Found(h, info.getPort())); done.countDown(); }
                next();
            }
            private void next() {
                NsdServiceInfo n;
                synchronized (lock) { resolving[0] = false; n = queue.poll(); if (n != null) resolving[0] = true; }
                if (n != null) nsd.resolveService(n, this);
            }
        };
        NsdManager.DiscoveryListener listener = new NsdManager.DiscoveryListener() {
            @Override public void onStartDiscoveryFailed(String t, int err) { log.line("discovery failed to start (" + err + ")"); done.countDown(); }
            @Override public void onStopDiscoveryFailed(String t, int err) { }
            @Override public void onDiscoveryStarted(String t) { log.line("looking for " + t); }
            @Override public void onDiscoveryStopped(String t) { }
            @Override public void onServiceLost(NsdServiceInfo info) { }
            @Override public void onServiceFound(NsdServiceInfo info) {
                boolean start;
                synchronized (lock) { if (resolving[0]) { queue.add(info); start = false; } else { resolving[0] = true; start = true; } }
                if (start) nsd.resolveService(info, resolver);
            }
        };
        try {
            nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener);
        } catch (RuntimeException e) {
            throw new IOException("discovery: " + e.getClass().getSimpleName());
        }
        try {
            done.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        } finally {
            try { nsd.stopServiceDiscovery(listener); } catch (RuntimeException ignored) { }
        }
        Found f = got.get();
        if (f == null) throw new IOException("no " + (PAIRING.equals(type) ? "pairing" : "connect") + " service of this TV found");
        return f;
    }

    /** is this one of this device's own addresses (the loopback counts) */
    static boolean isOwn(InetAddress a) {
        if (a.isLoopbackAddress()) return true;
        try { return NetworkInterface.getByInetAddress(a) != null; } catch (SocketException e) { return false; }
    }
}
