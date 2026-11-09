package sa.urtv.home.tilawah;

/**
 * 4.3 A2: the live station's stall watchdog. A station that runs dry (a rebuffer) for STALL_MS, or whose position stands still for FROZEN_MS while
 * it says it plays, is restarted quietly (the same station, no error said, the person hears it come back) instead of staying silent. At most
 * MAX_RESTARTS within WINDOW_MS: past that it is a real failure and the player says so. Plain Java: the service asks it once a second.
 */
final class StallWatch {
    static final long STALL_MS = 12_000, FROZEN_MS = 15_000, WINDOW_MS = 120_000;
    static final int MAX_RESTARTS = 3;

    enum Verdict { OK, RESTART, GIVE_UP }

    private long lastPos = -1, lastMoveAt = -1;
    private final long[] restarts = new long[MAX_RESTARTS];
    private int n;

    /** A new station (or the person's own pick): nothing is held against it. */
    void reset() { lastPos = -1; lastMoveAt = -1; n = 0; }

    /** A new player for the same station (after a quiet restart): the position starts again, the count of restarts stays. */
    void newPlayer() { lastPos = -1; lastMoveAt = -1; }

    /**
     * now: elapsedRealtime. live: a station is on and the person wants it to play. playing/prepared: the player's state. rebufferSince: when the
     * current rebuffer began (0: none). position: the player's position in ms.
     */
    Verdict check(long now, boolean live, boolean prepared, boolean playing, long rebufferSince, long position) {
        if (!live || !prepared) { lastPos = -1; lastMoveAt = -1; return Verdict.OK; }
        boolean stalled = rebufferSince > 0 && now - rebufferSince >= STALL_MS;
        if (!stalled && playing && rebufferSince == 0) {
            if (position != lastPos) { lastPos = position; lastMoveAt = now; }
            else if (lastMoveAt >= 0 && now - lastMoveAt >= FROZEN_MS) stalled = true;
        } else if (rebufferSince > 0) {
            lastMoveAt = now;   // a rebuffer is timed by its own start, not by the position
        }
        if (!stalled) return Verdict.OK;
        int recent = 0;
        for (int i = 0; i < n; i++) if (now - restarts[i] < WINDOW_MS) recent++;
        if (recent >= MAX_RESTARTS) return Verdict.GIVE_UP;
        long[] keep = new long[MAX_RESTARTS];
        int k = 0;
        for (int i = 0; i < n; i++) if (now - restarts[i] < WINDOW_MS) keep[k++] = restarts[i];
        keep[k++] = now;
        System.arraycopy(keep, 0, restarts, 0, k);
        n = k;
        lastPos = -1;
        lastMoveAt = -1;
        return Verdict.RESTART;
    }

    /** A live HLS address (the path ends in .m3u8, a query after it allowed): Playback gives it to ExoPlayer. */
    static boolean isHls(String url) {
        if (url == null) return false;
        String path = url.split("[?#]", 2)[0].toLowerCase(java.util.Locale.ROOT);
        return path.endsWith(".m3u8");
    }

    /** How many quiet restarts are counted now (for `optimize/music.sh status`). */
    int restarts() { return n; }
}
