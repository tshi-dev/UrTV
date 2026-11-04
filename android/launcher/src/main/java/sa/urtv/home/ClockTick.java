package sa.urtv.home;

/**
 * Which broadcasts redraw the home's clock line (MainActivity's tick). The minute tick, a time or zone change: always. A Wi-Fi state broadcast
 * (some TVs send several a minute while the network settles) only when the minute moved since the last redraw: each redraw reads the screensaver's
 * settings, and on a weak TV that was a stall on every one of them (a user report on 4.1). No Android in here: tests/lowram_test.py runs it.
 */
final class ClockTick {
    private ClockTick() { }

    /** lastMs: when the clock line was last drawn (0: never); nowMs: now (ms since the epoch). */
    static boolean redraw(boolean networkBroadcast, long lastMs, long nowMs) {
        if (!networkBroadcast || lastMs <= 0) return true;
        return nowMs / 60_000L != lastMs / 60_000L;
    }
}
