package sa.urtv.screensaver;

/**
 * 4.4 beta 4: asks the network for the time when the screensaver starts, at most once a day (the time of the last answer is kept in the app's
 * preferences, so a new process does not ask again), and keeps ClockSkew's answer for the page
 * (NowPlayingBridge.clockOff). Over plain http to the host the apps already use (raw.githubusercontent.com answers http with a redirect and its
 * «Date:»): a TV whose clock is a year off fails every https handshake (the certificates are «not yet valid»), so https could not tell. Net's own
 * socket, so the request is counted in the request log like the others; nothing is sent but the request line and Net's generic «urtv/2 (Android
 * TV)» agent: no id, no address of the TV, no setting.
 */
final class ClockCheck {
    private ClockCheck() {}

    static final String URL = "http://raw.githubusercontent.com/";
    static final long EVERY_MS = 24 * 3600_000L;
    private static volatile int state = -1;
    private static volatile long askedAt;
    private static final String AT = "clockCheckAt", OFF = "clockCheckOff";

    /** 1 off, 0 right, -1 not known (yet, or no network) */
    static int state() { return state; }

    static void start(android.content.Context c) {
        final android.content.SharedPreferences sp = c.getApplicationContext().getSharedPreferences("clock_check", android.content.Context.MODE_PRIVATE);
        long now = android.os.SystemClock.elapsedRealtime(), last = sp.getLong(AT, 0), wall = System.currentTimeMillis();
        if (state < 0 && last > 0) state = sp.getInt(OFF, -1);   // the last answer, until it is asked again
        if (askedAt != 0 && now - askedAt < EVERY_MS) return;
        if (last > 0 && wall - last >= 0 && wall - last < EVERY_MS) { askedAt = now; return; }   // answered less than a day ago (another process)
        askedAt = now;
        Thread t = new Thread(() -> {
            try {
                Net.Reply r = Net.get(URL, null, 4000, 8000, 64 * 1024);
                int s = ClockSkew.judge(System.currentTimeMillis(), ClockSkew.parse(r.headers.get("date")));
                if (s >= 0) { state = s; sp.edit().putLong(AT, System.currentTimeMillis()).putInt(OFF, s).apply(); }
                if (s == 1) android.util.Log.i("UrTVSaver", "clock: the TV's clock is more than 12 h from the network's");
            } catch (java.io.IOException | RuntimeException e) {
                askedAt = 0;   // no network now: asked again at the next start
            }
        }, "clock-check");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
}
