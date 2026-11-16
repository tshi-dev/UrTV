package sa.urtv.home.tilawah;

import java.util.Arrays;

/**
 * 4.3.3: a reciter who did not record all 114 surahs. reciters.json may give a reciter «missing», the numbers of the surahs it has no
 * recording of (none: all 114, as every reciter before). The surahs keep their own numbers (the server's 001.mp3 to 114.mp3, the person's
 * «سورة الكهف» is 18 for every reciter); the player steps over a missing one (next, previous, the end of a surah, «متابعة»), the surah grid
 * shows it as «غير متوفرة», the phone's pick of one plays the next one there is. Pure logic (no Android classes): tests/SurahSetRun.java.
 */
final class SurahSet {
    private SurahSet() { }

    static final int LAST = 114;
    private static final int[] NONE = new int[0];

    /** The «missing» list as the app keeps it: in range, no repeats, sorted. Anything else in it is dropped; all 114 missing is no list. */
    static int[] clean(int[] raw) {
        if (raw == null || raw.length == 0) return NONE;
        boolean[] seen = new boolean[LAST + 1];
        int count = 0;
        for (int n : raw) if (n >= 1 && n <= LAST && !seen[n]) { seen[n] = true; count++; }
        if (count == 0 || count == LAST) return NONE;   // a list that leaves nothing is a broken one: the reciter plays as before
        int[] out = new int[count];
        int i = 0;
        for (int n = 1; n <= LAST; n++) if (seen[n]) out[i++] = n;
        return out;
    }

    /** the reciter has a recording of surah n (missing as clean() gives it) */
    static boolean has(int n, int[] missing) {
        return n >= 1 && n <= LAST && Arrays.binarySearch(missing, n) < 0;
    }

    /** how many surahs the reciter has */
    static int count(int[] missing) { return LAST - missing.length; }

    /** n itself when the reciter has it, else the next one it has (after 114, from 1) */
    static int atOrAfter(int n, int[] missing) {
        if (n < 1) n = 1;
        if (n > LAST) n = LAST;
        for (int i = 0; i < LAST; i++) {
            int k = (n - 1 + i) % LAST + 1;
            if (has(k, missing)) return k;
        }
        return n;   // (clean() never leaves all missing)
    }

    /** the surah after n that the reciter has, from 1 again after 114 */
    static int next(int n, int[] missing) { return atOrAfter(n >= LAST ? 1 : n + 1, missing); }

    /** the surah before n that the reciter has, from 114 again before 1 */
    static int prev(int n, int[] missing) {
        int k = n;
        for (int i = 0; i < LAST; i++) {
            k = k <= 1 ? LAST : k - 1;
            if (has(k, missing)) return k;
        }
        return n;
    }
}
