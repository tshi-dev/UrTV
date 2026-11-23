package sa.urtv.home;

/**
 * 4.4 beta 3 (a user: «after about a week it gets very heavy»): the home card's page ran for days in one renderer (only weak TVs let it go, Ram.tight), its
 * memory growing with every redraw. Once a day, at night, while the home is NOT in front (another app, the screensaver, the screen off), the page is let go;
 * the next return builds it again behind the last picture (HomeSnapshot), as after the weak TVs' drop. Never while the home is in use. A TV that is never
 * away from the home at night gets it at any hour after two days, still only while the home is not in front. No Android in here (tests/NightRebuildRun.java).
 */
final class NightRebuild {
    private NightRebuild() { }

    static final long DAY_AGE_MS = 20 * 3600_000L, MAX_AGE_MS = 48 * 3600_000L;
    static final int FROM_HOUR = 1, TO_HOUR = 6;   // 1:00 to 5:59, the TV's own clock

    /** the page (built ageMs ago) is let go now: the home is not in front, and it is a night hour and the page is a day old, or it is two days old */
    static boolean due(boolean homeFront, long ageMs, int hour) {
        if (homeFront || ageMs < 0) return false;
        return ageMs >= MAX_AGE_MS || (ageMs >= DAY_AGE_MS && hour >= FROM_HOUR && hour < TO_HOUR);
    }

    /** how often the home, while away, looks again (the handler does not run while the TV sleeps; a return cancels it) */
    static final long CHECK_MS = 30 * 60_000L;
}
