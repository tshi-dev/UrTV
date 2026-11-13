package sa.urtv.home.tilawah;

/**
 * 4.3.1 why a live station stopped, in one plain line (no Android in it: tests/radio_fault_test.py runs it). The reason comes from what the player
 * said (PlayerFault's code, the HTTP status when the server answered), whether the TV has a network, and what the pick has been through:
 * no internet; the station did not answer; its stream is not one this TV plays; it stopped after N tries. Never a raw exception text.
 * It also writes the one record the TV keeps for the report the person can send (record): station, category, HTTP code, Android SDK, decoder.
 * No address, no token, nothing personal.
 */
final class RadioFault {
    private RadioFault() { }

    enum Reason { NO_INTERNET, NO_ANSWER, UNSUPPORTED, RETRIES, UNKNOWN }

    /**
     * @param online   the TV has a network that reaches the internet now
     * @param code     PlayerFault.code: io | timeout | malformed | unsupported | server_died | unknown
     * @param http     the server's status when it answered with one (0 = none)
     * @param played   the station gave sound at least once since the pick
     * @param formatEveryTime every failure so far said its format is not playable
     * @param attempts tries and reconnects made since the pick
     */
    static Reason reason(boolean online, String code, int http, boolean played, boolean formatEveryTime, int attempts) {
        if (!online) return Reason.NO_INTERNET;
        if (http >= 400) return Reason.NO_ANSWER;
        boolean format = code.equals("malformed") || code.equals("unsupported");
        if (!played && formatEveryTime && format) return Reason.UNSUPPORTED;
        if (played && attempts > 1) return Reason.RETRIES;
        if (code.equals("io") || code.equals("timeout")) return Reason.NO_ANSWER;
        return Reason.UNKNOWN;
    }

    static String message(Reason r, int attempts) {
        switch (r) {
            case NO_INTERNET: return sa.urtv.home.S.t("لا يوجد اتصال بالإنترنت، تحقق من الشبكة");
            case NO_ANSWER: return sa.urtv.home.S.t("المحطة لا تردّ الآن، جرّب بعد قليل");
            case UNSUPPORTED: return sa.urtv.home.S.t("صيغة هذا البث غير مدعومة على هذا التلفزيون");
            case RETRIES: return sa.urtv.home.S.t("توقف البث بعد %s محاولات", String.valueOf(attempts));
            default: return sa.urtv.home.S.t("تعذّر تشغيل البث");
        }
    }

    /** the report's line: station=<id> category=<code> http=<n> sdk=<n> decoder=<name>, each value reduced to letters, digits and . _ - */
    static String record(String station, String code, int http, int sdk, String decoder) {
        return "station=" + clean(station) + " category=" + clean(code) + " http=" + Math.max(0, http) + " sdk=" + sdk + " decoder=" + clean(decoder);
    }

    private static String clean(String s) {
        String t = s == null ? "" : s.replaceAll("[^A-Za-z0-9._-]", "");
        if (t.isEmpty()) return "-";
        return t.length() > 48 ? t.substring(0, 48) : t;
    }
}
