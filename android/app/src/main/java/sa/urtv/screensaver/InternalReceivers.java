package sa.urtv.screensaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;

/**
 * A receiver for a broadcast this app sends to itself (its own action, always with setPackage). From Android 13 a receiver registered at run time
 * says whether other apps may reach it, and an app targeting 34 that does not gets a SecurityException when it registers: these are NOT_EXPORTED.
 * (The system's own broadcasts, the minute tick and the packages, need no flag.)
 */
final class InternalReceivers {
    private InternalReceivers() { }

    static void register(Context c, BroadcastReceiver r, IntentFilter f) {
        if (Build.VERSION.SDK_INT >= 33) c.registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED);
        else c.registerReceiver(r, f);
    }

    /** Step 41: an action our home app sends (UpdateCover's close): reachable from other apps on API 33+, but only from a holder of the signature permission. */
    static void registerFromHome(Context c, BroadcastReceiver r, IntentFilter f, String signaturePermission) {
        if (Build.VERSION.SDK_INT >= 33) c.registerReceiver(r, f, signaturePermission, null, Context.RECEIVER_EXPORTED);
        else c.registerReceiver(r, f, signaturePermission, null);
    }
}
