package sa.urtv.home;

/**
 * The remote's Settings key OUTSIDE our home (a receiver on HDMI, Live TV, any app). With the stock launcher off nothing answers it there: on
 * a TCL the key's handler is a receiver inside the stock launcher, so the person could not reach the picture mode («توفير الطاقة» → قياسي،
 * رياضة، حيوي), which the TV keeps per input. Our accessibility service TypeService (already on for the phone remote) can see the key before
 * the app in front does; this says what it does with it:
 *
 * - any other key: PASS, at once, before anything is looked at or remembered (the service logs nothing about it);
 * - our home is in front: PASS, the home's own key path (RemoteKeys) opens OUR settings as before;
 * - the TV's stock home is enabled (a TV that kept it, or a later «receiver mode»): PASS, the stock receiver answers the key as the TV meant;
 * - otherwise the press (DOWN, first, not a repeat) opens the TV's settings ({@link #TV_SETTINGS_ACTIONS}); its repeats and release are
 *   consumed, so the app in front never gets half a key.
 *
 * Besides, the service asks Android for keys only while it may act ({@link #filterWanted}): with our home in front, or a stock home enabled,
 * no key goes through it at all. No Android in here (KeyEvent's numbers are written out): tests/SettingsKeyRun.java runs it.
 */
final class SettingsKey {
    // android.view.KeyEvent's values
    static final int ACTION_DOWN = 0, ACTION_UP = 1;
    /** Android's Settings key; most TVs take it in the system before anyone sees it, then it never reaches us */
    static final int KEYCODE_SETTINGS = 176;
    /** KEYCODE_NOTIFICATION: what a TCL remote's Settings key arrives as (the owner's TCL, UrTVKeys log; see docs/TVS.md) */
    static final int KEYCODE_NOTIFICATION = 83;

    /** the key goes on as it would have */
    static final int PASS = 0;
    /** the repeats and the release of a press that acted */
    static final int EAT = 1;
    /** open the TV's settings */
    static final int OPEN_TV_SETTINGS = 2;

    /**
     * What the key opens outside our home, tried in order: the first one the TV can open is used. Only intents seen on a TV go here. A known
     * intent for the TV's own picture / quick settings of the input in front goes FIRST, as one more line, once a TV test finds one
     * (docs/TVS.md says how to look); until then Android TV's settings, which on the owner's TCL is its own settings app.
     */
    static final String[] TV_SETTINGS_ACTIONS = {
        "android.settings.SETTINGS",
    };

    /** HOME answerers that are not a stock home: the setup wizard (left on when it owns the Wi-Fi), the settings apps' fallback home */
    static final String[] NOT_A_STOCK_HOME = {
        "com.google.android.tungsten.setupwraith", "com.android.tv.settings", "com.android.settings",
    };

    /** a Settings press that opened the TV's settings, until its release */
    private boolean holding;
    private int heldCode;

    static boolean isSettingsKey(int keyCode) {
        return keyCode == KEYCODE_SETTINGS || keyCode == KEYCODE_NOTIFICATION;
    }

    int decide(int keyCode, int action, int repeat, boolean homeFront, boolean stockHome) {
        if (!isSettingsKey(keyCode)) return PASS;   // every other key: untouched, nothing remembered
        if (holding && keyCode == heldCode) {
            // a new press (DOWN, first) is always a new press: the release may have gone to the settings window
            if (action == ACTION_DOWN && repeat == 0) holding = false;
            else {
                if (action == ACTION_UP) holding = false;
                return EAT;
            }
        }
        if (homeFront || stockHome) return PASS;
        if (action != ACTION_DOWN || repeat > 0) return PASS;
        holding = true;
        heldCode = keyCode;
        return OPEN_TV_SETTINGS;
    }

    /** Whether the service should ask Android for key events at all: only while it could act on the Settings key. */
    static boolean filterWanted(boolean homeFront, boolean stockHome) {
        return !homeFront && !stockHome;
    }

    /** The enabled HOME answerers (packages) include a stock home: one that is not ours, not the setup wizard, not a settings app's fallback. */
    static boolean stockHomeOn(Iterable<String> homePackages, String ours) {
        if (homePackages == null) return false;
        outer:
        for (String p : homePackages) {
            if (p == null || p.equals(ours) || p.startsWith("sa.urtv.")) continue;
            for (String n : NOT_A_STOCK_HOME) if (n.equals(p)) continue outer;
            return true;
        }
        return false;
    }
}
