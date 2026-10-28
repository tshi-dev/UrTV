package sa.urtv.home;

import java.util.Set;

/**
 * The shortcut keys of the remote, on our home screen. With the stock launcher switched off, nothing answers them any more: the system
 * sends them (as an explicit broadcast) to a receiver that lives in the stock launcher, so they would do nothing. While our home is in
 * front it sees the key first, and this says what to do with it: open YouTube or Netflix (only when that app is installed), open our
 * settings (the Settings key; the TV's own settings stay on the «الإعدادات» pill and tile), or open the inputs list (the Menu key's list: the «star» key does what Menu does).
 *
 * Only a press (DOWN, first, not a repeat) acts; the repeats and the release of a key that acted are consumed, so nothing else reacts to
 * them. A key the home does not map, or an app that is not installed, is left alone (PASS) and behaves as it did before. The adhan's
 * any-key rule is asked first by the caller (AdhanKeys), so a key that stopped the adhan never gets here.
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
    /** Linux KEY_JOURNAL (0x242): the star key of a TCL G08 remote. Android's generic key map has no name for it, so the keycode may be 0. */
    static final int SCAN_STAR = 578;

    static final String YOUTUBE = "com.google.android.youtube.tv", NETFLIX = "com.netflix.ninja";
    /** the apps the keys can open: the caller tells which of them are installed (and shown) */
    static final String[] PACKAGES = {YOUTUBE, NETFLIX};

    /** the key goes on as it would have */
    static final int PASS = 0;
    /** the key is consumed and nothing opens (the repeats and the release of a key that acted) */
    static final int EAT = 1;
    /** open {@link Decision#pkg} */
    static final int OPEN_APP = 2;
    /** open the TV's own settings (what the Settings key fell back to when our settings are missing; no key maps to it any more) */
    static final int OPEN_SETTINGS = 3;
    /** open the inputs list, as the Menu key does */
    static final int OPEN_INPUTS = 4;
    /** open OUR settings (the screensaver app's settings screen): what the remote's Settings key does on our home */
    static final int OPEN_OUR_SETTINGS = 5;

    static final class Decision {
        final int kind;
        final String pkg;

        Decision(int kind, String pkg) {
            this.kind = kind;
            this.pkg = pkg;
        }
    }

    private static final Decision PASS_IT = new Decision(PASS, null), EAT_IT = new Decision(EAT, null);

    /** the key whose press acted, until its release */
    private boolean holding;
    private int heldCode, heldScan;

    Decision decide(int keyCode, int scanCode, int action, int repeat, Set<String> installed) {
        if (holding && keyCode == heldCode && scanCode == heldScan) {
            // the release can go to the window that was opened and never reach us: a new press (DOWN, first) is always a new press
            if (action == ACTION_DOWN && repeat == 0) holding = false;
            else {
                if (action == ACTION_UP) holding = false;
                return EAT_IT;
            }
        }
        if (action != ACTION_DOWN || repeat > 0) return PASS_IT;
        Decision d = map(keyCode, scanCode, installed);
        if (d.kind != PASS) {
            holding = true;
            heldCode = keyCode;
            heldScan = scanCode;
        }
        return d;
    }

    /** The home screen went to the background: whatever key was held is forgotten (its release will not come here). */
    void reset() { holding = false; }

    /** Whether this key can open an app, so the caller looks up what is installed only for it, not for every key of the remote. */
    static boolean opensApp(int keyCode) {
        return keyCode == KEYCODE_BUTTON_3 || keyCode == KEYCODE_BUTTON_4;
    }

    private static Decision map(int keyCode, int scanCode, Set<String> installed) {
        switch (keyCode) {
            case KEYCODE_BUTTON_3: return app(YOUTUBE, installed);
            case KEYCODE_BUTTON_4: return app(NETFLIX, installed);
            case KEYCODE_SETTINGS:
            case KEYCODE_NOTIFICATION: return new Decision(OPEN_OUR_SETTINGS, null);
            default: break;
        }
        if (scanCode == SCAN_STAR) return new Decision(OPEN_INPUTS, null);
        return PASS_IT;
    }

    private static Decision app(String pkg, Set<String> installed) {
        return installed != null && installed.contains(pkg) ? new Decision(OPEN_APP, pkg) : PASS_IT;
    }
}
