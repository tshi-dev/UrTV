package sa.urtv.home.tilawah;

import android.content.Context;

/**
 * Changing the adhan's settings, in one place: the ADB receiver (MusicReceiver, for optimize/music.sh) and the phone control (PhoneBridge)
 * both come here, so a value is checked the same way whoever sends it. Each returns null when it saved, else the error text.
 */
final class AdhanSettings {
    private AdhanSettings() { }

    /** The muezzin for the prayers other than Fajr: 1..COUNT but a Fajr recording, or 0 for the person's own. */
    static String voice(Context c, Prefs prefs, int voice) {
        if (voice == Prefs.OWN) {
            if (!OwnAdhan.exists(c)) return "error: no recording of your own yet (optimize/music.sh adhan add FILE)";
            prefs.setOwnChosen(true);
        } else if (voice < 1 || voice > Adhan.VOICES) {
            return "error: the voice is 1-" + Adhan.VOICES + " or 0 for your own recording";
        } else if (AdhanVoices.isFajr(voice)) {
            return "error: voice " + voice + " is a Fajr recording; it is for Fajr only (optimize/music.sh adhan fajr-voice " + voice + ")";
        } else {
            prefs.setVoice(voice);
            prefs.setOwnChosen(false);
        }
        return null;
    }

    /** «مؤذن الفجر»: 1..COUNT, or 0 for the person's own. */
    static String fajrVoice(Context c, Prefs prefs, int voice) {
        if (voice == Prefs.OWN_FAJR) {   // 4.3 C16: the second recording
            if (!OwnAdhan.fajrExists(c)) return "error: no Fajr recording of your own yet (optimize/music.sh adhan add --fajr FILE)";
            prefs.setFajrOwn(true);
            prefs.setFajrOwnSecond(true);
        } else if (voice == Prefs.OWN) {
            if (!OwnAdhan.exists(c)) return "error: no recording of your own yet (optimize/music.sh adhan add FILE)";
            prefs.setFajrOwn(true);
        } else if (voice < 1 || voice > Adhan.VOICES) {
            return "error: the voice is 1-" + Adhan.VOICES + " or 0 for your own recording";
        } else {
            prefs.setFajrVoice(voice);
            Adhan.prefetchChosen(c);
        }
        return null;
    }

    /** The adhan's own volume, 10 to 100 percent. */
    static boolean volume(Prefs prefs, int percent) {
        if (percent < 10 || percent > 100) return false;
        prefs.setAdhanVolume(percent);
        return true;
    }

    static void on(Context c, Prefs prefs, boolean on) {
        prefs.setAdhanOn(on);
        AdhanScheduler.schedule(c);
    }
}
