package sa.urtv.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * 4.4 (R-410, BootHomeRule): «ابدأ بالرئيسية بعد التشغيل». At BOOT_COMPLETED, a few seconds after the TV's own boot input came up, our home is brought to the
 * front: through «إضافات تلفزيونك» (its accessibility service may press Home: Android 10+ lets no receiver start an activity), else started directly (Android 9
 * and older). The log has fixed words only (tag UrTVBoot).
 */
public final class BootHome {
    private BootHome() { }

    static final String KEY = "boot_home", TAG = "UrTVBoot";
    /** the TV's boot input first: the bring comes after it */
    public static final long DELAY_MS = 8_000;

    static Boolean chosen(Context c) {
        SharedPreferences p = Dock.prefs(c);
        return p.contains(KEY) ? p.getBoolean(KEY, false) : null;
    }

    public static boolean on(Context c) { return BootHomeRule.on(chosen(c), Build.MANUFACTURER, Build.BRAND); }

    public static void set(Context c, boolean on) { Dock.prefs(c).edit().putBoolean(KEY, on).commit(); }

    /** on the boot receiver's own thread, after DELAY_MS; oursHome and setupBusy from the caller (the setup's files) */
    public static void bring(Context c, boolean oursHome, boolean setupBusy) {
        if (!BootHomeRule.act(on(c), oursHome, setupBusy)) { Log.i(TAG, "boot home: left as the TV booted"); return; }
        android.os.Bundle b = ExtrasTrust.call(c, ExtrasWords.GO_HOME, null, null);
        if (b != null && b.getBoolean(ExtrasWords.RESULT, false)) { Log.i(TAG, "boot home: brought through the add-on"); return; }
        try {
            c.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setClassName(c.getPackageName(), c.getPackageName() + ".MainActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Log.i(TAG, "boot home: started");
        } catch (RuntimeException e) {
            Log.i(TAG, "boot home: not allowed here");
        }
    }
}
