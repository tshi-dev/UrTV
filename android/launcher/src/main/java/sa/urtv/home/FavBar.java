package sa.urtv.home;

/**
 * The favourites bar, exactly as docs/PROPOSALS-NOV2.md §1 says it (the owner's decision): the same tray on every look and every home
 * layout, always in its box; only a widget that opens into a scrolling row may move it, 40 px down. Every length is the spec's at
 * 1920×1080 and scales with the screen's height ({@link #px}). No Android in here: tests/FavBarRun.java checks the geometry.
 */
final class FavBar {
    private FavBar() {}

    /**
     * the tray: 122 from each side (x 122 to 1798), 226 high (4.4 beta 2: 128 and 218 before; the room the ×1.14 focus needs, its bottom where it was), radius 24 (the material's outer corner, GlassStyle.RADIUS_OUTER; it was 32 before «توحيد المظهر»). BOTTOM = the 5 % safe area: the tray's bottom is never lower than 54 above
     * the screen's, even while it is lowered for a widget. At rest it stands WIDGET_DROP (40) higher, REST_BOTTOM above the screen's bottom
     * (y 760 to 986), and lowering it puts it at y 800 to 1026 (build 179 review: it went to 1066, outside the safe area).
     */
    static final int SIDE = 122, TRAY_H = 226, RADIUS = 24, BOTTOM = 54, REST_BOTTOM = BOTTOM + 40;
    /** the tiles: 304×170 (a 16:9 banner), radius 16, 24 apart, 30 from the tray's sides and 28 above and below: five tiles are exactly 30 + 5×304 + 4×24 + 30 = 1676 wide */
    static final int TILE_W = 304, TILE_H = 170, TILE_RADIUS = 16, GAP = 24, PAD_SIDE = 30, PAD_TOP = 28;
    /** five tiles show; more scroll inside the tray */
    static final int SHOWN = 5;
    /** the focused tile: 6 up and 1.14 from its centre, in 150 ms (4.4 beta 2: the tray has the room for ROOM_SCALE, the owner's 1.14 with no ring); a tile picked up to move: a little more, still inside */
    static final float FOCUS_SCALE = 1.14f, LIFTED_SCALE = 1.16f;
    /** the focus the tray is made to hold (tests/fav_bar_test.py): 1.14, the owner's size for a focus with no ring */
    static final float ROOM_SCALE = 1.14f;
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

    /** the way a stored value means: the three (4.5: and «زجاجي»), and 4.4's first two («إطار», «تكبير وظل»: the focus on today's tiles) are C */
    static String look(String stored) { return BANNERS.equals(stored) || ICONS.equals(stored) || GLASS.equals(stored) ? stored : TILES; }

    /**
     * 4.5 «زجاجي» (the owner approved docs/mockups/glass/): a liquid-glass bar at the bottom, as wide as its icons, each app's icon a smooth
     * squircle 96 at 100 %, 28 apart, 22 from the bar's edge, the corner about a quarter of the icon; the size 50 to 150 % and 1 to 3 rows are
     * the person's («حجم شريط tvOS», «صفوف شريط tvOS»). The bar stays centred and the clock where it is.
     */
    static final String GLASS = "glass";
    static final int GLASS_ICON = 96, GLASS_GAP = 28, GLASS_PAD = 22;
    /** the squircle's corner over its side (tvOS Launcher's smooth squircle is about the same) */
    static final float GLASS_CORNER = 0.24f;

    /** the stored size ("50".."150", steps of 10) as a factor; anything else 1 */
    static float glassK(String pct) {
        try {
            int v = Integer.parseInt(pct.trim());
            return v < 50 || v > 150 ? 1f : v / 100f;
        } catch (RuntimeException e) {
            return 1f;
        }
    }

    /** the stored rows ("1".."3"); anything else one */
    static int glassRows(String rows) {
        try {
            int v = Integer.parseInt(rows.trim());
            return v < 1 || v > 3 ? 1 : v;
        } catch (RuntimeException e) {
            return 1;
        }
    }

    /** an icon's side at this size */
    static int glassCell(int screenH, float k) { return px(GLASS_ICON * k, screenH); }

    /** the space between two icons (across and down) */
    static int glassGap(int screenH, float k) { return px(GLASS_GAP * k, screenH); }

    /** the glass between the icons and the bar's edge */
    static int glassPad(int screenH, float k) { return px(GLASS_PAD * k, screenH); }

    /** the squircle's corner */
    static int glassRadius(int screenH, float k) { return Math.round(glassCell(screenH, k) * GLASS_CORNER); }

    /** the bar's corner: the icon's plus the glass around it, so the two curves run side by side */
    static int glassBarRadius(int screenH, float k) { return glassRadius(screenH, k) + glassPad(screenH, k); }

    /** the columns of {@code n} icons in {@code rows} rows (the last row may be shorter); never more rows than icons */
    static int glassCols(int n, int rows) { return Math.max(1, (n + Math.max(1, rows) - 1) / Math.max(1, rows)); }

    /** the rows actually used by {@code n} icons */
    static int glassRowsUsed(int n, int rows) { return n <= 0 ? 1 : (n + glassCols(n, rows) - 1) / glassCols(n, rows); }

    /** the most of the screen's height the bar and the space under it may take: the clock, the greeting and the cards keep their place above it */
    static final float GLASS_MAX_H = 0.62f;

    /**
     * The rows asked for, fewer when they would not fit under {@link #GLASS_MAX_H} at this size (150 % in three rows is two), at least one.
     */
    static int glassRowsFit(int screenH, float k, int rows) {
        int r = Math.max(1, Math.min(3, rows));
        while (r > 1 && r * glassCell(screenH, k) + (r - 1) * glassGap(screenH, k) + 2 * glassPad(screenH, k) + 2 * glassGrow(screenH, k) + px(REST_BOTTOM, screenH) > screenH * GLASS_MAX_H) r--;
        return r;
    }

    /** the row's height (the tray's): its rows of icons (as many as fit, glassRowsFit), the gaps between them, the glass above and below, and room for the focused one to grow */
    static int glassRowH(int screenH, float k, int rows, int n) {
        int r = glassRowsUsed(n, glassRowsFit(screenH, k, rows));
        return r * glassCell(screenH, k) + (r - 1) * glassGap(screenH, k) + 2 * glassPad(screenH, k) + 2 * glassGrow(screenH, k);
    }

    /** what the focused icon grows on each side (ZOOM_SCALE about its centre), kept free above and below the bar */
    static int glassGrow(int screenH, float k) { return (int) Math.ceil(glassCell(screenH, k) * (ZOOM_SCALE - 1f) / 2f); }

    /** the bar's width for {@code n} icons in {@code rows} rows */
    static int glassBarW(int screenH, float k, int rows, int n) {
        int c = glassCols(n, rows);
        return c * glassCell(screenH, k) + (c - 1) * glassGap(screenH, k) + 2 * glassPad(screenH, k);
    }

    /** where icon {@code i} sits in the grid: {row, column}, row by row, the first at the start side (the right in Arabic) */
    static int[] glassCellAt(int i, int n, int rows) {
        int c = glassCols(n, rows);
        return new int[] {i / c, i % c};
    }

    /**
     * ◀ ▶ in the glass grid: the index the focus goes to, or -1 at a row's end (it stays: it never wraps to the next row). rtl as sideStep.
     */
    static int glassSideStep(int index, int n, int rows, boolean right, boolean rtl) {
        int c = glassCols(n, rows), next = sideStep(index, n, right, rtl);
        return next < 0 || next / c != index / c ? -1 : next;
    }

    /** ▲ ▼ in the glass grid: the one above or under (the nearest in a shorter last row), or -1 out of the grid (the home's own up and down) */
    static int glassUpDown(int index, int n, int rows, boolean down) {
        int c = glassCols(n, rows), r = index / c + (down ? 1 : -1);
        if (r < 0 || r * c >= n) return -1;
        return Math.min(n - 1, r * c + index % c);
    }

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

    /** 4.4 beta 2 «محاذاة التطبيقات»: the row's gravity (android.view.Gravity's values, plain ints so this stays plain Java): center, right or left */
    static int alignGravity(String align) {
        if ("right".equals(align)) return 0x05 | 0x10;   // the screen's right side (absolute, the same in Arabic and English), centred up and down
        if ("left".equals(align)) return 0x03 | 0x10;    // the screen's left side (absolute), centred up and down
        return 0x11;                                     // Gravity.CENTER
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
