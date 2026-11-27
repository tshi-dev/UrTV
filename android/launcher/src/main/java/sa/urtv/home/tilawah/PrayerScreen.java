package sa.urtv.home.tilawah;

/**
 * 4.4 beta 4 (the owner, «Do all»): for each of the five prayers, whether its sound plays (PrayerSound: «silent» is no sound) and whether it SHOWS on the
 * screen (this class): the bar before the adhan, the bar while it plays and to the iqama, and «تنبيه عند الأذان». The two are separate; together they are the
 * four states the settings' picker names: «صوت وشاشة», «صوت فقط», «شاشة فقط», «لا شيء». A prayer's screen is on unless its flag is off. No Android in it
 * (tests/PrayerScreenRun.java runs it); Prefs keeps the flags (ps_<prayer>), the home's one truth, read by the screensaver through the launcher's provider.
 */
public final class PrayerScreen {
    private PrayerScreen() { }

    static final int BOTH = 0, SOUND_ONLY = 1, SCREEN_ONLY = 2, NONE = 3;
    /** the prayers' numbers (PrayerTimes) in the order the settings and the bar list them */
    static final int[] IDS = {0, 2, 3, 4, 5};

    /** the hidden mask as the home last knew it, for the places with no Context at hand (Prefs updates it, load reads it); 0 until it is loaded */
    private static volatile int cached;
    private static volatile boolean loaded;
    static void cache(int mask) { cached = mask; loaded = true; }
    static int cachedMask() { return cached; }
    /** this prayer is hidden by the mask the home last knew (the adhan playing: MusicService loads it when it starts) */
    public static boolean hiddenNow(int prayer) { return hidden(cached, prayer); }
    /** for the bar, which has a Context */
    public static int mask(android.content.Context c) { return maskOf(c); }
    private static int maskOf(android.content.Context c) { if (!loaded) cache(new Prefs(c).hideMask()); return cached; }

    static int state(boolean sound, boolean screen) { return sound ? (screen ? BOTH : SOUND_ONLY) : (screen ? SCREEN_ONLY : NONE); }
    static boolean sound(int state) { return state == BOTH || state == SOUND_ONLY; }
    static boolean screen(int state) { return state == BOTH || state == SCREEN_ONLY; }

    /** the words of a state (the settings' value, the phone page's) */
    static String word(int state) {
        switch (state) {
            case SOUND_ONLY: return sa.urtv.home.S.t("صوت فقط");
            case SCREEN_ONLY: return sa.urtv.home.S.t("شاشة فقط");
            case NONE: return sa.urtv.home.S.t("لا شيء");
            default: return sa.urtv.home.S.t("صوت وشاشة");
        }
    }

    /** the mask of the hidden prayers: bit p of PrayerTimes' number (0 Fajr, 2 Dhuhr, 3 Asr, 4 Maghrib, 5 Isha) */
    static boolean hidden(int mask, int prayer) { return prayer >= 0 && prayer <= 5 && (mask >> prayer & 1) != 0; }

    /** the same for the i-th prayer of the five (0..4), the bar's order */
    static boolean hiddenAt(int mask, int i) { return i >= 0 && i < IDS.length && hidden(mask, IDS[i]); }

    /**
     * The short summary for the settings row and the player's row (one rule, tests/prayer_summary_cases.tsv pins it here and in the screensaver's PrayerSummary):
     * «كلها صوت وشاشة» when no prayer differs, one that does by name («الفجر: شاشة فقط»), else how many («٢ مختلفة»); num writes the count in the digits of the setting
     */
    static String summary(int[] states, String[] names, java.util.function.IntFunction<String> num) {
        int differ = 0, first = -1;
        for (int i = 0; i < states.length; i++) if (states[i] != BOTH) { differ++; if (first < 0) first = i; }
        if (differ == 0) return sa.urtv.home.S.t("كلها صوت وشاشة");
        if (differ == 1) return names[first] + ": " + word(states[first]);
        return sa.urtv.home.S.t("%s مختلفة", num.apply(differ));
    }
}
