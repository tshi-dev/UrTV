package sa.urtv.home.setup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 4.4 «إنقاذ تلفزيونك» (UrTV Rescue, android/rescue): what «رجّع التلفزيون كما كان» sends over the TV's own adb, decided here with no Android in it (tests/RescuePlanRun.java).
 * Every package that is off now AND is ours to turn on: what our setup's ledger says it turned off (the home answers it, when the home is still there), and in any
 * case the TV's own homes, the setup wizard and the stock home's helpers (never the TV's own choices otherwise); Home given to the stock home (launcherx, else
 * tvlauncher, else nothing named); our accessibility entries out of the TV's list; the Home key. Never a removal, never a disable.
 */
final class RescuePlan {
    private RescuePlan() { }

    // the same names as SetupEngine's (written here: the rescue app is built without the engine; tests/rescue_test.py keeps them equal)
    static final String LAUNCHERX = "com.google.android.apps.tv.launcherx", LAUNCHERX_HOME = LAUNCHERX + "/.home.HomeActivity";
    static final String TVLAUNCHER = "com.google.android.tvlauncher", TVLAUNCHER_HOME = TVLAUNCHER + "/.MainActivity";
    static final String WRAITH = "com.google.android.tungsten.setupwraith";

    /** the TV's own homes and the setup wizard: always turned on when they are off (a TV with them off may not start) */
    static final String[] ALWAYS = {
        LAUNCHERX, TVLAUNCHER, WRAITH, "com.google.android.leanbacklauncher",
    };

    /** what was done, in plain words, and the commands */
    static final class Plan {
        final List<String> commands = new ArrayList<>();
        final List<String> enabled = new ArrayList<>();
        String home = "";
        boolean a11yChanged;
    }

    /**
     * {@code installed}, {@code off}: the TV's package lists (null: the TV did not say: nothing is planned); {@code ledger}: what our setup turned off (null when the home
     * did not answer); {@code a11y}: enabled_accessibility_services as the TV says it ("" or "null": none).
     */
    static Plan plan(Set<String> installed, Set<String> off, List<String> ledger, String a11y) {
        Plan p = new Plan();
        if (installed == null || off == null) return p;
        Set<String> want = new LinkedHashSet<>();
        for (String s : ALWAYS) want.add(s);
        for (String s : SetupLists.HOME_HELPERS) want.add(s);
        if (ledger != null) want.addAll(ledger);
        for (String pkg : want) {
            if (pkg == null || !pkg.matches("[A-Za-z0-9_.]+") || !off.contains(pkg)) continue;
            p.commands.add("pm enable " + pkg);
            p.enabled.add(pkg);
        }
        String stock = installed.contains(LAUNCHERX) ? LAUNCHERX_HOME : installed.contains(TVLAUNCHER) ? TVLAUNCHER_HOME : "";
        if (!stock.isEmpty()) {
            String pkg = stock.substring(0, stock.indexOf('/'));
            p.commands.add("cmd role add-role-holder --user 0 android.app.role.HOME " + pkg);
            p.commands.add("cmd package set-home-activity --user 0 " + stock);
            p.home = pkg;
        }
        String rest = WatchList.without(a11y, sa.urtv.home.ExtrasWords.SERVICE);
        String before = a11y == null || a11y.trim().equals("null") ? "" : a11y.trim();
        if (!rest.equals(before) && rest.matches("[A-Za-z0-9_.$/:]*")) {
            p.commands.add(rest.isEmpty() ? "settings delete secure enabled_accessibility_services" : "settings put secure enabled_accessibility_services '" + rest + "'");
            p.a11yChanged = true;
        }
        p.commands.add("input keyevent KEYCODE_HOME");
        return p;
    }

    /** a «pm list packages…; echo END_OF_LIST» answer: the names, or null when the end marker is missing (the TV did not say: never «nothing») */
    static Set<String> packages(String out) {
        if (out == null || !out.contains("END_OF_LIST")) return null;
        Set<String> s = new LinkedHashSet<>();
        for (String l : out.replace("\r", "").split("\n")) if (l.startsWith("package:")) s.add(l.substring(8).trim());
        return s;
    }

    /** the ledger's package names (disabled-by-us.txt and community-applied.txt as the home sends them: one per line) */
    static List<String> ledger(String text) {
        if (text == null) return null;
        List<String> out = new ArrayList<>();
        for (String l : text.split("\n")) { l = l.trim(); if (l.matches("[A-Za-z0-9_.]+")) out.add(l); }
        return out;
    }
}
