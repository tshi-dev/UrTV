package sa.urtv.home;

/**
 * The size of the box the home screen's cards (HomeCard's WebView) are drawn in, as plain arithmetic so that
 * tests/card_box_test.py can check it without Android. Everything is in pixels; the box sits dp(48) from the left and dp(84)
 * from the top, left of the greeting and clock block and above the app title and the dock.
 */
final class CardBox {
    private CardBox() { }

    /** One card is the size it always was: 461 x 250 dp. */
    static final int ONE_W = 461, ONE_H = 250;
    static final int LEFT = 48, TOP = 84;
    /** A pair is 58.44% of the screen wide: at 1920 that is 560 + 22 + 540. */
    static final float PAIR_WIDTH = 0.5844f;
    /** The height of today's single card (250 dp of 540) as a share of the screen's. */
    static final float PAIR_HEIGHT = 0.463f;

    /**
     * @param cards     how many cards (1 to 3)
     * @param clockLeft where the greeting and clock block starts, from the screen's left; 0 or less: not laid out yet
     * @param titleTop  top of the block with the app title and the dock; 0 or less: not laid out yet
     * @param dockTop   top of the dock's band, in the same coordinates
     * @param titleWide the widest title among the dock's apps
     * @return {width, height}
     */
    static int[] box(int cards, int screenW, int screenH, float density, int clockLeft, int titleTop, int dockTop, int titleWide) {
        if (cards <= 1) return new int[]{dp(ONE_W, density), dp(ONE_H, density)};
        int w = Math.round(screenW * PAIR_WIDTH), h = Math.round(screenH * PAIR_HEIGHT);
        int left = dp(LEFT, density), top = dp(TOP, density);
        if (clockLeft > 0) w = Math.min(w, clockLeft - left - dp(16, density));   // clear of the clock block
        if (titleTop > 0) {
            // down to the dock, or only to the app title when the widest title would reach into the box
            int titleLeft = screenW - dp(48, density) - dp(8, density) - titleWide;
            int limit = titleLeft >= left + w + dp(16, density) ? dockTop - dp(8, density) : titleTop - dp(6, density);
            h = limit - top;
        }
        return new int[]{Math.max(w, dp(300, density)), Math.max(h, dp(150, density))};
    }

    /** 4.4 «التخطيط: كنبة»: the strip's height as a share of the screen's (216 px of 1080) */
    static final float ROW_HEIGHT = 0.2f;

    /**
     * 4.4 «التخطيط: كنبة»: the cards in one strip above the apps tray, the screen's width less 48 dp each side, ending 8 dp above the block
     * of the app's name and the tray (before the screen is laid out: 74 % of the height). {width, height, distance from the screen's bottom}.
     */
    static int[] row(int screenW, int screenH, float density, int titleTop) { return row(screenW, screenH, density, titleTop, -1); }

    /**
     * the same, with the big glass clock on the screen: its digits end at clockBottom (px; ScenePlan.fixedBox), and the strip never covers them
     * (the owner: the stocks card over the big «2:08»): it starts 12 dp under them and is that much shorter (a card in it then shows fewer rows);
     * clockBottom -1: no big clock
     */
    /** 4.4 beta 4 (the owner: «كنبة» as couch-tv, the big clock always centred): the strip's gaps, 1080p px scaled with FavBar.px, 720p the exact 2/3: under the digits, over the apps row */
    static final int GAP_CLOCK = 32, GAP_APPS = 28;

    /** the apps row's top at 1080p with the focused tile's growth (ICON 112 + 2 x PAD_TOP 28 above REST_BOTTOM 94 = 818, less 8 for the 1.14 focus): 810 */
    static final int APPS_FOCUS_TOP = 1080 - FavBar.REST_BOTTOM - (FavBar.ICON + 2 * FavBar.PAD_TOP) - 8;

    static int[] row(int screenW, int screenH, float density, int titleTop, int clockBottom) {
        if (clockBottom >= 0) {   // the big glass clock is on: the strip is between its digits and the apps row, on the tray's own sides (the UI session's spacing)
            int top = clockBottom + FavBar.px(GAP_CLOCK, screenH);
            int bottom = FavBar.px(APPS_FOCUS_TOP - GAP_APPS, screenH);
            return new int[]{screenW - 2 * FavBar.px(FavBar.SIDE, screenH), Math.max(0, bottom - top), Math.max(0, screenH - bottom)};
        }
        int h = Math.round(screenH * ROW_HEIGHT);
        int end = titleTop > 0 ? titleTop - dp(8, density) : Math.round(screenH * 0.74f);
        if (clockBottom >= 0) h = Math.max(0, Math.min(h, end - clockBottom - dp(12, density)));
        return new int[]{screenW - 2 * dp(LEFT, density), h, Math.max(0, screenH - end)};
    }

    /** 4.4 beta 2 «tvOS»: the row's height (a card's title and three lines at 1080, with room for the focused one's 1.04), and its gap above the bar */
    static final float TVOS_ROW_HEIGHT = 0.235f;
    static final int TVOS_GAP = 28;

    /**
     * «tvOS» (the owner approved docs/mockups/tvos-preset/): the cards in one row exactly as wide as the tvOS bar's tray (FavBar.SIDE from each side),
     * ending TVOS_GAP (1080p px, scaled) above the block of the name and the bar; with the TV's large font the row is taller (up to 1.3), so a card's lines
     * wrap and are never cut. {width, height, distance from the screen's bottom}.
     */
    static int[] tvosRow(int screenW, int screenH, int titleTop, float fontScale) {
        int h = Math.round(screenH * TVOS_ROW_HEIGHT * Math.max(1f, Math.min(1.3f, fontScale)));
        int end = titleTop > 0 ? titleTop - FavBar.px(TVOS_GAP, screenH) : Math.round(screenH * 0.74f);
        return new int[]{screenW - 2 * FavBar.px(FavBar.SIDE, screenH), h, Math.max(0, screenH - end)};
    }

    /** «tvOS»: how many cards show across: three, two on a screen of 800 px or less with the TV's large font (1.15 and up) */
    static int tvosSlots(int screenH, float fontScale) { return screenH <= 800 && fontScale >= 1.15f ? 2 : 3; }

    /** whether a card's box (left, top, right, bottom, px) covers any of the big clock's digits' box */
    static boolean overClock(int[] card, int[] clock) {
        return clock != null && card != null && card[0] < clock[2] && clock[0] < card[2] && card[1] < clock[3] && clock[1] < card[3];
    }

    private static int dp(int v, float density) { return Math.round(v * density); }
}
