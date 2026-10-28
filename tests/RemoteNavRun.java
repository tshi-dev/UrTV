package sa.urtv.home;

/** For tests/remote_nav_test.py: RemoteNav (which call a phone key becomes in another app, who gets the arrows, when a key stops the adhan). Prints PASS/FAIL lines. */
public final class RemoteNavRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    static boolean is(RemoteNav.Step s, RemoteNav.Route r, int code) { return s.route == r && s.code == code; }

    public static void main(String[] args) {
        for (int sdk : new int[] {28, 30, 32, 33, 34}) for (boolean read : new boolean[] {false, true}) {
            check("back and home are global actions on API " + sdk + (read ? " (window readable)" : ""),
                is(RemoteNav.plan("back", sdk, read), RemoteNav.Route.GLOBAL, 1) && is(RemoteNav.plan("home", sdk, read), RemoteNav.Route.GLOBAL, 2));
        }
        for (boolean read : new boolean[] {false, true}) {
            check("Android 13+: the arrows and OK are the dpad global actions (16 to 20), whichever service", is(RemoteNav.plan("up", 33, read), RemoteNav.Route.GLOBAL, 16)
                && is(RemoteNav.plan("down", 34, read), RemoteNav.Route.GLOBAL, 17) && is(RemoteNav.plan("left", 33, read), RemoteNav.Route.GLOBAL, 18)
                && is(RemoteNav.plan("right", 34, read), RemoteNav.Route.GLOBAL, 19) && is(RemoteNav.plan("ok", 33, read), RemoteNav.Route.GLOBAL, 20));
        }
        check("Android 9 to 12 with the window readable: the arrows move the focus (View.FOCUS_*), OK clicks the focused node",
            is(RemoteNav.plan("up", 32, true), RemoteNav.Route.FOCUS, 33) && is(RemoteNav.plan("down", 28, true), RemoteNav.Route.FOCUS, 130)
            && is(RemoteNav.plan("left", 30, true), RemoteNav.Route.FOCUS, 17) && is(RemoteNav.plan("right", 31, true), RemoteNav.Route.FOCUS, 66)
            && is(RemoteNav.plan("ok", 32, true), RemoteNav.Route.CLICK, 0));
        check("Android 9 to 12 with a service that may not read the window: the arrows and OK have no route",
            is(RemoteNav.plan("up", 32, false), RemoteNav.Route.NONE, 0) && is(RemoteNav.plan("ok", 28, false), RemoteNav.Route.NONE, 0));
        check("a key that is not one of ours has no route (the phone's list is the only way in)", is(RemoteNav.plan("power", 34, true), RemoteNav.Route.NONE, 0)
            && is(RemoteNav.plan(null, 34, true), RemoteNav.Route.NONE, 0) && is(RemoteNav.plan("vol_up", 34, true), RemoteNav.Route.NONE, 0) && is(RemoteNav.plan("", 34, true), RemoteNav.Route.NONE, 0));
        check("the arrows reach another app: Android 13+ with either service, before it only with the typing/remote one",
            RemoteNav.dpadEverywhere(34, true, false) && RemoteNav.dpadEverywhere(33, true, true) && RemoteNav.dpadEverywhere(32, true, true)
            && !RemoteNav.dpadEverywhere(32, true, false) && !RemoteNav.dpadEverywhere(34, false, false) && !RemoteNav.dpadEverywhere(32, false, true));
        check("«إيقاف الأذان بأي زر»: every key but the volume ones stops a playing adhan first",
            RemoteNav.stopsAdhan("up", true, true) && RemoteNav.stopsAdhan("ok", true, true) && RemoteNav.stopsAdhan("back", true, true) && RemoteNav.stopsAdhan("home", true, true)
            && RemoteNav.stopsAdhan("play_pause", true, true) && !RemoteNav.stopsAdhan("vol_up", true, true) && !RemoteNav.stopsAdhan("vol_down", true, true) && !RemoteNav.stopsAdhan("mute", true, true));
        check("the adhan rule needs both the adhan playing and the setting on", !RemoteNav.stopsAdhan("up", false, true) && !RemoteNav.stopsAdhan("up", true, false) && !RemoteNav.stopsAdhan("up", false, false) && !RemoteNav.stopsAdhan(null, true, true));
        check("the words the phone hears are fixed", RemoteNav.DONE.equals("done") && RemoteNav.OFF.equals("service_off") && RemoteNav.LIMITED.equals("other_app_limited") && RemoteNav.FAILED.equals("failed"));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
