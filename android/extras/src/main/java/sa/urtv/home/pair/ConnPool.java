package sa.urtv.home.pair;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The open connections of the paired route, at most {@code max}. A handle is closed by its caller (PairHost.close / forget); one the caller never closed (the home was
 * killed mid-setup) goes when the pool is full (review F95). Review F131: only an IDLE one goes (the oldest not in a call: {@link #use} marks a call, {@link #done} ends it);
 * when every connection is in a call the new one is refused ({@code add} says false: «busy»), so a third caller never cuts a command of the first two. The size check and the
 * insert are one step under one lock. No Android in here: tests/ConnPoolRun.java runs it.
 */
final class ConnPool<T> {
    interface Closer<T> { void close(T t); }

    private static final class Entry<T> { final T conn; int calls; Entry(T c) { conn = c; } }

    private final Map<String, Entry<T>> open = new LinkedHashMap<>();   // in the order of last use: the first idle one is the oldest idle
    private final int max;
    private final Closer<T> closer;

    ConnPool(int max, Closer<T> closer) { this.max = max; this.closer = closer; }

    /** adds it; when full, the oldest idle connection is closed and dropped; false (nothing added) when every one is in a call */
    boolean add(String id, T conn) {
        T evicted = null;
        synchronized (open) {
            if (open.size() >= max) {
                Iterator<Map.Entry<String, Entry<T>>> it = open.entrySet().iterator();
                boolean found = false;
                while (it.hasNext()) {
                    Map.Entry<String, Entry<T>> e = it.next();
                    if (e.getValue().calls == 0) { evicted = e.getValue().conn; it.remove(); found = true; break; }
                }
                if (!found) return false;
            }
            open.put(id, new Entry<>(conn));
        }
        if (evicted != null) closer.close(evicted);
        return true;
    }

    /** the connection for one call (marked in a call, and the most recently used); null when there is none */
    T use(String id) {
        synchronized (open) {
            Entry<T> e = id == null ? null : open.remove(id);
            if (e == null) return null;
            e.calls++;
            open.put(id, e);
            return e.conn;
        }
    }

    /** the call ended */
    void done(String id) {
        synchronized (open) { Entry<T> e = id == null ? null : open.get(id); if (e != null && e.calls > 0) e.calls--; }
    }

    void remove(String id) {
        T c = null;
        synchronized (open) { Entry<T> e = id == null ? null : open.remove(id); if (e != null) c = e.conn; }
        if (c != null) closer.close(c);
    }

    void clear() {
        java.util.List<T> all = new java.util.ArrayList<>();
        synchronized (open) { for (Entry<T> e : open.values()) all.add(e.conn); open.clear(); }
        for (T c : all) closer.close(c);
    }

    int size() { synchronized (open) { return open.size(); } }
}
