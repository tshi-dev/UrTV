package sa.urtv.home;

/**
 * The phone remote's keys into ANOTHER app, through our accessibility service: which call a key becomes. No Android in it (the numbers are
 * written out), so tests/RemoteNavRun.java runs it. Back and Home are global actions on every Android. The arrows and OK are global actions
 * from Android 13 (GLOBAL_ACTION_DPAD_*); before that they are a focus move on the window in front (a node's focusSearch then ACTION_FOCUS, OK
 * is ACTION_CLICK on the focused node), which needs the service that may read the window (TypeService). Our own screens never come here:
 * they take the key through dispatchKeyEvent, which is exact.
 */
public final class RemoteNav {
    // AccessibilityService's GLOBAL_ACTION_* values
    static final int GLOBAL_BACK = 1, GLOBAL_HOME = 2, GLOBAL_DPAD_UP = 16, GLOBAL_DPAD_DOWN = 17, GLOBAL_DPAD_LEFT = 18, GLOBAL_DPAD_RIGHT = 19, GLOBAL_DPAD_CENTER = 20;
    // View.FOCUS_* values
    static final int FOCUS_UP = 33, FOCUS_DOWN = 130, FOCUS_LEFT = 17, FOCUS_RIGHT = 66;
    /** Android 13: the first with the dpad global actions */
    static final int DPAD_SDK = 33;

    enum Route { GLOBAL, FOCUS, CLICK, NONE }

    static final class Step {
        final Route route;
        final int code;
        Step(Route route, int code) { this.route = route; this.code = code; }
    }

    /** What the answers of the service are (fixed words, the only thing the phone hears). */
    public static final String DONE = "done", OFF = "service_off", LIMITED = "other_app_limited", FAILED = "failed";

    private static final Step NONE = new Step(Route.NONE, 0);

    /** The step for a phone key on this Android; `canReadWindow`: the service in hand may look at the window (TypeService, not SleepService). */
    static Step plan(String key, int sdk, boolean canReadWindow) {
        if (key == null) return NONE;
        switch (key) {
            case "back": return new Step(Route.GLOBAL, GLOBAL_BACK);
            case "home": return new Step(Route.GLOBAL, GLOBAL_HOME);
            default: break;
        }
        int global, focus;
        switch (key) {
            case "up": global = GLOBAL_DPAD_UP; focus = FOCUS_UP; break;
            case "down": global = GLOBAL_DPAD_DOWN; focus = FOCUS_DOWN; break;
            case "left": global = GLOBAL_DPAD_LEFT; focus = FOCUS_LEFT; break;
            case "right": global = GLOBAL_DPAD_RIGHT; focus = FOCUS_RIGHT; break;
            case "ok": global = GLOBAL_DPAD_CENTER; focus = 0; break;
            default: return NONE;
        }
        if (sdk >= DPAD_SDK) return new Step(Route.GLOBAL, global);
        if (!canReadWindow) return NONE;
        return key.equals("ok") ? new Step(Route.CLICK, 0) : new Step(Route.FOCUS, focus);
    }

    /** Do the arrows and OK reach another app with the services in this state? Back and Home need only one of the services. */
    static boolean dpadEverywhere(int sdk, boolean anyServiceOn, boolean typingServiceOn) {
        return anyServiceOn && (sdk >= DPAD_SDK || typingServiceOn);
    }

    /**
     * «إيقاف الأذان بأي زر»: while the adhan plays and the rule is on, a phone key stops it first and is used up, as on the TV's own remote
     * (AdhanKeys.reserved): the volume keys stay ordinary so the adhan can be turned down; every other key stops it.
     */
    public static boolean stopsAdhan(String key, boolean adhanLive, boolean stopAny) {
        if (!adhanLive || !stopAny || key == null) return false;
        switch (key) {
            case "vol_up": case "vol_down": case "mute": return false;
            default: return true;
        }
    }
}
