package sa.urtv.screensaver.games.word5;

/**
 * «خمّن الكلمة»: where the focus is on the keyboard (no Android). Four rows of seven letter keys in alphabet order (key = row * 7 + column, column 0 where the
 * row starts: the right side) and a fifth row of the three spellings the alphabet folds, «ة» «ى» «ء» (keys 28 to 30, columns 0 to 2), then «إدخال» (columns 3 and 4)
 * and «حذف» (columns 5 and 6). Every move wraps around, so no key is more than a few presses away and the focus can never leave the keyboard; going down to the
 * fifth row and up again returns to the column one came from.
 */
public final class Word5Keys {
    public static final int ROWS = 5, COLS = 7, LETTERS = 28, TAA = 28, YAA = 29, HAMZA = 30, ENTER = 31, DELETE = 32, KEYS = 33;
    public static final int UP = 0, DOWN = 1, NEXT = 2, PREV = 3;
    /** the five keys of the last row, in the order NEXT walks them */
    private static final int[] LAST = {TAA, YAA, HAMZA, ENTER, DELETE};

    private int key, col;

    public int key() { return key; }
    public void set(int k) {
        if (k < 0 || k >= KEYS) return;
        key = k;
        if (k < LETTERS) col = k % COLS;
        else if (k <= HAMZA) col = k - TAA;
        else if (k == ENTER) { if (col < 3 || col > 4) col = 3; }
        else if (col < 5) col = 5;
    }

    /** the key in the fifth row under column c */
    private static int last(int c) { return c <= 2 ? TAA + c : c <= 4 ? ENTER : DELETE; }

    public int move(int dir) {
        boolean letters = key < LETTERS;
        int row = letters ? key / COLS : ROWS - 1;
        if (dir == NEXT || dir == PREV) {
            if (letters) set(row * COLS + (col + (dir == NEXT ? 1 : COLS - 1)) % COLS);
            else {
                int at = 0;
                while (LAST[at] != key) at++;
                set(LAST[(at + (dir == NEXT ? 1 : LAST.length - 1)) % LAST.length]);
            }
            return key;
        }
        row = (row + (dir == DOWN ? 1 : ROWS - 1)) % ROWS;
        int c = col;
        set(row == ROWS - 1 ? last(c) : row * COLS + c);
        col = c;   // a key that spans columns keeps the column one came from
        return key;
    }
}
