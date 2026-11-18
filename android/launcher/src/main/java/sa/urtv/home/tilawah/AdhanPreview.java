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
        final Context app = c.getApplicationContext();
        // 4.4 (QA-8, R1): which voice plays reads the place (Friday's Dhuhr: a question to the screensaver), so it is picked on a thread; the
        // control centre's press never waits on the screensaver's process
        new Thread(() -> {
            Prefs p = new Prefs(app);
            int voice = prayer < 0 ? p.playVoiceFor(app, false) : p.playFor(app, prayer);
            if (voice == PrayerSound.SILENT) voice = p.playVoiceFor(app, prayer == PrayerTimes.FAJR);
            final int v = voice;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> MusicService.adhan(app, v, Adhan.name(v), SECONDS, prayer));
        }, "adhan-preview").start();
    }

    /** the control centre's prayer (0 Fajr … 4 Isha, its own order; -1 after Isha: tomorrow's Fajr) as PrayerTimes' number */
    public static int prayerOf(int controlIndex) {
        int[] map = {PrayerTimes.FAJR, PrayerTimes.DHUHR, PrayerTimes.ASR, PrayerTimes.MAGHRIB, PrayerTimes.ISHA};
        return controlIndex < 0 || controlIndex >= map.length ? PrayerTimes.FAJR : map[controlIndex];
    }
}
