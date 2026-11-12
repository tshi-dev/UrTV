package sa.urtv.home;

/**
 * «UrTV النظيف»'s prayer card: where it sits, as plain arithmetic (tests/clean_prayer_card_test.py runs it on real boxes). The card is one strip (five cells: the name over the time),
 * 380 x 76 dp at most. Everything in pixels, in the home's own coordinates. It is given the boxes of everything else on the home's top (the greeting, clock and date block, the quick
 * buttons with the settings button and the Wi-Fi mark, the adhan bar) and the top of the app title and the dock, and it never overlaps one of them and never leaves the safe area (5 %, and at
 * least 48 dp at the sides). Three places: «وسط الشاشة» (the middle of the screen across, the middle of the free band between those boxes and the apps bar down), «تحت الساعة» (under the clock
 * block, on its side) and «الجهة المقابلة للساعة» (at the far side, as high as the boxes there allow). When a place does not fit it falls back to the middle, which always does.
 */
final class PrayerCardBox {
    private PrayerCardBox() { }

    static final String CENTER = "center", BELOW = "below", OPPOSITE = "opposite";
    static final int W = 380, H = 76, GAP = 16;

    /**
     * @param clock    the greeting, clock and date block {left, top, right, bottom}
     * @param others   the boxes of the other things on the top of the home (the quick buttons, the adhan bar...), each {left, top, right, bottom}; may be empty
     * @param titleTop top of the app title and the dock under it
     * @return {left, top, right, bottom}
     */
    static int[] place(String pos, boolean rtl, int w, int h, float d, int[] clock, int[][] others, int titleTop) {
        int[][] all = new int[others.length + 1][];
        all[0] = clock;
        System.arraycopy(others, 0, all, 1, others.length);
        int gap = dp(GAP, d), tight = dp(8, d), side = Math.max(Math.round(w * 0.05f), dp(48, d)), top = Math.max(Math.round(h * 0.05f), dp(24, d));
        int limit = (titleTop > 0 ? titleTop - dp(6, d) : h - Math.round(h * 0.05f)), ch = dp(H, d);
        int cw = Math.min(dp(W, d), w - 2 * side);
        int[] r;
        if (OPPOSITE.equals(pos)) {
            int l = rtl ? side : w - side - cw;
            r = lowered(l, l + cw, Math.max(top, clock[1]), ch, all, gap);
            if (r[3] <= limit) return r;
        } else if (BELOW.equals(pos)) {
            int l = rtl ? clock[2] - cw : clock[0];
            l = Math.max(side, Math.min(l, w - side - cw));
            r = lowered(l, l + cw, clock[3] + tight, ch, all, tight);
            if (r[3] <= limit) return r;
        }
        // the middle: across the middle of the screen; down the middle of the band between the lowest box of the top and the apps bar
        int bottom = top;
        for (int[] o : all) bottom = Math.max(bottom, o[3]);
        int l = (w - cw) / 2, band = bottom + tight, t = Math.max(band, (band + limit - ch) / 2);
        return lowered(l, l + cw, t, ch, all, tight);
    }

    /** the card at `y`, moved down past every box it would touch (a box that shares some of its columns, within `gap`), until it touches none */
    private static int[] lowered(int l, int r, int y, int ch, int[][] boxes, int gap) {
        for (boolean moved = true; moved; ) {
            moved = false;
            for (int[] o : boxes) {
                boolean across = l < o[2] + gap && r > o[0] - gap, down = y < o[3] + gap && y + ch > o[1] - gap;
                if (across && down) { y = o[3] + gap; moved = true; }
            }
        }
        return new int[] {l, y, r, y + ch};
    }

    static String pos(String v) { return BELOW.equals(v) || OPPOSITE.equals(v) ? v : CENTER; }

    private static int dp(int v, float d) { return Math.round(v * d); }
}
