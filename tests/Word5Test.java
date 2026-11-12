package sa.urtv.screensaver.games.word5;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** For tests/games_board_test.py: «خمّن الكلمة» on a plain JVM. Prints PASS/FAIL lines. args: word5-answers.json word5-guesses.json */
public final class Word5Test {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static String colours(String guess, String answer) { StringBuilder b = new StringBuilder(); for (int c : Word5Game.score(guess, answer)) b.append(c == 3 ? 'G' : c == 2 ? 'Y' : '-'); return b.toString(); }
    static void type(Word5Game g, String w) { for (char c : w.toCharArray()) g.type(Word5Text.ALPHABET.indexOf(c)); }

    static String pack(String... words) {
        Arrays.sort(words, (x, y) -> Integer.compare(Word5Text.code(x), Word5Text.code(y)));
        StringBuilder b = new StringBuilder();
        for (String w : words) for (char c : w.toCharArray()) b.append("abcdefghijklmnopqrstuvwxyz01".charAt(Word5Text.ALPHABET.indexOf(c)));
        return b.toString();
    }
    static Word5Words.Answer ans(String word, String display, String en, int level, boolean kids) { return new Word5Words.Answer(word, display, en, level, kids); }
    static byte[] utf(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    static Word5Words of(String packed, Word5Words.Answer... answers) { return Word5Words.of(Word5Text.ALPHABET, packed, Arrays.asList(answers)); }

    public static void main(String[] a) throws Exception {
        // ---- spelling
        check("normalize: the harakat, the shadda and the tatweel go; أ إ آ ٱ are ا, ة is ه, ى is ي",
            Word5Text.normalize("مَدْرَسَة").equals("مدرسه") && Word5Text.normalize("إبريق").equals("ابريق") && Word5Text.normalize("آٱأ").equals("ااا") && Word5Text.normalize("مستشفى").equals("مستشفي")
            && Word5Text.normalize("كـتـاب").equals("كتاب") && Word5Text.normalize("شدّة").equals("شده") && Word5Text.normalize(null).isEmpty());
        check("alphabet: 28 different letters, and a word is a number below 28^5 that turns back into the same word",
            new HashSet<>(Arrays.asList(Word5Text.ALPHABET.split(""))).size() == 28 && Word5Text.code("ااااا") == 0 && Word5Text.code("ييييي") == Word5Text.CODES - 1
            && Word5Text.word(Word5Text.code("دجاجه")).equals("دجاجه") && Word5Text.code("اااب") == -1 && Word5Text.code("سؤالا") == -1 && Word5Text.code("دجاجة") == -1 && Word5Text.code("hello") == -1);

        // ---- colouring
        // ب ا ب ا ب against ا ب ب ا ا : places 3 and 4 right; the answer still has ا, ب, ا left: the first ب is yellow, the first ا yellow, the last ب gray
        check("colours: right place green, elsewhere yellow, absent gray", colours("كتابه", "كتابه").equals("GGGGG") && colours("بكتاه", "كتابه").equals("YYYYG") && colours("زززقق", "كتابه").equals("-----"));
        check("colours, repeated letters: a letter is yellow only as often as the answer still has it after the greens",
            colours("بابات", "اببال").equals("YYGG-") && colours("ممممم", "سلامه").equals("---G-") && colours("مماثل", "سلامه").equals("Y-G-Y") && colours("ااااب", "بباتت").equals("--G-Y")
            && colours("للسلس", "سلامه").equals("-GY--") && colours("سلسله", "سلامه").equals("GG--G"));
        boolean sound = true; Random rnd = new Random(5);
        for (int n = 0; n < 20000; n++) {
            char[] g = new char[5], w = new char[5];
            for (int i = 0; i < 5; i++) { g[i] = Word5Text.ALPHABET.charAt(rnd.nextInt(4)); w[i] = Word5Text.ALPHABET.charAt(rnd.nextInt(4)); }
            int[] c = Word5Game.score(new String(g), new String(w));
            for (char l : "ابتث".toCharArray()) {
                int inGuess = 0, inWord = 0, lit = 0;
                for (int i = 0; i < 5; i++) { if (g[i] == l) { inGuess++; if (c[i] != Word5Game.GRAY) lit++; } if (w[i] == l) inWord++; if ((c[i] == Word5Game.GREEN) != (g[i] == w[i])) sound = false; }
                sound &= lit == Math.min(inGuess, inWord);
            }
        }
        check("colours: 20,000 random pairs over four letters: green exactly where equal, and each letter lit min(in the guess, in the answer) times", sound);

        // ---- the lists
        String four = pack("دجاجه", "كتابه", "سلامه", "بابات");
        Word5Words.Answer hen = ans("دجاجه", "دجاجة", "hen", 1, true), writing = ans("كتابه", "كتابة", "", 2, false), safety = ans("سلامه", "سلامة", "safety", 3, false);
        Word5Words w = of(four, hen, writing, safety);
        check("lists: the packed guesses unpack (a..z01 for the 28 letters); a guess is accepted by binary search over the codes; three answers with their spelling to show",
            w != null && w.acceptedCount() == 4 && w.accepts("بابات") && !w.accepts("ببببب") && !w.accepts("باب") && !w.accepts(null) && w.answers.size() == 3 && w.answers.get(0).display.equals("دجاجة") && w.answers.get(1).en.isEmpty());
        check("lists: refuses a cut string, a character outside a..z01, words not rising, another alphabet, only answers that are not accepted (one such among good ones is just left out), a spelling that is not its word, a repeated answer, a bad level, no answers",
            of(four.substring(1), hen) == null && of(four.replace('a', 'A'), hen) == null && of(four.substring(5, 10) + four.substring(0, 5), hen) == null && of(four.substring(0, 5) + four.substring(0, 5), hen) == null
            && Word5Words.of(Word5Text.ALPHABET.substring(1) + "ا", four, Arrays.asList(hen)) == null && of(four, ans("ببببب", "ببببب", "", 1, false)) == null && of(four, hen, ans("ببببب", "ببببب", "", 1, false)).answers.size() == 1 && of(four, ans("دجاجه", "حمامة", "", 1, false)) == null
            && of(four, hen, hen) == null && of(four, ans("دجاجه", "دجاجة", "", 4, false)) == null && of(four) == null && of("", hen) == null);
        Word5Words all = sa.urtv.screensaver.GamesBoardData.word5(Files.readAllBytes(Paths.get(a[0])), Files.readAllBytes(Paths.get(a[1])));
        int kids = 0, total = all == null ? 0 : all.answers.size(); boolean shown = all != null;
        if (all != null) for (Word5Words.Answer x : all.answers) { if (x.kids) kids++; shown &= all.accepts(x.word) && Word5Text.normalize(x.display).equals(x.word); }
        check("bundled: the starter files load: 500 answers or more (40 or more for kids), 2,000 guesses or more, every answer accepted and shown in its own spelling; 4 bytes of heap a guess ("
            + (all == null ? 0 : all.acceptedBytes()) + " bytes here, about 0.12 MB for the 31,000 of the full list)", shown && total >= 500 && kids >= 40 && all.acceptedCount() >= 2000 && all.acceptedBytes() == 16 + 4 * all.acceptedCount());
        check("bundled: another version, an HTML page or a broken list give null",
            sa.urtv.screensaver.GamesBoardData.word5(utf("<html>404</html>"), Files.readAllBytes(Paths.get(a[1]))) == null && sa.urtv.screensaver.GamesBoardData.word5(Files.readAllBytes(Paths.get(a[0])), utf("{\"v\":2,\"A\":\"\",\"w\":\"\"}")) == null
            && sa.urtv.screensaver.GamesBoardData.word5(utf("{\"v\":1,\"w\":[[1]]}"), Files.readAllBytes(Paths.get(a[1]))) == null);

        // ---- a game
        Word5Game g = new Word5Game(w, "سلامه", 6);
        check("typing: five letters and no more; «إدخال» on a short row does nothing; delete takes the last letter, and on an empty row says so (Back then opens the pause card)",
            g.enter() == Word5Game.Entered.SHORT && !g.delete() && g.type(0) && g.type(1) && g.enter() == Word5Game.Entered.SHORT && g.typing().equals("اب") && g.delete() && g.typing().equals("ا") && g.delete() && !g.delete()
            && !g.type(28) && !g.type(-1));
        type(g, "ببببب");
        check("typing: a sixth letter is refused; any five keyboard letters may be guessed (the list is not a gate: «ممتاز» and «مسافر» were refused by it) while the list still says what it holds", !g.type(3) && w.allows("ببببب") && w.allows("ممتاز") && !w.accepts("ببببب") && !w.allows("باب") && !w.allows("abcde") && !w.allows(null) && g.row() == 0 && g.typing().equals("ببببب"));
        check("spelling: أ إ آ fold to ا, ة to ه, ى and ئ to ي, ؤ to و, ء to ا (what a key types and what the lists hold are the same word)", Word5Text.normalize("أإآ").equals("ااا") && Word5Text.normalize("مدرسة").equals("مدرسه") && Word5Text.normalize("مبنى").equals("مبني") && Word5Text.normalize("مئؤء").equals("ميوا"));
        for (int i = 0; i < 5; i++) g.delete();
        type(g, "بابات");
        check("a guess: accepted, coloured, the row moves on, the keyboard takes the colours (ب gray, ا yellow, ت gray)", g.enter() == Word5Game.Entered.NEXT && g.row() == 1 && g.guess(0).equals("بابات")
            && g.typing().isEmpty() && Arrays.equals(g.colours(0), Word5Game.score("بابات", "سلامه")) && g.key(1) == Word5Game.GRAY && g.key(0) == Word5Game.YELLOW && g.key(2) == Word5Game.GRAY && g.key(11) == 0);
        type(g, "كتابه"); g.enter();
        check("keyboard: a key keeps the best colour it has had (ا went from yellow to green and stays green)", g.key(0) == Word5Game.GREEN && g.key(Word5Text.ALPHABET.indexOf('ه')) == Word5Game.GREEN);
        type(g, "بابات"); g.enter();
        check("keyboard: ... a later yellow does not take the green back; the same guess twice is allowed and costs a try", g.key(0) == Word5Game.GREEN && g.row() == 3);
        type(g, "سلامه");
        check("win: the word itself wins on that row, and nothing can be typed after", g.enter() == Word5Game.Entered.WON && g.won() && g.over() && g.row() == 4 && !g.type(0) && !g.delete() && g.enter() == Word5Game.Entered.OVER);
        g = new Word5Game(w, "سلامه", 5);
        Word5Game.Entered last = null;
        for (int i = 0; i < 5; i++) { type(g, "دجاجه"); last = g.enter(); }
        check("loss: with 5 tries the fifth wrong guess ends it, lost, and the answer is there to show", last == Word5Game.Entered.LOST && !g.won() && g.over() && g.answer().equals("سلامه") && Word5Game.TRIES[0] == 6);
        check("duel: fewer tries wins, solving beats not solving, equal tries or two misses are a tie (the next word decides)",
            Word5Game.duel(true, 3, true, 4) == 1 && Word5Game.duel(true, 5, true, 2) == 2 && Word5Game.duel(true, 6, false, 6) == 1 && Word5Game.duel(false, 6, true, 6) == 2 && Word5Game.duel(true, 3, true, 3) == 0
            && Word5Game.duel(false, 6, false, 6) == 0);

        // ---- which word
        List<String> recent = new ArrayList<>(); boolean fresh = true; rnd = new Random(9);
        for (int n = 0; n < 200; n++) { Word5Words.Answer x = all.free(false, recent, rnd); fresh &= !recent.contains(x.word); Word5Words.remember(recent, x.word); }
        boolean kidsOnly = true;
        for (int n = 0; n < 100; n++) { Word5Words.Answer x = all.free(true, recent, rnd); kidsOnly &= x.kids; Word5Words.remember(recent, x.word); }
        check("free play: 200 words in a row and none twice; the kids filter gives only kids words (again when all of them were played); the memory holds the last 200", fresh && kidsOnly && recent.size() == 200);

        // ---- the keyboard's focus
        Word5Keys k = new Word5Keys();
        boolean wraps = k.key() == 0 && k.move(Word5Keys.PREV) == 6 && k.move(Word5Keys.NEXT) == 0 && k.move(Word5Keys.UP) == Word5Keys.TAA && k.move(Word5Keys.UP) == 21 && k.move(Word5Keys.DOWN) == Word5Keys.TAA
            && k.move(Word5Keys.DOWN) == 0;
        k.set(12);   // row 1, column 5
        boolean column = k.move(Word5Keys.DOWN) == 19 && k.move(Word5Keys.DOWN) == 26 && k.move(Word5Keys.DOWN) == Word5Keys.DELETE && k.move(Word5Keys.UP) == 26 && k.move(Word5Keys.DOWN) == Word5Keys.DELETE
            && k.move(Word5Keys.NEXT) == Word5Keys.TAA && k.move(Word5Keys.NEXT) == Word5Keys.YAA && k.move(Word5Keys.NEXT) == Word5Keys.HAMZA && k.move(Word5Keys.NEXT) == Word5Keys.ENTER && k.move(Word5Keys.PREV) == Word5Keys.HAMZA
            && k.move(Word5Keys.PREV) == Word5Keys.YAA && k.move(Word5Keys.UP) == 22 && k.move(Word5Keys.DOWN) == Word5Keys.YAA;
        boolean inside = true; rnd = new Random(3);
        for (int n = 0; n < 5000; n++) { int at = k.move(rnd.nextInt(4)); inside &= at >= 0 && at < Word5Keys.KEYS; }
        Set<Integer> reached = new HashSet<>(); k.set(0);
        for (int r = 0; r < 6; r++) { for (int c = 0; c < 7; c++) { reached.add(k.key()); k.move(Word5Keys.NEXT); } k.move(Word5Keys.DOWN); }
        check("keyboard focus: every move wraps (row ends, top to «ة», bottom to the first row); the fifth row (ة ى ء إدخال حذف) returns to the column one came from; 5,000 random moves never leave the 33 keys and all 33 are reachable",
            wraps && column && inside && reached.size() == 33);
        System.exit(failed == 0 ? 0 : 1);
    }
}
