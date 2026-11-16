package sa.urtv.home.tilawah;

/**
 * 4.4 «أذان كل صلاة»: which of the five prayers play the adhan's sound, and with what. No Android in it (tests/PrayerSoundRun.java runs it).
 * Each prayer's choice is one word, kept in the "tilawah" preferences as pp_<prayer>:
 *   ""          like the general adhan (the muezzin, or «مؤذن الفجر» at Fajr, or the person's own, as before 4.4): the default, nothing changes
 *   "silent"    no sound: the time, the card, the bar and «تنبيه عند الأذان» stay; only the sound is not played
 *   "v<N>"      the muezzin N (1..COUNT); a Fajr recording only at Fajr
 *   "own", "own_fajr"  the person's own recording; when it is gone, the general choice plays
 */
final class PrayerSound {
    private PrayerSound() { }

    static final int SILENT = -1;
    static final int FAJR = 0, DHUHR = 2;

    static boolean valid(String c) {
        if (c == null) return false;
        if (c.isEmpty() || c.equals("silent") || c.equals("own") || c.equals("own_fajr")) return true;
        if (!c.matches("v[0-9]{1,3}")) return false;
        int n = Integer.parseInt(c.substring(1));
        return n >= 1 && n <= AdhanVoices.COUNT;
    }

    /**
     * The voice to play (Prefs.OWN, Prefs.OWN_FAJR, 1..COUNT) or SILENT.
     * @param general what the prayer played before 4.4 (Prefs.playVoiceFor)
     */
    static int voice(String choice, int prayer, int general, boolean ownOk, boolean ownFajrOk) { return voice(choice, prayer, general, ownOk, ownFajrOk, false); }

    /** 4.4 touch «أذان الجمعة»: fridayDhuhr (the setting on, and it is Friday): a silent Dhuhr is heard, with the general voice */
    static int voice(String choice, int prayer, int general, boolean ownOk, boolean ownFajrOk, boolean fridayDhuhr) {
        if (!valid(choice) || choice.isEmpty()) return general;
        if (choice.equals("silent")) return prayer == DHUHR && fridayDhuhr ? general : SILENT;
        if (choice.equals("own")) return ownOk ? Prefs.OWN : general;
        if (choice.equals("own_fajr")) return ownFajrOk ? Prefs.OWN_FAJR : general;
        int n = Integer.parseInt(choice.substring(1));
        if (AdhanVoices.isFajr(n) && prayer != FAJR) return general;   // a Fajr recording («الصلاة خير من النوم») only at Fajr
        return n;
    }
}
