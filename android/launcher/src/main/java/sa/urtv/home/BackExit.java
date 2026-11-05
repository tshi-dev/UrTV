package sa.urtv.home;

/**
 * «الخروج من تلفزيونك» (the screensaver's settings, HomeExit there): how Back leaves our home where the TV keeps its own launcher and ours was
 * opened from its tile. "press" (the default, as always: one Back leaves, HomeBack.leaves), "hold" (a long Back leaves; a short one does
 * nothing) or "three" (three Backs in a row, each within THREE_GAP_MS of the one before). Never while ours is the default home (Back does not
 * leave it anyway), never from the scrolled grid (Back goes to the dock first). The first ignored Back of each visit shows a short hint.
 * One instance per home screen, fed every Back key event while {@link #guards} holds. No Android in here: tests/BackExitRun.java.
 */
final class BackExit {
    static final String PRESS = "press", HOLD = "hold", THREE = "three";
    /** how long Back must be held to leave with "hold" (Android's own long press is 500 ms: a little more, so a slow press is not one) */
    static final long HOLD_MS = 800;
    /** the most between two Backs of "three" */
    static final long THREE_GAP_MS = 1500;

    /** what to do with one key event */
    static final int EAT = 0, HINT = 1, LEAVE = 2;

    /** «&backexit=hold» or «&backexit=three» in the screensaver's fragment; anything else (an older screensaver, none) is one press, as always */
    static String fromFragment(String fragment) {
        if (fragment == null) return PRESS;
        for (String part : fragment.split("&")) {
            if (part.equals("backexit=" + HOLD)) return HOLD;
            if (part.equals("backexit=" + THREE)) return THREE;
        }
        return PRESS;
    }

    /** Does this Back go through BackExit? Only away from the default home, at the top, and with hold or three chosen. */
    static boolean guards(String mode, boolean defaultHome, boolean gridScrolled) {
        return !defaultHome && !gridScrolled && (HOLD.equals(mode) || THREE.equals(mode));
    }

    private String mode = PRESS;
    private boolean down, left, hinted;
    private int count;
    private long lastPress = Long.MIN_VALUE / 2;

    /** the mode of the press now going on (the hint's words) */
    String mode() { return mode; }

    /** A new visit (the home came to the front): the count starts again and the hint may show once more. */
    void reset() { down = false; left = false; hinted = false; count = 0; lastPress = Long.MIN_VALUE / 2; }

    /** The first Back of a press (repeat 0, key down), with the mode guards() was given. */
    int press(String mode, long eventTime) {
        this.mode = mode;
        down = true;
        left = false;
        if (THREE.equals(mode)) {
            count = eventTime - lastPress <= THREE_GAP_MS ? count + 1 : 1;
            lastPress = eventTime;
            if (count >= 3) { count = 0; left = true; return LEAVE; }
            return count == 1 ? hintOnce() : EAT;
        }
        return EAT;   // hold: what it is is known when it is held long enough, or let go
    }

    /** A repeat of a held Back (key down, repeat > 0). */
    int repeat(long eventTime, long downTime) {
        if (HOLD.equals(mode) && down && !left && eventTime - downTime >= HOLD_MS) { left = true; return LEAVE; }
        return EAT;
    }

    /** Back let go. A release with no press seen here (the press was on another screen) does nothing. */
    int release(long eventTime, long downTime) {
        if (!down) return EAT;
        down = false;
        if (!HOLD.equals(mode) || left) return EAT;
        if (eventTime - downTime >= HOLD_MS) { left = true; return LEAVE; }   // a remote that sends no repeats: the length of the press says it
        return hintOnce();
    }

    private int hintOnce() {
        if (hinted) return EAT;
        hinted = true;
        return HINT;
    }
}
