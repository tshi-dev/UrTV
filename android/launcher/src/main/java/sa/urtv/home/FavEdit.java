package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/**
 * «تعديل المفضلة» on the home screen: the favourites row edited in place with the remote. Pick an app up with OK, walk it along the row
 * with the arrows, drop it with OK; Back while carrying puts it back; the × under a tile (DOWN from the tile) removes it, and «تراجع»
 * (UP from the row, or Back while it shows) brings it back for a few seconds. No Android in here (key codes are written out, so it runs without the SDK): tests/FavEditRun.java
 * runs it. MainActivity draws what this says and saves {@link #row} through Dock.save, the store «ترتيب المفضلة» and dock.sh share.
 */
final class FavEdit {
    // android.view.KeyEvent's values
    static final int KEYCODE_BACK = 4, KEYCODE_DPAD_UP = 19, KEYCODE_DPAD_DOWN = 20, KEYCODE_DPAD_LEFT = 21, KEYCODE_DPAD_RIGHT = 22;

    /** the line at the top of the screen while the mode is on */
    static final String HINT = S.n("اختر تطبيقًا لتحريكه · × للحذف · رجوع للإنهاء");

    /** how long «تراجع» stays */
    static final long UNDO_MS = 5000;

    /** what a key did, for the screen to redraw */
    static final int NOTHING = 0, FOCUS = 1, PICKED = 2, MOVED = 3, DROPPED = 4, CANCELLED = 5, REMOVED = 6, TOO_FEW = 7, UNDONE = 8, EXIT = 9;

    private final List<String> row;
    private final List<String> start;   // the row as it was when the mode began, to tell whether anything changed
    private final int min;
    private List<String> before;        // the row as it was when the app was picked up; null while nothing is carried
    private int at;                     // the tile the focus is on (the carried one while carrying)
    private int from;                   // where the carried app was picked up
    private boolean onX, onUndo;
    private String undoPkg;
    private int undoAt;
    private long undoUntil;

    /** @param min the row never goes below this many apps (Dock.MIN) */
    FavEdit(List<String> row, int min, int focus) {
        this.row = new ArrayList<>(row);
        this.start = new ArrayList<>(row);
        this.min = min;
        this.at = Math.max(0, Math.min(focus, row.size() - 1));
    }

    List<String> row() { return new ArrayList<>(row); }
    int at() { return at; }
    boolean carrying() { return before != null; }
    boolean onX() { return onX; }
    boolean onUndo() { return onUndo; }
    boolean changed() { return !row.equals(start); }
    String undoPackage() { return undoPkg; }

    boolean undoAvailable(long now) { return undoPkg != null && now < undoUntil; }

    /**
     * Which way in the row a key goes. The row reads from the right in Arabic, so the app after another is on its left: RIGHT is toward
     * the start of the row (−1) and LEFT toward the end (+1). Left to right (an English layout) it is the other way round.
     */
    static int step(int keyCode, boolean rtl) {
        if (keyCode != KEYCODE_DPAD_LEFT && keyCode != KEYCODE_DPAD_RIGHT) return 0;
        boolean right = keyCode == KEYCODE_DPAD_RIGHT;
        return right == rtl ? -1 : 1;
    }

    /** A held key moves one step per {@code gapMs}, the first press always; the same pace the rest of the home screen uses. */
    static boolean stepAllowed(int repeat, long last, long now, long gapMs) {
        return repeat == 0 || now - last >= gapMs;
    }

    /** OK and Back act once per press: their repeats (a held key) are eaten, never a second pick-up or drop. */
    static boolean oncePerPress(int repeat) { return repeat == 0; }

    // ---------------------------------------------------------------- keys

    /** LEFT or RIGHT as a step along the row ({@link #step}): the carried app swaps with its neighbour; otherwise the focus moves. */
    int move(int delta, long now) {
        expire(now);
        if (delta == 0 || onUndo) return NOTHING;
        int to = at + delta;
        if (to < 0 || to >= row.size()) return NOTHING;   // the edge of the row: stay put
        if (carrying()) {
            String p = row.remove(at);
            row.add(to, p);
            at = to;
            return MOVED;
        }
        at = to;
        return FOCUS;
    }

    /**
     * UP. Carrying: to the start of the row, or to the row above when the layout has rows of {@code perRow} (0 = one row).
     * Otherwise: from the × back to the tile, from the tile to «تراجع» when it is showing.
     */
    int up(int perRow, long now) {
        expire(now);
        if (carrying()) {
            int to = perRow > 0 && perRow < row.size() ? Math.max(0, at - perRow) : 0;
            if (to == at) return NOTHING;
            row.add(to, row.remove(at));
            at = to;
            return MOVED;
        }
        if (onX) { onX = false; return FOCUS; }
        if (undoAvailable(now) && !onUndo) { onUndo = true; return FOCUS; }
        return NOTHING;
    }

    /** DOWN. Carrying: nothing. From a tile to its ×, from «تراجع» back to the tile. */
    int down(long now) {
        expire(now);
        if (carrying()) return NOTHING;
        if (onUndo) { onUndo = false; return FOCUS; }
        if (!onX && !row.isEmpty()) { onX = true; return FOCUS; }
        return NOTHING;
    }

    /** OK: on «تراجع» it undoes, on a × it removes, on a carried tile it drops, on any other tile it picks the app up. */
    int ok(long now) {
        expire(now);
        if (onUndo) return undo(now);
        if (row.isEmpty()) return NOTHING;
        if (onX) {
            if (row.size() <= min) return TOO_FEW;
            undoPkg = row.remove(at);
            undoAt = at;
            undoUntil = now + UNDO_MS;
            at = Math.min(at, row.size() - 1);
            onX = false;
            return REMOVED;
        }
        if (carrying()) {
            before = null;
            return DROPPED;
        }
        before = new ArrayList<>(row);
        from = at;
        return PICKED;
    }

    /**
     * Back: while carrying the app goes back where it was; while «تراجع» shows it undoes, wherever the focus is (on the TV the UP to
     * the chip did not always reach it, and the seconds ran out); otherwise the mode is over (the screen saves {@link #row}).
     */
    int back(long now) {
        expire(now);
        if (carrying()) {
            row.clear();
            row.addAll(before);
            at = from;
            before = null;
            return CANCELLED;
        }
        if (undoAvailable(now)) return undo(now);
        return EXIT;
    }

    /** «تراجع»: the removed app returns to its place, if it is still within its seconds. */
    int undo(long now) {
        if (!undoAvailable(now)) { expire(now); return NOTHING; }
        row.add(Math.min(undoAt, row.size()), undoPkg);
        at = Math.min(undoAt, row.size() - 1);
        undoPkg = null;
        onUndo = false;
        onX = false;
        return UNDONE;
    }

    /** The seconds are over: «تراجع» goes (the focus on it comes back to the tile). True when something went. */
    boolean expire(long now) {
        if (undoPkg == null || now < undoUntil) return false;
        undoPkg = null;
        onUndo = false;
        return true;
    }
}
