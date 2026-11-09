package sa.urtv.home;

/** For tests/settings_report_test.py: Report on a plain JVM. Prints the lines (one a line), then "url <the link>", then the cases' links ("null" for none). */
public final class SettingsReportRun {
    public static void main(String[] args) {
        java.util.List<String> l = Report.lines("4.0.12", "4.0.12", "11", 30, "120.0.6099.230");
        for (String s : l) System.out.println(s);
        System.out.println("url " + Report.url(l));
        System.out.println("odd " + Report.lines("4.0 beta; rm -rf", null, "11\nTV: me", 30, "").get(1) + " | " + Report.lines("x", "y", "11\nTV: me", 30, "").get(2)
                + " | " + Report.lines("x", "y", "z", 30, null).get(3));
        java.util.List<String> t = Report.lines("4.0.12", "4.0.12", "11", 30, "120", "sound", "glass", "dark", "football", java.util.Arrays.asList("java.lang.IllegalStateException", "a b\nTV: me", "x.Y"));
        System.out.println("topic " + String.join("|", t.subList(4, t.size())));
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 4000; i++) big.append('9');
        java.util.List<String> longer = new java.util.ArrayList<>(l);
        longer.add("Extras: " + big);
        System.out.println("cut " + Report.url(longer).equals(Report.url(l)));
    }
}
