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
        for (java.util.Map.Entry<String, String> e : m.entrySet()) b.append(b.length() > 0 ? "," : "").append(e.getKey()).append('=').append(validTarget(e.getValue()) ? e.getValue() : "");
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
            // 4.5: «youtube.2» / «youtube.3» hold the 2× and 3× targets, and a target may be @switcher or @center (an add-on older than 4.5 skips those)
            if (k.matches("[a-z]+(\\.[23])?") && validTarget(v)) m.put(k, v);
        }
        return m;
    }

    /** some button was given an app, or (4.5) a 2×/3× or the switcher or the control centre */
    static boolean anyChosen(java.util.Map<String, String> m) {
        for (String v : m.values()) if (chosen(v) || SWITCHER.equals(v) || CENTER.equals(v)) return true;
        return false;
    }

    /** 4.5: a button's three targets (1×, 2×, 3×; "" for none) from the choices */
    static String[] taps(java.util.Map<String, String> m, String id) {
        return new String[]{nz(m.get(id)), nz(m.get(id + ".2")), nz(m.get(id + ".3"))};
    }

    /** 4.5: the button needs the tap counter (a 2× or 3× is set, or its 1× is the switcher or the control centre); else the 4.4 path, unchanged */
    static boolean counted(String[] taps) {
        return !taps[1].isEmpty() || !taps[2].isEmpty() || SWITCHER.equals(taps[0]) || CENTER.equals(taps[0]);
    }

    private static String nz(String s) { return s == null ? "" : s; }

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

    // ---------------------------------------------------------------- 4.5: two and three presses (mockup docs/mockups/tools-45/02-taps.png)

    /** a target that is not an app: «التطبيقات المفتوحة» (the switcher) and «مركز التحكم» */
    static final String SWITCHER = "@switcher", CENTER = "@center";

    /** an app, the switcher, the control centre, or empty (the default) */
    static boolean validTarget(String s) { return validChoice(s) || SWITCHER.equals(s) || CENTER.equals(s); }

    /** the window between the presses of a double or triple press (tvOS Launcher's MULTI_TAP_WINDOW_MS, github.com/techgenius1/tvOS-Launcher, Apache-2.0) */
    static final long TAP_WINDOW_MS = 300;

    /** wait: a press was counted and the button has more presses set; ask {@link Taps#timeout} at {@link Taps#deadline} */
    static final int WAIT = 3;

    /**
     * How many presses a button got before it acts. A button with only one press set acts on its press at once (as before 4.5: no delay at all);
     * with a 2× or 3× it counts presses within TAP_WINDOW_MS of the last and acts on the highest count set as soon as it is reached, or when the
     * window passes (the count it reached; a count with no target falls back to the next lower one that has one). Every event of the button is
     * the add-on's while it counts (the app in front never gets half a press). taps[0..2]: the targets of 1×, 2×, 3× ("" = none).
     */
    static final class Taps {
        private String id;
        private int count;
        private long last;
        private String[] taps;
        /** when timeout() must be asked (uptime ms), while WAIT */
        long deadline;
        /** what to open on DONE-like answers (OPEN): an app, SWITCHER or CENTER */
        String target;

        static int highest(String[] taps) {
            for (int n = 3; n >= 1; n--) if (taps != null && taps.length >= n && validTarget(taps[n - 1]) && !taps[n - 1].isEmpty()) return n;
            return 0;
        }

        /** the target for {@code n} presses: that count's, else the next lower one that has one; null when none */
        static String forCount(String[] taps, int n) {
            for (int k = Math.min(n, 3); k >= 1; k--) if (taps != null && taps.length >= k && validTarget(taps[k - 1]) && !taps[k - 1].isEmpty()) return taps[k - 1];
            return null;
        }

        boolean pending() { return id != null; }

        /** a press (DOWN, repeat 0) of button {@code id} with its targets: OPEN (target set), WAIT (deadline set) or PASS (nothing set for it) */
        int press(String id, String[] taps, long now) {
            int max = highest(taps);
            if (max == 0) { reset(); return PASS; }
            if (max == 1) { reset(); target = forCount(taps, 1); return OPEN; }   // no 2× or 3×: at once, as before
            if (this.id != null && (!this.id.equals(id) || now - last > TAP_WINDOW_MS)) {   // another button, or too late: the first count stands
                reset();
            }
            this.id = id;
            this.taps = taps;
            count++;
            last = now;
            if (count >= max) { target = forCount(taps, count); reset(); return target == null ? EAT : OPEN; }
            deadline = now + TAP_WINDOW_MS;
            return WAIT;
        }

        /** the window passed (now >= deadline): the count reached acts; EAT when that count has no target and none below it */
        int timeout(long now) {
            if (id == null || now < deadline) return PASS;
            target = forCount(taps, count);
            reset();
            return target == null ? EAT : OPEN;
        }

        private void reset() { id = null; count = 0; taps = null; deadline = 0; }
    }
}
