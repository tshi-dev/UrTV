package sa.urtv.home;

import java.util.List;

/**
 * «أكمل المشاهدة» under the favourites (the idea from talkies, MIT: THIRD_PARTY_NOTICES.md): which Watch Next entries count, the line
 * under a card, which links a card may open and which pictures may be read, and how small a picture is made. No Android in here:
 * tests/TvRowsRun.java runs it.
 */
final class TvRowsPick {
    private TvRowsPick() { }

    static final String WATCH_NEXT = "watchnext";
    static final String WATCH_NEXT_TITLE = "أكمل المشاهدة";

    // android.media.tv.TvContract.WatchNextPrograms' types: what one is in the middle of (0), its next episode (1); a new
    // season (2) and a watchlist (3) are the app's promotion, not something watched
    static final int WATCH_NEXT_TYPE_CONTINUE = 0, WATCH_NEXT_TYPE_NEXT = 1;

    /** A Watch Next entry shown in «أكمل المشاهدة»: continue or next episode, browsable, from an app that is on the TV. */
    /**
     * How many cells «أكمل المشاهدة» is cut into: as many as the favourites, so a card is a favourite tile's size. Before the favourites are
     * on screen (both load in the background, the row may come first) the least the favourites can be, never one cell: one cell was the
     * whole screen's width, a 16:9 card two thirds of the screen tall, the favourites pushed up over the clock (the TV test of 3.147).
     */
    static int cells(int favourites, int least) { return favourites > 0 ? favourites : least; }

    /** The width of one of {@code n} cells on a screen {@code screenW} wide (dp): its two sides off, 20 dp between two cells (rowCell). */
    static float cellWidth(float screenW, float side, int n) { return (screenW - 2 * side - (n - 1) * 20f) / n; }

    /** A card's height (dp): 16:9 of its cell (AspectFrame). */
    static float cardHeight(float cellW) { return cellW * 9f / 16f; }

    static boolean watchNextShown(int type, boolean browsable, boolean appThere) {
        return (type == WATCH_NEXT_TYPE_CONTINUE || type == WATCH_NEXT_TYPE_NEXT) && browsable && appThere;
    }

    /** The line under a card: «الموسم ٢ · الحلقة ١ · Netflix», or the episode's own name, or the description; digits as the setting says. */
    static String meta(String season, String episode, String episodeTitle, String title, String description, String app, boolean latin) {
        StringBuilder m = new StringBuilder();
        if (!blank(season)) m.append("الموسم ").append(DigitsText.convert(season.trim(), latin));
        if (!blank(episode)) m.append(m.length() > 0 ? " · " : "").append("الحلقة ").append(DigitsText.convert(episode.trim(), latin));
        if (m.length() == 0 && !blank(episodeTitle) && !episodeTitle.trim().equals(title == null ? "" : title.trim())) m.append(episodeTitle.trim());
        if (m.length() == 0 && !blank(description)) m.append(description.trim());
        if (!blank(app)) m.append(m.length() > 0 ? " · " : "").append(app.trim());
        return m.toString();
    }

    /**
     * Whether a card may open its link: only into the app that published it. A link that names that app (its package or component)
     * opens as it is; one that names none (a plain https address, the usual kind) opens too, but pinned to that app (the caller sets
     * its package), so it never reaches another; one that names another app never opens.
     */
    static boolean linkAllowed(String rowPkg, String linkPkg, String componentPkg) {
        if (rowPkg == null || rowPkg.isEmpty()) return false;
        if (componentPkg != null) return rowPkg.equals(componentPkg) && (linkPkg == null || rowPkg.equals(linkPkg));
        return linkPkg == null || rowPkg.equals(linkPkg);
    }

    /** A content:// picture is read only from the publishing app's own provider (the authority's package), never another's. */
    static boolean pictureProviderAllowed(String rowPkg, String providerPkg) {
        return rowPkg != null && !rowPkg.isEmpty() && rowPkg.equals(providerPkg);
    }

    /**
     * The power of two to decode a picture of w×h with (BitmapFactory's inSampleSize) so that it comes out no bigger than twice the
     * card's size each way; never decoded at full size (an 8000×6000 JPEG would be 190 MB). 0 for a picture with no size.
     */
    static int sampleSize(int w, int h, int maxW, int maxH) {
        if (w <= 0 || h <= 0 || maxW <= 0 || maxH <= 0) return 0;
        int s = 1;
        while (w / s > 2 * maxW || h / s > 2 * maxH) s *= 2;
        return s;
    }

    /** The size a w×h picture is scaled to so that it fits maxW×maxH, its shape kept (never up). */
    static int[] fitted(int w, int h, int maxW, int maxH) {
        if (w <= maxW && h <= maxH) return new int[]{w, h};
        double k = Math.min(maxW / (double) w, maxH / (double) h);
        return new int[]{Math.max(1, (int) Math.round(w * k)), Math.max(1, (int) Math.round(h * k))};
    }

    /** What the screen shows, for telling a rebuild that changes nothing (a row's key, then each card's title and link). */
    static String signature(List<String[]> rowsAndCards) {
        StringBuilder b = new StringBuilder();
        for (String[] r : rowsAndCards) b.append(String.join("\u0001", r)).append('\u0002');
        return b.toString();
    }

    /** How far in, 0 to 1, from the position and the length; -1 when not known. */
    static float progress(long position, long duration) {
        return position > 0 && duration > 0 ? Math.min(1f, position / (float) duration) : -1f;
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty() || "null".equals(s.trim()); }
}
