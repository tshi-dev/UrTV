package sa.urtv.home.setup;

/**
 * F133 (review 4.4, batch 1): a BroadcastReceiver may call goAsync() ONCE per broadcast: the second call returns null and finishing it threw a NullPointerException
 * in the boot receiver's thread (a crash at boot). One goAsync in onReceive, shared by every task of that boot: each task takes a hold before its thread starts and
 * lets go at its end, onReceive holds once itself until it has started them all; the broadcast is finished once, when the last hold is let go. A null result
 * (no goAsync was possible) is never finished. Plain Java: tests/AsyncCountRun.java.
 */
final class AsyncCount {
    private final Runnable finish;   // what ends the broadcast (PendingResult.finish), null when there is none
    private int open = 1;            // onReceive's own hold
    private boolean finished;

    AsyncCount(Runnable finish) { this.finish = finish; }

    /** a task is about to start its thread: call before the thread starts */
    synchronized void hold() { if (!finished) open++; }

    /** a task ended, or onReceive has started them all: the last one ends the broadcast, once */
    void release() {
        boolean last;
        synchronized (this) {
            if (finished || open <= 0) return;
            open--;
            last = open == 0;
            if (last) finished = true;
        }
        if (last && finish != null) {
            try { finish.run(); } catch (RuntimeException ignored) { }
        }
    }
}
