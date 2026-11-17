package sa.urtv.screensaver;

/**
 * 4.3.4: «each setting needs two presses». The settings build their whole list again when a late answer comes (the home's «UrTV النظيف» question
 * on each return and 1.5 s later, «النسخ التجريبية»'s check): a row built again between OK's press and its release never gets the click, so the
 * first press did nothing. While OK is held, a new build waits for its release; at most HOLD_MS, so a release that went to another window (a
 * dialog opened on the press) never keeps the list from being built. No Android in here (tests/ok_hold_test.py runs it).
 */
final class OkHold {
    static final long HOLD_MS = 1000;
    /** KeyEvent's codes: DPAD_CENTER, ENTER, NUMPAD_ENTER, BUTTON_A (what a TV remote's OK sends); KeyEvent.ACTION_DOWN 0, ACTION_UP 1 */
    static final int CENTER = 23, ENTER = 66, NUMPAD_ENTER = 160, BUTTON_A = 96, DOWN = 0, UP = 1;

    private long since = -1;      // when OK went down; -1: not held
    private boolean pending;      // a build was asked for while OK was held

    static boolean isOk(int code) { return code == CENTER || code == ENTER || code == NUMPAD_ENTER || code == BUTTON_A; }

    /** every key the screen sees, before it is handled. True when a build waited and is due at this release (after its click). */
    boolean key(int code, int action, long now) {
        if (!isOk(code)) return false;
        if (action == DOWN) { if (since < 0) since = now; return false; }
        if (action != UP) return false;
        since = -1;
        boolean due = pending;
        pending = false;
        return due;
    }

    /** render() asks: true means wait (it is remembered and done at the release) */
    boolean defer(long now) {
        if (since < 0 || now - since > HOLD_MS) { since = -1; pending = false; return false; }
        pending = true;
        return true;
    }
}
