package sa.urtv.home;

/**
 * The words of the slim bar before the adhan: «أذان المغرب بعد ٨ دقائق» in the last 10 minutes before each prayer, and from the
 * adhan until the iqama «إقامة المغرب بعد ١٢ دقيقة» (a Friday's Dhuhr is «أذان الجمعة»). While the adhan plays, playing() says what is on and how to stop it. The page does the same on the screensaver (adhanBarText in template.html). No
 * Android in here, so tests/adhan_bar_test.py runs it on tests/adhan_bar_cases.tsv.
 */
final class AdhanBarText {
    /** how many minutes before the adhan the bar shows */
    static final int BEFORE_MIN = 10;
    /** minutes from adhan to iqama for Fajr, Dhuhr, Asr, Maghrib and Isha: what the page's «الإقامة بعد» has always used */
    static final int[] IQAMA = {25, 20, 20, 10, 20};

    private static final String[] NAMES = {S.n("الفجر"), S.n("الظهر"), S.n("العصر"), S.n("المغرب"), S.n("العشاء")};

    private AdhanBarText() { }

    /**
     * @param now    the moment, in millis
     * @param adhan  Fajr, Dhuhr, Asr, Maghrib and Isha today, in millis
     * @param friday a Friday's Dhuhr has no iqama countdown (the khutba comes first)
     * @return the bar's words, or null when it is not showing
     */
    static String text(long now, long[] adhan, boolean friday, boolean latin) {
        for (int i = 0; i < 5; i++) {
            double toAdhan = (adhan[i] - now) / 60000.0;
            String name = S.tv(i == 1 && friday ? "الجمعة" : NAMES[i]);
            if (toAdhan > 0 && toAdhan <= BEFORE_MIN) return S.t("أذان %s بعد %s", name, minutes((int) Math.ceil(toAdhan), latin));
            int gap = i == 1 && friday ? 0 : IQAMA[i];
            double toIqama = (adhan[i] + gap * 60000L - now) / 60000.0;
            if (now >= adhan[i] && toIqama > 0) return S.t("إقامة %s بعد %s", name, minutes((int) Math.ceil(toIqama), latin));
        }
        return null;
    }

    /** the hint on the bar while the adhan plays: any key stops it (the default), or only OK on the bar when «إيقاف الأذان بأي زر» is off */
    static final String STOP_OK = S.n("اختيار: إيقاف الأذان"), STOP_ANY = S.n("أي زر يوقف الأذان");

    /**
     * The bar while the adhan is playing: «أذان المغرب  ·  اختيار: إيقاف الأذان». `prayer` is PrayerTimes' number (FAJR 0, DHUHR 2, ASR 3,
     * MAGHRIB 4, ISHA 5), anything else (the adhan played from the computer) is just «الأذان».
     */
    static String playing(int prayer, boolean friday, boolean stopAny) {
        String name = prayer == 0 ? "الفجر" : prayer == 2 ? (friday ? S.n("الجمعة") : "الظهر") : prayer == 3 ? "العصر" : prayer == 4 ? "المغرب" : prayer == 5 ? "العشاء" : null;
        return (name == null ? S.t("الأذان") : S.t("أذان %s", S.tv(name))) + "  ·  " + S.tv(stopAny ? STOP_ANY : STOP_OK);
    }

    /** «١ دقيقة», «دقيقتين», «٨ دقائق», «١٢ دقيقة»: as the page says them. */
    static String minutes(int n, boolean latin) {
        if (n == 2) return S.t("دقيقتين");
        return n >= 3 && n <= 10 ? S.t("%s دقائق", GreetingText.digits(String.valueOf(n), latin)) : S.t("%s دقيقة", GreetingText.digits(String.valueOf(n), latin));
    }
}
