package sa.urtv.screensaver.games.hive;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * «خلية الحروف»: the board (no Android, tested on a plain JVM: tests/HiveTest.java). 5 rows of 5 hexagons (fair: both teams have five to cross), the odd rows shifted half a cell towards the end of
 * the row. Columns are LOGICAL: column 0 is where a row starts (the right side in Arabic, the left in English), so the rules never know the direction; only the
 * screen mirrors. Team 1 joins the two sides (column 0 with column 4), team 2 the top with the bottom (row 0 with row 4). A full board always has exactly one
 * winner (the hex rule), so there is no draw.
 */
public final class HiveBoard {
    public static final int ROWS = 5, COLS = 5, CELLS = ROWS * COLS;
    public static final int FREE = 0, TEAM_SIDES = 1, TEAM_TOP = 2;
    public static final int UP = 0, DOWN = 1, NEXT = 2, PREV = 3;

    public final char[] letter = new char[CELLS];
    public final int[] owner = new int[CELLS];

    public static int row(int cell) { return cell / COLS; }
    public static int col(int cell) { return cell % COLS; }
    public static int cell(int row, int col) { return row * COLS + col; }

    /** the cells that touch this one: two in its row, two (or fewer at an edge) in the row above and two below */
    public static int[] neighbours(int cell) {
        int r = row(cell), c = col(cell), n = 0;
        int[] out = new int[6];
        int shift = (r & 1) == 0 ? -1 : 0;   // an even row touches columns c-1 and c of the next rows, an odd one c and c+1
        int[][] at = {{r, c - 1}, {r, c + 1}, {r - 1, c + shift}, {r - 1, c + shift + 1}, {r + 1, c + shift}, {r + 1, c + shift + 1}};
        for (int[] p : at) if (p[0] >= 0 && p[0] < ROWS && p[1] >= 0 && p[1] < COLS) out[n++] = cell(p[0], p[1]);
        return Arrays.copyOf(out, n);
    }

    /** where the focus goes: along the row it wraps around, up and down it stops at the edge (the same column, which always touches) */
    public static int move(int cell, int dir) {
        int r = row(cell), c = col(cell);
        if (dir == NEXT) c = (c + 1) % COLS;
        else if (dir == PREV) c = (c + COLS - 1) % COLS;
        else if (dir == UP) r = Math.max(0, r - 1);
        else if (dir == DOWN) r = Math.min(ROWS - 1, r + 1);
        return cell(r, c);
    }

    private static boolean starts(int team, int cell) { return team == TEAM_SIDES ? col(cell) == 0 : row(cell) == 0; }
    private static boolean ends(int team, int cell) { return team == TEAM_SIDES ? col(cell) == COLS - 1 : row(cell) == ROWS - 1; }

    /** the shortest chain of this team's cells from its first side to its second, in order; null when the sides are not joined */
    public int[] chain(int team) {
        int[] from = new int[CELLS];
        Arrays.fill(from, -2);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < CELLS; i++) if (owner[i] == team && starts(team, i)) { from[i] = -1; queue.add(i); }
        while (!queue.isEmpty()) {
            int at = queue.poll();
            if (ends(team, at)) {
                List<Integer> path = new ArrayList<>();
                for (int p = at; p != -1; p = from[p]) path.add(0, p);
                int[] out = new int[path.size()];
                for (int i = 0; i < out.length; i++) out[i] = path.get(i);
                return out;
            }
            for (int nb : neighbours(at)) if (owner[nb] == team && from[nb] == -2) { from[nb] = at; queue.add(nb); }
        }
        return null;
    }

    /** the team whose sides are joined, or FREE */
    public int winner() { return chain(TEAM_SIDES) != null ? TEAM_SIDES : chain(TEAM_TOP) != null ? TEAM_TOP : FREE; }

    public int freeCells() { int n = 0; for (int o : owner) if (o == FREE) n++; return n; }
}
