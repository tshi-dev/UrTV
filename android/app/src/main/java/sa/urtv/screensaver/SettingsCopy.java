package sa.urtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import java.util.Map;
import java.util.Set;

/**
 * 4.4 (a report: «a reinstall of the screensaver reset its preferences»). The home keeps a copy of this app's settings (it asks ConfigProvider's
 * «settings_copy», READ_CONFIG, and keeps the text in its own files: sa.urtv.home.SaverSettingsCopy). A fresh install of this app, with nothing of its
 * own saved yet, asks the home for it once («saver_settings_copy» on its now-playing provider, READ_NOW_PLAYING), on a thread, only from a home that
 * carries our own signature, and takes it in whole (SettingsCopyRule.decode) or not at all.
 */
final class SettingsCopy {
    private SettingsCopy() { }

    static final String HOME_PKG = "sa.urtv.home";
    static final String HOME_AUTHORITY = "sa.urtv.home.nowplaying";
    static final String TRIED = "settings_copy_tried", PENDING = "settings_copy_pending";
    private static volatile boolean busy;

    /**
     * ConfigProvider's answer to the home: the settings as text; null while a fresh install has not asked for the home's copy yet (the home must not
     * keep a fresh install's defaults over the person's settings before they came back)
     */
    static String export(Context c) {
        new Prefs(c);   // a fresh install starts its restore here when this is its first question
        if (busy || c.getSharedPreferences("hub_seen", Context.MODE_PRIVATE).getBoolean(PENDING, false)) return null;
        return SettingsCopyRule.encode(c.getSharedPreferences("settings", Context.MODE_PRIVATE).getAll());
    }

    /** a restore was asked for and has not been tried yet (the process ended before its thread ran) */
    static boolean pending(Context c) {
        Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        return app.getSharedPreferences("hub_seen", Context.MODE_PRIVATE).getBoolean(PENDING, false);
    }

    /** Prefs' first start on a fresh install: the home's copy is asked for on a thread, once; never on the UI thread */
    static void restoreSoon(Context c, Set<String> ownKeys) {
        final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        final SharedPreferences seen = app.getSharedPreferences("hub_seen", Context.MODE_PRIVATE);
        if (busy) return;
        boolean pending = seen.getBoolean(PENDING, false);
        if (!pending && !SettingsCopyRule.shouldRestore(ownKeys, seen.getBoolean(TRIED, false))) return;
        busy = true;
        if (!pending) seen.edit().putBoolean(PENDING, true).putBoolean(TRIED, true).apply();   // once per install: a person who resets the settings later is not overruled
        Thread t = new Thread(() -> {
            try {
                if (!homeIsOurs(app)) return;
                Bundle b = app.getContentResolver().call(Uri.parse("content://" + HOME_AUTHORITY), "saver_settings_copy", null, null);
                Map<String, Object> vals = SettingsCopyRule.decode(b == null ? null : b.getString("copy"));
                if (vals == null) return;
                SharedPreferences sp = app.getSharedPreferences("settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor e = sp.edit();
                for (Map.Entry<String, Object> v : vals.entrySet()) {
                    Object x = v.getValue();
                    if (x instanceof Boolean) e.putBoolean(v.getKey(), (Boolean) x);
                    else if (x instanceof Integer) e.putInt(v.getKey(), (Integer) x);
                    else if (x instanceof Long) e.putLong(v.getKey(), (Long) x);
                    else if (x instanceof Float) e.putFloat(v.getKey(), (Float) x);
                    else if (x instanceof String) e.putString(v.getKey(), (String) x);
                    else if (x instanceof Set) { @SuppressWarnings("unchecked") Set<String> s = (Set<String>) x; e.putStringSet(v.getKey(), s); }
                }
                if (!e.commit()) return;
                android.util.Log.i("UrTVSaver", "settings restored from the home's copy: " + vals.size() + " values");
                app.sendBroadcast(new android.content.Intent(Prefs.CHANGED).setPackage(app.getPackageName()));   // a dream or a page already up starts over with them
            } catch (RuntimeException ignored) {   // no home, an older one, refused: the defaults stay
            } finally {
                seen.edit().putBoolean(PENDING, false).commit();   // asked once: from now on the home may keep a copy of what is here
                busy = false;
            }
        }, "settings-restore");
        t.setDaemon(true);
        t.start();
    }

    /** the provider is the home's, and the home is signed with our key (a squatting app cannot hand us settings) */
    static boolean homeIsOurs(Context c) {
        try {
            PackageManager pm = c.getPackageManager();
            ProviderInfo pi = pm.resolveContentProvider(HOME_AUTHORITY, 0);
            return pi != null && HOME_PKG.equals(pi.packageName) && pm.checkSignatures(c.getPackageName(), HOME_PKG) == PackageManager.SIGNATURE_MATCH;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
