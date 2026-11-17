package sa.urtv.screensaver.games.quran;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sa.urtv.screensaver.games.emoji.EmojiGame;

/**
 * The items of the two Quran games (no Android, tests/NewGamesTest.java), made for the multiple-choice engine EmojiGame.
 * «كمّل الآية»: a verse of assets/shared/verses.json (the mushaf text the project verifies against the KFGQPC / QPC Hafs text in CI) is cut at a word; the phone picks the
 * rest among the rests of other verses of about the same length. The text is only ever cut at a space and shown as it is stored, never edited, shortened or re-spelled.
 * «من هو القارئ؟»: a recitation clip of a reciter of the catalog the app already streams from (docs/GAMES-QURAN.md); the options are other reciters' names.
 */
public final class QuranItems {
    private QuranItems() {}
    public static final int WORDS_MIN = 6, WORDS_MAX = 24, VERSES_MIN = 100;
    /** the surahs whose first seconds are used as the clips: short ones that start with the recitation itself */
    public static final int[] CLIP_SURAHS = {1, 112, 113, 114, 108, 103, 110, 105, 106, 107, 109, 111, 102, 101, 100, 99, 97, 94};
    public static final int CLIP_SECONDS = 12;

    /** the verses of verses.json as puzzles: the start is the prompt, the rest the answer */
    public static List<EmojiGame.Puzzle> verses(Object tree) {
        List<EmojiGame.Puzzle> out = new ArrayList<>();
        if (!(tree instanceof List)) return null;
        for (Object o : (List<?>) tree) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) o; Object ref = m.get("ref"), u = m.get("uthmani"), sr = m.get("surah");
            if (!(ref instanceof String) || !(u instanceof String) || !(sr instanceof String) || ((String) ref).matches(".*[-,].*")) continue;   // a single verse only, not a range
            String text = ((String) u).trim(); String[] w = text.split(" ");
            if (w.length < WORDS_MIN || w.length > WORDS_MAX || text.indexOf('·') >= 0) continue;
            int cut = Math.max(3, Math.min(8, Math.round(w.length * 0.45f)));
            if (w.length - cut < 2) continue;
            StringBuilder a = new StringBuilder(), b = new StringBuilder();
            for (int i = 0; i < w.length; i++) { StringBuilder t = i < cut ? a : b; if (t.length() > 0) t.append(' '); t.append(w[i]); }
            String name = ((String) sr).replaceFirst("^سُورَةُ\\s*", "");
            out.add(EmojiGame.make((String) ref, a.toString(), b.toString(), "w" + Math.min(10, (w.length - cut) / 2)));
            refs.put((String) ref, name + " " + ((String) ref).substring(((String) ref).indexOf(':') + 1));
        }
        return out.size() >= VERSES_MIN ? out : null;
    }
    private static final java.util.Map<String, String> refs = new java.util.concurrent.ConcurrentHashMap<>();
    /** «name ayah» of a verse id, for the reveal («الرعد ٢٨» in the person's digits is the caller's job) */
    public static String where(String id) { return refs.get(id); }

    /** the clips: every reciter on a few of the short surahs; id «reciter|surah» (the surah's number is the file's name, 001.mp3) */
    public static List<EmojiGame.Puzzle> clips(Object tree) {
        List<EmojiGame.Puzzle> out = new ArrayList<>();
        if (!(tree instanceof Map) || !(((Map<?, ?>) tree).get("reciters") instanceof List)) return null;
        for (Object o : (List<?>) ((Map<?, ?>) tree).get("reciters")) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> r = (Map<?, ?>) o; Object id = r.get("id"), name = r.get("name"), server = r.get("server");
            if (!(id instanceof String) || !(name instanceof String) || !(server instanceof String) || !((String) server).startsWith("https://")) continue;
            for (int s : CLIP_SURAHS) out.add(EmojiGame.make(id + "|" + s, (String) server + String.format(java.util.Locale.ROOT, "%03d.mp3", s), (String) name, "reciter"));
        }
        return out.size() >= 6 * 6 ? out : null;
    }
}
