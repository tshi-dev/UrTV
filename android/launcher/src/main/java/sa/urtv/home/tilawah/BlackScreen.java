package sa.urtv.home.tilawah;

/**
 * 4.4 «شاشة سوداء» (GitHub #62): listening with the picture off. The rules, with no Android in them (tests/BlackScreenRun.java runs them):
 *   - it stays while a surah, a station, an adhan or a dua plays or loads; the screen is kept on only then (FLAG_KEEP_SCREEN_ON);
 *   - once nothing plays for GRACE_MS (the person paused it, the sleep timer ended it, it was stopped) it closes itself and lets the TV sleep;
 *   - any key closes it and goes back to the player, except the volume and mute keys, which keep changing the sound under the black screen.
 */
final class BlackScreen {
    private BlackScreen() { }

    /** a pause this short (the next surah loading, a station joining again) does not close it */
    static final long GRACE_MS = 4_000;

    /** something of ours is on now: the screen stays black and on */
    static boolean playing(int source, boolean playing, boolean buffering) {
        return source != Prefs.SOURCE_NONE && (playing || buffering);
    }

    /** close now: nothing has played for GRACE_MS */
    static boolean close(boolean playingNow, long quietSinceMs, long now) {
        return !playingNow && quietSinceMs > 0 && now - quietSinceMs >= GRACE_MS;
    }

    // KeyEvent's numbers for the keys that are left to the TV
    static final int VOLUME_UP = 24, VOLUME_DOWN = 25, VOLUME_MUTE = 164, MUTE = 91;

    /** this key closes the black screen (true), or is the TV's own (volume, mute) */
    static boolean closesOn(int keyCode) {
        return keyCode != VOLUME_UP && keyCode != VOLUME_DOWN && keyCode != VOLUME_MUTE && keyCode != MUTE;
    }
}
