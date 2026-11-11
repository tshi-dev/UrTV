package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Debug switches for a tester (`optimize/dock.sh debug <flag> [on|off]`, `debug all off`, `debug show`), flipped on a TV without a rebuild to find what makes the home janky:
 * each one turns one piece of the home's drawing off. Nothing is on by default and nothing here changes a setting of the person's; the home starts over when one changes
 * (DebugFlags.CHANGED), so it is read at the next draw. The names:
 *   traynoglass  the apps bar has no fill and no line (no glass behind the tiles)
 *   nopillpanel  the quick buttons' bar has no panel (its fill and line)
 *   nowifimark   the Wi-Fi mark is hidden
 *   nosnapshot   the home's last picture is neither shown at the start nor taken
 *   nocardglass  the cards keep their own look (no #hpanel and #hline from the tray's glass)
 */
final class DebugFlags {
    private DebugFlags() { }

    static final String CHANGED = "sa.urtv.home.DEBUG_CHANGED";
    static final String[] NAMES = {"traynoglass", "nopillpanel", "nowifimark", "nosnapshot", "nocardglass"};

    private static SharedPreferences sp(Context c) { return c.getApplicationContext().getSharedPreferences("debug_flags", Context.MODE_PRIVATE); }

    static boolean on(Context c, String name) {
        try { return sp(c).getBoolean(name, false); } catch (RuntimeException e) { return false; }
    }

    static boolean valid(String name) {
        if ("all".equals(name)) return true;
        for (String n : NAMES) if (n.equals(name)) return true;
        return false;
    }

    /** `all` is only ever turned off; returns false for a name that is not one */
    static boolean set(Context c, String name, boolean value) {
        if (!valid(name)) return false;
        SharedPreferences.Editor e = sp(c).edit();
        if ("all".equals(name)) { if (value) return false; for (String n : NAMES) e.putBoolean(n, false); }
        else e.putBoolean(name, value);
        e.commit();
        return true;
    }

    static String describe(Context c) {
        StringBuilder b = new StringBuilder();
        for (String n : NAMES) b.append(b.length() > 0 ? ", " : "").append(n).append('=').append(on(c, n) ? "on" : "off");
        return b.toString();
    }
}
