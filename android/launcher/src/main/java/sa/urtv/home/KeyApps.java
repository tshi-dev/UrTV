package sa.urtv.home;

/**
 * «أزرار الريموت»: which app the remote's YouTube, Netflix and Prime Video keys open, chosen by the owner (`optimize/dock.sh key`). An
 * empty choice is the default: the key's own app, as before. On our home RemoteKeys asks {@link #target}; outside it (another app, HDMI)
 * TypeService's key filter asks {@link Outside#decide}, and only for a key the owner has given another app: a key left on its default
 * passes untouched there, so the TV behaves as it did before anyone chose. The idea follows Free My TV's «Remote Buttons» (MIT).
 *
 * No Android in here (KeyEvent's numbers are written out): tests/KeyAppsRun.java runs it.
 */
final class KeyApps {
    static final String YOUTUBE = "youtube", NETFLIX = "netflix", PRIME = "prime";
    /** the keys an owner can give another app, in the order `dock.sh key` lists them */
    static final String[] KEYS = {YOUTUBE, NETFLIX, PRIME};

    // android.view.KeyEvent's values, and the vendor keys RemoteKeys knows (kept in step with it)
    static final int ACTION_DOWN = 0, ACTION_UP = 1;
    private static final int KEYCODE_BUTTON_3 = 190, KEYCODE_BUTTON_4 = 191;
    private static final int TCL_NETFLIX = 4062, TCL_YOUTUBE = 4065, TCL_PRIME = 4099;

    private KeyApps() {}

    /** The key's name, or null for any other key (the filter passes those at once, before anything is read). */
    static String keyOf(int keyCode) {
        switch (keyCode) {
            case KEYCODE_BUTTON_3:
            case TCL_YOUTUBE: return YOUTUBE;
            case KEYCODE_BUTTON_4:
            case TCL_NETFLIX: return NETFLIX;
            case TCL_PRIME: return PRIME;
            default: return null;
        }
    }

    static boolean isKey(String name) {
        for (String k : KEYS) if (k.equals(name)) return true;
        return false;
    }

    /** The app a key opens when nothing was chosen. */
    static String defaultPackage(String key) {
        if (YOUTUBE.equals(key)) return RemoteKeys.YOUTUBE;
        if (NETFLIX.equals(key)) return RemoteKeys.NETFLIX;
        if (PRIME.equals(key)) return RemoteKeys.PRIME;
        return null;
    }

    /** A package name as Android writes them (two or more parts), or empty (the default). Anything else is refused before it is saved. */
    static boolean validChoice(String s) {
        return s != null && (s.isEmpty() || s.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+") && s.length() <= 200);
    }

    /** The app the key opens: the owner's choice, else the key's own app. */
    static String target(String key, String choice) {
        return choice != null && !choice.isEmpty() && validChoice(choice) ? choice : defaultPackage(key);
    }

    /** The owner gave this key another app (only then does the filter outside our home act on it). */
    static boolean chosen(String choice) {
        return choice != null && !choice.isEmpty() && validChoice(choice);
    }

    /** the key goes on as it would have */
    static final int PASS = 0;
    /** the repeats and the release of a press that acted */
    static final int EAT = 1;
    /** open {@link #pkg} */
    static final int OPEN = 2;

    /** Outside our home: a press of a chosen key opens its app; its repeats and release are consumed, so the app in front never gets half a key. */
    static final class Outside {
        private boolean holding;
        private int heldCode;
        /** the package the last OPEN asked for */
        String pkg;

        /** choice: the owner's choice for this key (from keyOf), read by the caller only for one of these keys */
        int decide(int keyCode, int action, int repeat, boolean homeFront, String choice) {
            String key = keyOf(keyCode);
            if (key == null) return PASS;
            if (holding && keyCode == heldCode) {
                if (action == ACTION_DOWN && repeat == 0) holding = false;   // a new press: the release went to the app that opened
                else {
                    if (action == ACTION_UP) holding = false;
                    return EAT;
                }
            }
            if (homeFront || !chosen(choice)) return PASS;   // our home answers it itself (RemoteKeys); a default key is the TV's, as before
            if (action != ACTION_DOWN || repeat > 0) return PASS;
            holding = true;
            heldCode = keyCode;
            pkg = choice;
            return OPEN;
        }
    }
}
