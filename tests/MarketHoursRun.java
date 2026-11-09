package sa.urtv.screensaver;

/** For tests/your_tv_hub_test.py: MarketHours on a plain JVM. Prints open/closed for fixed instants (UTC millis, Riyadh is UTC+3). */
public final class MarketHoursRun {
    public static void main(String[] a) {
        for (String s : a) System.out.println(MarketHours.open(java.time.Instant.parse(s).toEpochMilli()) ? "open" : "closed");
    }
}
