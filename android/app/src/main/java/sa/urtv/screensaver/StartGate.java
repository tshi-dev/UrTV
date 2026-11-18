package sa.urtv.screensaver;

/**
 * 4.4 review F1: which start of the page (SaverStart.begin) is the current one, and whether a settings change arrived while it was still on its way. The page's view is
 * created only after a picture decode and a first frame, so for that while the host has no page to rebuild: a change is remembered (dirty) and looked at again once the
 * page exists, and a start that a newer one has replaced creates nothing (never two pages, never a view nobody releases). Main thread only.
 */
final class StartGate {
    private int gen;
    private boolean dirty;
    private boolean pending;

    /** a start begins: the earlier one (if any) is out of date */
    int begin() { pending = true; dirty = false; return ++gen; }
    /** the start with this token may still create the page */
    boolean current(int token) { return token == gen; }
    /** the page of this start was created: nothing is on its way now */
    void created(int token) { if (token == gen) pending = false; }
    /** the window went: nothing is on its way, and no start may create a page */
    void cancel() { gen++; pending = false; dirty = false; }
    /** a settings change arrived: true when a start is on its way (the change is kept for takeDirty), false when the page exists and can be rebuilt now */
    boolean changedWhileStarting() { if (!pending) return false; dirty = true; return true; }
    /** after the page exists: true once when a change came in during the start */
    boolean takeDirty() { boolean d = dirty; dirty = false; return d; }
}
