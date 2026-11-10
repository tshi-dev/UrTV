package sa.urtv.home;

/** tests/lowram_test.py: the weak-TV rules (a user report on 4.1, lag) on a JVM. Prints PASS/FAIL lines, then ALL PASS. */
public class LowRamRun {
    static int failed;
    static void check(String name, boolean ok, Object detail) { System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail)); if (!ok) failed++; }

    public static void main(String[] a) {
        check("Ram: Android's low-RAM flag is LOW whatever the memory", Ram.tier(true, 4096) == Ram.LOW, "");
        check("Ram: the 1.5–1.9 GB sets (1.68, 1.83) and 1999 MB are LOW", Ram.tier(false, 1680) == Ram.LOW && Ram.tier(false, 1830) == Ram.LOW && Ram.tier(false, 1450) == Ram.LOW && Ram.tier(false, 1999) == Ram.LOW, "");
        check("Ram: 2000 MB, the 2394 MB TCL and 2599 MB are MID", Ram.tier(false, 2000) == Ram.MID && Ram.tier(false, 2394) == Ram.MID && Ram.tier(false, 2599) == Ram.MID, "");
        check("Ram: 2600 MB, a «3 GB» TV (2.8 GB) are NORMAL; an unknown total (0) is NORMAL (as before)", Ram.tier(false, 2600) == Ram.NORMAL && Ram.tier(false, 2800) == Ram.NORMAL && Ram.tier(false, 0) == Ram.NORMAL, "");
        check("Ram: a forced tier is read by name; anything else is not a tier", Ram.named("low") == Ram.LOW && Ram.named("mid") == Ram.MID && Ram.named("normal") == Ram.NORMAL && Ram.named("auto") == -1 && Ram.named("x") == -1 && Ram.named(null) == -1, "");
        check("key filter: never on a weak TV; elsewhere as before (off with our home in front or a stock home on)",
            !SettingsKey.filterWanted(false, false, true) && SettingsKey.filterWanted(false, false, false) && !SettingsKey.filterWanted(true, false, false)
            && !SettingsKey.filterWanted(false, true, false), "");
        long m = 1_793_000_000_000L - 1_793_000_000_000L % 60_000L;   // the start of a minute
        check("clock: the minute tick, a time or zone change always redraw", ClockTick.redraw(false, m + 5_000, m + 6_000), "");
        check("clock: a Wi-Fi broadcast in the same minute does not; the next minute, or nothing drawn yet, does",
            !ClockTick.redraw(true, m + 1_000, m + 59_000) && ClockTick.redraw(true, m + 59_000, m + 60_000) && ClockTick.redraw(true, 0, m), "");
        check("markets live: at most every 60 s on a weak TV (30 s elsewhere as before), and the backoff stays",
            MarketData.liveDelay(0, true) == 60_000 && MarketData.liveDelay(0, false) == 30_000 && MarketData.liveDelay(2, true) == 120_000 && MarketData.liveDelay(1, false) == 60_000, MarketData.liveDelay(0, true));
        check("markets live by tier: LOW 60 s, MID 45 s, NORMAL 30 s; the backoff stays (60, 120)",
            MarketData.liveDelay(0, Ram.LOW) == 60_000 && MarketData.liveDelay(0, Ram.MID) == 45_000 && MarketData.liveDelay(0, Ram.NORMAL) == 30_000
            && MarketData.liveDelay(1, Ram.MID) == 60_000 && MarketData.liveDelay(2, Ram.MID) == 120_000, MarketData.liveDelay(0, Ram.MID));
        System.out.println(failed == 0 ? "ALL PASS" : failed + " FAILED");
    }
}
