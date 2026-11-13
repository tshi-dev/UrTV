package sa.urtv.home;

/**
 * 4.3.1 «شكل لوحة التحكم»: the two shapes of the control center and the arithmetic they share, with no Android in it (tests/ControlShapeRun.java runs it).
 * «عريض» (the default; the 4.2 look): a page in the middle of the screen, the rows two to a line. «طولي» (4.3's): the tall panel at the screen's edge, one row to a line.
 */
final class ControlShape {
    private ControlShape() { }

    static final String WIDE = "wide", TALL = "tall";
    static final float WIDE_W = 0.78f, WIDE_H = 0.82f, TALL_W = 560f / 1920f, SAFE = 0.05f;

    /** the saved choice: anything but «طولي» is the wide page (a TV that never chose gets it) */
    static boolean isWide(String saved) { return !TALL.equals(saved); }

    /** the panel's {width, height} in pixels: the wide page takes 78 % of the width and 82 % of the height, the tall panel 560 px of 1920 inside the 5 % safe area; never more than the screen */
    static int[] size(int screenW, int screenH, boolean wide) {
        if (wide) return new int[] {Math.round(screenW * WIDE_W), Math.round(screenH * WIDE_H)};
        return new int[] {Math.round(screenW * TALL_W), screenH - 2 * Math.round(screenH * SAFE)};
    }

    /** rows to a line */
    static int perLine(boolean wide) { return wide ? 2 : 1; }

    static final int STAY = -1, ABOVE = -2;

    /**
     * where the D-pad goes from row {@code i} of {@code n}: {right, left, up, down} as row indexes, {@link #STAY} (stays), or {@link #ABOVE} (up from the first line: the toggles).
     * The first of a pair is at the start side (the right one in Arabic); an odd last line: DOWN from the second cell above reaches its one cell.
     */
    static int[] go(int i, int n, boolean wide, boolean rtl) {
        int per = perLine(wide);
        int next = wide && i % 2 == 0 && i + 1 < n ? i + 1 : STAY, prev = wide && i % 2 == 1 ? i - 1 : STAY;
        int up = i < per ? ABOVE : i - per;
        int down = i + per < n ? i + per : (wide && i / 2 < (n - 1) / 2 ? n - 1 : STAY);
        return new int[] {rtl ? prev : next, rtl ? next : prev, up, down};
    }
}
