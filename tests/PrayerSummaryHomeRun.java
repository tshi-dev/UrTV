package sa.urtv.home.tilawah;

/** For tests/prayer_summary_test.py: the home's summary of each line of the case table (stdin: the five digits), one answer per line. */
public final class PrayerSummaryHomeRun {
    public static void main(String[] a) throws Exception {
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in, "UTF-8"));
        java.io.PrintStream out = new java.io.PrintStream(System.out, true, "UTF-8");
        String[] names = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
        for (String l; (l = in.readLine()) != null; ) {
            int[] st = new int[5];
            for (int i = 0; i < 5; i++) st[i] = l.charAt(i) - '0';
            out.println(PrayerScreen.summary(st, names, n -> String.valueOf((char) ('٠' + n))));
        }
    }
}
