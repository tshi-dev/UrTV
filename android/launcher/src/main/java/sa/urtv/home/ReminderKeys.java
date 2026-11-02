package sa.urtv.home;

/**
 * What a key does while a reminder's card is on the screensaver: the first press goes to the card and does not end the screensaver. OK
 * presses the button the card has focus on, a left or right arrow moves between its two buttons, and any other key puts the card away; each
 * is consumed whole (its repeats and its release too). Power, Home, the volume keys and the wake keys stay Android's. No Android state in here
 * (KeyEvent's numbers are written out, so it runs without the SDK): tests/ReminderKeysRun.java runs it.
 */
final class ReminderKeys {
    private static final int ACTION_DOWN = 0, ACTION_UP = 1;
    private static final int POWER = 26, SLEEP = 223, WAKEUP = 224, HOME = 3, VOLUME_UP = 24, VOLUME_DOWN = 25, MUTE = 164, UNKNOWN = 0;
    private static final int LEFT = 21, RIGHT = 22, CENTER = 23, ENTER = 66, NUMPAD_ENTER = 160, BUTTON_A = 96;

    /** the key goes on as it would have (no card, or one of Android's) */
    static final int PASS = 0;
    /** press the button that has the focus */
    static final int OK = 1;
    /** move the focus to the other button */
    static final int MOVE = 2;
    /** put the card away until the appointment changes */
    static final int AWAY = 3;
    /** consumed: the rest of a press the card took */
    static final int EAT = 4;

    private int swallowed = UNKNOWN;

    int decide(int keyCode, int action, int repeat, boolean cardShowing) {
        if (swallowed != UNKNOWN && keyCode == swallowed) {
            if (action == ACTION_UP) swallowed = UNKNOWN;
            return EAT;
        }
        if (!cardShowing || action != ACTION_DOWN || reserved(keyCode)) return PASS;
        if (repeat > 0) return EAT;   // a held key is one press
        swallowed = keyCode;
        if (keyCode == CENTER || keyCode == ENTER || keyCode == NUMPAD_ENTER || keyCode == BUTTON_A) return OK;
        if (keyCode == LEFT || keyCode == RIGHT) return MOVE;
        return AWAY;
    }

    private static boolean reserved(int k) {
        return k == POWER || k == SLEEP || k == WAKEUP || k == HOME || k == VOLUME_UP || k == VOLUME_DOWN || k == MUTE || k == UNKNOWN;
    }
}
