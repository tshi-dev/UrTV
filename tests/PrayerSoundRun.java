package sa.urtv.home.tilawah;

/** tests/prayer_sound_test.py runs this: one line per case, «name<TAB>ok|bad». */
public class PrayerSoundRun {
    static void out(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "bad")); }

    public static void main(String[] a) {
        int general = 3, fajrRec = -1;
        for (int v = 1; v <= AdhanVoices.COUNT; v++) if (AdhanVoices.isFajr(v)) { fajrRec = v; break; }
        out("nothing chosen: the general adhan, as before 4.4", PrayerSound.voice("", 4, general, true, true) == general);
        out("silent: no sound", PrayerSound.voice("silent", 2, general, true, true) == PrayerSound.SILENT);
        out("a muezzin of ours", PrayerSound.voice("v5", 3, general, false, false) == 5);
        out("a Fajr recording only at Fajr", PrayerSound.voice("v" + fajrRec, 0, general, false, false) == fajrRec && PrayerSound.voice("v" + fajrRec, 4, general, false, false) == general);
        out("the own recording while it is there; the general one when it is gone", PrayerSound.voice("own", 4, general, true, false) == Prefs.OWN
                && PrayerSound.voice("own", 4, general, false, false) == general);
        out("the own Fajr recording, likewise", PrayerSound.voice("own_fajr", 0, general, false, true) == Prefs.OWN_FAJR && PrayerSound.voice("own_fajr", 0, general, true, false) == general);
        out("a word that is not ours is the general adhan", PrayerSound.voice("loud", 4, general, true, true) == general && PrayerSound.voice(null, 4, general, true, true) == general
                && PrayerSound.voice("v0", 4, general, true, true) == general && PrayerSound.voice("v" + (AdhanVoices.COUNT + 1), 4, general, true, true) == general);
        out("valid words", PrayerSound.valid("") && PrayerSound.valid("silent") && PrayerSound.valid("own") && PrayerSound.valid("own_fajr") && PrayerSound.valid("v1")
                && !PrayerSound.valid("v1x") && !PrayerSound.valid("v-1") && !PrayerSound.valid(null));
    }
}
