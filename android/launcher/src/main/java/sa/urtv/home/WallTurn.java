package sa.urtv.home;

/**
 * 4.4 beta 4 (Xiaomi A14: the home's photo changed at every return from an app): when the home's photo moves on. Only on the timer: a return shows
 * the photo it left, unless the photo's interval is up. The time a photo was chosen is the wall clock's, kept in the prefs («wallpaperAt»), so a
 * home the TV stopped and started again (a trim, a killed process) counts the same interval. A time in the future (the clock went back) counts as
 * up, once. No Android in here (tests/WallTurnRun.java).
 */
final class WallTurn {
    private WallTurn() { }

    /** the photo chosen at `at` (0: none kept) has been up for the interval by `now` */
    static boolean due(long at, long now, long interval) {
        return at <= 0 || now < at || now - at >= interval;
    }

    /** how long until it is due: the rest of the interval, the whole interval when none can be measured */
    static long left(long at, long now, long interval) {
        if (at <= 0 || now < at) return interval;
        return Math.max(0, interval - (now - at));
    }
}
