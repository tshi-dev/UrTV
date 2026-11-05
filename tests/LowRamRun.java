package sa.urtv.home;

/** tests/lowram_test.py: the weak-TV rules (a user report on 4.1, lag) on a JVM. Prints PASS/FAIL lines, then ALL PASS. */
public class LowRamRun {
    static int failed;
    static void check(String name, boolean ok, Object detail) { System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail)); if (!ok) failed++; }

    public static void main(String[] a) {
        check("Ram: Android's low-RAM flag is weak whatever the memory", Ram.low(true, 4096), "");
        check("Ram: a «2 GB» TV (about 1.9 GB in all) and a 1.5 GB one are weak; 2559 MB too", Ram.low(false, 1900) && Ram.low(false, 1450) && Ram.low(false, 2559), "");
        check("Ram: a «3 GB» TV (about 2.8 GB) and 2560 MB are not; an unknown total (0) is not (as before)", !Ram.low(false, 2800) && !Ram.low(false, 2560) && !Ram.low(false, 0), "");
        check("key filter: never on a weak TV; elsewhere as before (off with our home in front or a stock home on)",
            !SettingsKey.filterWanted(false, false, true) && SettingsKey.filterWanted(false, false, false) && !SettingsKey.filterWanted(true, false, false)
            && !SettingsKey.filterWanted(false, true, false), "");
        long m = 1_793_000_000_000L - 1_793_000_000_000L % 60_000L;   // the start of a minute
        check("clock: the minute tick, a time or zone change always redraw", ClockTick.redraw(false, m + 5_000, m + 6_000), "");
        check("clock: a Wi-Fi broadcast in the same minute does not; the next minute, or nothing drawn yet, does",
            !ClockTick.redraw(true, m + 1_000, m + 59_000) && ClockTick.redraw(true, m + 59_000, m + 60_000) && ClockTick.redraw(true, 0, m), "");
        check("markets live: at most every 60 s on a weak TV (30 s elsewhere as before), and the backoff stays",
            MarketData.liveDelay(0, true) == 60_000 && MarketData.liveDelay(0, false) == 30_000 && MarketData.liveDelay(2, true) == 120_000 && MarketData.liveDelay(1, false) == 60_000, MarketData.liveDelay(0, true));
        System.out.println(failed == 0 ? "ALL PASS" : failed + " FAILED");
    }
}
