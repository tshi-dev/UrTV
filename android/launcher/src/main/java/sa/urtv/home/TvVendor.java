package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/**
 * Which app on this TV is its live TV (tuner) and which one is its source picker, found when the menu opens instead of fixed to TCL's. The
 * candidates are tried in order and the first one the TV has and can open is used. No Android in it (tests/TvVendorRun.java runs it):
 * MainActivity hands it the question «is this one installed and can it be opened?».
 *
 * A candidate is a package ("com.tcl.suspension": its launcher entry) or one activity of it ("com.cultraview.setting/.SourceActivity", a
 * leading dot meaning the package's own). The value in res/values/config.xml (live_tv_package, input_picker_package) is tried first, so a
 * TCL is exactly what it was. Every package here is also in the manifest's {@code <queries>}, or Android 11 would hide it.
 */
final class TvVendor {
    /** The TVs' live TV apps: they also take the standard passthrough link of an HDMI input when it is named to them. */
    static final String[] LIVE_TV = {
            "com.tcl.tv",                       // TCL
            "com.cultraview.livetv",            // CultraView (MediaTek boards sold under many names)
            "com.mediatek.wwtv.tvcenter",       // MediaTek: Xiaomi (PatchWall), Hisense and Haier Android sets
            "com.mstar.tv.tvplayer.ui",         // MStar: Skyworth and many others
            "com.sony.dtv.tvx",                 // Sony
            "org.droidtv.playtv",               // Philips
            "com.google.android.tv",            // Google's Live Channels
    };

    /** The TVs' own source pickers (the screen of HDMI 1, HDMI 2, AV…). */
    static final String[] PICKERS = {
            "com.tcl.suspension",               // TCL
            "com.cultraview.setting/.SourceActivity",   // CultraView
    };

    interface Probe { boolean usable(String candidate); }

    private TvVendor() {}

    /** The configured one first (when set), then the known ones, each once; blanks left out. */
    static List<String> candidates(String configured, String[] known) {
        List<String> out = new ArrayList<>();
        if (configured != null && !configured.trim().isEmpty()) out.add(configured.trim());
        for (String k : known) if (!out.contains(k)) out.add(k);
        return out;
    }

    /** Every candidate the TV can open, in order. */
    static List<String> usable(String configured, String[] known, Probe probe) {
        List<String> out = new ArrayList<>();
        for (String c : candidates(configured, known)) {
            boolean ok;
            try { ok = probe.usable(c); } catch (RuntimeException e) { ok = false; }   // a TV that throws for one asks about the next
            if (ok) out.add(c);
        }
        return out;
    }

    /** The first candidate the TV can open, null when none. */
    static String first(String configured, String[] known, Probe probe) {
        List<String> u = usable(configured, known, probe);
        return u.isEmpty() ? null : u.get(0);
    }

    /** The package of a candidate ("" for null). */
    static String pkg(String candidate) {
        if (candidate == null) return "";
        int slash = candidate.indexOf('/');
        return slash < 0 ? candidate : candidate.substring(0, slash);
    }

    /** The full class name of a candidate that names an activity, null for a bare package. */
    static String cls(String candidate) {
        if (candidate == null) return null;
        int slash = candidate.indexOf('/');
        if (slash < 0 || slash == candidate.length() - 1) return null;
        String c = candidate.substring(slash + 1);
        return c.startsWith(".") ? candidate.substring(0, slash) + c : c;
    }
}
