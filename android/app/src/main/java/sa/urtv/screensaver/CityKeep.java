package sa.urtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 4.4 (R-403): on some TCL Android 11 TVs the city went back to Riyadh after a power cycle. The city was saved with apply(), which queues the write:
 * a power cut soon after a change lost it, and a settings file the TV lost (or wrote back half) came back without it. The city is now written with
 * commit() (Prefs.setCity), and the same five keys go into a file of their own, also with commit(). The kept copy is read ONLY when the settings
 * file has no city at all (the first Prefs of a start): it never overwrites a city that is there, so it cannot bring an older one back (R-305).
 * The log has fixed words only (tag UrTVCity).
 */
final class CityKeep {
    private CityKeep() { }

    static final String FILE = "city_keep";
    static final String[] KEYS = {"cityName", "cityLat", "cityLon", "cityTz", "cityCountry"};

    /** the city just chosen, kept too (values in KEYS' order); false when the file could not be written */
    static boolean save(Context c, String... values) {
        SharedPreferences.Editor e = c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit();
        for (int i = 0; i < KEYS.length; i++) e.putString(KEYS[i], i < values.length && values[i] != null ? values[i] : "");
        return e.commit();
    }

    /** the settings file has no city: the kept one is written back into it (now); true when it was */
    static boolean restore(Context c, SharedPreferences settings) {
        if (settings.contains("cityName")) return false;
        SharedPreferences k = c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        if (!k.contains("cityName")) return false;
        SharedPreferences.Editor e = settings.edit();
        for (String key : KEYS) e.putString(key, k.getString(key, ""));
        boolean ok = e.commit();
        Log.i("UrTVCity", ok ? "city back from its kept copy" : "city kept copy not written back");
        return ok;
    }
}
