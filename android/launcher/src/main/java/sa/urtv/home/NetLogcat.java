package sa.urtv.home;

import android.net.TrafficStats;

/** G3: NetLog's lines to the log (tag UrTVNet), with the bytes this app received and sent since the line before (the first: since the app started), from TrafficStats. */
final class NetLogcat implements NetLog.Logger {
    private final int uid = android.os.Process.myUid();
    private long rx = TrafficStats.getUidRxBytes(uid), tx = TrafficStats.getUidTxBytes(uid);

    @Override public synchronized void window(long minutes, int requests, String byHost) {
        long r = TrafficStats.getUidRxBytes(uid), t = TrafficStats.getUidTxBytes(uid);
        boolean known = r >= 0 && rx >= 0 && t >= 0 && tx >= 0;   // TrafficStats.UNSUPPORTED is -1
        android.util.Log.i("UrTVNet", NetLog.line(minutes, requests, byHost, known ? r - rx : -1, known ? t - tx : -1));
        rx = r; tx = t;
    }
}
