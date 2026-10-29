package sa.urtv.home;

import android.app.Activity;

/**
 * «إعداد تلفزيوني» can turn off apps and switch the home screen, and it is in the TV's own apps list, where a child can open it: with a family PIN
 * set, its buttons ask for the PIN first (whatever «قفل الإعدادات» says). Public only so the setup package can reach the package-private Parental.
 */
public final class SetupGate {
    private SetupGate() { }

    public static void guard(Activity a, Runnable allowed, Runnable cancelled) {
        guard(a, "إعداد تلفزيوني", allowed, cancelled);
    }

    /** The same, with the screen's own name on the PIN prompt («الوضع الآمن»: its «رجّع واجهة التلفزيون الأصلية» undoes the setup too). */
    public static void guard(Activity a, String name, Runnable allowed, Runnable cancelled) {
        if (Parental.pin(a).isSet()) Parental.confirm(a, Parental.adhanFirst(a), name, allowed, cancelled);
        else allowed.run();
    }
}
