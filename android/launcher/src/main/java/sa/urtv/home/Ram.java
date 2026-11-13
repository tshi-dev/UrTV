package sa.urtv.home;

import android.app.ActivityManager;
import android.content.Context;

/**
 * A weak TV (a user report on 4.1: heavy lag after the update; 1.5–2 GB of memory is common): Android's own low-RAM flag, or under LOW_MB of memory
 * in all. On such a TV the home does less: the remote's keys outside the home are not filtered (TypeService), the markets' live mode starts off and
 * asks at most every 60 s, the home card page gets «lowram=1» (slower timers, no shadows), and the home lets its pictures and its card page go when
 * another app is in front (MainActivity.onTrimMemory). Read once: neither changes while the app runs.
 * Three tiers (LOW, MID, NORMAL) since step 47: see tier().
 */
public final class Ram {
    private Ram() { }

    /** the tiers: LOW does every cut, MID only the cheap memory ones, NORMAL none */
    public static final int LOW = 0, MID = 1, NORMAL = 2;

    /**
     * Under 2000 MB in all (the 1.5–1.9 GB sets: a «2 GB» Skyworth 1.68, Haier 1.83, Changhong are the ones that really struggle) is LOW, as is Android's own
     * low-RAM flag; 2000 to under 2600 MB (a TCL at 2394) is MID; 2600 and up is NORMAL. A «3 GB» TV reports about 2.8 GB. The saver app has the same numbers
     * (Prefs.ramTier; tests/ram_tier_test.py keeps the two the same).
     */
    static final long LOW_MB = 2000, MID_MB = 2600;

    private static volatile int tier = -1;

    /** The rule, with no Android in it (tests/RamRun.java): the low-RAM flag, or a known total under LOW_MB is LOW; under MID_MB MID. 0 = unknown: NORMAL (as before). */
    static int tier(boolean lowRamFlag, long totalMb) {
        if (lowRamFlag || (totalMb > 0 && totalMb < LOW_MB)) return LOW;
        return totalMb > 0 && totalMb < MID_MB ? MID : NORMAL;
    }

    /** a forced tier by name (the debug switch, `dock.sh ramtier`); -1 = none */
    static int named(String n) { return "low".equals(n) ? LOW : "mid".equals(n) ? MID : "normal".equals(n) ? NORMAL : -1; }

    /** This TV: read once (a forced tier from `dock.sh ramtier` wins, from the next start of the app). The one place that decides. */
    public static int tier(Context c) {
        int t = tier;
        if (t < 0) {
            boolean flag = false;
            long mb = 0;
            try {
                ActivityManager am = (ActivityManager) c.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                flag = am.isLowRamDevice();
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                mb = mi.totalMem / (1024 * 1024);
            } catch (RuntimeException ignored) { }
            t = tier(flag, mb);
            try {
                int f = named(c.getApplicationContext().getSharedPreferences("ram", Context.MODE_PRIVATE).getString("force", ""));
                if (f >= 0) t = f;
            } catch (RuntimeException ignored) { }
            tier = t;
        }
        return t;
    }

    /** the debug switch: "low", "mid", "normal" or "auto" (back to the real tier); true when it was understood */
    public static boolean force(Context c, String name) {
        if (!"auto".equals(name) && named(name) < 0) return false;
        c.getApplicationContext().getSharedPreferences("ram", Context.MODE_PRIVATE).edit().putString("force", "auto".equals(name) ? "" : name).commit();
        return true;
    }

    /** LOW: every cut (the key filter, the markets' live mode, the page's slower timers, no transitions, one photo layer) */
    public static boolean low(Context c) { return tier(c) == LOW; }

    /** ComponentCallbacks2's levels: TRIM_MEMORY_UI_HIDDEN and TRIM_MEMORY_MODERATE (the home in the background, the system short of memory) */
    static final int TRIM_UI_HIDDEN = 20, TRIM_MODERATE = 60;

    /**
     * Hotfix 4.3 (a user: the picture goes soft for a few seconds on the way back): the home lets its photos and its card page go only when the system really asks. LOW: as the app goes behind another
     * (UI_HIDDEN); MID: only from MODERATE on; NORMAL: never. (tests/RamRun.java)
     */
    static boolean trimNow(int tier, int level) { return tier == LOW ? level >= TRIM_UI_HIDDEN : tier == MID && level >= TRIM_MODERATE; }

    /** LOW or MID: the cheap memory cuts (the pictures and the card page let go behind another app, the markets a little slower) */
    public static boolean tight(Context c) { return tier(c) != NORMAL; }
}
