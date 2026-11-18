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
    public static void play(Context c, boolean fajr) { play(c, fajr ? PrayerTimes.FAJR : -1); }

    /**
     * 4.4: the muezzin and the level that prayer will have («أذان كل صلاة», «مستوى صوت الأذان»): a prayer set silent is heard with its general voice,
     * so the preview says something. `prayer`: PrayerTimes' number, -1 the general one.
     */
    public static void play(Context c, int prayer) {
        Prefs p = new Prefs(c);
        int voice = prayer < 0 ? p.playVoiceFor(c, false) : p.playFor(c, prayer);
        if (voice == PrayerSound.SILENT) voice = p.playVoiceFor(c, prayer == PrayerTimes.FAJR);
        MusicService.adhan(c, voice, Adhan.name(voice), SECONDS, prayer);
    }

    /** the control centre's prayer (0 Fajr … 4 Isha, its own order; -1 after Isha: tomorrow's Fajr) as PrayerTimes' number */
    public static int prayerOf(int controlIndex) {
        int[] map = {PrayerTimes.FAJR, PrayerTimes.DHUHR, PrayerTimes.ASR, PrayerTimes.MAGHRIB, PrayerTimes.ISHA};
        return controlIndex < 0 || controlIndex >= map.length ? PrayerTimes.FAJR : map[controlIndex];
    }
}
