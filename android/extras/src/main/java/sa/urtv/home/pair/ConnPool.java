package sa.urtv.home.pair;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The open connections of the paired route, at most {@code max}. A handle is closed by its caller (PairHost.close / forget); one the caller never closed (the home was
 * killed mid-setup) is the oldest: when the pool is full the next add closes and drops the oldest, so a dead caller can never leave every open answering «busy» (review F95).
 * The size check and the insert are one step under one lock. No Android in here: tests/ConnPoolRun.java runs it.
 */
final class ConnPool<T> {
    interface Closer<T> { void close(T t); }

    private final Map<String, T> open = new LinkedHashMap<>();
    private final int max;
    private final Closer<T> closer;

    ConnPool(int max, Closer<T> closer) { this.max = max; this.closer = closer; }

    /** adds it; returns the evicted handle's connection closed, or nothing: the oldest goes when full */
    void add(String id, T conn) {
        T evicted = null;
        synchronized (open) {
            if (open.size() >= max) {
                Iterator<Map.Entry<String, T>> it = open.entrySet().iterator();
                evicted = it.next().getValue();
                it.remove();
            }
            open.put(id, conn);
        }
        if (evicted != null) closer.close(evicted);
    }

    T get(String id) { synchronized (open) { return id == null ? null : open.get(id); } }

    void remove(String id) {
        T c;
        synchronized (open) { c = id == null ? null : open.remove(id); }
        if (c != null) closer.close(c);
    }

    void clear() {
        java.util.List<T> all;
        synchronized (open) { all = new java.util.ArrayList<>(open.values()); open.clear(); }
        for (T c : all) closer.close(c);
    }

    int size() { synchronized (open) { return open.size(); } }
}
