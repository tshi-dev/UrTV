package sa.urtv.home;

/**
 * «UrTV النظيف»'s prayer card: where it sits, as plain arithmetic (tests/clean_prayer_card_test.py runs it on real boxes). The card is one strip (five cells: the name over the time),
 * 380 x 76 dp at most. Everything in pixels, in the home's own coordinates. Three places, never outside the safe area (5 %, and at least 48 dp at the sides) and never over the clock
 * block or the app title and the dock: «وسط» (the middle of the screen, moved aside only as far as the clock block needs), «تحت الساعة» (under the clock block, on its side) and «الجهة
 * المقابلة للساعة» (at the far side, level with the clock). When a place does not fit it falls back to the middle, which always does.
 */
final class PrayerCardBox {
    private PrayerCardBox() { }

    static final String CENTER = "center", BELOW = "below", OPPOSITE = "opposite";
    static final int W = 380, H = 76, GAP = 16;

    /**
     * @param clock    the greeting, clock and date block {left, top, right, bottom}
     * @param titleTop top of the app title and the dock under it
     * @return {left, top, right, bottom}
     */
    static int[] place(String pos, boolean rtl, int w, int h, float d, int[] clock, int titleTop) {
        int gap = dp(GAP, d), side = Math.max(Math.round(w * 0.05f), dp(48, d)), top = Math.max(Math.round(h * 0.05f), dp(24, d));
        int limit = (titleTop > 0 ? titleTop - dp(6, d) : h - Math.round(h * 0.05f)), ch = dp(H, d);
        int cw = Math.min(dp(W, d), w - 2 * side);
        int freeL = rtl ? side : clock[2] + gap, freeR = rtl ? clock[0] - gap : w - side;   // the span beside the clock block
        int span = Math.max(dp(240, d), freeR - freeL);
        if (OPPOSITE.equals(pos)) {
            int ow = Math.min(cw, freeR - freeL);
            int l = rtl ? freeL : freeR - ow, t = Math.max(top, clock[1]);
            if (ow >= dp(240, d) && t + ch <= limit) return new int[] {l, t, l + ow, t + ch};
        } else if (BELOW.equals(pos)) {
            int l = rtl ? clock[2] - cw : clock[0];
            l = Math.max(side, Math.min(l, w - side - cw));
            int t = clock[3] + dp(8, d);   // closer than the sides' gap: the room under the clock block is short
            if (t + ch <= limit) return new int[] {l, t, l + cw, t + ch};
        }
        // the middle: level in the band under the top edge and above the app title; aside only when the clock block is in the way
        int l = (w - cw) / 2, t = Math.max(top, (top + limit - ch) / 2);
        boolean hitsClock = l < clock[2] + gap && l + cw > clock[0] - gap && t < clock[3] + gap && t + ch > clock[1] - gap;
        if (hitsClock) {
            int ow = Math.min(cw, span);
            l = rtl ? Math.max(freeL, freeR - ow - (freeR - freeL - ow) / 2) : freeL + (freeR - freeL - ow) / 2;
            l = Math.max(side, Math.min(l, w - side - ow));
            cw = ow;
            if (l + cw > clock[0] - gap && l < clock[2] + gap) t = Math.max(t, clock[3] + gap);   // no span beside it: under it
        }
        return new int[] {l, t, l + cw, t + ch};
    }

    static String pos(String v) { return BELOW.equals(v) || OPPOSITE.equals(v) ? v : CENTER; }

    private static int dp(int v, float d) { return Math.round(v * d); }
}
