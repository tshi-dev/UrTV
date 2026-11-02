package sa.urtv.home.tilawah;

/**
 * What a failed playback says, with no Android in it (tests/PlayerFaultRun.java runs it): a short code for `optimize/music.sh status`
 * and the sentence the player and the home card show. what and extra are MediaPlayer's own numbers.
 */
final class PlayerFault {
    private PlayerFault() { }

    static final int SERVER_DIED = 100;
    static final int IO = -1004, MALFORMED = -1007, UNSUPPORTED = -1010, TIMED_OUT = -110;

    /** io | timeout | malformed | unsupported | server_died | unknown */
    static String code(int what, int extra) {
        if (what == SERVER_DIED) return "server_died";
        switch (extra) {
            case IO: return "io";
            case TIMED_OUT: return "timeout";
            case MALFORMED: return "malformed";
            case UNSUPPORTED: return "unsupported";
            default: return "unknown";
        }
    }

    /** the sentence for the screen: what failed, for a station or for a surah */
    static String message(boolean radio, int what, int extra) {
        switch (code(what, extra)) {
            case "io":
            case "timeout":
                return radio ? sa.urtv.home.S.t("تعذّر الاتصال بالبث، تحقق من الإنترنت") : sa.urtv.home.S.t("تعذّر تحميل السورة، تحقق من الإنترنت");
            case "malformed":
            case "unsupported":
                return radio ? sa.urtv.home.S.t("صيغة البث غير مدعومة") : sa.urtv.home.S.t("ملف السورة غير صالح");
            case "server_died":
                return sa.urtv.home.S.t("توقف المشغّل");
            default:
                return radio ? sa.urtv.home.S.t("تعذّر تشغيل البث") : sa.urtv.home.S.t("تعذّر تشغيل السورة");
        }
    }
}
