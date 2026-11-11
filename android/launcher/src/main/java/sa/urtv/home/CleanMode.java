package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

/**
 * «UrTV النظيف»: one switch that shows the essentials only. It is a MASK over the home: nothing is deleted and none of the person's own choices (the cards, the
 * buttons, the greeting) is written over; the home reads this and hides, so turning it off brings every one of them back exactly as it was. What stays is always the clock, the
 * date and the apps bar, and, by the person's «تخصيص», the Wi-Fi mark, the greeting and the settings button, and the prayer line or card. Only CleanActivity writes it, after a description and an
 * explicit confirmation (and the family PIN when one is set). The rules are plain Java (parse, join, defaults); the two reads touch only this app's own preferences.
 */
final class CleanMode {
    private CleanMode() { }

    static final String PREFS = "clean_mode", ON = "on", KEEP = "keep";
    static final String WIFI = CleanKeep.WIFI, GREETING = CleanKeep.GREETING, SETTINGS = CleanKeep.SETTINGS;
    static final String PRAYER = "prayer", PLACE = "place";   // «بطاقة الصلاة» and «مكان البطاقة»

    static Set<String> parse(String csv) { return CleanKeep.parse(csv); }

    static String join(Set<String> keep) { return CleanKeep.join(keep); }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static boolean on(Context c) { return prefs(c).getBoolean(ON, false); }

    static Set<String> keep(Context c) { return parse(prefs(c).getString(KEEP, null)); }

    static String prayer(Context c) { return CleanKeep.prayer(prefs(c).getString(PRAYER, null)); }

    static String place(Context c) { return CleanKeep.place(prefs(c).getString(PLACE, null)); }

    static void setPrayer(Context c, String v) { prefs(c).edit().putString(PRAYER, CleanKeep.prayer(v)).apply(); }

    static void setPlace(Context c, String v) { prefs(c).edit().putString(PLACE, CleanKeep.place(v)).apply(); }

    /** only CleanActivity calls these, behind its confirmation */
    static void setOn(Context c, boolean on) { prefs(c).edit().putBoolean(ON, on).apply(); }

    static void setKeep(Context c, Set<String> keep) { prefs(c).edit().putString(KEEP, join(keep)).apply(); }
}
