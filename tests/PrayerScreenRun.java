package sa.urtv.home.tilawah;

/** For tests/prayer_screen_test.py: PrayerScreen's four states and mask on a JVM. One line per check: name TAB ok|FAIL. */
public final class PrayerScreenRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] a) {
        check("the four states from sound and screen", PrayerScreen.state(true, true) == PrayerScreen.BOTH && PrayerScreen.state(true, false) == PrayerScreen.SOUND_ONLY
            && PrayerScreen.state(false, true) == PrayerScreen.SCREEN_ONLY && PrayerScreen.state(false, false) == PrayerScreen.NONE);
        boolean back = true;
        for (int s = 0; s < 4; s++) back &= PrayerScreen.state(PrayerScreen.sound(s), PrayerScreen.screen(s)) == s;
        check("every state gives back its own sound and screen", back);
        check("the mask: bit p of the prayer's number (Fajr 0, Dhuhr 2, Asr 3, Maghrib 4, Isha 5), none hidden by 0",
            PrayerScreen.hidden(1 << 4, 4) && !PrayerScreen.hidden(1 << 4, 3) && !PrayerScreen.hidden(0, 0) && PrayerScreen.hidden(1, 0));
        check("the bar's order (0..4) is the numbers 0, 2, 3, 4, 5", PrayerScreen.hiddenAt(1 << 2, 1) && PrayerScreen.hiddenAt(1 << 5, 4) && !PrayerScreen.hiddenAt(1 << 5, 3) && !PrayerScreen.hiddenAt(0, 2));
        check("a number outside 0..5 (an alarm of an older version: -1) is never hidden", !PrayerScreen.hidden(0xFFFF, -1) && !PrayerScreen.hidden(0xFFFF, 6) && !PrayerScreen.hiddenAt(0xFFFF, 5));
    }
}
