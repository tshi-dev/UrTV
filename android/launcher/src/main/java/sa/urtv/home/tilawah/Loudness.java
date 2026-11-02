package sa.urtv.home.tilawah;

/**
 * How much the recitation, the radio and the adhan are lifted inside the player, with no Android in it (tests/LoudnessRun.java runs it).
 *
 * Why they were quiet: the recordings (mp3quran's surahs, the radio, the archive.org adhans) are mastered with wide dynamics, so their
 * loudness sits well under what TV programmes and apps play at, and our player played them at its unity gain (setVolume 1) with no
 * processing. At the same TV volume a surah sounded a good deal lower than the channel before it, and the adhan, played at its own
 * level, lower than the person expected for that level.
 *
 * The fix is a LoudnessEnhancer on the player's own audio session: a gain with a limiter behind it, so the quiet parts come up and the
 * peaks never clip. It changes nothing outside our player: the TV's volume, its mute and the adhan's own volume (setStreamVolume, at the
 * level the person chose and never above it) are what they were; on a muted TV this adds nothing that could be heard.
 */
final class Loudness {
    /** +6 dB: about what the recordings lack against TV loudness, and well inside what the limiter holds without pumping. */
    static final int GAIN_MB = 600;
    /** The most this ever asks for, whatever changes above: +9 dB. */
    static final int MAX_MB = 900;

    private Loudness() { }

    /** The gain for what plays, in millibels: the surah, the radio and the adhan get it, nothing else does. */
    static int targetMb(int source) {
        switch (source) {
            case Prefs.SOURCE_SURAH:
            case Prefs.SOURCE_RADIO:
            case Prefs.SOURCE_ADHAN:
                return Math.max(0, Math.min(MAX_MB, GAIN_MB));
            default:
                return 0;
        }
    }
}
