package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/**
 * The TV's list of accessibility services that are on (Settings.Secure enabled_accessibility_services: «pkg/cls:pkg/.Cls:…»), read with no
 * Android in it (tests/A11yListRun.java runs it). What A11yHeal decides from it: whether our service is in the list, what the list is without
 * ours (the nudge that makes Android bind the list again), which entries a list lost since a list seen before, and whether to heal.
 * Other apps' entries are never changed, re-ordered or re-added: their text goes back exactly as it was.
 */
public final class A11yList {
    private A11yList() { }

    /** What check() decides: nothing to do, heal (switch on and nudge), our service is not in the list, or we may not write the setting. */
    public static final String OK = "ok", HEAL = "heal", NOT_LISTED = "not_listed", NO_PERMISSION = "no_permission";

    /** The entries in order, as written (empty for null, "null" or ""). */
    static List<String> entries(String list) {
        List<String> out = new ArrayList<>();
        if (list == null || list.equals("null")) return out;
        for (String e : list.split(":")) if (!e.trim().isEmpty()) out.add(e.trim());
        return out;
    }

    /** «pkg/.Cls» and «pkg/pkg.Cls» are the same service. */
    static String full(String component) {
        int slash = component.indexOf('/');
        if (slash <= 0 || slash == component.length() - 1) return component;
        String pkg = component.substring(0, slash), cls = component.substring(slash + 1);
        return cls.startsWith(".") ? pkg + "/" + pkg + cls : component;
    }

    static boolean has(String list, String component) {
        String want = full(component);
        for (String e : entries(list)) if (full(e).equals(want)) return true;
        return false;
    }

    /** The list with every entry of these components taken out; the others as written, in their order. Empty when nothing else is left. */
    static String without(String list, String... components) {
        StringBuilder sb = new StringBuilder();
        for (String e : entries(list)) {
            boolean ours = false;
            for (String c : components) ours |= full(e).equals(full(c));
            if (ours) continue;
            if (sb.length() > 0) sb.append(':');
            sb.append(e);
        }
        return sb.toString();
    }

    /** The entries of `before` that `now` no longer has (reported only: never put back by us). */
    static List<String> lost(String before, String now) {
        List<String> out = new ArrayList<>();
        for (String e : entries(before)) if (!has(now, e) && !out.contains(e)) out.add(e);
        return out;
    }

    /**
     * Heal only when our service is in the list but Android does not run it, or the master switch (accessibility_enabled) is not 1, and only
     * with WRITE_SECURE_SETTINGS. `switchValue` is the setting as read (null when it is not set).
     */
    static String decide(boolean listed, String switchValue, boolean running, boolean canWrite) {
        if (!listed) return NOT_LISTED;
        if (running && "1".equals(switchValue)) return OK;
        return canWrite ? HEAL : NO_PERMISSION;
    }

    /**
     * After our own update (UpdateCarry): which of OUR components the list had right before the install and no longer has. Only these may be
     * added back, and only when the install is what lost them; another app's entry is never added, and an entry the owner removed before the
     * update is not in `before`, so it stays off.
     */
    static List<String> ownLost(String before, String now, String... ours) {
        List<String> out = new ArrayList<>();
        for (String c : ours) if (has(before, c) && !has(now, c) && !out.contains(c)) out.add(c);
        return out;
    }

    /** The list as written with these entries appended (each once); the others untouched, in their order. */
    static String with(String list, List<String> add) {
        StringBuilder sb = new StringBuilder();
        for (String e : entries(list)) {
            if (sb.length() > 0) sb.append(':');
            sb.append(e);
        }
        for (String c : add) {
            if (has(sb.toString(), c)) continue;
            if (sb.length() > 0) sb.append(':');
            sb.append(c);
        }
        return sb.toString();
    }
}
