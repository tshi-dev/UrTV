package sa.urtv.screensaver.games.word5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * «خمّن الكلمة»: the two word lists (no Android). GamesBoardData fills it from word5-answers.json and word5-guesses.json (docs/GAMES-DATA.md).
 *
 * The accepted guesses arrive as one packed string, five characters a word (character k of a..z01 is the k-th keyboard letter), sorted. They are kept as one
 * sorted int[] of word codes (Word5Text.code) and found by binary search: 4 bytes a word, so the whole published list (about 31,000 words) is about 0.12 MB of
 * heap, with no String per word and no HashSet; the same on a low-RAM TV. An answer's spelling must normalize to its word; an answer that is not among the guesses is left out (it could never be typed).
 * Anything wrong: of() gives null.
 */
public final class Word5Words {
    public static final int ACCEPTED_MAX = 120_000, ANSWERS_MAX = 5000, RECENT_MAX = 200;

    public static final class Answer {
        public final String word, display, en; public final int level; public final boolean kids;
        public Answer(String word, String display, String en, int level, boolean kids) { this.word = word; this.display = display; this.en = en; this.level = level; this.kids = kids; }
    }

    private final int[] accepted;
    public final List<Answer> answers;

    private Word5Words(int[] accepted, List<Answer> answers) { this.accepted = accepted; this.answers = Collections.unmodifiableList(answers); }

    public int acceptedCount() { return accepted.length; }
    /** the heap the accepted list holds, in bytes */
    public int acceptedBytes() { return 16 + 4 * accepted.length; }

    /** is this (normalized) word one we know? */
    public boolean accepts(String word) { int c = Word5Text.code(word); return c >= 0 && Arrays.binarySearch(accepted, c) >= 0; }

    /**
     * may this (normalized) word be guessed? Any five letters of the keyboard: the published list is only a few thousand common words and refused ordinary ones («ممتاز», «مسافر»),
     * and there is no complete dictionary to ship. The list still decides which words are ANSWERS (curated) and is what {@link #accepts} reports.
     */
    public boolean allows(String word) { return Word5Text.code(word) >= 0; }

    public static Word5Words of(String alphabet, String packed, List<Answer> answers) {
        if (!Word5Text.ALPHABET.equals(alphabet) || packed == null || packed.isEmpty() || packed.length() % Word5Text.LENGTH != 0 || packed.length() / Word5Text.LENGTH > ACCEPTED_MAX) return null;
        int[] codes = new int[packed.length() / Word5Text.LENGTH];
        for (int n = 0, at = 0; n < codes.length; n++) {
            int code = 0;
            for (int i = 0; i < Word5Text.LENGTH; i++) {
                char c = packed.charAt(at++);
                int k = c >= 'a' && c <= 'z' ? c - 'a' : c == '0' ? 26 : c == '1' ? 27 : -1;
                if (k < 0) return null;
                code = code * Word5Text.LETTERS + k;
            }
            if (n > 0 && code <= codes[n - 1]) return null;   // sorted, no word twice
            codes[n] = code;
        }
        if (answers == null || answers.isEmpty() || answers.size() > ANSWERS_MAX) return null;
        Set<String> seen = new HashSet<>();
        List<Answer> kept = new ArrayList<>();
        for (Answer a : answers) {
            if (a.word == null || a.display == null || a.en == null || a.level < 1 || a.level > 3 || a.display.isEmpty() || a.display.length() > 12 || a.en.length() > 40) return null;
            int code = Word5Text.code(a.word);
            if (code < 0 || !Word5Text.normalize(a.display).equals(a.word) || !seen.add(a.word)) return null;
            if (Arrays.binarySearch(codes, code) >= 0) kept.add(a);   // the two files are fetched one by one: an answer the guesses do not hold yet is left out, never unguessable
        }
        if (kept.isEmpty()) return null;
        return new Word5Words(codes, kept);
    }

    // ---- which word a game gets

    private List<Answer> pool(boolean kids) {
        List<Answer> v = new ArrayList<>();
        for (Answer a : answers) if (!kids || a.kids) v.add(a);
        return v;
    }

    /** free play: any word of the filter that is not one of the last 200 played on this TV (when all are, any) */
    public Answer free(boolean kids, List<String> recent, Random rnd) {
        List<Answer> pool = pool(kids), fresh = new ArrayList<>();
        for (Answer a : pool) if (recent == null || !recent.contains(a.word)) fresh.add(a);
        if (!fresh.isEmpty()) pool = fresh;
        return pool.get(rnd.nextInt(pool.size()));
    }

    /** the list of the last words played, newest last, at most 200 */
    public static void remember(List<String> recent, String word) {
        recent.remove(word);
        recent.add(word);
        while (recent.size() > RECENT_MAX) recent.remove(0);
    }
}
