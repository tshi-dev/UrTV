package sa.urtv.home.tilawah;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * «شاشة التوقف وقت الأذان»: starts the screensaver the way the home's own screensaver button does (its full-screen preview, the same page, today's
 * settings). Only called while our home is in front, so Android lets the app start it from the alarm; Home or Back leaves it.
 */
final class AdhanSaver {
    private static final String PKG = "sa.urtv.screensaver";

    private AdhanSaver() { }

    /** true when it was started; never throws (an app that is not installed, or a start Android refuses, only costs the screensaver) */
    static boolean start(Context c) {
        try {
            Intent i = new Intent().setComponent(new ComponentName(PKG, PKG + ".MainActivity"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            if (c.getPackageManager().resolveActivity(i, 0) == null) return false;
            c.startActivity(i);
            Log.i("UrTVAdhan", "screensaver started at the adhan");
            return true;
        } catch (RuntimeException e) {
            Log.i("UrTVAdhan", "screensaver not started: " + e.getClass().getSimpleName());
            return false;
        }
    }
}
