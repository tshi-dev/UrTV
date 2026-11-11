package sa.urtv.screensaver.games.domino;

import java.util.List;

/**
 * Where each tile of the chain lies on the table: a snake that grows both ways from the round's first tile, turns at the table's edges and always
 * fits. Plain Java (no Android): the screen only draws what this gives. In tile units (one unit = a half tile's side) the first tile is at the
 * middle; the right arm runs right, turns down at the edge and comes back one row lower; the left arm is its mirror, up. Rows are two units apart.
 * A double lies across the line, but in line where the snake turns (the corner tile and the one after it). A tile, once laid, stays where it is
 * until the unit or the number of rows changes: only then does the whole table move.
 */
public final class Snake {
    /** One tile on the table: the centres of its two halves in pixels, and the number on each. */
    public static final class Slot {
        public final Tile tile; public final float x1, y1, x2, y2; public final int v1, v2;
        Slot(Tile tile, float x1, float y1, int v1, float x2, float y2, int v2) { this.tile = tile; this.x1 = x1; this.y1 = y1; this.v1 = v1; this.x2 = x2; this.y2 = y2; this.v2 = v2; }
        public boolean upright() { return x1 == x2; }
        public float cx() { return (x1 + x2) / 2; }
        public float cy() { return (y1 + y2) / 2; }
    }

    /** The side of a half tile in pixels. */
    public final float unit;
    /** The tiles in the chain's order, left end first. */
    public final Slot[] slots;
    /** Rows above and below the first tile's row. */
    public final int up, down;

    private Snake(float unit, Slot[] slots, int up, int down) { this.unit = unit; this.slots = slots; this.up = up; this.down = down; }

    /** The largest unit from max down in steps of 8 (then 2, never under 8) at which the chain fits a table of w by h pixels. */
    public static Snake lay(List<DominoGame.Placed> chain, int anchor, float w, float h, int max) {
        for (int u = max; ; u -= u > 40 ? 8 : 2) {
            Snake s = lay(chain, anchor, w, h, (float) u);
            if (s != null || u <= 8) return s != null ? s : new Snake(u, new Slot[0], 0, 0);
        }
    }

    /** The chain at a given unit; null when it does not fit. */
    static Snake lay(List<DominoGame.Placed> chain, int anchor, float w, float h, float u) {
        int n = chain.size();
        float half = (float) Math.floor(w / u) / 2f;         // half the table's width in units
        if (half < 3) return null;
        float[][] c = new float[n][4];                        // x1, y1, x2, y2 in units, the first tile at 0,0
        int[][] v = new int[n][2];
        int up = 0, down = 0;
        if (n > 0) {
            DominoGame.Placed a = chain.get(anchor);
            boolean dbl = a.tile.isDouble();
            if (dbl) c[anchor] = new float[]{0, -0.5f, 0, 0.5f}; else c[anchor] = new float[]{-0.5f, 0, 0.5f, 0};
            v[anchor][0] = a.first; v[anchor][1] = a.second;
            float start = dbl ? 0.5f : 1f;
            down = arm(chain, anchor, +1, start, half, c, v);
            up = arm(chain, anchor, -1, start, half, c, v);
        }
        float top = -2 * up - 1, bottom = 2 * down + 1;       // every row keeps room for a double across
        if ((bottom - top) * u > h) return null;
        float ox = w / 2f, oy = h / 2f - (top + bottom) / 2f * u;
        Slot[] slots = new Slot[n];
        for (int i = 0; i < n; i++) slots[i] = new Slot(chain.get(i).tile, ox + c[i][0] * u, oy + c[i][1] * u, v[i][0], ox + c[i][2] * u, oy + c[i][3] * u, v[i][1]);
        return new Snake(u, slots, up, down);
    }

    /** One arm from the first tile: side +1 is the chain's right (drawn right, then down), -1 its left (the mirror: left, then up). Returns its rows beyond the first. */
    private static int arm(List<DominoGame.Placed> chain, int anchor, int side, float start, float half, float[][] c, int[][] v) {
        float px = start, py = 0;      // the open face's middle, in the arm's own frame (always: right, then down)
        int h = 1, rows = 0;           // h: the row runs right (+1) or back (-1)
        boolean turned = false;        // the last tile was a corner: the next lies under it, in line
        for (int i = anchor + side; i >= 0 && i < chain.size(); i += side) {
            DominoGame.Placed t = chain.get(i);
            int near = side > 0 ? t.first : t.second, far = side > 0 ? t.second : t.first;
            float x1, y1, x2, y2;
            if (turned) {                                        // under the corner tile, running the other way
                x1 = px; y1 = py + 0.5f; x2 = px + h; y2 = y1;
                px += 1.5f * h; py += 0.5f; turned = false;
            } else {
                boolean dbl = t.tile.isDouble();
                float room = h > 0 ? half - px : px + half;
                if (room - (dbl ? 1 : 2) >= 1) {                 // in the row, and a corner still fits after it
                    if (dbl) { x1 = px + 0.5f * h; y1 = py - 0.5f; x2 = x1; y2 = py + 0.5f; px += h; }
                    else { x1 = px + 0.5f * h; y1 = py; x2 = px + 1.5f * h; y2 = py; px += 2 * h; }
                } else {                                         // the corner: upright, down to the next row
                    x1 = px + 0.5f * h; y1 = py; x2 = x1; y2 = py + 1;
                    px = x1; py += 1.5f; h = -h; turned = true; rows++;
                }
            }
            c[i] = new float[]{side * x1, side * y1, side * x2, side * y2};
            v[i][0] = near; v[i][1] = far;
        }
        return rows;
    }
}
