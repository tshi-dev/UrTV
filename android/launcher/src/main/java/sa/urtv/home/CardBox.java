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
    static int[] row(int screenW, int screenH, float density, int titleTop) {
        int h = Math.round(screenH * ROW_HEIGHT);
        int end = titleTop > 0 ? titleTop - dp(8, density) : Math.round(screenH * 0.74f);
        return new int[]{screenW - 2 * dp(LEFT, density), h, Math.max(0, screenH - end)};
    }

    private static int dp(int v, float density) { return Math.round(v * density); }
}
