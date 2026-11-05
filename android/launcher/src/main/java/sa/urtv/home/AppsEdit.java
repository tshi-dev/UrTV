package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/**
 * «تعديل التطبيقات»: «كل التطبيقات» edited in place with the remote, as «تعديل المفضلة» edits the favourites (FavEdit). Pick an app up with
 * OK, walk it across the grid with the arrows (the same Arabic directions, {@link FavEdit#step}), drop it with OK; Back while carrying
 * puts it back. The × under a tile (DOWN from the tile) hides it, and «تراجع» (UP from the first row, or Back while it shows) brings it
 * back for a few seconds. «المخفية», after the last app while something is hidden, lists the hidden apps; one chosen comes back (where
 * the saved grid will show it). UP from the first row while carrying puts the app at the end of the favourites, while they have room (Dock.MAX).
 * No Android in here: tests/AppsEditRun.java runs it. MainActivity draws what this says and, when the mode ends, saves the order
 * (Dock.saveApps, the "apps_order" preference), what was hidden or shown (the "hidden" list dock.sh hide / show share) and the favourites.
 */
final class AppsEdit {
    /** the line at the top of the screen while the mode is on */
    static final String HINT = S.n("اختر تطبيقًا لتحريكه · × للإخفاء · ارفعه للمفضلة · رجوع للإنهاء");

    /** «المخفية»: the last place of the grid while something is hidden */
    static final String HIDDEN_TITLE = S.n("المخفية");

    /** what a key did, for the screen to redraw */
    static final int NOTHING = 0, FOCUS = 1, PICKED = 2, MOVED = 3, DROPPED = 4, CANCELLED = 5, HIDDEN = 6, UNDONE = 7, EXIT = 8,
            TO_FAVOURITES = 9, FAVOURITES_FULL = 10, LIST = 11, SHOWN = 12;

    private final List<String> order;
    private final List<String> hidden;
    private final List<String> hiddenStart;
    private final List<String> toFavourites = new ArrayList<>();
    private final int cols, favourites, favouritesMax;
    private List<String> before;   // the order as it was when the app was picked up; null while nothing is carried
    private int at;                // the place the focus is on (the carried app while carrying); order.size() is «المخفية»
    private int from;
    private boolean onX, onUndo, moved;
    private String undoPkg;
    private int undoAt;
    private long undoUntil;

    /**
     * @param order the grid as it shows, @param hidden the apps hidden now (installed), @param cols the grid's columns,
     * @param favourites how many favourites there are, @param favouritesMax how many there may be (Dock.MAX)
     */
    AppsEdit(List<String> order, List<String> hidden, int cols, int favourites, int favouritesMax, int focus) {
        this.order = new ArrayList<>(order);
        this.hidden = new ArrayList<>(hidden);
        this.hiddenStart = new ArrayList<>(hidden);
        this.cols = Math.max(1, cols);
        this.favourites = favourites;
        this.favouritesMax = favouritesMax;
        this.at = Math.max(0, Math.min(focus, last()));
    }

    List<String> order() { return new ArrayList<>(order); }
    List<String> hidden() { return new ArrayList<>(hidden); }
    List<String> toFavourites() { return new ArrayList<>(toFavourites); }
    int at() { return at; }
    boolean carrying() { return before != null; }
    boolean onX() { return onX; }
    boolean onUndo() { return onUndo; }
    /** the focus is on «المخفية» */
    boolean onHiddenList() { return at == order.size(); }
    /** «المخفية» is in the grid (something is hidden) */
    boolean hiddenListShown() { return !hidden.isEmpty(); }
    /** an app was dropped somewhere else than it was (the order is saved only then) */
    boolean moved() { return moved; }
    /** hidden in this visit */
    List<String> hiddenNow() { List<String> l = new ArrayList<>(hidden); l.removeAll(hiddenStart); return l; }
    /** shown again in this visit */
    List<String> shownNow() { List<String> l = new ArrayList<>(hiddenStart); l.removeAll(hidden); return l; }
    boolean changed() { return moved || !hiddenNow().isEmpty() || !shownNow().isEmpty() || !toFavourites.isEmpty(); }
    String undoPackage() { return undoPkg; }
    boolean undoAvailable(long now) { return undoPkg != null && now < undoUntil; }

    /** the last place the focus can be on: «المخفية» when it shows, else the last app */
    private int last() { return hidden.isEmpty() ? Math.max(0, order.size() - 1) : order.size(); }

    /**
     * The grid as it shows: the owner's saved order first (the apps still there), then the rest as they come (by name), so a new app
     * goes at the end and one that went is simply not there. Nothing saved: the rest alone, by name, as always.
     */
    static List<String> topUp(List<String> chosen, List<String> rest, int min) {
        List<String> add = new ArrayList<>(), pool = new ArrayList<>(rest);
        java.util.Collections.sort(pool);   // package names: the same on every language (labels follow it, build 180: Downloader in Arabic, Apple TV in English)
        for (int n = chosen.size(); n < min && !pool.isEmpty(); n++) add.add(pool.remove(0));
        return add;
    }

    /** (the favourites' top-up, above) the order the grid shows */
    static List<String> arrange(List<String> byName, String saved) {
        List<String> out = new ArrayList<>();
        if (saved != null) for (String p : saved.split(",")) if (!p.isEmpty() && byName.contains(p) && !out.contains(p)) out.add(p);
        for (String p : byName) if (!out.contains(p)) out.add(p);
        return out;
    }

    /** Kids mode edits nothing: it shows only the allowed apps, so a saved order or row would lose the others. */
    static boolean allowed(boolean kids) { return !kids; }

    // ---------------------------------------------------------------- keys

    /**
     * LEFT or RIGHT as a step ({@link FavEdit#step}). Carrying: the app swaps with its neighbour, across the rows too. Otherwise the focus
     * moves within its row (the ends of a row stay, as on the home screen); on a × to the neighbour's ×.
     */
    int move(int delta, long now) {
        expire(now);
        if (delta == 0 || onUndo) return NOTHING;
        int to = at + delta;
        if (carrying()) {
            if (to < 0 || to >= order.size()) return NOTHING;
            order.add(to, order.remove(at));
            at = to;
            return MOVED;
        }
        int end = onX ? order.size() - 1 : last();
        if (to < 0 || to > end || to / cols != at / cols) return NOTHING;
        at = to;
        return FOCUS;
    }

    /**
     * UP. Carrying: to the row above; from the first row into the favourites, at their end (or FAVOURITES_FULL, still carried).
     * Otherwise: from the × to its tile, from a tile to the one above, from the first row to «تراجع» while it shows.
     */
    int up(long now) {
        expire(now);
        if (carrying()) {
            if (at >= cols) {
                order.add(at - cols, order.remove(at));
                at -= cols;
                return MOVED;
            }
            if (favourites + toFavourites.size() >= favouritesMax) return FAVOURITES_FULL;
            toFavourites.add(order.remove(at));
            before = null;
            moved = true;
            at = Math.min(at, last());
            return TO_FAVOURITES;
        }
        if (onX) { onX = false; return FOCUS; }
        if (at >= cols) { at -= cols; return FOCUS; }
        if (undoAvailable(now) && !onUndo) { onUndo = true; return FOCUS; }
        return NOTHING;
    }

    /**
     * DOWN. Carrying: to the row below (the last place when that row is shorter). Otherwise: from «تراجع» back to the tile, from a tile
     * to its ×, from a × to the tile below (the last one when that row is shorter); «المخفية» has no ×.
     */
    int down(long now) {
        expire(now);
        if (onUndo) { onUndo = false; return FOCUS; }
        if (carrying()) {
            int to = Math.min(at + cols, order.size() - 1);
            if (to / cols == at / cols) return NOTHING;
            order.add(to, order.remove(at));
            at = to;
            return MOVED;
        }
        if (onHiddenList() || order.isEmpty()) return NOTHING;
        if (!onX) { onX = true; return FOCUS; }
        int to = Math.min(at + cols, last());
        if (to / cols == at / cols) return NOTHING;
        at = to;
        onX = false;
        return FOCUS;
    }

    /** OK: on «تراجع» it undoes, on a × it hides, on «المخفية» it asks for the list, on a carried tile it drops, on another picks up. */
    int ok(long now) {
        expire(now);
        if (onUndo) return undo(now);
        if (onHiddenList()) return hidden.isEmpty() ? NOTHING : LIST;
        if (onX) {
            undoPkg = order.remove(at);
            hidden.add(undoPkg);
            undoAt = at;
            undoUntil = now + FavEdit.UNDO_MS;
            onX = false;
            at = Math.min(at, last());
            return HIDDEN;
        }
        if (carrying()) {
            if (!order.equals(before)) moved = true;
            before = null;
            return DROPPED;
        }
        before = new ArrayList<>(order);
        from = at;
        return PICKED;
    }

    /** Back: while carrying the app goes back; while «تراجع» shows it undoes; otherwise the mode is over (the screen saves). */
    int back(long now) {
        expire(now);
        if (carrying()) {
            order.clear();
            order.addAll(before);
            at = from;
            before = null;
            return CANCELLED;
        }
        if (undoAvailable(now)) return undo(now);
        return EXIT;
    }

    /** «تراجع»: the hidden app returns to its place, if it is still within its seconds. */
    int undo(long now) {
        if (!undoAvailable(now)) { expire(now); return NOTHING; }
        hidden.remove(undoPkg);
        order.add(Math.min(undoAt, order.size()), undoPkg);
        at = order.indexOf(undoPkg);
        undoPkg = null;
        onUndo = false;
        onX = false;
        return UNDONE;
    }

    /**
     * From the «المخفية» list: the app comes back at {@code place} (-1: the end), with the focus on it; never while one is carried. The
     * screen passes where the grid will show it once saved: by name while no order is kept, else at the end ({@link #arrange}).
     */
    int show(String pkg, int place, long now) {
        expire(now);
        if (carrying() || !hidden.remove(pkg)) return NOTHING;
        if (pkg.equals(undoPkg)) undoPkg = null;
        at = place < 0 || place > order.size() ? order.size() : place;
        order.add(at, pkg);
        onX = false;
        onUndo = false;
        return SHOWN;
    }

    /** The seconds are over: «تراجع» goes. True when something went. */
    boolean expire(long now) {
        if (undoPkg == null || now < undoUntil) return false;
        undoPkg = null;
        onUndo = false;
        return true;
    }
}
