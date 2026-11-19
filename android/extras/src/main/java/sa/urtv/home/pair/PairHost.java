package sa.urtv.home.pair;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 4.4 beta 3, wireless debugging in the ADD-ON (the owner's rule: the libraries live here only, never in the home or the screensaver). The home's setup reaches it through
 * ExtrasCallProvider (a signature permission: only our home): the TV's factory page ARMS the pairing, the phone page's six digits come through the home, the pairing runs
 * on this app's own thread, and then the home's SetupEngine sends its commands here one by one over the TLS connection this app holds (open, run, exec, close).
 * Everything is called on the home's background threads (a provider call blocks the caller's thread, never a UI thread of either app). The code, an address or a
 * command's text is never a log line: fixed words only (tag UrTVPair).
 */
public final class PairHost {
    private PairHost() { }

    static final String TAG = "UrTVPair";
    private static final PairState STATE = new PairState();
    private static final Map<String, PairClient.Connection> OPEN = new HashMap<>();
    /** how many connections may be open at once (the setup uses one; a stuck caller cannot pile them up) */
    static final int MAX_OPEN = 2;

    public static boolean available() { return PairClient.available(); }

    public static boolean paired(Context c) { return PairClient.paired(c); }

    public static void arm() { STATE.arm(System.currentTimeMillis()); Log.i(TAG, "pairing armed"); }

    public static void disarm() { STATE.disarm(); Log.i(TAG, "pairing disarmed"); }

    /** "off", "armed", "pairing", "paired" or "failed" */
    public static String status() { return STATE.state(System.currentTimeMillis()); }

    /** why the last pairing failed: "no_dialog", "wrong" or "failed" */
    public static String why() { return STATE.why(); }

    public static void retry() { STATE.retry(System.currentTimeMillis()); }

    /** the six digits from the phone page (through the home): "ok" (the pairing runs on its own thread), "not_armed", "busy" or "bad_code" */
    public static String submit(final Context c, String typed) {
        final String digits = PairCode.clean(typed);
        if (digits == null) return "bad_code";
        if (PairState.PAIRING.equals(status())) return "busy";
        if (!STATE.begin(System.currentTimeMillis())) return "not_armed";
        Log.i(TAG, "pairing: a code arrived");   // the code itself is never a log line
        final Context app = c.getApplicationContext();
        try {
            new Thread(() -> {
                try {
                    PairClient.pair(app, digits, 0, line -> Log.i(TAG, line));
                    STATE.finish(true, "");
                    Log.i(TAG, "pairing: paired");
                } catch (IOException e) {
                    String why = reason(String.valueOf(e.getMessage()));
                    STATE.finish(false, why);
                    Log.w(TAG, "pairing failed: " + why);
                }
            }, "extras-pair").start();
        } catch (RuntimeException e) {
            STATE.finish(false, "failed");
            return "busy";
        }
        return "ok";
    }

    /** the failure's short word from the exception's text (never the code: PairClient never puts it in one) */
    static String reason(String message) {
        return message.contains("pairing service") ? "no_dialog" : message.contains("pairing failed") || message.contains("did not accept") ? "wrong" : "failed";
    }

    /** a connection over the paired route; its handle (a random word), or throws with a short reason: "not_paired", "no_port" (wireless debugging is off) or "refused" */
    public static String open(Context c) throws IOException {
        if (!available()) throw new IOException("old_android");
        if (!paired(c)) throw new IOException("not_paired");
        synchronized (OPEN) { if (OPEN.size() >= MAX_OPEN) throw new IOException("busy"); }
        PairClient.Connection conn;
        try {
            conn = PairClient.connect(c.getApplicationContext(), line -> Log.i(TAG, line));
        } catch (IOException e) {
            String m = String.valueOf(e.getMessage());
            throw new IOException(m.contains("service of this TV found") ? "no_port" : m.contains("not paired") ? "not_paired" : "refused");
        }
        String id = UUID.randomUUID().toString();
        synchronized (OPEN) { OPEN.put(id, conn); }
        Log.i(TAG, "connection open");
        return id;
    }

    private static PairClient.Connection get(String id) throws IOException {
        synchronized (OPEN) {
            PairClient.Connection conn = id == null ? null : OPEN.get(id);
            if (conn == null) throw new IOException("closed");
            return conn;
        }
    }

    /** one shell command, its output with the `URTV_RC=` marker (the home splits it as AdbClient.withCode) */
    public static String run(String id, String command) throws IOException { return get(id).run(command); }

    /** a command fed {@code size} bytes read from the home's own file (a one-time content address the home made for it) */
    public static String exec(Context c, String id, String command, Uri from, long size) throws IOException {
        PairClient.Connection conn = get(id);
        try (InputStream in = c.getContentResolver().openInputStream(from)) {
            if (in == null) throw new IOException("the file is not there");
            return conn.exec(command, in, size);
        }
    }

    public static void close(String id) {
        PairClient.Connection conn;
        synchronized (OPEN) { conn = id == null ? null : OPEN.remove(id); }
        if (conn != null) { conn.close(); Log.i(TAG, "connection closed"); }
    }

    /** the home's setup ended with nothing pending: the key and its mark go (the TV forgets it with «إلغاء تراخيص تصحيح USB») */
    public static void forget(Context c) {
        synchronized (OPEN) { for (PairClient.Connection conn : OPEN.values()) conn.close(); OPEN.clear(); }
        PairClient.forget(c);
        STATE.disarm();
        Log.i(TAG, "pairing forgotten");
    }
}
