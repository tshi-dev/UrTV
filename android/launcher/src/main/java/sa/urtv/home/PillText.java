package sa.urtv.home;

/**
 * What the home screen's pills say, with no Android in it (tests/PillTextRun.java runs it). A pill shows its icon only; the
 * focused one widens to show its name (Pill.java), so the text a pill has and the text it shows are two things: this is both.
 */
final class PillText {
    private PillText() {}

    /** the words on screen: the name while the pill has the focus, nothing (icon only) otherwise */
    static String shown(String label, boolean focused) { return focused ? label : ""; }

    /** the screensaver pill's name: OK starts the screensaver, a long OK opens its settings, and the focused pill says so */
    static final String SAVER = S.n("شاشة التوقف · مطوّلًا: الإعدادات");

    /** «مؤقت النوم», or with the minutes left while the timer runs: «مؤقت النوم ٢٣ د» */
    static String sleep(int minutesLeft) {
        return minutesLeft <= 0 ? S.t("مؤقت النوم") : S.t("مؤقت النوم %s د", digits(String.valueOf(minutesLeft)));
    }

    /** the network pill's name: the cable, the Wi-Fi network by name when it can be read (else «واي فاي»), or «غير متصل» */
    static String network(boolean wired, boolean wifi, String ssid) {
        if (wired) return S.t("شبكة سلكية");
        if (!wifi) return S.t("غير متصل");
        String s = ssid == null ? "" : ssid.replace("\"", "");
        return s.isEmpty() || s.contains("unknown ssid") ? S.t("واي فاي") : s;
    }

    /** 0-9 as ٠-٩, the digits the whole home screen uses */
    static String digits(String s) {
        return DigitsText.convert(s, false);
    }
}
