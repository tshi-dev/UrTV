package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sa.urtv.screensaver.games.hive.HiveBank;
import sa.urtv.screensaver.games.word5.Word5Words;

/**
 * «خلية الحروف» and «خمّن الكلمة»: their published files (docs/GAMES-DATA.md) turned into what the two engines play with (no Android; tests/HiveTest.java and
 * tests/Word5Test.java read the bundled starters through it). The same code reads a downloaded file and a bundled starter. Anything that is not the documented
 * form gives null, and the caller keeps what it had.
 */
public final class GamesBoardData {
    private GamesBoardData() {}

    private static final String[] NEVER = {"إسرائيل", "اسرائيل", "الصحراء الغربية", "israel", "western sahara"};   // never offered as a country
    private static final String[] HINT_BOXES = {"animal", "country", "city"};

    private static Map<?, ?> object(byte[] raw, int max) {
        if (raw == null || raw.length == 0 || raw.length > max) return null;
        Object o;
        try { o = MiniJson.parse(new String(raw, StandardCharsets.UTF_8)); } catch (RuntimeException e) { return null; }
        if (!(o instanceof Map)) return null;
        Object v = ((Map<?, ?>) o).get("v");
        return v instanceof Double && (Double) v == 1 ? (Map<?, ?>) o : null;
    }

    /**
     * The hive's questions: the quiz bank (quiz.json or its starter, checked by QuizBankCheck), letter-hive.json (which quiz ids start with which letter: «a» by
     * the Arabic answer, «e» by the English one) and, when there, khanat.json for the category question's sample answers (animals, countries, cities).
     */
    public static HiveBank hive(byte[] quiz, byte[] index, byte[] khanat) {
        List<QuizPick.Item> items = QuizBankCheck.parse(quiz);
        Map<?, ?> ix = object(index, 250_000);
        if (items == null || ix == null || !HiveBank.AR.equals(ix.get("L")) || !(ix.get("a") instanceof Map) || !(ix.get("e") instanceof Map)) return null;
        Map<String, Character> ar = letters((Map<?, ?>) ix.get("a"), HiveBank.AR), en = letters((Map<?, ?>) ix.get("e"), HiveBank.EN);
        if (ar == null || en == null) return null;
        HiveBank bank = new HiveBank();
        for (QuizPick.Item it : items) {
            Character a = ar.get(it.id), e = en.get(it.id);
            if (a == null && e == null) continue;
            if (!bank.add(it.id, a == null ? 0 : a, e == null || !QuizPick.playable(it, true) ? 0 : e, it.diff, it.kids, it.q, it.a, it.w)) return null;
        }
        if (bank.size() == 0) return null;
        Map<?, ?> kh = object(khanat, 200_000);
        if (kh != null && kh.get("s") instanceof Map) {
            Map<?, ?> samples = (Map<?, ?>) kh.get("s");
            for (int n = 0; n < 6; n++) for (String box : HINT_BOXES) for (Map.Entry<?, ?> letter : samples.entrySet()) {   // in turn from each box, so a hint line mixes them
                if (!(letter.getValue() instanceof Map) || !(((Map<?, ?>) letter.getValue()).get(box) instanceof List)) continue;
                List<?> list = (List<?>) ((Map<?, ?>) letter.getValue()).get(box);
                if (n >= list.size() || !(list.get(n) instanceof List) || ((List<?>) list.get(n)).size() != 2) continue;
                Object a = ((List<?>) list.get(n)).get(0), e = ((List<?>) list.get(n)).get(1);
                String key = String.valueOf(letter.getKey());
                if (a instanceof String && key.length() == 1 && allowed((String) a)) bank.hint(key.charAt(0), (String) a);
                if (e instanceof String && !((String) e).isEmpty() && allowed((String) e)) {
                    String s = (String) e;
                    bank.hint(Character.toUpperCase(s.charAt(0)), Character.toUpperCase(s.charAt(0)) + s.substring(1));
                }
            }
        }
        return bank;
    }

    private static boolean allowed(String s) {
        String low = s.toLowerCase(java.util.Locale.ROOT);
        for (String n : NEVER) if (low.contains(n)) return false;
        return true;
    }

    /** id -> its letter; null when the index is not letters of this alphabet over lists of ids */
    private static Map<String, Character> letters(Map<?, ?> byLetter, String alphabet) {
        Map<String, Character> out = new HashMap<>();
        for (Map.Entry<?, ?> e : byLetter.entrySet()) {
            String l = String.valueOf(e.getKey());
            if (l.length() != 1 || alphabet.indexOf(l.charAt(0)) < 0 || !(e.getValue() instanceof List)) return null;
            for (Object id : (List<?>) e.getValue()) {
                if (!(id instanceof String) || out.size() > 12_000) return null;
                out.put((String) id, l.charAt(0));
            }
        }
        return out;
    }

    /** word5-answers.json and word5-guesses.json */
    public static Word5Words word5(byte[] answers, byte[] guesses) {
        Map<?, ?> a = object(answers, 120_000), g = object(guesses, 700_000);
        if (a == null || g == null || !(a.get("w") instanceof List) || !(g.get("A") instanceof String) || !(g.get("w") instanceof String)) return null;
        List<Word5Words.Answer> list = new ArrayList<>();
        for (Object o : (List<?>) a.get("w")) {
            if (!(o instanceof List) || ((List<?>) o).size() != 5) return null;
            List<?> f = (List<?>) o;
            if (!(f.get(0) instanceof String) || !(f.get(1) instanceof String) || !(f.get(2) instanceof String) || !(f.get(3) instanceof Double) || !(f.get(4) instanceof Double)) return null;
            list.add(new Word5Words.Answer((String) f.get(0), (String) f.get(1), (String) f.get(2), ((Double) f.get(3)).intValue(), (Double) f.get(4) == 1));
        }
        return Word5Words.of((String) g.get("A"), (String) g.get("w"), list);
    }
}
