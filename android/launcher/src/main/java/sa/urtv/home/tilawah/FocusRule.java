package sa.urtv.home.tilawah;

/**
 * 4.3.3 G1: what the player does when another sound takes the TV's audio, and when it takes it back. Pure logic (no Android classes), so
 * tests/FocusRuleRun.java runs it on a plain JVM; MusicService feeds it AudioManager's numbers.
 *
 * Before, a focus loss for good (AUDIOFOCUS_LOSS) paused the surah or the station and waited for a gain that Android never sends after a
 * loss for good: a short sound that took the focus (a TV's settings page that plays a sample, another screensaver, a system chime that asks
 * for the focus) left «تلاوة» silent until the person pressed play, and after 30 minutes the player closed. Now:
 *   - a short loss (a call, the voice search, a notification): paused, and it plays again when the focus comes back (as before);
 *   - «may duck»: it plays on at a fifth of its loudness, full again when the focus comes back (as before);
 *   - a loss for good: paused, then watched for TAKE_BACK_MS. When nothing else plays for QUIET_CHECKS checks in a row in that while and the
 *     TV is not in a call, the player asks for the focus again and goes on by itself. Another player that is still playing after that while
 *     is the person's choice (a video they opened): it stays paused, and the person's play key brings it back.
 */
final class FocusRule {
    private FocusRule() { }

    // AudioManager's numbers, copied so the rule needs no Android
    static final int GAIN = 1;
    static final int LOSS = -1;
    static final int LOSS_TRANSIENT = -2;
    static final int LOSS_TRANSIENT_CAN_DUCK = -3;
    /** AudioManager.MODE_NORMAL: no call on the TV */
    static final int MODE_NORMAL = 0;

    /** how long after a loss for good the player may take the sound back */
    static final long TAKE_BACK_MS = 60_000;
    /** how often it looks */
    static final long CHECK_MS = 2_000;
    /** this many looks in a row with nothing else playing */
    static final int QUIET_CHECKS = 2;
    /** the loudness while another sound may duck it */
    static final float DUCK = 0.2f;

    enum Act { PAUSE_UNTIL_GAIN, PAUSE_AND_WATCH, DUCK, GAIN, NOTHING }

    /** What a focus change asks for. `ours`: something of ours is playing (or was paused by an earlier loss and waits). */
    static Act on(int change, boolean ours) {
        switch (change) {
            case LOSS: return ours ? Act.PAUSE_AND_WATCH : Act.NOTHING;
            case LOSS_TRANSIENT: return ours ? Act.PAUSE_UNTIL_GAIN : Act.NOTHING;
            case LOSS_TRANSIENT_CAN_DUCK: return Act.DUCK;
            case GAIN: return Act.GAIN;
            default: return Act.NOTHING;
        }
    }

    enum Look { TAKE_BACK, AGAIN, GIVE_UP }

    /**
     * One look while watching after a loss for good.
     * @param sinceLossMs  how long since the loss
     * @param othersPlay   another app's media plays now (AudioManager.isMusicActive: ours is paused, so it is someone else's)
     * @param mode         AudioManager.getMode(): a call is never talked over
     * @param quietSoFar   looks in a row before this one with nothing else playing
     * @param wanted       the person still wants it (no pause, stop or other pick meanwhile)
     */
    static Look look(long sinceLossMs, boolean othersPlay, int mode, int quietSoFar, boolean wanted) {
        if (!wanted) return Look.GIVE_UP;
        boolean quiet = !othersPlay && mode == MODE_NORMAL;
        if (quiet && quietSoFar + 1 >= QUIET_CHECKS) return Look.TAKE_BACK;
        if (sinceLossMs + CHECK_MS > TAKE_BACK_MS) return Look.GIVE_UP;   // the next look would be past the while: another player's for good
        return Look.AGAIN;
    }

    /**
     * 4.4 beta 4 (Xiaomi A9: the Quran radio stopped after the screensaver): the screensaver took the sound for good while it showed, the watch
     * above gave up after TAKE_BACK_MS, and nothing asked again when it ended. When the dream ends: take the sound back if it was lost during
     * the dream, the person still wants it (no pause, stop or other pick meanwhile), and the loss is at most DREAM_BACK_MS old (a screensaver
     * that ran all night does not start the radio in the morning).
     */
    static final long DREAM_BACK_MS = 2 * 3600_000L;

    static boolean afterDream(boolean lostInDream, boolean waiting, boolean wanted, long sinceLossMs) {
        return lostInDream && waiting && wanted && sinceLossMs >= 0 && sinceLossMs <= DREAM_BACK_MS;
    }

    /** the quiet looks in a row after this one */
    static int quietAfter(boolean othersPlay, int mode, int quietSoFar) {
        return !othersPlay && mode == MODE_NORMAL ? quietSoFar + 1 : 0;
    }

    /**
     * Was something of ours on when the adhan starts, so it comes back after it? Playing, loading with play asked, or paused by another
     * sound and waiting for it to end (before 4.3.3 that last one was forgotten by the adhan and stayed silent after it).
     */
    static boolean onBeforeAdhan(boolean playing, boolean loadingWanted, boolean waitingForFocus) {
        return playing || loadingWanted || waitingForFocus;
    }

    /** The home or the player came to the front while ours plays without a foreground notice (Android refused it earlier): ask again now. */
    static boolean promote(boolean ourPlaying, boolean foreground) {
        return ourPlaying && !foreground;
    }
}
