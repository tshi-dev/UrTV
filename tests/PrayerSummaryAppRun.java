package sa.urtv.screensaver;

/** For tests/prayer_summary_test.py: the screensaver's summary of each line of the case table (stdin: the five digits), one answer per line. */
public final class PrayerSummaryAppRun {
    public static void main(String[] a) throws Exception {
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in, "UTF-8"));
        java.io.PrintStream out = new java.io.PrintStream(System.out, true, "UTF-8");
        for (String l; (l = in.readLine()) != null; ) out.println(PrayerSummary.of(l, n -> String.valueOf((char) ('٠' + n))));
    }
}
