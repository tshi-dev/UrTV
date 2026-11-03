package sa.urtv.home.setup;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import sa.urtv.home.S;
import sa.urtv.home.Tr;

/**
 * «العربية | English» on the installer's start screen: the language of the whole system, the same as `optimize/saver.sh lang en`. The screensaver
 * holds the setting (Prefs.setLang through SaverReceiver.set, reached by the same provider call the phone page uses, PHONE_SETTINGS); this app
 * keeps it too at once, so the next screen is built in it. Arabic is the default of everything: nothing here reads the TV's own language.
 */
final class SetupLang {
    private SetupLang() { }

    private static final Uri SAVER = Uri.parse("content://sa.urtv.screensaver.config");
    static final String AR = Tr.AR, EN = Tr.EN;

    /** Tells the screensaver (false when it is missing, older, or not signed with our key: the choice then lives here until its next word). */
    static boolean save(Context c, String lang) {
        try {
            Bundle extras = new Bundle();
            extras.putString("lang", Tr.norm(lang));
            Bundle r = c.getContentResolver().call(SAVER, "phone_set", null, extras);
            return r != null && r.getBoolean("ok");
        } catch (RuntimeException e) {
            Log.w("UrTVSetup", "language not saved by the screensaver (" + e.getClass().getSimpleName() + ")");
            return false;
        }
    }

    /** A button pressed: kept, saved for the system, and the screen built again in it. */
    static void choose(Activity a, String lang) {
        String v = Tr.norm(lang);
        if (v.equals(S.lang())) return;
        S.set(a, v);
        save(a, v);
        a.recreate();
    }
}
