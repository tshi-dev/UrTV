package sa.urtv.screensaver.games.word5;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * «خمّن الكلمة»: the two word lists (no Android; made by .github/tools/make_word5.py from games/word5).
 *
 * The accepted guesses (about 25,700) are kept as one sorted int[] of word codes (Word5Text.code) and found by binary search: 4 bytes a word, about 0.1 MB of
 * heap for the whole list, no String and no HashSet, so it is the same on a low-RAM TV. The file is smaller still: «W5A1», the count (4 bytes), then each code
 * as the difference from the one before it, as a varint (about 2 bytes a word).
 *
 * The answers are lines of 5 tab-separated fields: the word (normalized), the spelling to show at the end, the English gloss (may be empty), level 1..3,
 * kids 0/1. Every answer must be an accepted guess. Anything wrong in either file: parse gives null.
 */
public final class Word5Words {
    public static final int BYTES_MAX = 600_000, ACCEPTED_MAX = 120_000, ANSWERS_MAX = 5000, RECENT_MAX = 200;

    public static final class Answer {
        public final String word, display, en; public final int level; public final boolean kids;
        Answer(String word, String display, String en, int level, boolean kids) { this.word = word; this.display = display; this.en = en; this.level = level; this.kids = kids; }
    }

    private final int[] accepted;
    public final List<Answer> answers;

    private Word5Words(int[] accepted, List<Answer> answers) { this.accepted = accepted; this.answers = Collections.unmodifiableList(answers); }

    public int acceptedCount() { return accepted.length; }
    /** the heap the accepted list holds, in bytes */
    public int acceptedBytes() { return 16 + 4 * accepted.length; }

    /** is this (normalized) word one we know? */
    public boolean accepts(String word) { int c = Word5Text.code(word); return c >= 0 && Arrays.binarySearch(accepted, c) >= 0; }

    public static Word5Words parse(byte[] acceptedFile, byte[] answersFile) {
        int[] codes = codes(acceptedFile);
        if (codes == null || answersFile == null || answersFile.length == 0 || answersFile.length > BYTES_MAX) return null;
        List<Answer> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String line : new String(answersFile, StandardCharsets.UTF_8).split("\n")) {
            if (line.isEmpty()) continue;
            String[] f = line.split("\t", -1);
            if (f.length != 5 || !f[3].matches("[123]") || !f[4].matches("[01]") || f[1].isEmpty() || f[1].length() > 12 || f[2].length() > 40) return null;
            int code = Word5Text.code(f[0]);
            if (code < 0 || Arrays.binarySearch(codes, code) < 0 || !Word5Text.normalize(f[1]).equals(f[0]) || !seen.add(f[0]) || seen.size() > ANSWERS_MAX) return null;
            for (int i = 0; i < f[2].length(); i++) if (f[2].charAt(i) < ' ' || f[2].charAt(i) > '~') return null;
            out.add(new Answer(f[0], f[1], f[2], f[3].charAt(0) - '0', f[4].equals("1")));
        }
        return out.isEmpty() ? null : new Word5Words(codes, out);
    }

    private static int[] codes(byte[] b) {
        if (b == null || b.length < 8 || b.length > BYTES_MAX || b[0] != 'W' || b[1] != '5' || b[2] != 'A' || b[3] != '1') return null;
        int count = ((b[4] & 255) << 24) | ((b[5] & 255) << 16) | ((b[6] & 255) << 8) | (b[7] & 255);
        if (count <= 0 || count > ACCEPTED_MAX) return null;
        int[] out = new int[count];
        int at = 8; long last = -1;
        for (int n = 0; n < count; n++) {
            long delta = 0; int shift = 0;
            while (true) {
                if (at >= b.length || shift > 28) return null;
                int x = b[at++] & 255;
                delta |= (long) (x & 127) << shift; shift += 7;
                if ((x & 128) == 0) break;
            }
            long code = last + delta;
            if (delta <= 0 || code >= Word5Text.CODES) return null;   // strictly rising, every one a real word
            out[n] = (int) (last = code);
        }
        return at == b.length ? out : null;
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

    /** the word of the day: the same on every TV with the same list, and no word again until the whole list has had its day (a fixed shuffle, then day by day) */
    public Answer daily(boolean kids, long epochDay) {
        List<Answer> pool = pool(kids);
        Collections.shuffle(pool, new Random(20261111L));
        return pool.get((int) Math.floorMod(epochDay, (long) pool.size()));
    }

    /** the list of the last words played, newest last, at most 200 */
    public static void remember(List<String> recent, String word) {
        recent.remove(word);
        recent.add(word);
        while (recent.size() > RECENT_MAX) recent.remove(0);
    }
}
