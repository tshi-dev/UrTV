package sa.urtv.home;

/**
 * The favourites bar, exactly as docs/PROPOSALS-NOV2.md §1 says it (the owner's decision): the same tray on every look and every home
 * layout, always in its box; only a widget that opens into a scrolling row may move it, 40 px down. Every length is the spec's at
 * 1920×1080 and scales with the screen's height ({@link #px}). No Android in here: tests/FavBarRun.java checks the geometry.
 */
final class FavBar {
    private FavBar() {}

    /**
     * the tray: 128 from each side (x 128 to 1792), 218 high, radius 32. BOTTOM = the 5 % safe area: the tray's bottom is never lower than 54 above
     * the screen's, even while it is lowered for a widget. At rest it stands WIDGET_DROP (40) higher, REST_BOTTOM above the screen's bottom
     * (y 768 to 986), and lowering it puts it at y 808 to 1026 (build 179 review: it went to 1066, outside the safe area).
     */
    static final int SIDE = 128, TRAY_H = 218, RADIUS = 32, BOTTOM = 54, REST_BOTTOM = BOTTOM + 40;
    /** the tiles: 304×170 (a 16:9 banner), radius 16, 24 apart, and 24 from the tray's four sides: five tiles are exactly 24 + 5×304 + 4×24 + 24 = 1664 wide */
    static final int TILE_W = 304, TILE_H = 170, TILE_RADIUS = 16, GAP = 24, PAD_SIDE = 24, PAD_TOP = 24;
    /** five tiles show; more scroll inside the tray */
    static final int SHOWN = 5;
    /** the focused tile: 6 up and 1.08 from its centre, a 3 px ring, in 150 ms; a tile picked up to move: a little more, still inside */
    static final float FOCUS_SCALE = 1.08f, LIFTED_SCALE = 1.12f;
    static final int LIFT = 6, RING = 3, MS = 150;
    /** the app's name: 30 px, its box 56 above the tray's top (about 16 px between its baseline and the tray) */
    static final int NAME_PX = 30, NAME_GAP = 16;
    /** the one move: 40 px down in 250 ms while a scrolling widget has the focus */
    static final int WIDGET_DROP = 40, DROP_MS = 250;

    /** the tray's glass: white at 14 % with a 28 % line over a photo (a dark look), at 72 % with an 85 % line on a light look */
    static final int FILL_DARK = 0x24FFFFFF, LINE_DARK = 0x47FFFFFF, FILL_LIGHT = 0xB8FFFFFF, LINE_LIGHT = 0xD9FFFFFF;
    /** the focus ring: DGA green on a light look, a light green over a photo */
    static final int RING_LIGHT = 0xFF1B8354, RING_DARK = 0xFF8FE0B4;

    static int ring(boolean light) { return light ? RING_LIGHT : RING_DARK; }

    /** a tile's width on this screen: what is left of the tray for five, so rounding never pushes the fifth past it (304 at 1080p) */
    static int tileW(int screenH) {
        int inner = px(1920 - 2 * SIDE, screenH) - 2 * px(PAD_SIDE, screenH) - (SHOWN - 1) * px(GAP, screenH);
        return inner / SHOWN;
    }

    /**
     * How far the tray scrolls (in its content's pixels, + toward the content's end) to bring a focused tile ({@code rectLeft}..{@code rectRight}) fully
     * in view with the tray's inset ({@code inset}, 24) free on its side: 0 when it is whole inside already, else its edge goes to the inset, and
     * never past the content's ends. The stock scroll view brings a focused child flush with its edge (0 px inset, and the next tile cut to a sliver).
     */
    static int scrollDelta(int rectLeft, int rectRight, int scrollX, int viewW, int inset, int contentW) {
        int left = scrollX + inset, right = scrollX + viewW - inset, d;
        if (rectLeft >= left && rectRight <= right) return 0;
        d = rectLeft < left ? rectLeft - left : rectRight - right;
        int to = Math.max(0, Math.min(Math.max(0, contentW - viewW), scrollX + d));
        return to - scrollX;
    }

    /** a length of the 1080p spec on a screen this many pixels high */
    static int px(float spec, int screenH) { return Math.round(spec * screenH / 1080f); }

    /**
     * The name's shift so its centre is over the tile's ({@code centre}, in its parent's coordinates), kept inside the parent: a name at
     * {@code left}, {@code width} wide, in a parent {@code parentW} wide.
     */
    static float nameX(float centre, int left, int width, int parentW) {
        float x = centre - width / 2f;
        x = Math.max(0, Math.min(parentW - width, x));
        return x - left;
    }

    /**
     * ◀ ▶ in a row of the home (the favourites, the pills): the index the focus goes to from `index`, or -1 at the edge (it stays). `rtl` is
     * the row as it is drawn (Dir.layout() when it was built): in Arabic the first item is on the right, so ▶ goes to the one before; in English
     * the first is on the left and ▶ goes to the next one (tester step 20: the favourites went the wrong way in English).
     */
    static int sideStep(int index, int count, boolean right, boolean rtl) {
        int next = right == rtl ? index - 1 : index + 1;
        return next < 0 || next >= count ? -1 : next;
    }
}
