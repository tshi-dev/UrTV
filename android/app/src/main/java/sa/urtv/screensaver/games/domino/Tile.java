package sa.urtv.screensaver.games.domino;

import java.util.ArrayList;
import java.util.List;

/** One domino of the double-six set: two halves, low <= high. Immutable; plain Java (no Android), see docs/DOMINO.md. */
public final class Tile {
    public final int low, high;

    public Tile(int a, int b) {
        if (a < 0 || b < 0 || a > 6 || b > 6) throw new IllegalArgumentException("half out of 0..6");
        low = Math.min(a, b); high = Math.max(a, b);
    }

    public boolean isDouble() { return low == high; }
    public int pips() { return low + high; }
    public boolean has(int n) { return low == n || high == n; }
    /** The half that is not n (n itself for a double). */
    public int other(int n) { return low == n ? high : low; }
    /** 0..27, the same for the same tile. */
    public int id() { return high * (high + 1) / 2 + low; }

    /** The 28 tiles, in id order. */
    public static List<Tile> set() {
        List<Tile> v = new ArrayList<>(28);
        for (int h = 0; h <= 6; h++) for (int l = 0; l <= h; l++) v.add(new Tile(l, h));
        return v;
    }

    @Override public boolean equals(Object o) { return o instanceof Tile && ((Tile) o).low == low && ((Tile) o).high == high; }
    @Override public int hashCode() { return id(); }
    @Override public String toString() { return low + "|" + high; }
}
