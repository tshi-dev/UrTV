package sa.urtv.home;

import android.content.Context;

/** 4.4 kids mode as others may read it (NowPlayingProvider's ".../kids", the screensaver's page): on or off, nothing else of the parental state */
public final class KidsNow {
    private KidsNow() { }

    public static boolean on(Context c) {
        try { return Parental.state(c).kids; } catch (RuntimeException e) { return false; }
    }
}
