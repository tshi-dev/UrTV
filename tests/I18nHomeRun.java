package sa.urtv.home;

/**
 * The words of the home's plain-Java classes, one «key<TAB>value» per line, for tests/i18n_home_test.py: run in Arabic (the default) they
 * must be what they were before «اللغة» (tests/i18n_home_cases.tsv), run with -Durtv.lang=en they must have no Arabic letter. Each value
 * is what a screen shows: a constant marked with S.n goes through S.tv, as the screens do.
 */
public final class I18nHomeRun {
    private static void p(String k, String v) { System.out.println(k + "\t" + (v == null ? "null" : v.replace("\n", "\\n"))); }

    public static void main(String[] args) throws Exception {
        p("control.name", S.tv(ControlText.NAME));
        p("control.mirror", S.tv(ControlText.MIRROR) + " / " + S.tv(ControlText.MIRROR_HINT));
        p("control.bluetooth", S.tv(ControlText.BLUETOOTH) + " / " + S.tv(ControlText.BLUETOOTH_HINT));
        p("control.mbps", ControlText.mbps(12.34) + " / " + ControlText.mbps(150));
        for (int i = 0; i <= 4; i++) p("control.signal." + i, ControlText.signal(i));
        p("control.net.wired", ControlText.networkDetail(true, false, 0));
        p("control.net.none", ControlText.networkDetail(false, false, 0));
        p("control.net.wifi", ControlText.networkDetail(false, true, -60));
        p("control.out.speaker", ControlText.outputs(new int[] {ControlText.BUILTIN_SPEAKER}, null));
        p("control.out.two", ControlText.outputs(new int[] {ControlText.BLUETOOTH_A2DP, ControlText.HDMI_ARC, ControlText.WIRED_HEADSET},
                new String[] {"JBL Flip", null, null}));
        p("control.out.none", ControlText.outputs(new int[0], null));
        p("control.sleepoff", S.tv(ControlText.SLEEP_OFF_NOTICE));
        p("pill.saver", S.tv(PillText.SAVER));
        p("pill.sleep", PillText.sleep(0) + " / " + PillText.sleep(23));
        p("pill.net", PillText.network(true, false, null) + " / " + PillText.network(false, false, null) + " / " + PillText.network(false, true, null)
                + " / " + PillText.network(false, true, "\"Home\""));
        for (int i = 0; i <= 3; i++) p("greeting." + i, GreetingText.greeting(i));
        int[] codes = {0, 1, 2, 3, 45, 51, 61, 65, 66, 71, 95, 96};
        for (int c : codes) p("sky." + c, GreetingText.sky(c));
        p("sky.dust", GreetingText.skyWord(0, 200) + " / " + GreetingText.skyWord(0, 500) + " / " + GreetingText.skyWord(99, 200));
        p("weather", GreetingText.weather(0, 0, "Riyadh", 31.2, true) + " / " + GreetingText.weather(99, 0, "Riyadh", 30, false)
                + " / " + GreetingText.weather(61, 450, "Riyadh", 25, true));
        p("clock", ClockText.time(19, 30, false, true) + " / " + ClockText.time(7, 5, false, false) + " / " + ClockText.time(19, 30, true, true));
        long m = 60_000L, base = 1_000_000_000_000L;
        long[] adhan = {base, base + 300 * m, base + 500 * m, base + 700 * m, base + 800 * m};
        p("bar.before", AdhanBarText.text(base - 8 * m, adhan, false, true));
        p("bar.iqama", AdhanBarText.text(base + 13 * m, adhan, false, false));
        p("bar.friday", AdhanBarText.text(base + 298 * m, adhan, true, true));
        p("bar.playing", AdhanBarText.playing(4, false, true) + " / " + AdhanBarText.playing(2, true, false) + " / " + AdhanBarText.playing(9, false, false));
        StringBuilder mins = new StringBuilder();
        for (int i = 1; i <= 12; i++) mins.append(i > 1 ? " / " : "").append(AdhanBarText.minutes(i, true));
        p("bar.minutes", mins.toString());
        p("zone", ClockZone.label("Dubai", "") + " / " + ClockZone.label("", ClockZone.calendar("Asia/Bahrain", null)) + " / "
                + ClockZone.label("Cairo", ClockZone.calendar(null, "jafari")));
        p("tvrows", S.tv(TvRowsPick.WATCH_NEXT_TITLE) + " / " + TvRowsPick.meta("2", "1", null, null, null, "Netflix", true));
        p("input", InputDisplay.label(null, null, "HDMI 1", true));
        for (FamilyDns.Result r : FamilyDns.Result.values()) p("familydns." + r, S.tv(FamilyDns.arabic(r)));
        try {   // new with «اللغة»: not in the cases of before
            p("hijri.en", (String) MonthNames.class.getDeclaredMethod("hijriEn", String.class, int.class, int.class, int.class)
                    .invoke(null, "Monday", 22, 4, 1448));
        } catch (NoSuchMethodException ignored) { }
        sa.urtv.home.tilawah.I18nHomeTilawahRun.main(args);
    }
}
