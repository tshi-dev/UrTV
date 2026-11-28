package sa.urtv.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * 4.4 (R-501, docs/research/LAUNCHERS-GITHUB.md idea 4; an idea of aerial-tv, MIT, written our own way): some TVs' «startup managers» (Skyworth, Vidda)
 * turn an app's own boot receivers off, and then the adhan, the reminders, the Home key heal and the boot home never run after a reboot. The app that
 * owns a component may put it back itself: at the home's first show in a process, and again 5 s and 15 s later (a manager that acts at boot), every
 * receiver of ours that the manifest declares enabled but that is now set off goes back to its manifest state (DEFAULT). Nothing of another app is
 * read or touched, and a receiver the manifest itself declares off stays off. The log (UrTVReceivers) has fixed words and a count only.
 */
public final class ReceiverHeal {
    static final String TAG = "UrTVReceivers";
    static final long[] AT_MS = {0, 5_000, 15_000};
    private static boolean startedThisProcess;

    private ReceiverHeal() { }

    static void onStart(Context c) {
        if (startedThisProcess) return;
        startedThisProcess = true;
        final Context app = c.getApplicationContext();
        Handler h = new Handler(Looper.getMainLooper());
        for (long at : AT_MS) h.postDelayed(() -> new Thread(() -> check(app), "receiver-heal").start(), at);
    }

    static int check(Context c) {
        int n = 0;
        try {
            PackageManager pm = c.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(c.getPackageName(), PackageManager.GET_RECEIVERS | PackageManager.MATCH_DISABLED_COMPONENTS);
            if (pi.receivers == null) return 0;
            for (ActivityInfo r : pi.receivers) {
                ComponentName cn = new ComponentName(c.getPackageName(), r.name);
                if (!ReceiverState.heal(r.enabled, pm.getComponentEnabledSetting(cn))) continue;
                pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
                n++;
            }
            if (n > 0) Log.w(TAG, "receivers turned off by the TV put back: " + n);
        } catch (RuntimeException | PackageManager.NameNotFoundException e) {
            Log.w(TAG, "receiver check failed: " + e.getClass().getSimpleName());
        }
        return n;
    }
}
