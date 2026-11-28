package sa.urtv.screensaver;

/**
 * 4.4 beta 4 (a user: the still picture stayed up to about 3 s when the screensaver started): when the last picture of the page (SaverSnapshot), drawn over
 * the page at the start, gives way. At once when the live page has painted; else after WAIT_MS at most (was 3 s: a still clock and photo that long
 * looked frozen); and at once when the minute turns while it is up, since the picture shows the clock of the minute it was made in. No Android in
 * it (tests/CoverWaitRun.java runs it).
 */
final class CoverWait {
    private CoverWait() {}

    static final long WAIT_MS = 1500, MINUTE_MS = 60_000;

    /** pageLive: the page has painted (its view is fully shown); waited: ms since the page's view was added; shownAt and now: wall-clock ms */
    static boolean done(boolean pageLive, long waited, long shownAt, long now) {
        return pageLive || waited >= WAIT_MS || shownAt / MINUTE_MS != now / MINUTE_MS;
    }
}
