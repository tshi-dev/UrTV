package sa.urtv.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The family lock's decisions, with no Android in them (tests/ParentalPolicyRun.java runs them). Everything here is OFF until the owner
 * sets a PIN and turns something on: with no PIN nothing asks for one, and with kids mode off every app is shown.
 *
 * A «target» is what the home screen is asked to open: an app's package name, or one of the «@» names for a pill or for our own screens
 * (a package name can't hold an «@», so they never meet). A launch from a tile, a row of the grid, the menu or a pill passes its targets
 * (the pill's own name, and the package it opens when it knows it); it asks for the PIN when any of them is locked and has not been
 * opened with the PIN since the home screen last came to the front.
 *
 * It is a family lock on what the home screen opens, not a full lock: the TV's own settings, the Play Store opened another way, the
 * remote's assistant button, ADB and another launcher all go around it.
 */
final class ParentalPolicy {
    private ParentalPolicy() {}

    /** «المصادر», «واي فاي», «الإعدادات» (the TV's), «المهام», «شاشة التوقف», «تلاوة وأذان», «مؤقت النوم», «مركز التحكم»: the pills that can be locked */
    static final String INPUTS = "@inputs", WIFI = "@wifi", TV_SETTINGS = "@tvsettings", TASKS = "@tasks", SAVER = "@saver",
            LISTEN = "@listen", SLEEP = "@sleep", CENTER = "@center";
    /** «قفل الإعدادات»: our own «ترتيب المفضلة» and the long-press menu of the tiles */
    static final String OWN_SETTINGS = "@urtv";
    /** «الرقابة الأبوية» itself: always asks once a PIN is set, it has no toggle */
    static final String PARENTAL = "@parental";

    /** The pills a parent can lock, in the order the settings list them. */
    static final String[] PILLS = {INPUTS, WIFI, TV_SETTINGS, TASKS, SAVER, LISTEN, SLEEP, CENTER};

    /** Where the home screen can be asked to open something from. */
    static final int TILE = 0, GRID = 1, MENU_ARRANGE = 2, MENU = 3, PILL_INPUTS = 4, PILL_WIFI = 5, PILL_TV_SETTINGS = 6, PILL_TASKS = 7,
            CARD_TASKS = 8, PILL_SAVER = 9, PILL_SAVER_SETTINGS = 10, PILL_LISTEN = 11, PILL_SLEEP = 12, INPUT_LIVE_TV = 13,
            INPUT_PICKER = 14, SLEEP_ACCESSIBILITY = 15, MENU_KEY = 16, PARENTAL_SCREEN = 17, ARRANGE_SCREEN = 18, MENU_UPDATE = 19, UPDATE_SCREEN = 20,
            REMOTE_APP = 21, REMOTE_SETTINGS = 22, PILL_CENTER = 23, PILL_UPDATES = 24, OUR_SETTINGS = 25, MENU_APP = 26;

    /**
     * The targets a launch path asks about. pkg: the app it opens, where it knows it (a tile, a row, a package the pill opens); the TV's
     * settings and Wi-Fi pills pass the package their intent resolves to.
     */
    static String[] targets(int path, String pkg) {
        List<String> t = new ArrayList<>();
        switch (path) {
            case TILE:
            case GRID:
            case PILL_UPDATES:   // the updates bell opens the Play Store: locked by that app, like its tile
            case REMOTE_APP:   // a shortcut key of the remote (YouTube, Netflix): locked exactly as the app's own tile is
                break;
            case MENU:
            case MENU_ARRANGE:
            case ARRANGE_SCREEN:
            case MENU_UPDATE:
            case UPDATE_SCREEN:
            case OUR_SETTINGS:   // the «إعدادات تلفزيونك» tile of «مركز التحكم» and the remote's Settings key: our settings screen, behind «قفل الإعدادات»
                t.add(OWN_SETTINGS);
                break;
            case MENU_APP:   // the menu's «تفاصيل التطبيق» and «حذف التطبيق»: Android's own screens for that app, so «قفل الإعدادات», the TV's settings' lock and the app's own
                t.add(OWN_SETTINGS);
                t.add(TV_SETTINGS);
                break;
            case PARENTAL_SCREEN:
                t.add(PARENTAL);
                break;
            case PILL_INPUTS:
            case MENU_KEY:
                t.add(INPUTS);
                break;
            case PILL_WIFI:
                t.add(WIFI);
                break;
            case PILL_TV_SETTINGS:
            case REMOTE_SETTINGS:
            case SLEEP_ACCESSIBILITY:
                t.add(TV_SETTINGS);
                break;
            case PILL_TASKS:
            case CARD_TASKS:
                t.add(TASKS);
                break;
            case PILL_SAVER:
            case PILL_SAVER_SETTINGS:
                t.add(SAVER);
                break;
            case PILL_LISTEN:
                t.add(LISTEN);
                break;
            case PILL_SLEEP:
                t.add(SLEEP);
                break;
            case PILL_CENTER:   // opening «مركز التحكم»: its tiles for turning off now, freeing memory, the speed test and the sound output are covered by this
                t.add(CENTER);
                break;
            case INPUT_LIVE_TV:
            case INPUT_PICKER:
                break;
            default:
                break;
        }
        if (pkg != null && !pkg.isEmpty()) t.add(pkg);
        return t.toArray(new String[0]);
    }

    /** What `dock.sh pin lock/unlock` may name: an app's package, one of the pills, or «قفل الإعدادات» (@urtv). Never anything else in the lists. */
    static boolean validTarget(String t) {
        if (t == null) return false;
        if (t.equals(OWN_SETTINGS)) return true;
        for (String p : PILLS) if (p.equals(t)) return true;
        return t.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+");
    }

    /** The «locked:» line of `pin status`: the list is kept when the PIN is off, and the line says plainly that nothing asks then. */
    static String lockedLine(State s) {
        if (s.locked.isEmpty()) return "none";
        return join(s.locked) + (s.pinSet ? "" : " (kept, but nothing asks while there is no PIN)");
    }

    /** What the owner set. Nothing set (no PIN, no kids mode) is the home screen as it always was. */
    static final class State {
        final boolean pinSet, kids;
        final Set<String> locked, allowed;

        State(boolean pinSet, boolean kids, Set<String> locked, Set<String> allowed) {
            this.pinSet = pinSet;
            this.kids = kids;
            this.locked = locked == null ? new HashSet<>() : locked;
            this.allowed = allowed == null ? new HashSet<>() : allowed;
        }

        static State nothingSet() { return new State(false, false, null, null); }
    }

    /**
     * The state from what is kept: whether a PIN is set, the kids-mode flag and the two lists. Kids mode counts only with a PIN (without
     * one it could never be left), and the lists mean nothing without one: a TV that never had a PIN is the home screen as it always was.
     */
    static State stateFrom(boolean pinSet, boolean kidsKept, String lockedCsv, String allowedCsv) {
        return new State(pinSet, pinSet && kidsKept, parse(lockedCsv), parse(allowedCsv));
    }

    /** The targets opened with the PIN since the home screen last came to the front. */
    static final class Session {
        private final Set<String> open = new HashSet<>();

        synchronized void unlock(String... targets) { Collections.addAll(open, targets); }

        synchronized boolean has(String target) { return open.contains(target); }

        synchronized void clear() { open.clear(); }
    }

    /** Whether opening these targets asks for the PIN now. */
    static boolean needsPin(State s, Session session, String... targets) {
        if (!s.pinSet) return false;
        for (String t : targets) {
            if (session.has(t)) continue;
            if (PARENTAL.equals(t) || s.locked.contains(t)) return true;
        }
        return false;
    }

    /** Kids mode: only the apps marked as allowed are on the home screen. Off, every app is. */
    static boolean visible(State s, String pkg) {
        return !s.kids || s.allowed.contains(pkg);
    }

    static List<String> visible(State s, List<String> pkgs) {
        List<String> out = new ArrayList<>();
        for (String p : pkgs) if (visible(s, p)) out.add(p);
        return out;
    }

    /** Leaving kids mode asks for the PIN, always (the session does not count); turning it on needs a PIN and an app to show. */
    static boolean leavingKidsNeedsPin(State s) { return s.kids && s.pinSet; }

    static boolean canStartKids(State s) { return s.pinSet && !s.allowed.isEmpty(); }

    /** The lists are kept as «a,b,c»; only names that are targets or package names are read back. */
    static Set<String> parse(String csv) {
        Set<String> out = new HashSet<>();
        if (csv == null) return out;
        for (String p : csv.split(",")) {
            p = p.trim();
            if (p.matches("@?[A-Za-z0-9_.]+")) out.add(p);
        }
        return out;
    }

    static String join(Set<String> set) {
        List<String> l = new ArrayList<>(set);
        Collections.sort(l);
        return String.join(",", l);
    }
}
