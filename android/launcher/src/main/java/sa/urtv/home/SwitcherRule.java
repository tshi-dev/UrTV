package sa.urtv.home;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 4.5 «التطبيقات المفتوحة» (mockup docs/mockups/tools-45/03-switcher.png): which apps it lists and may stop, how, and its memory line. Without root an
 * app can only be stopped for good by the shell (`am force-stop`, over the home's own ADB link, which the setup made); without that link only its
 * background processes (ActivityManager.killBackgroundProcesses), and the screen says so (03b). Never our own apps, the TV's home, its setup wizard,
 * the system, an input method or what keeps the TV's inputs (the lists the setup already protects). The idea is tvOS Launcher's recent-apps
 * overlay (github.com/techgenius1/tvOS-Launcher, Apache-2.0); the code is ours. No Android in here (tests/SwitcherRuleRun.java runs it).
 */
final class SwitcherRule {
    private SwitcherRule() { }

    /** at most this many cards (a TV row) */
    static final int MAX = 8;

    /** a package name as Android writes them */
    static boolean validPkg(String p) { return p != null && p.length() <= 200 && p.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"); }

    /**
     * May the switcher list and stop it: a valid name, not ours (sa.urtv.*), not Android's own (android, com.android.*, com.google.android.gms,
     * the Play Store), not a home or the setup wizard ({@code homes}), not one the setup keeps on ({@code keep}: inputs, keyboards, the
     * protected features), not an input method ({@code imes}).
     */
    static boolean stoppable(String pkg, Collection<String> homes, Collection<String> keep, Collection<String> imes) {
        if (!validPkg(pkg)) return false;
        if (pkg.startsWith("sa.urtv.") || pkg.equals("android") || pkg.startsWith("com.android.")) return false;
        if (pkg.equals("com.google.android.gms") || pkg.equals("com.android.vending") || pkg.equals("com.google.android.tungsten.setupwraith")) return false;
        return !homes.contains(pkg) && !keep.contains(pkg) && !imes.contains(pkg);
    }

    /** the cards: the recently used apps, newest first, the ones that may not be stopped left out, at most MAX, each once */
    static List<String> cards(List<String> newestFirst, Collection<String> homes, Collection<String> keep, Collection<String> imes) {
        List<String> out = new ArrayList<>();
        for (String p : newestFirst) {
            if (out.size() >= MAX) break;
            if (!out.contains(p) && stoppable(p, homes, keep, imes)) out.add(p);
        }
        return out;
    }

    /** the shell commands that stop these for good (each checked again: nothing else ever reaches the shell) */
    static List<String> stopCommands(List<String> pkgs, Collection<String> homes, Collection<String> keep, Collection<String> imes) {
        List<String> out = new ArrayList<>();
        for (String p : pkgs) if (stoppable(p, homes, keep, imes)) out.add("am force-stop " + p);
        return out;
    }

    /** the memory line's numbers, «used of total» in GB with one decimal (Latin digits: the screen turns them into the person's digits) */
    static String[] memory(long availBytes, long totalBytes) {
        if (totalBytes <= 0 || availBytes < 0 || availBytes > totalBytes) return null;
        double gb = 1024.0 * 1024 * 1024;
        return new String[]{String.format(Locale.ROOT, "%.1f", (totalBytes - availBytes) / gb), String.format(Locale.ROOT, "%.1f", totalBytes / gb)};
    }

    /** how full, 0..100, for the bar */
    static int percent(long availBytes, long totalBytes) {
        if (totalBytes <= 0 || availBytes < 0 || availBytes > totalBytes) return 0;
        return (int) Math.round(100.0 * (totalBytes - availBytes) / totalBytes);
    }
}
