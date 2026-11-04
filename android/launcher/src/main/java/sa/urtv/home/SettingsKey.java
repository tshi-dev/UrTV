package sa.urtv.home;

/**
 * The remote's Settings key OUTSIDE our home (a receiver on HDMI, Live TV, any app). With the stock launcher off nothing answers it there: on
 * a TCL the key's handler is a receiver inside the stock launcher, so the person could not reach the picture mode («توفير الطاقة» → قياسي،
 * رياضة، حيوي), which the TV keeps per input. Our accessibility service TypeService (already on for the phone remote) can see the key before
 * the app in front does; this says what it does with it:
 *
 * - any other key: PASS, at once, before anything is looked at or remembered (the service logs nothing about it);
 * - our home is in front: PASS, the home's own key path (RemoteKeys) answers it (a short press the TV's settings, a long one OUR settings);
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
    /** the Settings key of a TCL G05 remote (a vendor keycode: it may reach only the window in front, never this service; kept in step with RemoteKeys) */
    static final int KEYCODE_TCL_SETTINGS = 4077;

    /** the key goes on as it would have */
    static final int PASS = 0;
    /** the repeats and the release of a press that acted */
    static final int EAT = 1;
    /** open the TV's settings */
    static final int OPEN_TV_SETTINGS = 2;
    /** our home is in front but the key never reaches it (homeKeyLost): hand the event to the home's own key path (RemoteKeys) */
    static final int TO_HOME = 3;

    /**
     * The Intent action Android TV's SystemUI starts for the Settings / Notification key. On a Haier MatrixTV (Android 14, 4.1 reports) it
     * resolves to nothing: SystemUI takes the key, starts nothing, and our home never sees it, so the key did nothing there. Where it resolves to
     * nothing, the service also listens while our home is in front and hands the key to it (TO_HOME).
     */
    static final String NOTIFICATION_PANEL_ACTION = "android.app.action.TOGGLE_NOTIFICATION_HANDLER_PANEL";

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
        return keyCode == KEYCODE_SETTINGS || keyCode == KEYCODE_NOTIFICATION || keyCode == KEYCODE_TCL_SETTINGS;
    }

    /** homeKeyLost: this TV's system takes the key before our home (NOTIFICATION_PANEL_ACTION resolves to nothing) */
    int decide(int keyCode, int action, int repeat, boolean homeFront, boolean stockHome, boolean homeKeyLost) {
        if (!isSettingsKey(keyCode)) return PASS;   // every other key: untouched, nothing remembered
        if (homeFront && homeKeyLost) { holding = false; return TO_HOME; }   // every event of it (press, repeats, release): the home tells short from long
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

    int decide(int keyCode, int action, int repeat, boolean homeFront, boolean stockHome) {
        return decide(keyCode, action, repeat, homeFront, stockHome, false);
    }

    /** Whether the service should ask Android for key events at all: only while it could act on the Settings key. */
    static boolean filterWanted(boolean homeFront, boolean stockHome) {
        return !homeFront && !stockHome;
    }

    /**
     * The same on a weak TV (Ram.low): never. A filtered key goes through our main thread and Android waits for the answer, so on a busy 2 GB TV
     * every key in YouTube or on HDMI could lag (a user report on 4.1). There the Settings key outside our home is the TV's own again.
     */
    static boolean filterWanted(boolean homeFront, boolean stockHome, boolean lowRam) {
        return filterWanted(homeFront, stockHome, lowRam, false);
    }

    /** ... and on a TV whose system takes the key before our home (homeKeyLost), also while our home is in front (never on a weak TV) */
    static boolean filterWanted(boolean homeFront, boolean stockHome, boolean lowRam, boolean homeKeyLost) {
        if (lowRam) return false;
        return homeFront ? homeKeyLost : filterWanted(false, stockHome);
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
