package sa.urtv.home.tilawah;

import android.content.Context;

/**
 * 4.3.3 «الأذان والإقامة» in «مركز التحكم» (its times are the home's own, ControlText.nextPrayer): OK plays a short preview of the muezzin that prayer
 * will have (Fajr's own for Fajr), as the voice picker's preview does: the volume and «أذان مختصر» are those of the adhan, nothing is kept, and the
 * real adhan is never held back by it (AdhanReceiver judges by the prayer's own time, not by when something last played).
 */
public final class AdhanPreview {
    private AdhanPreview() { }

    /** how long the preview plays */
    public static final int SECONDS = 15;

    /** OK on the row: SECONDS of the muezzin of that prayer (Fajr's own for Fajr) */
    public static void play(Context c, boolean fajr) {
        int voice = new Prefs(c).playVoiceFor(c, fajr);
        MusicService.adhan(c, voice, Adhan.name(voice), SECONDS);
    }
}
