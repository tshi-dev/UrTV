package sa.urtv.home;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

/**
 * Puts the TV to sleep for «مؤقت النوم». An app that is not part of the system cannot press the power key: PowerManager.goToSleep
 * needs a permission only the system holds, and DevicePolicyManager.lockNow needs a device admin that the TV does not offer. The one
 * way left that needs no root is an accessibility service: Android lets it ask for the lock-screen action, which turns the screen
 * off the way the power key does (API 28 and later). It listens to nothing and reads nothing (no window content): one more call needs it: the phone remote's keys (Back, Home, and from Android 13 the arrows and OK, all global actions). Typing into other apps is a separate service (TypeService) the owner turns on on its own.
 *
 * The person turns it on once in the TV's accessibility settings, or the agent does it over ADB (optimize/dock.sh sleep-setup).
 * Until then SleepTimer stops the sound and brings the screensaver up instead, and says so.
 */
public class SleepService extends AccessibilityService {
    private static volatile SleepService running;

    @Override protected void onServiceConnected() { running = this; }
    @Override public boolean onUnbind(android.content.Intent i) { running = null; return super.onUnbind(i); }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) { }
    @Override public void onInterrupt() { }

    /** Is the service on? (the person or ADB enabled it, and Android has started it) */
    static boolean enabled() { return running != null; }

    /**
     * A phone key into the app in front when only this service is on: Back and Home, and from Android 13 the arrows and OK, all global actions
     * (this service may not read the window, so before Android 13 the arrows are left to TypeService). One of RemoteNav's fixed words.
     */
    static String press(String key) {
        SleepService s = running;
        if (s == null) return RemoteNav.OFF;
        RemoteNav.Step st = RemoteNav.plan(key, Build.VERSION.SDK_INT, false);
        String r = st.route == RemoteNav.Route.GLOBAL ? (s.performGlobalAction(st.code) ? RemoteNav.DONE : RemoteNav.FAILED) : RemoteNav.LIMITED;
        RemoteTrace.line(key, st.route, r);
        return r;
    }

    /** Turn the screen off now; false when the service is off or this Android is older than 9. */
    static boolean sleepNow() {
        SleepService s = running;
        return s != null && Build.VERSION.SDK_INT >= 28 && s.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }
}
