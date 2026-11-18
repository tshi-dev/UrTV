package sa.urtv.screensaver;

import java.util.*;

/** For tests/howto_test.py: «طريقة اللعب» (GameHowTo, the registry's HOWTO table, the «!» focus in GameChips) on a plain JVM. Prints PASS/FAIL lines. */
public final class HowToTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }

    public static void main(String[] a) {
        // the registry's table
        Set<String> ids = new HashSet<>(); for (GameRegistry.Game g : GameRegistry.ALL) ids.add(g.id);
        boolean rows = GameRegistry.HOWTO.length == 19, ok = true, phonesOk = true, limits = true, packs = GameRegistry.howto("packs") == null && GameRegistry.howto("nothing") == null;
        Set<String> seen = new HashSet<>();
        for (String[] r : GameRegistry.HOWTO) {
            if (!ids.contains(r[0]) || r[0].equals("packs") || !seen.add(r[0])) ok = false;
            GameHowTo.Content c = GameHowTo.of(r);
            if (c == null) { ok = false; continue; }
            if (c.steps.size() < 3 || c.steps.size() > 5 || c.scoring.isEmpty() || c.end.isEmpty()) ok = false;
            if (c.hasPhones() != Arrays.asList("quiz", "khanat", "missing-word", "odd-one", "charades", "letter-hive", "word5", "domino").contains(r[0])) phonesOk = false;
            for (String t : r) if (t.length() > GameHowTo.TEXT_MAX || t.matches(".*[·•].*")) limits = false;
        }
        check("howto: 19 rows, one for each of the games of the list but «حزم المحتوى», each with a goal, 3 to 5 steps, the points and the end; the packs and an unknown game have none", rows && ok && packs);
        check("howto: «بالجوالات» is there for «الأسرع» and the seven older games (which can also be played with one remote) and not for the ten party games (played with phones only: their steps say it); no text is over 400 characters, none has «·» or «•»", phonesOk && limits);
        int listed = 0; for (GameRegistry.Game g : GameRegistry.ALL) if (GameRegistry.howto(g.id) != null) listed++;
        check("howto: every ready game of the registry has its row (19 of the 20 entries)", listed == 19 && GameRegistry.ALL.length == 20);
        // GameHowTo.of
        check("howto: a row without a goal or without a step is no content", GameHowTo.of(new String[]{"x", "", "p", "s", "e", "one"}) == null && GameHowTo.of(new String[]{"x", "goal", "p", "s", "e"}) == null && GameHowTo.of(null) == null && GameHowTo.of(new String[]{"x", "g", "", "", "", "one"}) != null);
        GameHowTo.Content c = GameHowTo.of(new String[]{"x", "g", "", "s", "e", "a", "b"});
        check("howto: an empty phones text means no phones block", c != null && !c.hasPhones() && c.steps.size() == 2 && c.scoring.equals("s") && c.end.equals("e"));
        // the data file's block
        Object j = MiniJson.parse("{\"goal\":\"Win\",\"steps\":[\"One\",{\"ar\":\"اثنان\",\"en\":\"Two\"},\"\"],\"phones\":{\"ar\":\"جوال\"},\"scoring\":\"1 each\",\"end\":\"After 5\"}");
        GameHowTo.Content en = GameHowTo.fromJson(j, true), ar = GameHowTo.fromJson(j, false);
        check("howto: a howto block of a data file is read in the language asked, the other language when one is missing; an empty step is dropped",
            en != null && en.goal.equals("Win") && en.steps.equals(Arrays.asList("One", "Two")) && en.phones.equals("جوال") && ar != null && ar.steps.get(1).equals("اثنان") && ar.scoring.equals("1 each") && ar.hasPhones());
        StringBuilder many = new StringBuilder("{\"goal\":\"g\",\"steps\":["); for (int i = 0; i < 12; i++) many.append(i > 0 ? "," : "").append("\"s").append(i).append("\""); many.append("]}");
        String longText = "x".repeat(401);
        check("howto: more than 8 steps are cut to 8; a text of 401 characters, a control character, a missing goal, no steps, steps of another kind, or a block that is not an object are no content",
            GameHowTo.fromJson(MiniJson.parse(many.toString()), true).steps.size() == 8 && GameHowTo.fromJson(MiniJson.parse("{\"goal\":\"" + longText + "\",\"steps\":[\"a\"]}"), true) == null
            && GameHowTo.fromJson(MiniJson.parse("{\"goal\":\"g\\u0001h\",\"steps\":[\"a\"]}"), true) == null && GameHowTo.fromJson(MiniJson.parse("{\"steps\":[\"a\"]}"), true) == null && GameHowTo.fromJson(MiniJson.parse("{\"goal\":\"g\",\"steps\":[]}"), true) == null
            && GameHowTo.fromJson(MiniJson.parse("{\"goal\":\"g\",\"steps\":\"a\"}"), true) == null && GameHowTo.fromJson(MiniJson.parse("{\"goal\":\"g\",\"steps\":[1,2]}"), true) == null && GameHowTo.fromJson(MiniJson.parse("[1]"), true) == null && GameHowTo.fromJson(null, true) == null);
        // the «!» in the chips
        GameChips plain = new GameChips().add("a", "A", new String[]{"1", "2"}, 0).add("b", "B", new String[]{"1", "2"}, 0);
        plain.row = 0; boolean upNothing = !plain.move(0, -1) && !plain.help;
        GameChips h = new GameChips().add("a", "A", new String[]{"1", "2"}, 0).add("b", "B", new String[]{"1", "2"}, 0); h.hasHelp = true;
        check("howto: a game with no panel keeps the chips as they were (up from the first row goes nowhere)", upNothing && h.row == 2);
        h.row = 0; h.col = 0;
        boolean up = h.move(0, -1) && h.help, side = !h.move(1, 0) && h.help && h.col == 0, upAgain = !h.move(0, -1) && h.help, down = h.move(0, 1) && !h.help && h.row == 0;
        check("howto: up from the first row reaches the «!», sideways and up do nothing there, down comes back to the first row; from the second row up goes to the first, not to the «!»", up && side && upAgain && down && (h.move(0, 1) && h.row == 1) && h.move(0, -1) && !h.help && h.row == 0);
        check("howto: the «!» is not saved with the chips (only keys and picks)", h.save().equals("a=0;b=0"));
        System.out.println(failed == 0 ? "DONE" : "FAILED " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
