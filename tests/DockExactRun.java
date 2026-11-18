package sa.urtv.home;

import java.util.*;

/** For tests/dock_exact_test.py: AppsEdit.floor and topUp, `dock.sh set` with 3 apps (cause 11). Prints PASS/FAIL lines. */
public final class DockExactRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        List<String> chosen = Arrays.asList("a.youtube", "b.netflix", "c.shahid"), rest = Arrays.asList("z.game1", "y.game2", "x.files");
        check("cause 11: an exact row of 3 is not topped up", AppsEdit.topUp(chosen, rest, AppsEdit.floor(true, 3, 5)).isEmpty());
        check("a row saved by the home's own editors keeps the floor of 5 (two added, by package)", AppsEdit.topUp(chosen, rest, AppsEdit.floor(false, 3, 5)).equals(Arrays.asList("x.files", "y.game2")));
        check("an exact row none of whose apps is on this TV is filled (never an empty row)", AppsEdit.floor(true, 0, 5) == 5);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
