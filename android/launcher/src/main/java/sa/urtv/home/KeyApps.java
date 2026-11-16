package sa.urtv.home;

/**
 * «أزرار الريموت»: the app the owner gave one of this TV's remote buttons (RemoteButtons: which buttons, and which button a key is). An empty
 * choice is the default: the button does what it did before. On our home MainActivity asks the choice first (a chosen button opens its app,
 * behind the family lock as its tile) and leaves a default button to RemoteKeys, untouched. Over other apps «إضافات تلفزيونك» (ExtrasA11yService)
 * asks {@link Outside#decide}, and asks Android for key events only while some button was given an app ({@link #filterWanted}): a button left
 * on default passes untouched, as before anyone chose. The idea and the brands' codes are Free My TV's «Remote Buttons» (MIT, see RemoteButtons);
 * the first version is by 7odaifa-ab (GitHub #59).
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

    /** The choices in one line, "youtube=PKG,netflix=,…" (the order of the map): what the home hands the add-on. */
    static String line(java.util.Map<String, String> m) {
        StringBuilder b = new StringBuilder();
        for (java.util.Map.Entry<String, String> e : m.entrySet()) b.append(b.length() > 0 ? "," : "").append(e.getKey()).append('=').append(validChoice(e.getValue()) ? e.getValue() : "");
        return b.toString();
    }

    /** The line back to a map; a part that is not "id=PKG" (a word, then a package name or nothing) is left out. */
    static java.util.Map<String, String> parse(String line) {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        if (line == null) return m;
        for (String part : line.split(",")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String k = part.substring(0, eq), v = part.substring(eq + 1);
            if (k.matches("[a-z]+") && validChoice(v)) m.put(k, v);
        }
        return m;
    }

    /** some button was given an app */
    static boolean anyChosen(java.util.Map<String, String> m) {
        for (String v : m.values()) if (chosen(v)) return true;
        return false;
    }

    /**
     * «إضافات تلفزيونك» asks Android for key events for these buttons only while another app is in front, some button was given an app, and a filter is
     * safe on this TV (not TCL's Android 11). Every button on its default: no filter for them at all, the TV's keys as before anyone chose.
     */
    static boolean filterWanted(boolean homeFront, boolean anyChosen, boolean filterSafe) {
        return !homeFront && anyChosen && filterSafe;
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
        /** Xiaomi: the rest of a burst whose first press our home answered (it opened the app, then left the front): eaten until then */
        private long quietUntil = Long.MIN_VALUE / 2;
        /** the package the last OPEN asked for */
        String pkg;

        /** a press of this key acted and its repeats or release are still to come */
        boolean holding(int keyCode) { return holding && keyCode == heldCode; }

        /** our home left the front at uptime now (the add-on's side): with a burst window, a chosen press that soon is the rest of the home's press */
        void leftHome(long now, long burstMs) { if (burstMs > 0) quietUntil = now + burstMs; }

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
            if (homeFront || !chosen(choice)) return PASS;   // our home answers it itself (MainActivity); a default button is the TV's, as before
            if (action != ACTION_DOWN || repeat > 0) return PASS;
            holding = true;
            heldCode = keyCode;
            boolean burst = burstMs > 0 && (id.equals(lastId) && eventTime - lastDown < burstMs || eventTime < quietUntil);
            lastId = id;
            lastDown = eventTime;
            if (burst) return EAT;
            pkg = choice;
            return OPEN;
        }
    }
}
