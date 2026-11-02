package sa.urtv.screensaver;

/** For tests/clock_setting_test.py: the screensaver's side of «ساعة ٢٤» (ClockSetting.java) on a plain JVM. */
public final class ClockSettingRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("receiver: on and off are accepted", ClockSetting.valid("on") && ClockSetting.valid("off"), "");
        check("receiver: anything else is refused (24, ON, yes, 1, true, empty, null)",
                !ClockSetting.valid("24") && !ClockSetting.valid("ON") && !ClockSetting.valid("yes") && !ClockSetting.valid("1")
                && !ClockSetting.valid("true") && !ClockSetting.valid("") && !ClockSetting.valid(null), "");
        check("fragment: nothing while off, so the page and the URL are as they were", ClockSetting.fragment(false).isEmpty(), ClockSetting.fragment(false));
        check("fragment: &clock=24 while on", ClockSetting.fragment(true).equals("&clock=24"), ClockSetting.fragment(true));
        check("show: on and off", ClockSetting.describe(true).equals("on") && ClockSetting.describe(false).equals("off"), "");
        check("settings row «نظام الساعة»: two choices, «١٢ ساعة» first and «٢٤ ساعة»",
                ClockSetting.CHOICES.length == 2 && ClockSetting.CHOICES[0].equals("١٢ ساعة") && ClockSetting.CHOICES[1].equals("٢٤ ساعة"),
                String.join(",", ClockSetting.CHOICES));
        check("settings row: its value is the one set (off = «١٢ ساعة», on = «٢٤ ساعة»)",
                ClockSetting.label(false).equals("١٢ ساعة") && ClockSetting.label(true).equals("٢٤ ساعة"), ClockSetting.label(false) + " " + ClockSetting.label(true));
        check("settings row: the picker checks the choice that is set", ClockSetting.choice(false) == 0 && ClockSetting.choice(true) == 1, "");
        check("settings row: choosing «٢٤ ساعة» turns it on, «١٢ ساعة» off, and back again gives the same choice",
                ClockSetting.fromChoice(1) && !ClockSetting.fromChoice(0)
                && ClockSetting.choice(ClockSetting.fromChoice(0)) == 0 && ClockSetting.choice(ClockSetting.fromChoice(1)) == 1, "");
        check("settings row: on and off give the same fragment as the agent's command", ClockSetting.fragment(ClockSetting.fromChoice(1)).equals("&clock=24")
                && ClockSetting.fragment(ClockSetting.fromChoice(0)).isEmpty(), "");
        if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    }
}
