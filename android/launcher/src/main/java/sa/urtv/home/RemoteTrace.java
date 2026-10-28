package sa.urtv.home;

import java.util.Locale;

/** The log of the phone remote's keys into other apps (tag UrTVRemote): the key's word, the route taken and the answer, never anything of the screen. */
final class RemoteTrace {
    private RemoteTrace() { }

    static void line(String key, RemoteNav.Route route, String result) {
        android.util.Log.i("UrTVRemote", "key=" + key + " route=" + route.name().toLowerCase(Locale.ROOT) + " result=" + result);
    }
}
