package sa.urtv.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.util.List;

/**
 * Our accessibility services after an update of this app (the owner's TCL, Android 14): Android unbinds the services of a package that is
 * replaced and binds them again only when it is told the list changed or at the next boot; when nothing else is bound at that moment it also
 * writes accessibility_enabled=0 itself (AccessibilityManagerService.updateAccessibilityEnabledSettingLocked). The TV test saw exactly that:
 * our TypeService still in enabled_accessibility_services, the switch 0, nothing bound, until a reboot.
 *
 * So, after MY_PACKAGE_REPLACED, after a boot and when the home first shows: if one of our services is in the list but Android does not run it,
 * or the switch is not 1, and we hold WRITE_SECURE_SETTINGS (granted with Family DNS), the switch goes to 1 and the list is written again:
 * once without our entries, then exactly as it was, which is a change Android listens to and binds the list again. Other apps' entries are
 * never taken out or added: their text goes back as it was. An entry the list lost since we last saw it is only reported (tag UrTVA11y).
 */
public final class A11yHeal extends BroadcastReceiver {
    static final String TAG = "UrTVA11y";
    static final String TYPE = "sa.urtv.home/sa.urtv.home.TypeService", SLEEP = "sa.urtv.home/sa.urtv.home.SleepService";
    private static final String LIST = Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, SWITCH = Settings.Secure.ACCESSIBILITY_ENABLED;
    /** time for Android to bind a service it is going to bind anyway, before we judge it is not running */
    private static final long SETTLE_MS = 8_000;
    private static boolean checkedThisProcess;

    /** MY_PACKAGE_REPLACED and BOOT_COMPLETED (manifest; only the system sends them). */
    @Override public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        final String why = intent != null && Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ? "replaced" : "boot";
        final PendingResult pending = goAsync();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                try { check(app, why); } finally { pending.finish(); }
            }
        }, SETTLE_MS);
    }

    /** The home's first show in this process: the same check, a moment later. */
    static void onStart(final Context c) {
        if (checkedThisProcess) return;
        checkedThisProcess = true;
        final Context app = c.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { @Override public void run() { check(app, "start"); } }, SETTLE_MS);
    }

    /** One of our services is in the TV's list but Android does not run it (the phone page says so instead of doing nothing). */
    public static boolean stuck(Context c) {
        String list = read(c, LIST);
        return (A11yList.has(list, TYPE) && !TypeService.enabled()) || (A11yList.has(list, SLEEP) && !SleepService.enabled());
    }

    static void check(Context c, String why) {
        try {
            String list = read(c, LIST), sw = read(c, SWITCH);
            SharedPreferences sp = c.getSharedPreferences("a11y", Context.MODE_PRIVATE);
            List<String> lost = A11yList.lost(sp.getString("seen", null), list);
            if (!lost.isEmpty()) Log.w(TAG, "the list lost entries since we last saw it (not put back by us): " + lost);
            sp.edit().putString("seen", list == null ? "" : list).apply();
            boolean listed = A11yList.has(list, TYPE) || A11yList.has(list, SLEEP);
            boolean running = !stuck(c);   // each of ours that is listed is bound
            boolean canWrite = c.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
            String d = A11yList.decide(listed, sw, running, canWrite);
            Log.i(TAG, "check " + why + ": listed=" + listed + " switch=" + sw + " running=" + running + " -> " + d);
            if (!A11yList.HEAL.equals(d)) return;
            if (!"1".equals(sw)) Settings.Secure.putInt(c.getContentResolver(), SWITCH, 1);
            if (!running) {   // the same list again, through a change Android listens to: ours out for a moment, then the list exactly as it was
                Settings.Secure.putString(c.getContentResolver(), LIST, A11yList.without(list, TYPE, SLEEP));
                Settings.Secure.putString(c.getContentResolver(), LIST, list);
            }
            Log.i(TAG, "healed " + why + ": switch=" + read(c, SWITCH) + " list kept=" + list.equals(read(c, LIST)));
        } catch (RuntimeException e) {   // SecurityException: the permission was taken back meanwhile
            Log.w(TAG, "check " + why + " failed: " + e.getClass().getSimpleName());
        }
    }

    private static String read(Context c, String key) {
        try { return Settings.Secure.getString(c.getContentResolver(), key); } catch (RuntimeException e) { return null; }
    }
}
