package sa.urtv.home;

/**
 * The naming rule of the sources list, with no Android in it (tests/InputDisplayRun.java runs it): the name the owner gave an input, else the
 * name of the HDMI-CEC device behind its port (what the console or box reports about itself), else the TV's own; and « · وضع الاستعداد» after
 * it when the input is in standby. InputLabels reads the TV; this decides. The idea of showing the CEC name and the standby state is from
 * talkies (https://github.com/rajupp/talkies, MIT, (c) 2026 TechPP), see THIRD_PARTY_NOTICES.md.
 */
final class InputDisplay {
    static final int CEC_MAX = 40;   // characters (code points) kept of a device's own name
    static final String STANDBY = S.n(" · وضع الاستعداد");

    private InputDisplay() {}

    /**
     * A device's own name, made fit to show: trimmed, cut at CEC_MAX characters, "" when there is nothing usable (null, empty, more than one
     * line, a bar that would break the columns of «input-names», an invisible character by the rule of InputNames.clean, or a hardware id the TV
     * wrote instead of a name: a slash, or HW and digits as a word). The device chose the text, so it gets no benefit of the doubt.
     */
    static String cecName(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.indexOf('|') >= 0 || s.indexOf('/') >= 0 || !InputNames.plain(s) || s.matches("(?s).*\\bHW\\d*\\b.*")) return "";
        if (s.codePointCount(0, s.length()) > CEC_MAX) s = s.substring(0, s.offsetByCodePoints(0, CEC_MAX)).trim();
        return s;
    }

    /** The name without the state: the owner's, else the CEC device's (raw as the device reports it), else the TV's own. */
    static String name(String custom, String cecRaw, String own) {
        if (custom != null && !custom.isEmpty()) return custom;
        String cec = cecName(cecRaw);
        return cec.isEmpty() ? own : cec;
    }

    /** What the row shows: the name, and the standby mark when the input is in standby (whoever gave the name). */
    static String label(String custom, String cecRaw, String own, boolean standby) {
        String n = name(custom, cecRaw, own);
        return standby ? n + S.tv(STANDBY) : n;
    }
}
