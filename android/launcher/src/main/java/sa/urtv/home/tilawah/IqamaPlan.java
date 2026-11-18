package sa.urtv.home.tilawah;

/**
 * 4.4 «الإقامة»: a sound and a notice at the iqama, like the adhan's. The iqama is the adhan's time plus MINUTES (the same table as the home's
 * bar «إقامة العصر بعد …», AdhanBarText.IQAMA, and the page's); a Friday's Dhuhr has none (the khutba first). Both are off by default.
 *   sound   "" none (the default), "chime" our short bell (res/raw/iqama_chime.ogg), "a<id>" one of «صوتياتي» (AudioStore)
 *   notice  «تنبيه الإقامة»: «حان وقت إقامة صلاة العصر» where the person is; over another app as the kick-off notice is (KickoffPlan.where:
 *           an overlay with the TV's permission, else a text notice; never over the screensaver or another app in kids mode)
 * No Android in it: tests/iqama_test.py runs it.
 */
final class IqamaPlan {
    private IqamaPlan() { }

    /** minutes from the adhan to the iqama, by PrayerTimes' number (FAJR 0, SUNRISE 1 has none, DHUHR 2 … ISHA 5) */
    static final int[] MINUTES = {25, 0, 20, 20, 10, 20};
    /** an iqama alarm this late (standby, a clock that jumped) is dropped, never played late */
    static final long LATE_MS = 2 * 60_000L;

    /** the iqama's moment for the adhan at `adhanAt`, or -1 (no prayer, Sunrise, a Friday's Dhuhr) */
    static long at(int prayer, long adhanAt, boolean friday) {
        if (prayer < 0 || prayer >= MINUTES.length || MINUTES[prayer] <= 0 || adhanAt <= 0) return -1;
        if (prayer == PrayerTimes.DHUHR && friday) return -1;
        return adhanAt + MINUTES[prayer] * 60_000L;
    }

    static boolean validSound(String s) {
        return s != null && (s.isEmpty() || s.equals("chime") || s.matches("a[0-9]{1,9}"));
    }

    /** anything to do at the iqama */
    static boolean wanted(String sound, boolean notice) { return notice || (validSound(sound) && !sound.isEmpty()); }

    /** the alarm came at its moment (not later than LATE_MS, not more than a minute early) */
    static boolean onTime(long due, long now) { return due > 0 && now - due <= LATE_MS && due - now <= 60_000L; }

    /**
     * the sound plays: chosen, the screen on, no adhan playing (an iqama never cuts it), not in the quiet hours, and not over another app's
     * sound (a video, a call): the notice still shows there
     */
    static boolean sound(String sound, boolean screenOn, boolean adhanLive, boolean quiet, boolean othersPlay) {
        return validSound(sound) && !sound.isEmpty() && screenOn && !adhanLive && !quiet && !othersPlay;
    }
}
