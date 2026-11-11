package sa.urtv.screensaver.games.word5;

/**
 * «خمّن الكلمة»: one word, its rows and the keyboard's colours (no Android; tests/Word5Test.java). A guess is taken only when it is five letters and a word we
 * know; one we do not know costs no try. Colouring with repeated letters: the right places first (green), then, left to right, a letter is yellow only while
 * the answer still has one of it that is not yet accounted for; the rest are gray. A key keeps the best colour it has had.
 */
public final class Word5Game {
    public static final int GRAY = 1, YELLOW = 2, GREEN = 3;
    public static final int[] TRIES = {6, 5, 7};
    public enum Entered { SHORT, UNKNOWN, NEXT, WON, LOST, OVER }

    private final Word5Words words; private final String answer;
    public final int tries;
    private final String[] guesses; private final int[][] colours;
    private final int[] keys = new int[Word5Text.LETTERS];
    private final StringBuilder typing = new StringBuilder();
    private int row; private boolean won;

    public Word5Game(Word5Words words, String answer, int tries) {
        this.words = words; this.answer = answer; this.tries = tries;
        guesses = new String[tries]; colours = new int[tries][];
    }

    /** green, yellow or gray for each of the five letters */
    public static int[] score(String guess, String answer) {
        int[] out = new int[Word5Text.LENGTH], left = new int[Word5Text.LETTERS];
        for (int i = 0; i < out.length; i++) {
            if (guess.charAt(i) == answer.charAt(i)) out[i] = GREEN;
            else left[Word5Text.ALPHABET.indexOf(answer.charAt(i))]++;
        }
        for (int i = 0; i < out.length; i++) {
            if (out[i] == GREEN) continue;
            int at = Word5Text.ALPHABET.indexOf(guess.charAt(i));
            if (left[at] > 0) { out[i] = YELLOW; left[at]--; } else out[i] = GRAY;
        }
        return out;
    }

    /** OK on a letter key (0..27 in keyboard order): false when the row is full or the game is over */
    public boolean type(int letter) {
        if (over() || letter < 0 || letter >= Word5Text.LETTERS || typing.length() >= Word5Text.LENGTH) return false;
        typing.append(Word5Text.ALPHABET.charAt(letter));
        return true;
    }

    /** «حذف» (and Back): false when there is nothing to delete */
    public boolean delete() {
        if (over() || typing.length() == 0) return false;
        typing.setLength(typing.length() - 1);
        return true;
    }

    /** «إدخال» */
    public Entered enter() {
        if (over()) return Entered.OVER;
        if (typing.length() < Word5Text.LENGTH) return Entered.SHORT;
        String guess = typing.toString();
        if (!words.accepts(guess)) return Entered.UNKNOWN;   // the row shakes, the letters stay, no try is lost
        int[] c = score(guess, answer);
        guesses[row] = guess; colours[row] = c; row++;
        for (int i = 0; i < c.length; i++) { int k = Word5Text.ALPHABET.indexOf(guess.charAt(i)); keys[k] = Math.max(keys[k], c[i]); }
        typing.setLength(0);
        if (guess.equals(answer)) { won = true; return Entered.WON; }
        return row == tries ? Entered.LOST : Entered.NEXT;
    }

    public boolean over() { return won || row == tries; }
    public boolean won() { return won; }
    /** the rows played so far; it is also the row being typed while the game goes on */
    public int row() { return row; }
    public String typing() { return typing.toString(); }
    public String guess(int r) { return guesses[r]; }
    public int[] colours(int r) { return colours[r]; }
    /** a key's colour: 0 not tried yet, else GRAY, YELLOW or GREEN */
    public int key(int letter) { return keys[letter]; }
    public String answer() { return answer; }

    /** «تحدّي»: both played the same word. 1 or 2 for the winner (fewer tries; solving beats not solving), 0 for a tie, which goes to the next word */
    public static int duel(boolean won1, int rows1, boolean won2, int rows2) {
        if (won1 != won2) return won1 ? 1 : 2;
        if (!won1 || rows1 == rows2) return 0;
        return rows1 < rows2 ? 1 : 2;
    }
}
