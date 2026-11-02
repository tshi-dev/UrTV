package sa.urtv.home.tilawah;

/**
 * What the home screen's «يُتلى الآن» card says, with no Android in it (tests/NowPlayingTextRun.java runs it): two lines, the
 * surah or station and then who recites it (and the sleep timer when one is set); a hint instead of the second line while the card
 * has the focus.
 */
final class NowPlayingText {
    private NowPlayingText() {}

    /** «سورة الفاتحة», or the station's name */
    static String title(boolean radio, String surahName, String stationName) {
        return radio ? stationName : sa.urtv.home.S.t("سورة %s", surahName);
    }

    /** who recites: the reciter, or «بث مباشر» for a station */
    static String who(boolean radio, String reciter) { return radio ? sa.urtv.home.S.t("بث مباشر") : reciter; }

    /** what the second line says instead of «يُتلى الآن» while the sound is loading */
    static final String BUFFERING = sa.urtv.home.S.n("جارٍ التحميل…");

    /**
     * The same with a note in front when something is wrong or slow: «جارٍ التحميل…  ·  عبدالرحمن السديس» while it loads, or what failed
     * («تعذّر تحميل السورة، تحقق من الإنترنت  ·  …»). An empty note is the plain line.
     */
    static String sub(boolean radio, String reciter, int sleepMinutes, String note) {
        String plain = sub(radio, reciter, sleepMinutes);
        return note == null || note.isEmpty() ? plain : note + "  ·  " + plain;
    }

    /** the note: what failed first, else that it is loading, else none */
    static String note(String error, boolean buffering) {
        return error != null && !error.isEmpty() ? error : buffering ? sa.urtv.home.S.tv(BUFFERING) : "";
    }

    /** The card's second line: «عبدالرحمن السديس», «بث مباشر»; with a sleep timer set: «… · نوم ٢٣ د» (minutes left, 0 = none). The words
     *  «يُتلى الآن» are the card's spoken name (NowCard's content description), not a line: the line is for who recites. */
    static String sub(boolean radio, String reciter, int sleepMinutes) {
        String s = radio ? sa.urtv.home.S.t("بث مباشر") : reciter;
        return sleepMinutes > 0 ? sa.urtv.home.S.t("%s  ·  نوم %s د", s, digits(String.valueOf(sleepMinutes))) : s;
    }

    /** What the keys do while the card has the focus: OK pauses or plays, a long OK opens the player, ◀ ▶ the previous and next surah (not for a station) */
    static String hint(boolean radio) {
        return radio ? sa.urtv.home.S.t("اختيار: إيقاف/تشغيل  ·  مطولًا: فتح")
                : sa.urtv.home.S.t("اختيار: إيقاف/تشغيل  ·  يمين/يسار: السورة  ·  مطولًا: فتح");
    }

    /** 0-9 as ٠-٩ */
    static String digits(String s) {
        return sa.urtv.home.DigitsText.convert(s, false);
    }
}
