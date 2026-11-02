package sa.urtv.home;

import android.content.Context;
import android.content.pm.ResolveInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * The name shown for an app: the one the owner's agent gave it on this TV (optimize/dock.sh name), else its own if it
 * has Arabic letters, else the Arabic one from res/values/app_names.xml, else its own. With «اللغة» English the app's own name comes
 * before the Arabic list (YouTube, not يوتيوب); a name the owner's agent gave stays theirs.
 */
final class AppNames {
    static final int MAX_LEN = 30;
    private static Map<String, String> table;   // package -> Arabic name, read once

    private AppNames() {}

    static String label(Context c, ResolveInfo ri) {
        return label(c, ri, given(c));
    }

    static String label(Context c, ResolveInfo ri, Map<String, String> given) {
        String set = given.get(ri.activityInfo.packageName);
        if (set != null) return set;
        String own = String.valueOf(ri.loadLabel(c.getPackageManager()));
        if (hasArabic(own) || S.en()) return own;
        String ar = table(c).get(ri.activityInfo.packageName);
        return ar != null ? ar : own;
    }

    /** Where the shown name comes from, for dock.sh names: agent, own, table. */
    static String source(Context c, ResolveInfo ri, Map<String, String> given) {
        if (given.containsKey(ri.activityInfo.packageName)) return "agent";
        String own = String.valueOf(ri.loadLabel(c.getPackageManager()));
        S.init(c);
        return !hasArabic(own) && !S.en() && table(c).containsKey(ri.activityInfo.packageName) ? "table" : "own";
    }

    /** Names set on this TV, kept as "pkg|name" lines in the home screen's preferences. */
    static Map<String, String> given(Context c) {
        Map<String, String> m = new HashMap<>();
        for (String row : Dock.prefs(c).getString("names", "").split("\n")) {
            int bar = row.indexOf('|');
            if (bar > 0) m.put(row.substring(0, bar), row.substring(bar + 1));
        }
        return m;
    }

    static void save(Context c, Map<String, String> m) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) b.append(e.getKey()).append('|').append(e.getValue()).append('\n');
        Dock.prefs(c).edit().putString("names", b.toString()).apply();
    }

    /** A name the agent may set: Arabic letters, one line, at most MAX_LEN characters, no separators. */
    static boolean valid(String name) {
        return name.length() <= MAX_LEN && hasArabic(name) && name.indexOf('|') < 0 && name.indexOf(';') < 0
            && name.indexOf('\n') < 0;
    }

    static boolean hasArabic(String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 0x0600 && ch <= 0x06FF) || (ch >= 0x0750 && ch <= 0x077F) || (ch >= 0xFB50 && ch <= 0xFEFC)) return true;
        }
        return false;
    }

    private static synchronized Map<String, String> table(Context c) {
        if (table == null) {
            Map<String, String> t = new HashMap<>();
            for (String row : c.getApplicationContext().getResources().getStringArray(R.array.app_names)) {
                int bar = row.indexOf('|');
                if (bar > 0) t.put(row.substring(0, bar).trim(), row.substring(bar + 1).trim());
            }
            table = t;
        }
        return table;
    }
}
