package sa.urtv.home;

/**
 * When a paired phone may read and write «محفظتي» (Portfolio.editing), without Android (tests/portfolio_window_test.py runs it). Only the TV
 * opens the window, for EDIT_MS from the owner's last interaction there (the tab on the screen, «أسهل من الجوال», or the pairing screen
 * after the PIN when «اقفل محفظتي» is on): nothing a phone does renews it, so a paired phone cannot keep itself in. It runs on a monotonic
 * clock (SystemClock.elapsedRealtime), so setting the TV's clock back or forward neither extends nor ends it; a time before the opening
 * (never on a monotonic clock) reads as closed. Locking the portfolio, wiping it, or ending a phone session closes it.
 */
final class PortfolioWindow {
    private long openedAt = -1, until;

    /** The owner, on the TV: the phone may edit for ms from now (now: elapsedRealtime). */
    synchronized void open(long now, long ms) {
        openedAt = now;
        until = now + ms;
    }

    synchronized void close() {
        openedAt = -1;
        until = 0;
    }

    synchronized boolean open(long now) { return openedAt >= 0 && now >= openedAt && now < until; }

    /** What the pairing screen does when asked for «محفظتي» (its extra is a wish, never an authorization). */
    static final int SHOW = 0, ASK = 1, OPEN = 2;

    /**
     * editing: the window is already open (the owner opened it on the TV); locked and pinSet: «اقفل محفظتي» with a family PIN. SHOW: the
     * portfolio's pairing view as it is; ASK: the PIN first (cancelled: the plain phone view, nothing opened); OPEN: no lock, the screen
     * on the TV is the owner's interaction, as the tab itself is.
     */
    static int entry(boolean editing, boolean locked, boolean pinSet) {
        if (editing) return SHOW;
        return locked && pinSet ? ASK : OPEN;
    }
}
