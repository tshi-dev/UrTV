package sa.urtv.screensaver;

/**
 * A countdown that can be paused (no Android, tests/GamesKitTest.java): the time is given by the caller, so a test uses a fake clock. Back (the pause card)
 * and HOME stop it; the time left does not move while it is stopped. A clock that was never started, or started with 0 («بدون وقت»), never ends.
 */
final class GameClock {
    private long total, endAt, leftAtPause; private boolean running, paused;

    void start(long ms, long now) { total = ms; endAt = now + ms; running = ms > 0; paused = false; }
    void stop() { running = false; paused = false; }
    void pause(long now) { if (running && !paused) { leftAtPause = Math.max(0, endAt - now); paused = true; } }
    void resume(long now) { if (running && paused) { endAt = now + leftAtPause; paused = false; } }
    boolean running() { return running; }
    boolean paused() { return paused; }
    long total() { return total; }
    long leftMs(long now) { return !running ? 0 : paused ? leftAtPause : Math.max(0, endAt - now); }
    /** whole seconds left, rounded up: 15 … 1, then 0 */
    int leftSeconds(long now) { return (int) ((leftMs(now) + 999) / 1000); }
    boolean over(long now) { return running && !paused && now >= endAt; }
}
