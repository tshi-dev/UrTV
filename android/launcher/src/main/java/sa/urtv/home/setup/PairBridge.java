package sa.urtv.home.setup;

import android.content.Context;

import java.io.IOException;

import sa.urtv.home.setup.pair.PairCode;
import sa.urtv.home.setup.pair.PairState;

/**
 * What the phone page and the TV's factory page share about the pairing (wireless debugging): the TV's page arms it, the phone page types the six digits (while armed only), the pairing runs in
 * the setup's foreground service while Settings shows the dialog, and both pages read the result. The code is held only by the running job and never logged. Public because the phone package
 * calls it; with the pairing package and PairRoute it is the whole feature (take them out and these two or three calls).
 */
public final class PairBridge {
    private PairBridge() { }

    private static final PairState STATE = new PairState();

    public static void arm() { STATE.arm(System.currentTimeMillis()); android.util.Log.i("UrTVFactory", "pairing armed"); }

    public static void disarm() { STATE.disarm(); }

    /** "off", "armed", "pairing", "paired" or "failed" */
    public static String status() { return STATE.state(System.currentTimeMillis()); }

    /** why the last pairing failed: "no_dialog" (no pairing service of this TV: the dialog is not open) or "wrong" (the code was not accepted) or "failed" */
    public static String why() { return STATE.why(); }

    public static void retry() { STATE.retry(System.currentTimeMillis()); }

    /** the phone page's code: "ok" (taken, the pairing runs), "not_armed", "busy" or "bad_code" */
    public static String submit(final Context c, String typed) {
        final String digits = PairCode.clean(typed);
        if (digits == null) return "bad_code";
        String now = status();
        if (PairState.PAIRING.equals(now)) return "busy";
        if (!STATE.begin(System.currentTimeMillis())) return "not_armed";
        android.util.Log.i("UrTVFactory", "pairing: a code arrived from the phone page");   // the code itself is never a log line
        SetupService.run(c, new Runnable() {
            @Override public void run() {
                try {
                    PairRoute.pair(c, digits, 0);
                    STATE.finish(true, "");
                    android.util.Log.i("UrTVFactory", "pairing: paired");
                } catch (IOException e) {
                    String m = String.valueOf(e.getMessage());
                    String why = m.contains("pairing service") ? "no_dialog" : m.contains("pairing failed") || m.contains("did not accept") ? "wrong" : "failed";
                    STATE.finish(false, why);
                    android.util.Log.w("UrTVFactory", "pairing failed: " + why);
                }
            }
        });
        return "ok";
    }
}
