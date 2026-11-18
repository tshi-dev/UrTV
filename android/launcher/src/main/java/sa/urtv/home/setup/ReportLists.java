package sa.urtv.home.setup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The report's «tcl-safe: N off, …» lines (SetupReport), as optimize/share.py writes them: a package that was already off before UrTV touched the TV
 * (the backup's «disabled packages», taken before anything was changed) is never counted as one we turned off. Reports had listed a maker's own
 * disabled apps (Nokia, TCL BeyondTV) as our changes. No Android in here (tests/report_lists_test.py runs it).
 */
final class ReportLists {
    private ReportLists() { }

    /** the lines for one list: nothing when none of its packages is on this TV in any state */
    static List<String> lines(String name, List<String> pkgs, Set<String> installed, Set<String> disabled, Set<String> before) {
        List<String> off = new ArrayList<>(), already = new ArrayList<>(), on = new ArrayList<>(), out = new ArrayList<>();
        int absent = 0;
        for (String pk : pkgs) {
            if (disabled.contains(pk)) {
                List<String> to = before != null && before.contains(pk) ? already : off;
                if (!to.contains(pk)) to.add(pk);
            } else if (installed.contains(pk)) { if (!on.contains(pk)) on.add(pk); }
            else absent++;
        }
        if (off.isEmpty() && already.isEmpty() && on.isEmpty() && absent == 0) return out;
        out.add(name + ": " + off.size() + " off, " + on.size() + " still on, " + absent + " not on this TV"
            + (already.isEmpty() ? "" : ", " + already.size() + " were already off before UrTV (not ours)"));
        if (!off.isEmpty()) out.add("  off: " + String.join(", ", off));
        if (!already.isEmpty()) out.add("  already off before UrTV: " + String.join(", ", already));
        if (!on.isEmpty()) out.add("  on: " + String.join(", ", on));
        return out;
    }
}
