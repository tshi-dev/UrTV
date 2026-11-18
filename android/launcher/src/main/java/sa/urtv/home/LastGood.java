package sa.urtv.home;

/**
 * Reads that can fail for a while (the screensaver's settings: the TV kills its process and will not start it from the background): the last
 * good value is kept and handed out, marked stale, while a read fails; a stale value may be shown but must never be taken for a fresh answer
 * (it must not become a remembered place or decide the adhan alarm). Reads within shareMs of the last good one share it. No Android: tests/PlaceMemoryRun.java.
 */
public final class LastGood<T> {
    public interface Query<T> { T ask(); }   // null: no answer

    public static final class Read<T> {
        public final T value;
        public final boolean stale;
        Read(T value, boolean stale) { this.value = value; this.stale = stale; }
    }

    private T last;
    private long lastAt;

    /** the last good value, never a query (null when there was none) */
    public synchronized T peek() { return last; }

    /** 4.5: the next read asks again whatever the time (a setting just changed: Prefs.LOOK_CHANGED); the last answer stays for peek */
    public synchronized void expire() { expired = true; }
    private boolean expired;

    public synchronized Read<T> read(long now, long shareMs, Query<T> query) {
        if (!expired && last != null && now - lastAt >= 0 && now - lastAt < shareMs) return new Read<>(last, false);
        expired = false;
        T v = query.ask();
        if (v != null) { last = v; lastAt = now; return new Read<>(v, false); }
        return new Read<>(last, true);   // the last good one (null when there never was one)
    }
}
