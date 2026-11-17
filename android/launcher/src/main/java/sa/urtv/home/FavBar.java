package sa.urtv.home;

/**
 * The favourites bar, exactly as docs/PROPOSALS-NOV2.md §1 says it (the owner's decision): the same tray on every look and every home
 * layout, always in its box; only a widget that opens into a scrolling row may move it, 40 px down. Every length is the spec's at
 * 1920×1080 and scales with the screen's height ({@link #px}). No Android in here: tests/FavBarRun.java checks the geometry.
 */
final class FavBar {
    private FavBar() {}

    /**
     * the tray: 128 from each side (x 128 to 1792), 218 high, radius 24 (the material's outer corner, GlassStyle.RADIUS_OUTER; it was 32 before «توحيد المظهر»). BOTTOM = the 5 % safe area: the tray's bottom is never lower than 54 above
     * the screen's, even while it is lowered for a widget. At rest it stands WIDGET_DROP (40) higher, REST_BOTTOM above the screen's bottom
     * (y 768 to 986), and lowering it puts it at y 808 to 1026 (build 179 review: it went to 1066, outside the safe area).
     */
    static final int SIDE = 128, TRAY_H = 218, RADIUS = 24, BOTTOM = 54, REST_BOTTOM = BOTTOM + 40;
    /** the tiles: 304×170 (a 16:9 banner), radius 16, 24 apart, and 24 from the tray's four sides: five tiles are exactly 24 + 5×304 + 4×24 + 24 = 1664 wide */
    static final int TILE_W = 304, TILE_H = 170, TILE_RADIUS = 16, GAP = 24, PAD_SIDE = 24, PAD_TOP = 24;
    /** five tiles show; more scroll inside the tray */
    static final int SHOWN = 5;
    /** the focused tile: 6 up and 1.08 from its centre, a 3 px ring, in 150 ms; a tile picked up to move: a little more, still inside */
    static final float FOCUS_SCALE = 1.08f, LIFTED_SCALE = 1.12f;
    /** 4.4 «شكل التطبيقات: تكبير وظل»: the focused app's size (the ring's look is FOCUS_SCALE) */
    static final float ZOOM_SCALE = 1.14f;
    static final int LIFT = 6, RING = 3, MS = 200;   // 4.4 the same easing everywhere: Motion.MS (plain Java here, tests/fav_bar_test.py runs it)
    /** the app's name: 30 px, its box 56 above the tray's top (about 16 px between its baseline and the tray) */
    static final int NAME_PX = 30, NAME_GAP = 16;
    /** the one move: 40 px down in 250 ms while a scrolling widget has the focus */
    static final int WIDGET_DROP = 40, DROP_MS = 250;

    /**
     * the tray's own glass and ring, as they were before «توحيد المظهر»: now the tray wears GlassStyle.material(…, BAR), which is this white glass only while the switch is off and
     * the apps bar's fill is «زجاجي» (its default there). The ring of the focused tile is the material's too (the look's focus colour or the chosen accent, in the focus style).
     */
    static final int FILL_DARK = 0x24FFFFFF, LINE_DARK = 0x47FFFFFF, FILL_LIGHT = 0xB8FFFFFF, LINE_LIGHT = 0xD9FFFFFF;   // the same numbers as GlassStyle.GLASS_* (tests/sync_look_test.py); no other class in here, so tests/FavBarRun.java runs it alone

    /** the most a tray ever shows whole at once, and the most favourites there are (Dock.MAX) */
    static final int MAX_SHOWN = 10;
    /** 4.3.1 (the owner): up to ten tiles show whole at once, with no scrolling: 9 and 10 are smaller (151 and 141 px wide at 1080p, 16:9), the default stays five at 304. Step 28 had cut it to eight as too small to read. */

    /**
     * How many tiles the tray shows whole at once: the setting ("5" to "10", or "all": every favourite, up to 10, so never a scroll), at least five (the tray never
     * shows fewer than the tiles it was drawn for) and never more than ten. An unknown word is five.
     */
    static int shown(String setting, int favourites) {
        if ("all".equals(setting)) return Math.max(SHOWN, Math.min(MAX_SHOWN, favourites));
        try {
            return Math.max(SHOWN, Math.min(MAX_SHOWN, Integer.parseInt(setting.trim())));
        } catch (RuntimeException e) {
            return SHOWN;
        }
    }

    /** a tile's width on this screen when {@code n} show: what is left of the tray, (inner width - (n-1) x 24) / n, so rounding never pushes the last past it (304 for 5, 213 for 7, 141 for 10 at 1080p) */
    static int tileW(int screenH, int n) {
        int inner = px(1920 - 2 * SIDE, screenH) - 2 * px(PAD_SIDE, screenH) - (n - 1) * px(GAP, screenH);
        return inner / n;
    }

    /**
     * The width of tile {@code i} of {@code n}: the same, and the pixels that rounding left over (up to n - 1) go one each to the first tiles, so the n tiles and
     * their gaps fill the tray's inner width exactly and the next tile starts right at the tray's padding edge, not 2 px inside it (a sliver at 7, step 28).
     */
    static int tileW(int screenH, int n, int i) {
        int inner = px(1920 - 2 * SIDE, screenH) - 2 * px(PAD_SIDE, screenH) - (n - 1) * px(GAP, screenH);
        return inner / n + (i < inner % n ? 1 : 0);
    }

    /** the tray's height when {@code n} tiles show: the tile's height (16:9) with the same 24 above and below it, so no empty band under small tiles (218 for five) */
    static int trayH(int screenH, int n) {
        return Math.round(tileW(screenH, n) * (float) TILE_H / TILE_W) + 2 * px(PAD_TOP, screenH);
    }

    /** the focused tile's lift (px): 6 for five or six tiles, none for smaller ones: it stays on its own centre line (at ten it jumped out of line) */
    static int lift(int screenH, int n) { return n <= 6 ? px(LIFT, screenH) : 0; }

    /** a tile's width with the five of the default */
    static int tileW(int screenH) { return tileW(screenH, SHOWN); }

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

    /**
     * 4.4 «شكل التطبيقات», three ways of the row (the owner approved the three: docs/mockups/couch-apps/ on work/433-design): A «بنرات»,
     * each app's TV banner as Google TV draws it, 256×144 at 1080, radius 16; B «أيقونات», each app's launcher icon in a rounded square,
     * 112, further apart (40); C «مربعات ملونة», today's tray of tiles. A and B have no tray, and every way is centred (CENTER_HORIZONTAL
     * in a row as wide as the screen's tray, so a short row sits in the middle and a long one scrolls with the focus).
     */
    static final String BANNERS = "banners", ICONS = "icons", TILES = "tiles";
    static final int BANNER_W = 256, BANNER_H = 144, ICON = 112, ICON_GAP = 40, ICON_RADIUS = 24;

    /** the way a stored value means: the three, and 4.4's first two («إطار», «تكبير وظل»: the focus on today's tiles) are C */
    static String look(String stored) { return BANNERS.equals(stored) || ICONS.equals(stored) ? stored : TILES; }

    /** «تكبير وظل» (bigger with a shadow, the name under it): always for A and B; for C as it was chosen ("zoom") */
    static boolean zoom(String stored) { return !TILES.equals(look(stored)) || "zoom".equals(stored); }

    /** the width of cell {@code i} of {@code n} shown: A and B their own size, C the tray's share (tileW) */
    static int cellW(String look, int screenH, int n, int i) {
        if (BANNERS.equals(look)) return px(BANNER_W, screenH);
        if (ICONS.equals(look)) return px(ICON, screenH);
        return tileW(screenH, n, i);
    }

    /** a cell's height over its width: 16:9 for a banner or a tile, 1 for an icon's square */
    static float aspect(String look) { return ICONS.equals(look) ? 1f : 9f / 16f; }

    /** the space between two cells */
    static int gap(String look, int screenH) { return px(ICONS.equals(look) ? ICON_GAP : GAP, screenH); }

    /** a cell's corners */
    static int radius(String look, int screenH) { return px(ICONS.equals(look) ? ICON_RADIUS : TILE_RADIUS, screenH); }

    /** the row's height: its cells with the same 24 above and below (room for the focused one to grow), or the tray's (trayH) */
    static int rowH(String look, int screenH, int n) {
        if (BANNERS.equals(look)) return px(BANNER_H, screenH) + 2 * px(PAD_TOP, screenH);
        if (ICONS.equals(look)) return px(ICON, screenH) + 2 * px(PAD_TOP, screenH);
        return trayH(screenH, n);
    }

    /** the row's inset on each side: the tray's 24 (its glass edge); none with no tray (A, B), so six banners fit the tray's width */
    static int padSide(String look, int screenH) { return TILES.equals(look) ? px(PAD_SIDE, screenH) : 0; }

    /** where a row of {@code n} cells starts (from its start edge) in a width {@code w}: centred, or 0 when it is wider (it scrolls) */
    static int centredStart(String look, int screenH, int n, int w) {
        int row = 0;
        for (int i = 0; i < n; i++) row += cellW(look, screenH, Math.max(n, SHOWN), i);
        row += Math.max(0, n - 1) * gap(look, screenH) + 2 * padSide(look, screenH);
        return Math.max(0, (w - row) / 2);
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
