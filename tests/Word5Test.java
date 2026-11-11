package sa.urtv.screensaver.games.word5;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** For tests/games_board_test.py: «خمّن الكلمة» on a plain JVM. Prints PASS/FAIL lines. args: accepted.bin answers.tsv */
public final class Word5Test {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static String colours(String guess, String answer) { StringBuilder b = new StringBuilder(); for (int c : Word5Game.score(guess, answer)) b.append(c == 3 ? 'G' : c == 2 ? 'Y' : '-'); return b.toString(); }
    static void type(Word5Game g, String w) { for (char c : w.toCharArray()) g.type(Word5Text.ALPHABET.indexOf(c)); }

    static byte[] file(int... codes) {
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
        o.write('W'); o.write('5'); o.write('A'); o.write('1');
        for (int s = 24; s >= 0; s -= 8) o.write(codes.length >>> s);
        int last = -1;
        for (int c : codes) { int d = c - last; last = c; while (d > 127) { o.write((d & 127) | 128); d >>>= 7; } o.write(d); }
        return o.toByteArray();
    }
    static byte[] utf(String s) { return s.getBytes(StandardCharsets.UTF_8); }

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
        int c1 = Word5Text.code("دجاجه"), c2 = Word5Text.code("كتابه"), c3 = Word5Text.code("سلامه"), c4 = Word5Text.code("بابات");
        int[] four = {c1, c2, c3, c4}; Arrays.sort(four);
        String ans = "دجاجه\tدجاجة\then\t1\t1\nكتابه\tكتابة\t\t2\t0\nسلامه\tسلامة\tsafety\t3\t0\n";
        Word5Words w = Word5Words.parse(file(four), utf(ans));
        check("lists: parse; a guess is accepted by binary search over the codes; three answers with their spelling to show",
            w != null && w.acceptedCount() == 4 && w.accepts("بابات") && !w.accepts("ببببب") && !w.accepts("باب") && !w.accepts(null) && w.answers.size() == 3 && w.answers.get(0).display.equals("دجاجة") && w.answers.get(1).en.isEmpty());
        byte[] good = file(four), cut = Arrays.copyOf(good, good.length - 1), more = Arrays.copyOf(good, good.length + 1), magic = good.clone(); magic[3] = '2';
        check("lists: refuses a cut file, trailing bytes, another magic, codes not rising, a code past 28^5, an answer that is not accepted, a spelling that is not its word, a repeated answer, a bad level",
            Word5Words.parse(cut, utf(ans)) == null && Word5Words.parse(more, utf(ans)) == null && Word5Words.parse(magic, utf(ans)) == null && Word5Words.parse(file(5, 5), utf(ans)) == null
            && Word5Words.parse(file(c1, Word5Text.CODES), utf("دجاجه\tدجاجة\t\t1\t1\n")) == null && Word5Words.parse(good, utf("ببببب\tببببب\t\t1\t0\n")) == null
            && Word5Words.parse(good, utf("دجاجه\tحمامة\t\t1\t0\n")) == null && Word5Words.parse(good, utf(ans + ans)) == null && Word5Words.parse(good, utf("دجاجه\tدجاجة\t\t4\t0\n")) == null
            && Word5Words.parse(good, utf("")) == null && Word5Words.parse(utf("<html>404</html>"), utf(ans)) == null);
        Word5Words all = Word5Words.parse(Files.readAllBytes(Paths.get(a[0])), Files.readAllBytes(Paths.get(a[1])));
        int kids = 0; boolean shown = all != null;
        if (all != null) for (Word5Words.Answer x : all.answers) { if (x.kids) kids++; shown &= all.accepts(x.word) && Word5Text.normalize(x.display).equals(x.word); }
        check("bundled: the real lists parse: 560 answers (40 or more for kids), over 25,000 accepted guesses in about 0.1 MB of heap (" + (all == null ? 0 : all.acceptedBytes()) + " bytes, a file of "
            + Files.size(Paths.get(a[0])) + "), every answer accepted and shown in its own spelling", shown && all.answers.size() == 560 && kids >= 40 && all.acceptedCount() > 25000 && all.acceptedBytes() < 130_000 && Files.size(Paths.get(a[0])) < 60_000);

        // ---- a game
        Word5Game g = new Word5Game(w, "سلامه", 6);
        check("typing: five letters and no more; «إدخال» on a short row does nothing; delete takes the last letter, and on an empty row says so (Back then opens the pause card)",
            g.enter() == Word5Game.Entered.SHORT && !g.delete() && g.type(0) && g.type(1) && g.enter() == Word5Game.Entered.SHORT && g.typing().equals("اب") && g.delete() && g.typing().equals("ا") && g.delete() && !g.delete()
            && !g.type(28) && !g.type(-1));
        type(g, "ببببب");
        check("typing: a sixth letter is refused; a word we do not know costs no try and its letters stay for fixing", !g.type(3) && g.enter() == Word5Game.Entered.UNKNOWN && g.row() == 0 && g.typing().equals("ببببب"));
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
        Set<String> days = new HashSet<>();
        for (long d = 20000; d < 20560; d++) days.add(all.daily(false, d).word);
        check("word of the day: the same word for the same day, another the next day, and all 560 have their day before one comes again",
            all.daily(false, 20403).word.equals(all.daily(false, 20403).word) && days.size() == 560 && all.daily(false, 20000).word.equals(all.daily(false, 20560).word) && all.daily(true, 20403).kids && all.daily(false, -3) != null);

        // ---- the keyboard's focus
        Word5Keys k = new Word5Keys();
        boolean wraps = k.key() == 0 && k.move(Word5Keys.PREV) == 6 && k.move(Word5Keys.NEXT) == 0 && k.move(Word5Keys.UP) == Word5Keys.ENTER && k.move(Word5Keys.UP) == 21 && k.move(Word5Keys.DOWN) == Word5Keys.ENTER
            && k.move(Word5Keys.DOWN) == 0;
        k.set(12);   // row 1, column 5
        boolean column = k.move(Word5Keys.DOWN) == 19 && k.move(Word5Keys.DOWN) == 26 && k.move(Word5Keys.DOWN) == Word5Keys.DELETE && k.move(Word5Keys.UP) == 26 && k.move(Word5Keys.DOWN) == Word5Keys.DELETE
            && k.move(Word5Keys.NEXT) == Word5Keys.ENTER && k.move(Word5Keys.PREV) == Word5Keys.DELETE && k.move(Word5Keys.NEXT) == Word5Keys.ENTER && k.move(Word5Keys.UP) == 22;
        boolean inside = true; rnd = new Random(3);
        for (int n = 0; n < 5000; n++) { int at = k.move(rnd.nextInt(4)); inside &= at >= 0 && at < Word5Keys.KEYS; }
        Set<Integer> reached = new HashSet<>(); k.set(0);
        for (int r = 0; r < 5; r++) { for (int c = 0; c < 7; c++) { reached.add(k.key()); k.move(Word5Keys.NEXT); } k.move(Word5Keys.DOWN); }
        check("keyboard focus: every move wraps (row ends, top to «إدخال», bottom to the first row); a wide key returns to the column one came from, or its own middle; 5,000 random moves never leave the 30 keys and all 30 are reachable",
            wraps && column && inside && reached.size() == 30);
        System.exit(failed == 0 ? 0 : 1);
    }
}
