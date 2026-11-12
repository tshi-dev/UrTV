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
 *   nokeyfilter  the accessibility service does not filter the remote's keys (every key reached the home through it, on the home's own main thread, when the TV's system takes the Settings key)
 *   nolift       a tile's focus does not lift it (no translationZ, so no elevation shadow)
 *   noglow       a tile's focus does not repaint the full-screen glow (animateGlow, fadeGlow)
 *   noshine      no shine fade on the focused tile
 *   noscale      no scale spring on the focused tile
 *   notitle      no app title (setText, placeName, its fade) for the focused tile
 *   plainfocus   all of the five off, and no label fade, no lift of the tile and no glide of the page: the highlight only
 *   nobackdrop   the photo, its dimming and the scrims are drawn each frame as before; by default they are ONE cached hardware layer (a static backdrop costs a single texture blit per frame instead of a photo and three translucent full-screen passes)
 *   keytrace     one log line per key on the home (tag UrTVKeyTrace): queue, dispatch, focus, layout, draw and the next six frames' intervals (KeyTrace)
 */
final class DebugFlags {
    private DebugFlags() { }

    static final String CHANGED = "sa.urtv.home.DEBUG_CHANGED";
    static final String[] NAMES = {"traynoglass", "nopillpanel", "nowifimark", "nosnapshot", "nocardglass", "nokeyfilter", "keytrace", "nolift", "noglow", "noshine", "noscale", "notitle", "plainfocus", "nobackdrop"};

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

    static final int LIFT = 1, GLOW = 2, SHINE = 4, SCALE = 8, TITLE = 16, PLAIN = 32;
    /** which parts of a tile's focus animation are off (read once when the home is built; the home starts over at each change) */
    static int focusMask(Context c) {
        int m = 0;
        if (on(c, "nolift")) m |= LIFT;
        if (on(c, "noglow")) m |= GLOW;
        if (on(c, "noshine")) m |= SHINE;
        if (on(c, "noscale")) m |= SCALE;
        if (on(c, "notitle")) m |= TITLE;
        if (on(c, "plainfocus")) m |= LIFT | GLOW | SHINE | SCALE | TITLE | PLAIN;
        return m;
    }

    static String describe(Context c) {
        StringBuilder b = new StringBuilder();
        for (String n : NAMES) b.append(b.length() > 0 ? ", " : "").append(n).append('=').append(on(c, n) ? "on" : "off");
        return b.toString();
    }
}
