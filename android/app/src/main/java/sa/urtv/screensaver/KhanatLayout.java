package sa.urtv.screensaver;

/**
 * Where «خمس خانات» puts one tile per side on its two screens (no Android, tests/GamesKitTest.java), on the 1920 x 1080 design inside the 96 px safe area, x from the START
 * edge. Up to five sides the tiles stand in one row; from six (individual mode: 2 to 10 players) they stand in two rows of at most five, so a tile is never narrower than
 * 326 px and «لاعب ١٠» always fits at 1080p and at 720p (the screen scales the whole design).
 */
final class KhanatLayout {
    static final float LEFT = 96, WIDTH = 1728, GAP = 24;

    /** the marking screen: columns per row */
    static int cols(int sides) { return Math.min(sides, 5); }
    static int rows(int sides) { return (sides + cols(sides) - 1) / cols(sides); }

    /** the marking screen's tile `i` of `sides`: {x, y, w, h}. One row: 300 high from y 332 (as before); two rows: 148 high, the second 16 below the first (ends at 644, the buttons are at 664). */
    static float[] tile(int sides, int i) {
        int cols = cols(sides);
        if (rows(sides) == 1) { float w = Math.min(560, (WIDTH - (sides - 1) * GAP) / sides); return new float[] {LEFT + i * (w + GAP), 332, w, 300}; }
        float w = (WIDTH - (cols - 1) * GAP) / cols;
        return new float[] {LEFT + (i % cols) * (w + GAP), 332 + (i / cols) * (148 + 16), w, 148};
    }

    /** «من خلّص أول؟»: one button per side plus «لا أحد»: columns per row (a single row up to five buttons) */
    static int whoCols(int buttons) { return buttons <= 5 ? buttons : (buttons + 1) / 2; }
    /** button `i` of `buttons`: {x, y, w, h}, rows 104 high 24 apart from y 320 */
    static float[] who(int buttons, int i) {
        int cols = whoCols(buttons); float w = (WIDTH - (cols - 1) * GAP) / cols;
        return new float[] {LEFT + (i % cols) * (w + GAP), 320 + (i / cols) * (104 + GAP), w, 104};
    }

    /** the focus on the marking screen after an arrow (sides 0..n-1 are the tiles, n the next-box button, n + 1 the examples button); the same rule as the keys */
    static int moveMark(int sides, int sel, int dx, int dy) {
        int cols = cols(sides);
        if (sel >= sides) {
            if (dy < 0) return (rows(sides) - 1) * cols;   // up from the buttons: the first tile of the last row
            if (dx != 0) return Math.max(sides, Math.min(sides + 1, sel + dx));
            return sel;
        }
        if (dy > 0) return sel + cols < sides ? sel + cols : sides;
        if (dy < 0) return sel - cols >= 0 ? sel - cols : sel;
        if (dx != 0) return Math.max(0, Math.min(sides - 1, sel + dx));
        return sel;
    }

    /** the focus on «من خلّص أول؟» after an arrow */
    static int moveWho(int buttons, int sel, int dx, int dy) {
        int cols = whoCols(buttons);
        int to = sel + dx + dy * cols;
        return Math.max(0, Math.min(buttons - 1, to));
    }
}
