package sa.urtv.home;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

/**
 * 4.3.5 (an LG-branded CVTE Android 9 set: the LG logo, then a black screen): a TV without a working system WebView (none installed, turned off, or
 * one too old or broken to load) throws from {@code new WebView}. The home's card made one at every start, so the home died at every start. Every
 * WebView of the home is made here: null when it cannot be, and after one failure none is tried again in this process (the card simply stays off).
 */
final class WebViews {
    private WebViews() { }

    private static volatile boolean broken;

    /** a new WebView, or null when this TV cannot make one (the caller does without it) */
    static WebView make(Context c) {
        if (broken) return null;
        try {
            return new WebView(c);
        } catch (RuntimeException | LinkageError e) {   // AndroidRuntimeException «No WebView installed», a missing or broken native library
            broken = true;
            Log.w("UrTVWeb", "no WebView on this TV: " + e.getClass().getName());   // the class only: nothing of the TV
            return null;
        }
    }

    /** a WebView could not be made in this process */
    static boolean broken() { return broken; }
}
