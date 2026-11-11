package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** The games kit and the two games built on it, on a plain JVM (tests/games_kit_test.py runs it): args = assets/games/khanat.json, assets/games/missing-word.json */
public class GamesKitTest {
    static int failed;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static byte[] b(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    public static void main(String[] a) throws Exception {
        byte[] khanatRaw = Files.readAllBytes(Paths.get(a[0])), mwRaw = Files.readAllBytes(Paths.get(a[1]));
        registry(); score(); chips(); clock(); dataCheck();
        khanat(khanatRaw); missingWord(mwRaw);
        System.exit(failed == 0 ? 0 : 1);
    }

    static void registry() {
        Set<String> ids = new HashSet<>(), acts = new HashSet<>(); int phones = 0;
        for (GameRegistry.Game g : GameRegistry.ALL) { ids.add(g.id); acts.add(g.activity); if (g.phones) phones++; }
        check("registry: eight games, ids and activities unique, only «الأسرع» needs phones", GameRegistry.ALL.length == 8 && ids.size() == 8 && acts.size() == 8 && phones == 1 && GameRegistry.byId("quiz").phones && GameRegistry.byId("quiz").ready);
        check("registry: every game has a name and one line, no «·» or «•» in them", Arrays.stream(GameRegistry.ALL).allMatch(g -> !g.name.isEmpty() && !g.line.isEmpty() && !(g.name + g.line).matches(".*[·•].*")));
        int f = GameRegistry.firstFocus("");
        check("registry: the list's focus starts on the game played last, else on the first ready game with no phones (else the first)",
            GameRegistry.firstFocus("quiz") == 0 && (GameRegistry.ALL[f].ready && !GameRegistry.ALL[f].phones || f == 0) && GameRegistry.firstFocus("nothing") == f && GameRegistry.byId("nothing") == null);
    }

    static void score() {
        GameScore s = new GameScore(4);
        s.add(0, 10); s.add(1, 20); s.add(2, 20); s.add(9, 50); s.add(-1, 50);
        check("score: sides on one score share the place (1, 1, 3, 4), a side out of range is ignored", s.rank(1) == 1 && s.rank(2) == 1 && s.rank(0) == 3 && s.rank(3) == 4 && s.best() == 20);
        check("score: ranked keeps the order of equal sides, a tie for first is seen", Arrays.equals(s.ranked(), new int[]{1, 2, 0, 3}) && s.tie() && s.leaders() == 2);
        s.newRound(); s.add(0, 15);
        check("score: «last» is what a side won since the round began", s.last[0] == 15 && s.last[1] == 0 && s.points[0] == 25 && !s.tie() && s.after(3) == 0);
        check("score: 1 to 8 sides; one side never ties; nobody on 0", new GameScore(0).n == 1 && new GameScore(99).n == 8 && !new GameScore(1).tie() && new GameScore(3).nobody() && new GameScore(3).tie());
        check("score: eight default team names, all different", GameScore.TEAMS.length == 8 && new HashSet<>(Arrays.asList(GameScore.TEAMS)).size() == 8);
    }

    static void chips() {
        GameChips c = new GameChips().add("rounds", "الجولات", new String[]{"3", "5", "8"}, 1).add("time", "الوقت", new String[]{"60", "90", "0"}, 1);
        check("chips: the focus starts on the start button, so OK starts with last time's choices", c.onButton() && !c.pick() && c.picked("rounds") == 1);
        check("chips: up reaches the rows on the picked chip; an arrow at an edge does not move", c.move(0, -1) && c.row == 1 && c.col == 1 && c.move(1, 0) && c.col == 2 && !c.move(1, 0) && c.pick() && c.picked("time") == 2 && c.move(0, 1) && c.onButton() && !c.move(0, 1) && !c.move(1, 0));
        check("chips: saved as keys and indexes only", c.save().equals("rounds=1;time=2"));
        GameChips d = new GameChips().add("rounds", "", new String[]{"3", "5", "8"}, 1).add("time", "", new String[]{"60", "90", "0"}, 1);
        d.load("time=2;rounds=7;junk;x=1;rounds=abc");
        check("chips: load keeps the default for a bad index, an unknown key or junk", d.picked("time") == 2 && d.picked("rounds") == 1);
        d.setOff("time", 2, true);
        d.row = 1; d.col = 2;
        check("chips: a chip switched off cannot be picked, and a picked one falls back to the first offered", d.picked("time") == 0 && !d.pick() && d.picked("time") == 0);
    }

    static void clock() {
        GameClock k = new GameClock();
        check("clock: never started = not running, never over", !k.running() && !k.over(1000) && k.leftMs(5) == 0);
        k.start(15000, 1000);
        check("clock: 15 s count down in whole seconds, rounded up", k.leftSeconds(1000) == 15 && k.leftSeconds(1001) == 15 && k.leftSeconds(2000) == 14 && k.leftSeconds(15999) == 1 && !k.over(15999) && k.over(16000));
        k.pause(6000); 
        check("clock: paused, the time left does not move and it is never over", k.paused() && k.leftMs(999999) == 10000 && !k.over(999999));
        k.resume(100000);
        check("clock: resumed, it goes on from where it stopped", k.leftMs(100000) == 10000 && k.over(110000) && !k.over(109999));
        k.start(0, 0);
        check("clock: «بدون وقت» (0) never ends", !k.running() && !k.over(Long.MAX_VALUE));
    }

    static void dataCheck() {
        check("data: an HTML page, an empty file, a list where an object is expected, broken JSON are refused", GameDataCheck.root(b("<html>404</html>"), '{') == null && GameDataCheck.root(new byte[0], '{') == null && GameDataCheck.root(null, '{') == null
            && GameDataCheck.root(b("[1]"), '{') == null && GameDataCheck.root(b("{\"a\":"), '{') == null && GameDataCheck.root(b(" {\"a\":1}"), '{') != null && GameDataCheck.root(b("[1]"), '[') != null);
        check("data: a file over the cap is refused before it is parsed", GameDataCheck.root(new byte[GameDataCheck.BYTES_MAX + 1], '{') == null);
        check("data: texts 1 to max without control characters; whole numbers in range; plain ids", GameDataCheck.text("أ", 5) && !GameDataCheck.text("", 5) && !GameDataCheck.text("abcdef", 5) && !GameDataCheck.text("a\nb", 5) && !GameDataCheck.text(1.0, 5)
            && GameDataCheck.whole(2.0, 1, 3) == 2 && GameDataCheck.whole(2.5, 1, 3) < 0 && GameDataCheck.whole(4.0, 1, 3) < 0 && GameDataCheck.whole("2", 1, 3) < 0 && GameDataCheck.id("mw-geo.1:x_2") && !GameDataCheck.id("a b") && !GameDataCheck.id(""));
    }

    // ---------------------------------------------------------------- «خمس خانات»

    static void khanat(byte[] raw) {
        KhanatData d = KhanatData.STARTER.parse(raw);
        check("khanat: the bundled starter passes its check: 25 letters, 3 rare, five boxes", d != null && d.ar.letters.size() == 25 && d.ar.rare.size() == 3 && d.boxes.equals(Arrays.asList("animal", "plant", "object", "country", "name")));
        if (d == null) return;
        check("khanat: samples for a letter and a box, at most five asked; none for an unknown letter or box", !d.samples(false, "ب", "animal", 5).isEmpty() && d.samples(false, "ب", "animal", 2).size() == 2 && d.samples(false, "?", "animal", 5).isEmpty() && d.samples(false, "ب", "zzz", 5).isEmpty());
        check("khanat: the English game has its own letters from the samples' English names, each sample under its first letter", d.en.letters.size() >= 15 && d.letters(true, true).size() > d.letters(true, false).size()
            && d.samples(true, "C", "country", 8).stream().allMatch(x -> x.startsWith("C")) && !d.samples(true, "C", "country", 8).isEmpty());
        check("khanat: «حروف صعبة» adds the rare letters", d.letters(false, true).size() == 28 && d.letters(false, false).size() == 25);
        boolean never = true;
        for (String l : d.letters(false, true)) for (String box : new String[]{"country", "city", "saudi_city"}) for (String s : d.samples(false, l, box, 99)) if (s.contains("إسرائيل") || s.contains("الصحراء الغربية")) never = false;
        check("khanat: no sample lists Israel or Western Sahara as a country", never);
        String text = new String(raw, StandardCharsets.UTF_8);
        check("khanat: refused: an HTML page, another version, an unknown box, a sample over 24 characters, a letter twice, too few samples for a download",
            KhanatData.parse(b("<html>"), 1) == null && KhanatData.parse(b(text.replace("\"v\":1", "\"v\":2")), 1) == null && KhanatData.parse(b(text.replaceFirst("\"animal\":\\[\\[", "\"zebra\":[[")), 1) == null
            && KhanatData.parse(b(text.replaceFirst("\\[\\[\"", "[[\"" + new String(new char[25]).replace('\0', 'ب'))), 1) == null && KhanatData.parse(b(text.replace("\"R\":\"", "\"R\":\"ا")), 1) == null
            && KhanatData.parse(raw, 100000) == null && KhanatData.parse(raw, 1) != null);

        String[] boxes = d.boxes.toArray(new String[0]);
        KhanatEngine e = new KhanatEngine(3, 2, boxes, d.letters(false, false), new Random(7));
        String first = e.letter;
        check("khanat: a game starts on a letter of the list, round 1", e.phase == KhanatEngine.Phase.LETTER && d.ar.letters.contains(first) && e.round == 0 && e.boxes.length == 5);
        e.write(); e.stop(true);
        check("khanat: OK during the writing asks which team finished", e.phase == KhanatEngine.Phase.WHO);
        e.finishedFirst(1);
        // box 0: team 0 valid alone, team 1 valid; box 1..4: team 1 valid, team 0 and 2 the same answer in box 1
        e.cycle(0); e.cycle(1); e.nextBox();
        e.set(0, KhanatEngine.SAME); e.set(2, KhanatEngine.SAME); e.set(1, KhanatEngine.VALID); e.nextBox();
        for (int i = 2; i < 5; i++) { e.set(1, KhanatEngine.VALID); e.nextBox(); }
        check("khanat: 10 for a valid answer, 5 for one another team gave too, and +5 once to the team that finished first with every box valid", e.phase == KhanatEngine.Phase.SCORE && e.score.points[0] == 15 && e.score.points[1] == 55 && e.score.points[2] == 5 && e.bonusGiven);
        check("khanat: cycle goes none, valid, same, none; nothing is marked outside the reveal", KhanatEngine.points(0) == 0 && KhanatEngine.points(1) == 10 && KhanatEngine.points(2) == 5 && e.marks[0][0] == KhanatEngine.VALID);
        e.cycle(0);
        e.next();
        check("khanat: the next round has another letter", e.phase == KhanatEngine.Phase.LETTER && e.round == 1 && !e.letter.equals(first) && e.lastRound());
        e.write(); e.stop(false);
        check("khanat: when the clock ends nobody finished first", e.phase == KhanatEngine.Phase.REVEAL && e.finisher == -1);
        for (int i = 0; i < 5; i++) { if (i < 4) e.set(0, KhanatEngine.VALID); e.nextBox(); }
        e.next();
        check("khanat: 15 + 40 = 55 = a tie for first after the last round plays ONE more round with a single box", e.score.points[0] == 55 && e.phase == KhanatEngine.Phase.LETTER && e.tiebreak && e.boxes.length == 1);
        e.write(); e.stop(true); e.finishedFirst(0); e.set(0, KhanatEngine.VALID); e.nextBox();
        check("khanat: no finishing bonus in the tie-break; then the end", e.score.points[0] == 65 && !e.bonusGiven);
        e.next();
        check("khanat: the game ends after the tie-break (a tie after it would stand)", e.phase == KhanatEngine.Phase.END && !e.score.tie());
        KhanatEngine g = new KhanatEngine(2, 1, boxes, d.letters(false, false), new Random(1));
        g.write(); g.stop(true); g.finishedFirst(0); g.set(0, KhanatEngine.VALID); g.nextBox(); for (int i = 1; i < 5; i++) g.nextBox();
        check("khanat: a team that stopped early with an empty box keeps its valid boxes, without the bonus", g.score.points[0] == 10 && !g.bonusGiven);
        Set<String> seen = new HashSet<>(); boolean repeat = false;
        KhanatEngine many = new KhanatEngine(2, 8, boxes, d.letters(false, false), new Random(3));
        for (int r = 0; r < 8; r++) { if (!seen.add(many.letter)) repeat = true; many.write(); many.stop(false); for (int i = 0; i < 5; i++) { many.set(r % 2, KhanatEngine.VALID); many.nextBox(); } many.next(); }
        check("khanat: eight rounds, never the same letter twice", !repeat && seen.size() == 8);
        many.finish();
        check("khanat: «إنهاء الجولة» ends the game with the totals as they are", many.phase == KhanatEngine.Phase.END);
    }

    // ---------------------------------------------------------------- «الكلمة المفقودة»

    static void missingWord(byte[] raw) {
        List<MissingWord.Item> all = MissingWord.STARTER.parse(raw);
        check("missing word: the bundled starter passes its check, every statement has one blank", all != null && all.size() >= 40 && all.stream().allMatch(x -> MissingWord.oneBlank(x.text[0]) && !x.word[0].isEmpty()));
        if (all == null) return;
        String text = new String(raw, StandardCharsets.UTF_8);
        check("missing word: refused: an HTML page, a list as the root, another version, a statement with two blanks, an id twice, a level 4, too few items for a download",
            MissingWord.parse(b("<html>"), 1) == null && MissingWord.parse(b("[]"), 1) == null && MissingWord.parse(b(text.replace("\"v\":1", "\"v\":3")), 1) == null && MissingWord.parse(b(text.replaceFirst("___", "___ ___")), 1) == null
            && MissingWord.parse(b(text.replaceFirst("\"i\":\"[^\"]+\"", "\"i\":\"" + all.get(1).id + "\"")), 1) == null && MissingWord.parse(b(text.replaceFirst("\"d\":\\d", "\"d\":4")), 1) == null
            && MissingWord.parse(raw, all.size() + 1) == null && MissingWord.PUBLISHED.parse(raw) == null);
        check("missing word: an English game plays only the items with real English (no Arabic letter in them); a saying has none", all.stream().filter(x -> x.hasEnglish()).allMatch(x -> MissingWord.oneBlank(x.text[1]) && !x.text[1].matches(".*[\\u0600-\\u06FF].*"))
            && MissingWord.available(all, MissingWord.SAYING, false, true) == 0 && MissingWord.available(all, MissingWord.FACT, false, true) > 0 && MissingWord.offered(all, MissingWord.MIX, false, true) && !MissingWord.offered(all, MissingWord.SAYING, false, true));
        List<MissingWord.Item> ten = MissingWord.pick(all, MissingWord.MIX, false, false, 10, new HashSet<String>(), new Random(5));
        Set<String> ids = new HashSet<>(); int facts = 0;
        for (MissingWord.Item it : ten) { ids.add(it.id); if (it.deck.equals(MissingWord.FACT)) facts++; }
        check("missing word: «مزيج» picks ten different items, the two kinds in turn", ten.size() == 10 && ids.size() == 10 && facts == 5);
        List<MissingWord.Item> again = MissingWord.pick(all, MissingWord.SAYING, false, false, 10, ids, new Random(5));
        check("missing word: a deck gives only its kind, and what was asked lately is not asked again while others are left", again.size() == 10 && again.stream().allMatch(x -> x.deck.equals(MissingWord.SAYING) && !ids.contains(x.id)));
        check("missing word: the hint is the word's own first letter («ال» skipped in Arabic)", new MissingWord.Item("x", "fact", 1, false, new String[]{"a ___", "a ___"}, new String[]{"الفرج", "Relief"}).firstLetter(false).equals("ف")
            && new MissingWord.Item("x", "fact", 1, false, new String[]{"a ___", "a ___"}, new String[]{"الفرج", "Relief"}).firstLetter(true).equals("R"));

        MissingWord.Engine e = new MissingWord.Engine(2, ten.subList(0, 4), true);
        check("missing word: the first team is asked the first statement", e.phase == MissingWord.Engine.Phase.ASK && e.turn == 0 && e.index == 0 && e.total() == 4 && e.worth() == 10);
        e.judge(MissingWord.Engine.CORRECT);
        check("missing word: no verdict before the word is shown", e.index == 0 && e.score.points[0] == 0);
        e.reveal(); e.judge(MissingWord.Engine.CORRECT);
        check("missing word: right = 10, then the next team's turn with the next statement", e.score.points[0] == 10 && e.turn == 1 && e.index == 1 && e.lastSide == 0 && e.lastPoints == 10);
        check("missing word: the hint is once per statement and halves the points", e.hint() && !e.hint() && e.worth() == 5);
        e.reveal();
        check("missing word: no hint once the word is shown", !e.hint());
        e.judge(MissingWord.Engine.CORRECT);
        check("missing word: right after the hint = 5; the hint is new for the next statement", e.score.points[1] == 5 && !e.hinted && e.turn == 0);
        e.reveal(); e.judge(MissingWord.Engine.STOLEN);
        check("missing word: stolen = 5 to the NEXT team, nothing to the team whose turn it was", e.score.points[1] == 10 && e.score.points[0] == 10 && e.lastSide == 1);
        e.reveal(); e.judge(MissingWord.Engine.WRONG);
        check("missing word: wrong = 0, and after the last statement the game ends", e.phase == MissingWord.Engine.Phase.END && e.current() == null && e.score.tie() && e.lastSide == -1);
        MissingWord.Engine solo = new MissingWord.Engine(1, ten, true);
        solo.reveal(); solo.judge(MissingWord.Engine.STOLEN);
        check("missing word: one team alone: nothing to steal", !solo.canSteal() && solo.score.points[0] == 0 && solo.turn == 0 && solo.index == 1);
        MissingWord.Engine off = new MissingWord.Engine(3, ten, false);
        off.reveal(); off.judge(MissingWord.Engine.STOLEN);
        check("missing word: with «الخطف» off a steal gives nothing; three teams take turns", off.score.best() == 0 && off.turn == 1);
        off.finish();
        check("missing word: «إنهاء الجولة» ends the game; an empty game is over at once", off.phase == MissingWord.Engine.Phase.END && new MissingWord.Engine(2, new ArrayList<MissingWord.Item>(), false).phase == MissingWord.Engine.Phase.END);
    }
}
