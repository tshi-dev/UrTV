package sa.urtv.home.tilawah;

/**
 * 4.4 (a report: «the screen froze after a day of Quran»): the media session was given new metadata and a new playback state every second of play,
 * about 86,000 calls a day into the system, each one waking every controller of it (the TV's media notification, its home, the Assistant). Now the
 * metadata goes only when what it says changes, and the state only when the state, the actions or the speed change, or the position is off from where
 * the controllers already put it (they move it on themselves from the last state's time and speed): a seek, a new surah, a rebuffer. Plain Java
 * (tests/SessionPushRun.java).
 */
final class SessionPush {
    /** a position this far from the controllers' own is sent again (a seek, a rebuffer, a new track) */
    static final long DRIFT_MS = 2000;

    private String meta;
    private int state = Integer.MIN_VALUE;
    private long actions = Long.MIN_VALUE;
    private float speed = Float.NaN;
    private long pos, at;
    private boolean active;

    /** the metadata (title, artist, album, duration) as one key: true when it differs from the one sent last */
    boolean metaChanged(String title, String artist, String album, long duration) {
        String k = title + "\u0000" + artist + "\u0000" + album + "\u0000" + duration;
        if (k.equals(meta)) return false;
        meta = k;
        return true;
    }

    /** true when this playback state must be sent: something other than the moving position changed, or the position drifted from the sent one's course */
    boolean stateDue(int st, long acts, float spd, long position, long nowMs) {
        long expected = pos + (long) ((nowMs - at) * speed);
        boolean due = st != state || acts != actions || spd != speed || Math.abs(position - expected) > DRIFT_MS;
        if (!due) return false;
        state = st; actions = acts; speed = spd; pos = position; at = nowMs;
        return true;
    }

    /** setActive(true) once, not every second */
    boolean activate() {
        if (active) return false;
        active = true;
        return true;
    }

    /** a new session (or a released one): everything is sent again */
    void reset() { meta = null; state = Integer.MIN_VALUE; actions = Long.MIN_VALUE; speed = Float.NaN; active = false; }
}
