package sa.urtv.home;

import java.util.Set;

/**
 * The shortcut keys of the remote, on our home screen. With the stock launcher switched off, nothing answers them any more: the system
 * sends them (as an explicit broadcast) to a receiver that lives in the stock launcher, so they would do nothing. While our home is in
 * front it sees the key first, and this says what to do with it: open YouTube or Netflix (only when that app is installed), the Settings
 * key (a user's request): a short press opens the TV's own settings, a long press ({@link #LONG_MS}) OUR settings; or open the inputs list
 * (the Menu key's list: the «star» key does what Menu does). TCL's Guard key (4140) does nothing there.
 *
 * Only a press (DOWN, first, not a repeat) acts; the repeats and the release of a key that acted are consumed, so nothing else reacts to
 * them. The Settings key waits to know which press it is: its DOWN is consumed and kept, and it acts once, either when it has been held
 * {@link #LONG_MS} (a repeat that late: our settings) or on its release (held less: the TV's settings; held longer by a remote that sends
 * no repeats: ours). A key the home does not map, or an app that is not installed, is left alone (PASS) and behaves as it did before. The
 * adhan's any-key rule is asked first by the caller (AdhanKeys), so a key that stopped the adhan never gets here.
 *
 * No Android state in here (KeyEvent's numbers are written out, so it runs without the SDK): tests/RemoteKeysRun.java runs it.
 */
final class RemoteKeys {
    // android.view.KeyEvent's values
    private static final int ACTION_DOWN = 0, ACTION_UP = 1;
    /** The system itself takes 176 before any app sees it (a TV test: `input keyevent 176` did nothing on our home, 83 worked): kept for the TVs that pass it on. */
    private static final int KEYCODE_SETTINGS = 176;
    /** KEYCODE_NOTIFICATION: the Settings key of the owner's TCL remote reaches the app with this number (UrTVKeys logged it), not 176. */
    private static final int KEYCODE_NOTIFICATION = 83;
    private static final int KEYCODE_BUTTON_3 = 190;   // the YouTube key of a Google TV remote
    private static final int KEYCODE_BUTTON_4 = 191;   // the Netflix key
    /** TCL's vendor keys (the UrTVKeys log and the Clean Your TV notes): a keycode of 1000 and up reaches only the window in front, so the home must answer it. */
    private static final int TCL_NETFLIX = 4062, TCL_YOUTUBE = 4065, TCL_PRIME = 4099;
    /**
     * TCL's Guard key (4140, a TCL 65P735 in the Clean Your TV notes): it opens TCL's memory cleaner (com.tcl.guard), which kills apps in the
     * background, our adhan among them. On our home it does nothing: the press, its repeats and its release are consumed.
     */
    private static final int TCL_GUARD = 4140;
    /** the Settings key of a TCL G05 remote (UrTVKeys logged key=4077) */
    private static final int TCL_SETTINGS = 4077;
    /** Linux KEY_JOURNAL (0x242): the star key of a TCL G08 remote. Android's generic key map has no name for it, so the keycode may be 0. */
    static final int SCAN_STAR = 578;

    static final String YOUTUBE = "com.google.android.youtube.tv", NETFLIX = "com.netflix.ninja", PRIME = "com.amazon.amazonvideo.livingroom";
    /** the apps the keys can open: the caller tells which of them are installed (and shown) */
    static final String[] PACKAGES = {YOUTUBE, NETFLIX, PRIME};

    /** the key goes on as it would have */
    static final int PASS = 0;
    /** the key is consumed and nothing opens (the repeats and the release of a key that acted) */
    static final int EAT = 1;
    /** open {@link Decision#pkg} */
    static final int OPEN_APP = 2;
    /** open the TV's own settings: a short press of the Settings key */
    static final int OPEN_SETTINGS = 3;
    /** open the inputs list, as the Menu key does */
    static final int OPEN_INPUTS = 4;
    /** open OUR settings (the screensaver app's settings screen): a long press of the Settings key */
    static final int OPEN_OUR_SETTINGS = 5;

    /** ms the Settings key is held for a long press (Android's own long-press time is 400 to 500) */
    static final long LONG_MS = 500;

    static final class Decision {
        final int kind;
        final String pkg;

        Decision(int kind, String pkg) {
            this.kind = kind;
            this.pkg = pkg;
        }
    }

    private static final Decision PASS_IT = new Decision(PASS, null), EAT_IT = new Decision(EAT, null);

    /** the key whose press acted (or, for the Settings key, is still deciding), until its release */
    private boolean holding;
    private int heldCode, heldScan;
    /** the Settings key is held and has not acted yet: its release, or a repeat {@link #LONG_MS} late, decides */
    private boolean settingsPending;

    /** heldMs: how long the key has been down (the event's time minus its down time); only the Settings key reads it */
    Decision decide(int keyCode, int scanCode, int action, int repeat, long heldMs, Set<String> installed) {
        if (holding && keyCode == heldCode && scanCode == heldScan) {
            // the release can go to the window that was opened and never reach us: a new press (DOWN, first) is always a new press
            if (action == ACTION_DOWN && repeat == 0) holding = settingsPending = false;
            else if (settingsPending) {
                if (action == ACTION_UP) {
                    holding = settingsPending = false;
                    return new Decision(heldMs >= LONG_MS ? OPEN_OUR_SETTINGS : OPEN_SETTINGS, null);
                }
                if (heldMs < LONG_MS) return EAT_IT;
                settingsPending = false;   // held long enough: ours now, its later repeats and release are only consumed
                return new Decision(OPEN_OUR_SETTINGS, null);
            } else {
                if (action == ACTION_UP) holding = false;
                return EAT_IT;
            }
        }
        if (action != ACTION_DOWN || repeat > 0) return PASS_IT;
        if (isSettings(keyCode)) {
            holding = settingsPending = true;
            heldCode = keyCode;
            heldScan = scanCode;
            return EAT_IT;   // nothing yet: short or long is known at its release or once it is held long enough
        }
        Decision d = map(keyCode, scanCode, installed);
        if (d.kind != PASS) {
            holding = true;
            heldCode = keyCode;
            heldScan = scanCode;
        }
        return d;
    }

    /** The home screen went to the background: whatever key was held is forgotten (its release will not come here). */
    void reset() { holding = settingsPending = false; }

    /** the remote's Settings key: 176, TCL's 83 (KEYCODE_NOTIFICATION) and the TCL G05's 4077 */
    static boolean isSettings(int keyCode) {
        return keyCode == KEYCODE_SETTINGS || keyCode == KEYCODE_NOTIFICATION || keyCode == TCL_SETTINGS;
    }

    /** Whether this key can open an app, so the caller looks up what is installed only for it, not for every key of the remote. */
    static boolean opensApp(int keyCode) {
        return keyCode == KEYCODE_BUTTON_3 || keyCode == KEYCODE_BUTTON_4
                || keyCode == TCL_NETFLIX || keyCode == TCL_YOUTUBE || keyCode == TCL_PRIME;
    }

    private static Decision map(int keyCode, int scanCode, Set<String> installed) {
        switch (keyCode) {
            case KEYCODE_BUTTON_3:
            case TCL_YOUTUBE: return app(YOUTUBE, installed);
            case KEYCODE_BUTTON_4:
            case TCL_NETFLIX: return app(NETFLIX, installed);
            case TCL_PRIME: return app(PRIME, installed);
            case TCL_GUARD: return EAT_IT;   // TCL's cleaner would kill our adhan: nothing opens
            default: break;   // the Settings key is decided in decide(): short or long
        }
        if (scanCode == SCAN_STAR) return new Decision(OPEN_INPUTS, null);
        return PASS_IT;
    }

    private static Decision app(String pkg, Set<String> installed) {
        return installed != null && installed.contains(pkg) ? new Decision(OPEN_APP, pkg) : PASS_IT;
    }
}
