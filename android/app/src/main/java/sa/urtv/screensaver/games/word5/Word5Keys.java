package sa.urtv.screensaver.games.word5;

/**
 * «خمّن الكلمة»: where the focus is on the keyboard (no Android). Four rows of seven letter keys in alphabet order (key = row * 7 + column, column 0 where the
 * row starts: the right side) and under them two wide keys, «إدخال» under columns 0 to 3 and «حذف» under columns 4 to 6. Every move wraps around, so no key is
 * more than a few presses away and the focus can never leave the keyboard; going down to a wide key and up again returns to the column one came from.
 */
public final class Word5Keys {
    public static final int ROWS = 4, COLS = 7, ENTER = 28, DELETE = 29, KEYS = 30;
    public static final int UP = 0, DOWN = 1, NEXT = 2, PREV = 3;

    private int key, col;

    public int key() { return key; }
    public void set(int k) { if (k >= 0 && k < KEYS) { key = k; if (k < ENTER) col = k % COLS; else if (wide(col) != k) col = k == ENTER ? 1 : 5; } }

    private static int wide(int col) { return col <= 3 ? ENTER : DELETE; }

    public int move(int dir) {
        boolean letters = key < ENTER;
        int row = letters ? key / COLS : ROWS;
        if (dir == NEXT || dir == PREV) {
            if (!letters) { set(key == ENTER ? DELETE : ENTER); return key; }
            col = (col + (dir == NEXT ? 1 : COLS - 1)) % COLS;
        } else row = (row + (dir == DOWN ? 1 : ROWS)) % (ROWS + 1);
        key = row == ROWS ? wide(col) : row * COLS + col;
        return key;
    }
}
