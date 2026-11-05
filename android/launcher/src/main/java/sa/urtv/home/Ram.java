package sa.urtv.home;

import android.app.ActivityManager;
import android.content.Context;

/**
 * A weak TV (a user report on 4.1: heavy lag after the update; 1.5–2 GB of memory is common): Android's own low-RAM flag, or under LOW_MB of memory
 * in all. On such a TV the home does less: the remote's keys outside the home are not filtered (TypeService), the markets' live mode starts off and
 * asks at most every 60 s, the home card page gets «lowram=1» (slower timers, no shadows), and the home lets its pictures and its card page go when
 * another app is in front (MainActivity.onTrimMemory). Read once: neither changes while the app runs.
 */
public final class Ram {
    private Ram() { }

    /** 2.5 GB: a «2 GB» TV reports about 1.8–1.9 GB in all and a «3 GB» one about 2.8 GB, so this splits them */
    static final long LOW_MB = 2560;

    private static volatile Boolean low;

    /** The rule, with no Android in it (tests/RamRun.java): the low-RAM flag, or a known total under LOW_MB. 0 = unknown: not low (as before). */
    static boolean low(boolean lowRamFlag, long totalMb) { return lowRamFlag || (totalMb > 0 && totalMb < LOW_MB); }

    /** This TV: read once. */
    public static boolean low(Context c) {
        Boolean l = low;
        if (l == null) {
            boolean flag = false;
            long mb = 0;
            try {
                ActivityManager am = (ActivityManager) c.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                flag = am.isLowRamDevice();
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                mb = mi.totalMem / (1024 * 1024);
            } catch (RuntimeException ignored) { }
            l = low(flag, mb);
            low = l;
        }
        return l;
    }
}
