package sa.urtv.home;

/**
 * 4.4 beta 4 (a user: «يُتلى الآن» stayed on the home while paused, and nothing closed it): whether the card shows. While a surah or a station
 * plays, always; paused, for HIDE_PAUSED_MS from the moment it was first seen paused, then it hides (the playback stays paused: the player and
 * the «تلاوة وأذان» pill still have it, until the service's own stop after 30 minutes). Back on the card closes it at once (NowPlaying.close).
 * No Android in here (tests/NowCardRuleRun.java).
 */
final class NowCardRule {
    private NowCardRule() { }

    static final long HIDE_PAUSED_MS = 10 * 60_000L;

    /** when the card was first seen paused: 0 while it plays or shows nothing, `now` at the first paused look, else what it was */
    static long pausedSince(boolean show, boolean playing, long was, long now) {
        if (!show || playing) return 0;
        return was > 0 && was <= now ? was : now;
    }

    static boolean visible(boolean show, boolean playing, long pausedSince, long now) {
        if (!show) return false;
        return playing || pausedSince <= 0 || now - pausedSince < HIDE_PAUSED_MS;
    }
}
