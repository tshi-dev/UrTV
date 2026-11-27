package sa.urtv.home.setup;

import android.content.Context;

/**
 * 4.4 beta 3, what the phone page and the TV's factory page share about the wireless-debugging pairing (the pairing itself runs in the add-on: PairRoute). The TV's page
 * ARMS it (behind the family PIN); only then does the phone page show «إقران التلفزيون» and take six digits, which go to the add-on and are never logged or kept here.
 * The phone page asks the state often, so it is kept HERE while armed: the add-on is asked only while this home armed it (never when it did not). Every call that reaches
 * the add-on is made on the caller's thread: the phone server's and the factory page's background threads, never a UI thread.
 */
public final class PairBridge {
    private PairBridge() { }

    /** the add-on's own window is ten minutes (PairState.ARMED_MS); the home forgets a little later, never sooner */
    static final long ARMED_MS = 11 * 60_000L;
    private static volatile long armedUntil;
    private static volatile String last = "off", lastWhy = "";

    private static boolean armedHere() { return System.currentTimeMillis() < armedUntil; }

    /** the factory page: arm (true when the add-on answered) */
    static boolean arm(Context c) {
        boolean ok = PairRoute.arm(c);
        if (ok) { armedUntil = System.currentTimeMillis() + ARMED_MS; last = "armed"; lastWhy = ""; android.util.Log.i("UrTVFactory", "pairing armed"); }
        return ok;
    }

    static void disarm(Context c) {
        boolean was = armedHere();
        armedUntil = 0;
        last = "off";
        if (was) PairRoute.disarm(c);
    }

    static void retry(Context c) { PairRoute.retry(c); armedUntil = System.currentTimeMillis() + ARMED_MS; }

    /** "off", "armed", "pairing", "paired" or "failed": the add-on's word while armed here, "off" otherwise (no call) */
    public static String status(Context c) {
        if (!armedHere()) return "off";
        String s = PairRoute.state(c);
        if (s.isEmpty()) s = "failed";   // the add-on stopped answering: said, never silent
        last = s;
        if ("failed".equals(s)) lastWhy = PairRoute.failure(c);
        return s;
    }

    /** why the last pairing failed: "no_dialog", "wrong", "failed" */
    public static String why() { return lastWhy; }

    /** the phone page's six digits: "ok" (the add-on pairs on its own thread), "not_armed", "busy", "bad_code" or "no_addon" */
    public static String submit(Context c, String typed) {
        if (!armedHere()) return "not_armed";
        android.util.Log.i("UrTVFactory", "pairing: a code arrived from the phone page");   // the code itself is never a log line
        // a wrong code leaves the add-on «failed» and the TV says «اكتب الرمز الجديد»: the next code is a new try, so arm it again first (review F94)
        if ("failed".equals(PairRoute.state(c))) retry(c);
        return PairRoute.submit(c, typed);
    }
}
