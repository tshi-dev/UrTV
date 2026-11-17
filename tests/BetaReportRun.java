package sa.urtv.home;

/** For tests/beta_report_test.py: Report.withChannel on a plain JVM. Prints the lines of each case, joined by "|". */
public final class BetaReportRun {
    public static void main(String[] args) {
        java.util.List<String> l = Report.lines("4.3.3", "4.3.3", "11", 30, "120");
        for (String c : new String[] {"beta", "latest", "next", null, "x\nTV: me"}) System.out.println(String.join("|", Report.withChannel(l, c)));
    }
}
