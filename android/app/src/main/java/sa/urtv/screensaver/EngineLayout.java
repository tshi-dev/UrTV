package sa.urtv.screensaver;

/**
 * Where the engine puts the answer stamps and the item tiles below its question block (no Android, tests/GameEngineTest.java): on the kit's 1920 x 1080 design inside the 96 px safe area,
 * from y 440 to 960. Up to three things stand in one column, four to six in two columns of up to three rows, always big enough to read at 3 metres and at 1280 x 720 (the tests scale the
 * design by 2/3 and ask for stamps of 56 px and words of 29 px at the least, R5), and never touching each other.
 */
final class EngineLayout {
    private EngineLayout() {}

    static final float TOP = 440, BOTTOM = 960, LEFT = 96, RIGHT = 1824, GAP = 20;

    /** the stamp `i` of `n` (2 to 6): {x from the start edge, y, w, h} in design pixels */
    static float[] rect(int i, int n) {
        int cols = n <= 3 ? 1 : 2, rows = (n + cols - 1) / cols;
        float h = Math.min(132, (BOTTOM - TOP - (rows - 1) * GAP) / rows), w = (RIGHT - LEFT - (cols - 1) * GAP) / cols;
        int col = cols == 1 ? 0 : i / rows, row = cols == 1 ? i : i % rows;   // a column is filled before the next one: the reading order in a right-to-left screen
        return new float[] {LEFT + col * (w + GAP), TOP + row * (h + GAP), w, h};
    }
}
