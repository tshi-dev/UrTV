package sa.urtv.home.setup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 4.3.1 «التطبيقات المعطّلة»: which turned-off apps the page lists, and in what order, with no Android in it (tests/disabled_apps_test.py runs it). Every app the TV has turned
 * off by hand (the person's, UrTV's or the maker's: enabled state 2 or 3) is listed; one UrTV turned off (the ledger) carries a small note, the others none. An app that is only
 * «disabled until used» (state 4: a new app waiting for its first run) is NOT off and never listed, nor is Android itself or UrTV's own apps. This page only ever turns apps on.
 */
final class DisabledApps {
    private DisabledApps() { }

    /** PackageManager.COMPONENT_ENABLED_STATE_DISABLED (the maker's or a script's) and _DISABLED_USER (the person's, `pm disable-user`) */
    static final int DISABLED = 2, DISABLED_USER = 3;

    static final class Row {
        final String pkg, name;
        final boolean byUrtv;
        Row(String pkg, String name, boolean byUrtv) { this.pkg = pkg; this.name = name; this.byUrtv = byUrtv; }
    }

    /** whether the app counts as turned off: the person's or the maker's, not «disabled until used» and not on */
    static boolean off(int enabledState) { return enabledState == DISABLED || enabledState == DISABLED_USER; }

    static boolean ours(String pkg) { return pkg.equals("android") || pkg.startsWith("sa.urtv.") || pkg.equals("com.urtv.player"); }

    /** the rows: UrTV's own first (the person asked for those to come back most), then by name; names equal to the package are shown as the package */
    static List<Row> rows(List<String> pkgs, List<String> names, List<Integer> states, List<String> ledger) {
        List<Row> out = new ArrayList<>();
        for (int i = 0; i < pkgs.size(); i++) {
            String p = pkgs.get(i);
            if (p == null || ours(p) || !off(states.get(i))) continue;
            String n = names.get(i) == null || names.get(i).trim().isEmpty() ? p : names.get(i).trim();
            out.add(new Row(p, n, ledger.contains(p)));
        }
        Collections.sort(out, (a, b) -> a.byUrtv != b.byUrtv ? (a.byUrtv ? -1 : 1) : a.name.toLowerCase(Locale.ROOT).compareTo(b.name.toLowerCase(Locale.ROOT)));
        return out;
    }

    /** how many of the rows were turned off by UrTV (the side note) */
    static int byUrtv(List<Row> rows) {
        int n = 0;
        for (Row r : rows) if (r.byUrtv) n++;
        return n;
    }
}
