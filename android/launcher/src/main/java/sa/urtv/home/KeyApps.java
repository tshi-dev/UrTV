package sa.urtv.home;

/**
 * «أزرار الريموت»: the app the owner gave one of this TV's remote buttons (RemoteButtons: which buttons, and which button a key is). An
 * empty choice is the default: the button does what it did before. On our home RemoteKeys still opens YouTube, Netflix and Prime Video
 * by default and asks the choice first; any other button with a choice opens it there too. Outside our home TypeService's key filter
 * asks {@link Outside#decide}, and only for a button the owner changed: a button left on default passes untouched, as before anyone
 * chose. The idea and the brands' codes are Free My TV's «Remote Buttons» (MIT, see RemoteButtons).
 *
 * No Android in here: tests/KeyAppsRun.java runs it.
 */
final class KeyApps {
    // android.view.KeyEvent's values
    static final int ACTION_DOWN = 0, ACTION_UP = 1;

    private KeyApps() {}

    /** A package name as Android writes them (two or more parts), or empty (the default). Anything else is refused before it is saved. */
    static boolean validChoice(String s) {
        return s != null && (s.isEmpty() || s.length() <= 200 && s.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"));
    }

    /** The owner gave this button another app. */
    static boolean chosen(String choice) {
        return choice != null && !choice.isEmpty() && validChoice(choice);
    }

    /** the button goes on as it would have */
    static final int PASS = 0;
    /** the repeats and the release of a press that acted, and the rest of a Xiaomi burst */
    static final int EAT = 1;
    /** open {@link #pkg} */
    static final int OPEN = 2;

    /** A chosen button's press opens its app; its repeats and release are consumed, so the app in front never gets half a key. */
    static final class Outside {
        private boolean holding;
        private int heldCode;
        private String lastId;
        private long lastDown = Long.MIN_VALUE / 2;
        /** the package the last OPEN asked for */
        String pkg;

        /**
         * id: the button (RemoteButtons.idOf), null for any other key; choice: the owner's app for it; burstMs: Xiaomi's burst window
         * (0 elsewhere), eventTime in ms.
         */
        int decide(String id, int keyCode, int action, int repeat, long eventTime, boolean homeFront, String choice, long burstMs) {
            if (id == null) return PASS;
            if (holding && keyCode == heldCode) {
                if (action == ACTION_DOWN && repeat == 0) holding = false;   // a new press: the release went to the app that opened
                else {
                    if (action == ACTION_UP) holding = false;
                    return EAT;
                }
            }
            if (homeFront || !chosen(choice)) return PASS;   // our home answers it itself (RemoteKeys); a default button is the TV's, as before
            if (action != ACTION_DOWN || repeat > 0) return PASS;
            holding = true;
            heldCode = keyCode;
            boolean burst = burstMs > 0 && id.equals(lastId) && eventTime - lastDown < burstMs;
            lastId = id;
            lastDown = eventTime;
            if (burst) return EAT;
            pkg = choice;
            return OPEN;
        }
    }
}
