package sa.urtv.home;

/**
 * 4.4 beta 2, the owner's test of build 525 («كل التطبيقات»: the first row cut at the top of the screen, the focused tile partly off): which grid
 * row is the first on the screen. Whole rows only: the rows shown start at a safe top, the focused row is always whole above a safe bottom, and the
 * page moves only when the focus leaves the rows shown, by as few rows as it takes. No Android (tests/tile_art_test.py runs it).
 */
final class GridWindow {
    private GridWindow() { }

    /** the safe top (under the screen's edge and the overscan) and bottom, as shares of the screen's height */
    static final float SAFE_TOP = 0.10f, SAFE_BOTTOM = 0.08f;

    /**
     * @param tops    each row's top, in the page's coordinates
     * @param bottoms each row's bottom
     * @param at      the focused row
     * @param first   the first row shown now
     * @param safeTop the screen's y where the first row shown starts
     * @param safeEnd the screen's y the focused row must end above
     */
    static int first(int[] tops, int[] bottoms, int at, int first, int safeTop, int safeEnd) {
        int n = tops.length;
        if (n == 0) return 0;
        at = Math.max(0, Math.min(at, n - 1));
        first = Math.max(0, Math.min(first, n - 1));
        if (at < first) return at;   // up past the first row shown: it becomes the first
        // down: the fewest rows moved so the focused row ends above the safe end
        int f = first;
        while (f < at && bottoms[at] - tops[f] > safeEnd - safeTop) f++;
        return f;
    }
}
