package sa.urtv.screensaver;

/**
 * «ساعة ٢٤»: the clock and every time of day on the screens as 24-hour («١٩:٣٠») instead of 12-hour («٧:٣٠ م»). Off by default. It
 * goes to the page and the home screen in the settings fragment as {@code &clock=24}, only while the setting is on, so with it off
 * the fragment (and the page) is exactly what it was. The settings screen's row «نظام الساعة» (SettingsActivity) and the agent's
 * `optimize/saver.sh clock24 on|off` change the same setting. Plain Java on purpose: tests/ClockSettingRun.java runs it on a JVM.
 */
final class ClockSetting {
    private ClockSetting() { }

    /** what the receiver accepts: on or off, nothing else */
    static boolean valid(String v) { return "on".equals(v) || "off".equals(v); }

    /** What goes in the settings fragment: nothing while the setting is off (the page is then as it was). */
    static String fragment(boolean on) { return on ? "&clock=24" : ""; }

    /** «نظام الساعة», the settings screen's row: its two choices, the 12-hour clock (the default) first; the row's value is the one set */
    static final String[] CHOICES = {"١٢ ساعة", "٢٤ ساعة"};

    /** the choice of CHOICES that is set now: what the row shows as its value, and the one checked in its picker */
    static int choice(boolean on) { return on ? 1 : 0; }

    static String label(boolean on) { return CHOICES[choice(on)]; }

    /** the picker's choice i as the setting: «٢٤ ساعة» turns it on, anything else is the 12-hour clock */
    static boolean fromChoice(int i) { return i == 1; }

    /** the word optimize/saver.sh show prints */
    static String describe(boolean on) { return on ? "on" : "off"; }
}
