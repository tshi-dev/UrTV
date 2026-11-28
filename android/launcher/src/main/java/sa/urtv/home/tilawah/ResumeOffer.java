package sa.urtv.home.tilawah;

/**
 * Known bug (tester step 71): the radio stopped silently when the TV updated System WebView (Android ends the home's process: no error, no restart). The service
 * writes «was playing» (the time) when a recitation or a station starts and clears it on every pause or stop of the person's; a home that starts with it still
 * written, and nothing of ours playing, knows the sound was cut off, and the bar at the top offers «كمّل» (never starts it by itself). No Android in here:
 * tests/ResumeOfferRun.java runs it.
 */
public final class ResumeOffer {
    private ResumeOffer() { }

    /** how long after the sound started the cut-off is still worth an offer (a radio left on all day is cut off after hours) */
    /** the same numbers as Prefs.SOURCE_SURAH and SOURCE_RADIO (tests/resume_offer_test.py checks it): Prefs needs Android, this file must not */
    static final int SURAH = 1, RADIO = 2;
    static final long MAX_AGE_MS = 24L * 3600_000L;

    /** the offer is due: «was playing» is written (at > 0, not from the future, not older than MAX_AGE_MS), nothing of ours plays now, it is a recitation or a station */
    public static boolean due(long playingAt, long now, boolean ourPlaying, int source) {
        if (playingAt <= 0 || ourPlaying) return false;
        if (source != SURAH && source != RADIO) return false;
        long age = now - playingAt;
        return age >= 0 && age <= MAX_AGE_MS;
    }
}
