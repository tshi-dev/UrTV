package sa.urtv.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The favourites row: one set of rules for the home screen, its «ترتيب المفضلة» screen and ADB (DockReceiver). */
final class Dock {
    static final int MIN = 5;    // the row never shows fewer apps than this
    static final int MAX = 10;   // tiles share the row width, so 10 still fit at 1080p
    static final String CHANGED = "sa.urtv.home.DOCK_CHANGED";   // tells a running home screen to redraw

    private Dock() {}

    /** The launcher's settings file (named after MainActivity, which wrote it first). */
    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("MainActivity", Context.MODE_PRIVATE);
    }

    /** «الرجوع يفتح شاشة التوقف»: Back on our home starts the screensaver (HomeBack.startsSaver). Off until it is turned on (`dock.sh backsaver on`). */
    static boolean backSaver(Context c) { return prefs(c).getBoolean("back_saver", false); }

    static void setBackSaver(Context c, boolean on) { prefs(c).edit().putBoolean("back_saver", on).apply(); }

    /** This TV's remote, as RemoteButtons knows it (by maker, brand, model and Android version; read once). */
    static int remoteTv() {
        return RemoteButtons.tv(android.os.Build.MANUFACTURER, android.os.Build.BRAND, android.os.Build.MODEL, android.os.Build.DEVICE,
                android.os.Build.VERSION.SDK_INT);
    }

    /** «أزرار الريموت»: the app the owner gave one of this remote's buttons (RemoteButtons ids), empty = what it did before (`dock.sh key`). */
    static String keyApp(Context c, String key) { return prefs(c).getString("key_app_" + key, ""); }

    static void setKeyApp(Context c, String key, String pkg) { prefs(c).edit().putString("key_app_" + key, pkg == null ? "" : pkg).apply(); }

    /** the package is on this TV and enabled */
    static boolean installed(Context c, String pkg) {
        try {
            return c.getPackageManager().getApplicationInfo(pkg, 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * The rows in the screensaver's settings (ConfigSnapshot.keyApps, "youtube=PKG,netflix=,prime="; null = no answer): when they change, the
     * home's own choices take them. Only on a change, so `dock.sh key` (which sends both) and an older screensaver leave them as set.
     */
    static void followKeyApps(Context c, String tv) {
        if (tv == null) return;
        SharedPreferences p = prefs(c);
        if (tv.equals(p.getString("key_apps_tv", null))) return;
        SharedPreferences.Editor e = p.edit().putString("key_apps_tv", tv);
        for (String part : tv.split(",")) {
            int eq = part.indexOf('=');
            if (eq < 0) continue;
            String k = part.substring(0, eq), v = part.substring(eq + 1);
            if (RemoteButtons.byId(remoteTv(), k) != null && KeyApps.validChoice(v)) e.putString("key_app_" + k, v);
        }
        e.apply();
    }

    /** every key's choice, for RemoteKeys */
    static Map<String, String> keyApps(Context c) {
        Map<String, String> m = new LinkedHashMap<>();
        for (RemoteButtons.Button b : RemoteButtons.buttons(remoteTv())) m.put(b.id, keyApp(c, b.id));
        return m;
    }
    /**
     * The row in the screensaver's settings (ConfigSnapshot.backSaver; null = no real answer): when it changes, the switch above takes its
     * value. Only on a change, so `dock.sh backsaver` (which sends both) and an older screensaver that says nothing leave it as set.
     */
    static void followBackSaver(Context c, Boolean tv) {
        SharedPreferences p = prefs(c);
        if (!HomeBack.rowChanged(p.getBoolean("back_saver_tv", false), tv)) return;
        p.edit().putBoolean("back_saver", tv).putBoolean("back_saver_tv", tv).apply();
    }

    /** Saved order, or the defaults from config.xml; may name apps that are no longer installed. */
    static List<String> saved(Context c) {
        String s = prefs(c).getString("dock", null);
        return s != null ? Arrays.asList(s.isEmpty() ? new String[0] : s.split(","))
                         : Arrays.asList(c.getResources().getStringArray(R.array.dock_apps));
    }

    /** The stock home screens of Google TV and Android TV: they list only TV (leanback) apps. */
    static final String[] GOOGLE_HOMES = {
        "com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher", "com.google.android.leanbacklauncher",
    };

    /**
     * Every app the home screen can open, one activity per package: its TV activity (LEANBACK_LAUNCHER) if it has one,
     * else its ordinary launcher activity. The same list for the grid, «ترتيب المفضلة» and ADB.
     * Queried with GET_RESOLVED_FILTER: {@link #isTv} tells which kind each one is. {@link #leftOut} says why the
     * others are not in it.
     */
    static List<ResolveInfo> catalogue(Context c) {
        return scan(c, null, null);
    }

    /** The launchable apps hidden with the "hidden" list (dock.sh hide, or the × of «تعديل التطبيقات»): «المخفية» lists these. */
    static List<ResolveInfo> hiddenApps(Context c) {
        List<ResolveInfo> out = new ArrayList<>();
        scan(c, null, out);
        return out;
    }

    /** The launchable apps that are not on the home screen, package -> why. */
    static Map<String, String> leftOut(Context c) {
        Map<String, String> why = new LinkedHashMap<>();
        scan(c, why, null);
        return why;
    }

    /**
     * Shows what the TV's own home screen would: next to a Google home (which lists only TV apps; counted if it is
     * installed, even disabled by install.sh), apps with only an ordinary launcher activity stay out, as they are
     * mostly the maker's helpers; next to a maker's own home (CVTE, MediaTek sets) they come in, as its store and
     * settings are among them. On top of that rule: the hidden apps in config.xml, other home screens, and what was
     * hidden or shown from ADB (optimize/dock.sh hide / show).
     */
    private static List<ResolveInfo> scan(Context c, Map<String, String> why, List<ResolveInfo> hiddenOut) {
        PackageManager pm = c.getPackageManager();
        Set<String> config = new HashSet<>(Arrays.asList(c.getResources().getStringArray(R.array.hidden_apps)));
        Set<String> hidden = prefSet(c, "hidden"), shown = prefSet(c, "shown");
        Set<String> homes = new HashSet<>();
        for (ResolveInfo ri : pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)) {
            homes.add(ri.activityInfo.packageName);
        }
        homes.remove(c.getPackageName());
        boolean googleHome = false;
        for (String g : GOOGLE_HOMES) {   // installed, enabled or not: the HOME query above skips a disabled stock home
            try {
                pm.getPackageInfo(g, 0);
                googleHome = true;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        Map<String, ResolveInfo> out = new LinkedHashMap<>();
        Set<String> seenHidden = new HashSet<>();   // a hidden app found as a TV activity is not counted again as an ordinary one
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(q, PackageManager.GET_RESOLVED_FILTER)) {
                String p = ri.activityInfo.packageName;
                if (p.equals(c.getPackageName()) || out.containsKey(p) || (why != null && why.containsKey(p)) || seenHidden.contains(p)) continue;
                String reason = hidden.contains(p) ? "hidden with dock.sh hide or the × of «تعديل التطبيقات»"
                        : shown.contains(p) ? null
                        : config.contains(p) ? "hidden in config.xml"
                        : homes.contains(p) ? "a home screen"
                        : UsbViewers.is(p) ? null   // 4.3 B8: the TV's viewer of a USB stick is always in the row (issue #50)
                        : googleHome && category.equals(Intent.CATEGORY_LAUNCHER)
                                ? "no TV activity, and the TV's own home lists only TV apps" : null;
                if (reason == null) out.put(p, ri);
                else if (why != null) why.put(p, reason);
                if (hidden.contains(p) && seenHidden.add(p) && hiddenOut != null) hiddenOut.add(ri);
            }
        }
        return new ArrayList<>(out.values());
    }

    /** Packages hidden ("hidden") or shown against the rule ("shown") from ADB. */
    static Set<String> prefSet(Context c, String key) {
        String s = prefs(c).getString(key, "");
        return s.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(s.split(",")));
    }

    /** Hide or show these packages: one list gains them, the other loses them; the home screen redraws. */
    static void hideOrShow(Context c, List<String> pkgs, boolean hide) {
        Set<String> add = prefSet(c, hide ? "hidden" : "shown"), drop = prefSet(c, hide ? "shown" : "hidden");
        add.addAll(pkgs);
        drop.removeAll(pkgs);
        prefs(c).edit().putString(hide ? "hidden" : "shown", String.join(",", add))
                .putString(hide ? "shown" : "hidden", String.join(",", drop)).apply();
        c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));
    }

    /** «كل التطبيقات» in the owner's order («تعديل التطبيقات»), or null: by name, as always (AppsEdit.arrange). */
    static String appsOrder(Context c) {
        return prefs(c).getString("apps_order", null);
    }

    /**
     * What «تعديل التطبيقات» did, kept at once and the home screen told once: the grid's order (null: as it was), the apps hidden and
     * shown again (the lists dock.sh hide / show keep; a hidden app never keeps a place in the order), and the favourites (null: as they were).
     */
    static void saveApps(Context c, List<String> order, List<String> hide, List<String> show, List<String> row) {
        SharedPreferences.Editor e = prefs(c).edit();
        if (order != null) e.putString("apps_order", String.join(",", order));
        if (!hide.isEmpty() || !show.isEmpty()) {   // as hideOrShow (dock.sh hide / show), both in this one edit
            Set<String> hidden = prefSet(c, "hidden"), shown = prefSet(c, "shown");
            hidden.addAll(hide);
            hidden.removeAll(show);
            shown.removeAll(hide);
            shown.addAll(show);
            e.putString("hidden", String.join(",", hidden)).putString("shown", String.join(",", shown));
        }
        if (row != null) e.putString("dock", String.join(",", row));
        e.apply();
        c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));
    }

    /** «رجوع للترتيب الأصلي»: «كل التطبيقات» by name again (what is hidden stays hidden: «المخفية» brings it back). */
    static void resetAppsOrder(Context c) {
        prefs(c).edit().remove("apps_order").apply();
        c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));
    }

    /** Found as a TV activity (else as an ordinary launcher one): it is started with that same category. */
    static boolean isTv(ResolveInfo ri) {
        return ri.filter != null && ri.filter.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
    }

    /** Apps the home screen can show, package -> name, from {@link #catalogue}. */
    static Map<String, String> launchable(Context c) {
        Map<String, String> out = new LinkedHashMap<>(), given = AppNames.given(c);
        for (ResolveInfo ri : catalogue(c)) out.put(ri.activityInfo.packageName, AppNames.label(c, ri, given));
        return out;
    }

    /** The row as the home screen shows it: saved apps still installed, topped up to MIN alphabetically. */
    static List<String> shown(Context c, Map<String, String> apps) {
        List<String> row = new ArrayList<>();
        for (String p : saved(c)) if (apps.containsKey(p) && !row.contains(p) && row.size() < MAX) row.add(p);
        List<String> rest = new ArrayList<>(apps.keySet());
        rest.removeAll(row);
        Collections.sort(rest, (x, y) -> apps.get(x).compareToIgnoreCase(apps.get(y)));
        while (row.size() < MIN && !rest.isEmpty()) row.add(rest.remove(0));
        return row;
    }

    static void save(Context c, List<String> row) {
        prefs(c).edit().putString("dock", String.join(",", row)).apply();
        c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));
    }

    static void reset(Context c) {
        prefs(c).edit().remove("dock").apply();
        c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));
    }
}
