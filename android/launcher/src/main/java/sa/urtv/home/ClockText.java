package sa.urtv.home;

/**
 * The one place that writes a time of day for this app's screens, with no Android in it (tests/ClockTextRun.java runs it): the
 * screensaver's «ساعة ٢٤» setting is off (12-hour, «٧:٣٠ م», as always) or on («١٩:٣٠»). The home screen's clock, the sun line under
 * the greeting and the adhan times of «تلاوة وأذان» all go through here, so they agree. Digits are DigitsText's, hindi or latin.
 * An hour of 24 or more (an Isha after midnight, «24:30») wraps: 00:30 on a 24-hour clock, 12:30 ص on a 12-hour one.
 */
public final class ClockText {
    private ClockText() {}

    /** the setting's value as the page reads it: «&clock=24» is on, anything else (or nothing) is the 12-hour clock */
    public static boolean isOn(String setting) { return "24".equals(setting); }

    /** «٧:٣٠» or, on a 24-hour clock, «٠٧:٣٠»: the face of the home screen's clock, without the ص or م */
    public static String face(int h, int mi, boolean clock24, boolean latin) {
        h = Math.floorMod(h, 24);
        int shown = clock24 ? h : h % 12 == 0 ? 12 : h % 12;
        return DigitsText.convert((clock24 && shown < 10 ? "0" : "") + shown + ":" + (mi < 10 ? "0" : "") + mi, latin);
    }

    /** «ص» or «م», nothing on a 24-hour clock (the home screen hides the little label then) */
    public static String suffix(int h, boolean clock24) {
        return clock24 ? "" : Math.floorMod(h, 24) < 12 ? S.t("ص") : S.t("م");
    }

    /** «٧:٣٠ م», or «١٩:٣٠» */
    public static String time(int h, int mi, boolean clock24, boolean latin) {
        String s = suffix(h, clock24);
        return face(h, mi, clock24, latin) + (s.isEmpty() ? "" : " " + s);
    }

    /** the same from "HH:mm" (also "24:30"); a text that is not a time is returned as it is */
    public static String time(String hhmm, boolean clock24, boolean latin) {
        int colon = hhmm == null ? -1 : hhmm.indexOf(':');
        if (colon < 0) return hhmm;
        try {
            return time(Integer.parseInt(hhmm.substring(0, colon).trim()), Integer.parseInt(hhmm.substring(colon + 1).trim()), clock24, latin);
        } catch (NumberFormatException e) {
            return hhmm;
        }
    }
}
