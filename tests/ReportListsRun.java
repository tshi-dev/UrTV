package sa.urtv.home.setup;

import java.util.*;

/** For tests/report_lists_test.py: ReportLists (the report's lines per list) and SetupEngine.ledgerText's rule, on a plain JVM. */
public final class ReportListsRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        Set<String> installed = new HashSet<>(Arrays.asList("a.one", "a.two", "a.three", "a.four")), disabled = new HashSet<>(Arrays.asList("a.one", "a.two"));
        Set<String> before = new HashSet<>(Collections.singletonList("a.two"));
        List<String> l = ReportLists.lines("tcl-safe", Arrays.asList("a.one", "a.two", "a.three", "a.gone"), installed, disabled, before);
        check("an app off before UrTV is not counted as ours, and is named apart", l.get(0).equals("tcl-safe: 1 off, 1 still on, 1 not on this TV, 1 were already off before UrTV (not ours)")
            && l.contains("  off: a.one") && l.contains("  already off before UrTV: a.two") && l.contains("  on: a.three"));
        List<String> nob = ReportLists.lines("tcl-safe", Arrays.asList("a.one", "a.two"), installed, disabled, Collections.emptySet());
        check("no backup: as before", nob.get(0).equals("tcl-safe: 2 off, 0 still on, 0 not on this TV") && nob.contains("  off: a.one, a.two"));
        check("a list with nothing on this TV: no lines", ReportLists.lines("x", Arrays.asList("z.z"), installed, disabled, before).size() == 1
            && ReportLists.lines("x", Collections.emptyList(), installed, disabled, before).isEmpty());
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
