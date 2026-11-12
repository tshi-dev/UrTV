package sa.urtv.screensaver.games.word5;

/**
 * «خمّن الكلمة»: the one spelling rule for answers, accepted guesses and what is typed (games/word5/SPEC.md «Normalization»; no Android). Diacritics and the
 * tatweel go, أ إ آ ٱ become ا, ة becomes ه, ى and ئ become ي, ؤ becomes و, ء becomes ا; what is left has to be five of the keyboard's 28 letters. A word is then one number below 28^5
 * (its letters as base-28 digits), which is how the accepted list is stored and searched.
 */
public final class Word5Text {
    private Word5Text() {}

    public static final String ALPHABET = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي";
    public static final int LETTERS = 28, LENGTH = 5, CODES = 28 * 28 * 28 * 28 * 28;

    public static String normalize(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; s != null && i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 0x064B && c <= 0x065F) || c == 0x0670 || c == 0x0640) continue;   // harakat, shadda, sukun, the small alef, tatweel
            if (c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ') c = 'ا';
            else if (c == 'ة') c = 'ه';
            else if (c == 'ى' || c == 'ئ') c = 'ي';
            else if (c == 'ؤ') c = 'و';
            else if (c == 'ء') c = 'ا';   // the keyboard's «ء» key
            b.append(c);
        }
        return b.toString();
    }

    /** the word as a number, or -1 when it is not five keyboard letters (give it a normalized word) */
    public static int code(String word) {
        if (word == null || word.length() != LENGTH) return -1;
        int code = 0;
        for (int i = 0; i < LENGTH; i++) {
            int at = ALPHABET.indexOf(word.charAt(i));
            if (at < 0) return -1;
            code = code * LETTERS + at;
        }
        return code;
    }

    public static String word(int code) {
        char[] c = new char[LENGTH];
        for (int i = LENGTH - 1; i >= 0; i--) { c[i] = ALPHABET.charAt(code % LETTERS); code /= LETTERS; }
        return new String(c);
    }
}
