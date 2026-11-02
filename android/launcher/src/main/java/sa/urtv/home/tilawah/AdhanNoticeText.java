package sa.urtv.home.tilawah;

/**
 * «تنبيه عند الأذان»: the words of the notice, and how long it stays. No Android in it (tests/adhan_options_test.py runs it).
 * PrayerTimes' number of the prayer: FAJR 0, DHUHR 2, ASR 3, MAGHRIB 4, ISHA 5; anything else (an alarm of an older version has none) is just «الصلاة».
 */
final class AdhanNoticeText {
    /** the notice stays 5 seconds: a long toast (3.5 s) and then a short one, cut at 5 s */
    static final long SHOW_MS = 5_000, FIRST_MS = 3_500;

    private static final String[] NAMES = {"الفجر", null, "الظهر", "العصر", "المغرب", "العشاء"};

    private AdhanNoticeText() { }

    static String text(int prayer) {
        String name = prayer >= 0 && prayer < NAMES.length ? NAMES[prayer] : null;
        return name == null ? "حان وقت الصلاة" : "حان وقت صلاة " + name;
    }
}
