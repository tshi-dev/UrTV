package sa.urtv.screensaver.games.hive;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** For tests/games_board_test.py: «خلية الحروف» on a plain JVM (a fake clock, a seeded Random). Prints PASS/FAIL lines. args: the quiz starter, letter-hive.json, khanat.json */
public final class HiveTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static long[] now = {1_000_000L};

    static String line(String id, String ar, String en, int level, int kids) {
        return id + "\t" + ar + "\t" + en + "\t" + level + "\t" + kids + "\tس " + id + "\tQ " + id + "\tج " + id + "\tA " + id + "\tx\tx\ty\ty\tz\tz\n";
    }
    /** a bank from test lines (id, ar, en, level, kids, 10 texts) and hint lines (letter, samples); null when a line is refused */
    static HiveBank bank(String items, String hints) {
        HiveBank b = new HiveBank();
        for (String l : items.split("\n")) {
            String[] f = l.split("\t", -1);
            if (!b.add(f[0], f[1].isEmpty() ? 0 : f[1].charAt(0), f[2].isEmpty() ? 0 : f[2].charAt(0), Integer.parseInt(f[3]), f[4].equals("1"), new String[] {f[5], f[6]}, new String[] {f[7], f[8]},
                new String[][] {{f[9], f[10]}, {f[11], f[12]}, {f[13], f[14]}})) return null;
        }
        for (String l : hints.split("\n")) { String[] f = l.split("\t"); for (int i = 1; i < f.length; i++) b.hint(f[0].charAt(0), f[i]); }
        return b;
    }
    /** a game from this seed on, the first whose board shows every one of these letters (three letters stay out of each game) */
    static HiveGame game(HiveBank b, HiveGame.Settings s, long seed, String need) {
        for (;; seed += 1000) {
            now[0] = 1_000_000L;
            HiveGame g = new HiveGame(b, s, HiveBoard.TEAM_SIDES, () -> now[0], new Random(seed));
            if (new String(g.board.letter).chars().filter(c -> need.indexOf(c) >= 0).count() == need.length()) return g;
        }
    }
    static HiveGame game(HiveBank b, HiveGame.Settings s, long seed) { return game(b, s, seed, ""); }
    static int cellOf(HiveGame g, char letter) { for (int i = 0; i < HiveBoard.CELLS; i++) if (g.board.letter[i] == letter && g.board.owner[i] == 0) return i; return -1; }
    /** pick, reveal, the older judge(true): the hexagon goes to the team that was in turn */
    static boolean yesNoOwner(HiveGame g, int cell) { int t = g.turn(); return right(g, cell) && g.board.owner[cell] == t && g.phase() == HiveGame.Phase.PICK; }
    static boolean right(HiveGame g, int cell) { return g.pick(cell) && g.reveal() && g.judge(true); }

    public static void main(String[] a) throws Exception {
        // ---- the board
        boolean symmetric = true; int links = 0;
        for (int i = 0; i < HiveBoard.CELLS; i++) for (int n : HiveBoard.neighbours(i)) {
            links++;
            boolean back = false;
            for (int m : HiveBoard.neighbours(n)) back |= m == i;
            symmetric &= back && n != i;
        }
check("board: 25 cells, every neighbour link goes both ways, 56 links in all (5 rows of 4, 4 row gaps of 9)", symmetric && links == 2 * (5 * 4 + 4 * 9) && HiveBoard.CELLS == 25);
        check("board: a middle cell touches 6, the corners 2 or 3, an even row leans to the row start and an odd row to its end",
            HiveBoard.neighbours(HiveBoard.cell(1, 2)).length == 6 && HiveBoard.neighbours(HiveBoard.cell(0, 0)).length == 2 && HiveBoard.neighbours(HiveBoard.cell(0, 4)).length == 3
            && Arrays.equals(HiveBoard.neighbours(HiveBoard.cell(2, 2)), new int[] {11, 13, 6, 7, 16, 17}) && Arrays.equals(HiveBoard.neighbours(HiveBoard.cell(1, 2)), new int[] {6, 8, 2, 3, 12, 13}));
        check("board: the focus wraps along a row and stops at the top and the bottom; up and down always land on a touching cell",
            HiveBoard.move(4, HiveBoard.NEXT) == 0 && HiveBoard.move(0, HiveBoard.PREV) == 4 && HiveBoard.move(2, HiveBoard.UP) == 2 && HiveBoard.move(22, HiveBoard.DOWN) == 22
            && HiveBoard.move(7, HiveBoard.DOWN) == 12 && HiveBoard.move(12, HiveBoard.UP) == 7);
        HiveBoard b = new HiveBoard();
        for (int c = 0; c < 5; c++) b.owner[HiveBoard.cell(2, c)] = HiveBoard.TEAM_SIDES;
        check("win: team 1 joins the two sides with a whole row, and the chain is that row in order from the row start", b.winner() == HiveBoard.TEAM_SIDES
            && Arrays.equals(b.chain(HiveBoard.TEAM_SIDES), new int[] {10, 11, 12, 13, 14}) && b.chain(HiveBoard.TEAM_TOP) == null);
        b.owner[HiveBoard.cell(2, 2)] = HiveBoard.FREE;
        check("win: one gap in the row and nobody has won", b.winner() == HiveBoard.FREE);
        b.owner[HiveBoard.cell(1, 2)] = HiveBoard.TEAM_SIDES;   // (1,2) touches (2,2) and (2,3), not (2,1): no bridge
        boolean noBridge = b.winner() == HiveBoard.FREE;
        b.owner[HiveBoard.cell(1, 1)] = HiveBoard.TEAM_SIDES;   // (2,1) -> (1,1) -> (1,2) -> (2,3)
        check("win: a detour over the next row joins only through cells that really touch", noBridge && b.winner() == HiveBoard.TEAM_SIDES && b.chain(HiveBoard.TEAM_SIDES).length == 6);
        b = new HiveBoard();
        int[] down = {HiveBoard.cell(0, 2), HiveBoard.cell(1, 1), HiveBoard.cell(2, 2), HiveBoard.cell(3, 1), HiveBoard.cell(4, 1)};
        for (int c : down) b.owner[c] = HiveBoard.TEAM_TOP;
        check("win: team 2 joins top and bottom with a zigzag of 5 (both teams have five to cross), and team 1 holding nothing has not won", b.winner() == HiveBoard.TEAM_TOP && Arrays.equals(b.chain(HiveBoard.TEAM_TOP), down));
        b.owner[HiveBoard.cell(2, 2)] = HiveBoard.FREE; b.owner[HiveBoard.cell(2, 0)] = HiveBoard.TEAM_TOP;   // (1,1) touches (2,1) and (2,2) only
        check("win: a zigzag that leans the wrong way does not touch", b.winner() == HiveBoard.FREE);
        Random rnd = new Random(7); boolean oneWinner = true;
        for (int n = 0; n < 20000; n++) {
            HiveBoard f = new HiveBoard();
            for (int i = 0; i < HiveBoard.CELLS; i++) f.owner[i] = 1 + rnd.nextInt(2);
            oneWinner &= (f.chain(1) != null) != (f.chain(2) != null);
        }
        check("win: 20,000 random full boards, every one has exactly one winner (no draw, never two)", oneWinner);

        // ---- the bank
        StringBuilder items = new StringBuilder();
        for (char l : HiveBank.AR.toCharArray()) if (l != 'ظ' && l != 'ث') for (int n = 0; n < 3; n++) items.append(line("q-" + (int) l + "-" + n, "" + l, n == 0 ? "A" : n == 1 ? "B" : "", n + 1, n == 0 ? 1 : 0));
        String hints = "ت\tتمساح\tتونس\nA\tAnt\n";
        HiveBank small = bank(items.toString(), hints);
        check("bank: parses; a question is found under its Arabic letter and its English one; hints by letter",
            small != null && small.size() == 78 && small.of('ب').size() == 3 && small.of('A').size() == 26 && small.of('B').size() == 26 && small.of('C').isEmpty() && small.of('ظ').isEmpty()
            && small.hints('ت').length == 2 && small.hints('ث').length == 0);
        check("bank: refuses a letter outside the alphabets, level 4, a repeated id, a question of no letter; keeps the first six hints of a letter, none of a stranger",
            bank(line("a", "ء", "", 1, 0), "") == null && bank(line("a", "ب", "b", 1, 0), "") == null && bank(line("a", "ب", "", 4, 0), "") == null && bank(line("a", "ب", "", 1, 0) + line("a", "ت", "", 1, 0), "") == null
            && bank(line("a", "", "", 1, 0), "") == null && bank(line("a", "", "Q", 1, 0), "") != null && bank(line("a", "ب", "", 1, 0), "ظ\t1\t2\t3\t4\t5\t6\t7\t1\n?\tx\n").hints('ظ').length == 6
            && bank(line("a", "ب", "", 1, 0), "?\tx\n").hints('?').length == 0);
        byte[] quiz = Files.readAllBytes(Paths.get(a[0])), index = Files.readAllBytes(Paths.get(a[1])), khanat = Files.readAllBytes(Paths.get(a[2]));
        HiveBank starter = sa.urtv.screensaver.GamesBoardData.hive(quiz, index, khanat);
        int withQuestions = 0, withHints = 0;
        if (starter != null) for (char l : HiveBank.AR.toCharArray()) { if (!starter.of(l).isEmpty()) withQuestions++; if (starter.hints(l).length > 0) withHints++; }
        boolean sixEach = starter != null;
        if (starter != null) for (char l : HiveBank.AR.toCharArray()) sixEach &= "ظذض".indexOf(l) >= 0 ? starter.of(l).isEmpty() : starter.of(l).size() >= 6;
        check("starter: the bundled files load (the quiz starter joined with the letter index, hints from the samples, the items the index embeds): 25 Arabic letters with 6 questions or more each (ظ ذ ض none), nearly all with sample answers, English letters too",
            starter != null && starter.size() >= 150 && withQuestions == 25 && sixEach && withHints >= 25 && starter.of('ظ').isEmpty() && starter.of('A').size() >= 4 && starter.of('X').isEmpty() && starter.hints('B').length == 6 && starter.hints('ب').length == 6);
        check("starter: a bank without the samples file still loads (no hints); a broken index, another version, a broken embedded item, an HTML page or a broken quiz bank give null",
            sa.urtv.screensaver.GamesBoardData.hive(quiz, index, null) != null && sa.urtv.screensaver.GamesBoardData.hive(quiz, index, null).hints('ب').length == 0
            && sa.urtv.screensaver.GamesBoardData.hive(quiz, "{\"v\":1}".getBytes(StandardCharsets.UTF_8), khanat) == null
            && sa.urtv.screensaver.GamesBoardData.hive(quiz, new String(index, StandardCharsets.UTF_8).replace("\"v\":1", "\"v\":2").getBytes(StandardCharsets.UTF_8), khanat) == null
            && sa.urtv.screensaver.GamesBoardData.hive(quiz, new String(index, StandardCharsets.UTF_8).replace("\"d\":", "\"x\":").getBytes(StandardCharsets.UTF_8), khanat) == null
            && sa.urtv.screensaver.GamesBoardData.hive(quiz, "<html>404</html>".getBytes(StandardCharsets.UTF_8), khanat) == null && sa.urtv.screensaver.GamesBoardData.hive("[]".getBytes(StandardCharsets.UTF_8), index, khanat) == null);
        boolean banned = false;
        for (char l : (HiveBank.AR + HiveBank.EN).toCharArray()) for (String h : starter.hints(l)) { String t = h.toLowerCase(); banned |= t.contains("israel") || t.contains("إسرائيل") || t.contains("western sahara") || t.contains("الصحراء الغربية"); }
        check("starter: neither Israel nor Western Sahara is offered as a sample answer", !banned);

        // ---- a game
        HiveGame.Settings s = new HiveGame.Settings(); s.alternate = true;   // the older checks below are about the turns alternating (the setting «بالتناوب»)
        HiveGame g = game(small, s, 1, "بت");
        HiveGame g2 = game(small, s, 2);
        boolean drawn = true; Set<Character> third = new HashSet<>();
        for (int seed = 0; seed < 300; seed++) {
            String l = new String(game(small, s, seed).board.letter);
            drawn &= l.chars().distinct().count() == 25 && l.indexOf('ظ') < 0 && l.indexOf('ث') < 0;
            for (char x : HiveBank.AR.toCharArray()) if (x != 'ظ' && x != 'ث' && l.indexOf(x) < 0) third.add(x);
        }
        check("start: a board is 25 different letters of the 28, shuffled differently from game to game; the letters with no question (here ظ and ث) are never shown and the third one left out changes; team 1 picks first",
            drawn && third.size() > 10 && !Arrays.equals(g.board.letter, g2.board.letter) && g.phase() == HiveGame.Phase.PICK && g.turn() == HiveBoard.TEAM_SIDES);
        StringBuilder uneven = new StringBuilder();
        for (char l : HiveBank.AR.toCharArray()) for (int n = 0; n < (l == 'ذ' ? 1 : 6); n++) uneven.append(line("u-" + (int) l + "-" + n, "" + l, "", 2, 0));
        int outShort = 0, outLong = 0;
        for (int seed = 0; seed < 1000; seed++) { String l = new String(game(bank(uneven.toString(), ""), s, seed).board.letter); if (l.indexOf('ذ') < 0) outShort++; if (l.indexOf('ب') < 0) outLong++; }
        check("start: a letter with few questions stays out more often than the others, not always (ذ with one question: out of " + outShort + " games in 1000, ب with six: " + outLong + ")", outShort > 2 * outLong && outShort < 800 && outLong > 20);
        int c = cellOf(g, 'ب');
        check("turn: picking a free hexagon asks a question of its letter, options hidden, 15 seconds on the clock", g.pick(c) && g.phase() == HiveGame.Phase.ASK && g.question().kind == HiveGame.Kind.BANK
            && g.question().item.ar == 'ب' && !g.optionsShown() && g.leftMs() == 15000 && !g.pick(c) && !g.judge(true));
        int rp = g.question().rightPlace();
        check("turn: «خيارات» shows the four options once a turn, the right one in its place in both languages", g.canShowOptions() && g.showOptions() && g.optionsShown() && !g.showOptions()
            && g.question().option(rp, false).equals(g.question().item.a[0]) && g.question().option(rp, true).equals(g.question().item.a[1])
            && new HashSet<>(Arrays.asList(g.question().option(0, false), g.question().option(1, false), g.question().option(2, false), g.question().option(3, false))).size() == 4);
        String firstId = g.question().item.id;
        check("turn: OK shows the answer, «صح» gives the hexagon to the team in turn and the turn passes", g.reveal() && g.phase() == HiveGame.Phase.JUDGE && g.judge(true) && g.board.owner[c] == HiveBoard.TEAM_SIDES
            && g.phase() == HiveGame.Phase.PICK && g.turn() == HiveBoard.TEAM_TOP && !g.pick(c));
        c = cellOf(g, 'ت'); g.pick(c); String missed = g.question().item.id; g.reveal();
        check("turn: «خطأ» leaves the hexagon free and passes the turn", g.judge(false) && g.board.owner[c] == HiveBoard.FREE && g.turn() == HiveBoard.TEAM_SIDES);
        g.pick(c);
        check("no repeats: the other team picking that hexagon gets another question (the answer of the first was shown)", !g.question().item.id.equals(missed));
        String second = g.question().item.id; g.reveal(); g.judge(false); g.pick(c); String third2 = g.question().item.id; g.reveal(); g.judge(false); g.pick(c);
        check("no repeats: three questions of a letter are three different ones; when the letter has run out it asks the category question with its sample answers, which the host judges like any other",
            new HashSet<>(Arrays.asList(missed, second, third2)).size() == 3 && g.question().kind == HiveGame.Kind.CATEGORY && g.question().letter == 'ت' && g.question().hints.length == 2 && g.question().item == null
            && !g.canShowOptions() && g.reveal() && g.judge(false) && g.board.owner[c] == HiveBoard.FREE);
        g.pick(c);
        check("no repeats: ... and that one can be asked again as often as it is missed", g.question().kind == HiveGame.Kind.CATEGORY && g.reveal() && g.judge(true) && g.board.owner[c] == HiveBoard.TEAM_TOP);

        // time, steal
        g = game(small, s, 3, "جد"); c = cellOf(g, 'ج'); g.pick(c); String kept = g.question().item.id;
        now[0] += 14999; g.tick(); boolean before = !g.timeUp() && g.leftMs() == 1;
        now[0] += 1; g.tick();
        check("time: at 15 s the time is up, the question stays (no steal by default) and waits for OK; the host still judges", before && g.timeUp() && g.phase() == HiveGame.Phase.ASK && g.answering() == HiveBoard.TEAM_SIDES
            && g.leftMs() == 0 && g.reveal() && g.judge(true) && g.board.owner[c] == HiveBoard.TEAM_SIDES);
        c = cellOf(g, 'د'); g.pick(c); kept = g.question().item.id;
        check("skip: leaving a question from the pause card passes the turn without showing the answer, and the hexagon keeps that same question for whoever picks it next",
            g.skip() && g.turn() == HiveBoard.TEAM_SIDES && g.board.owner[c] == 0 && g.pick(c) && g.question().item.id.equals(kept) && g.canShowOptions());
        s = new HiveGame.Settings(); s.steal = true; s.seconds = 20; s.alternate = true;
        g = game(small, s, 4, "رز"); c = cellOf(g, 'ر'); g.pick(c); kept = g.question().item.id; g.showOptions();
        now[0] += 20000; g.tick();
        check("steal (a setting): when the time is up the other team may answer the same question with a new time, and no second «خيارات»", g.phase() == HiveGame.Phase.STEAL && g.answering() == HiveBoard.TEAM_TOP
            && g.turn() == HiveBoard.TEAM_SIDES && g.question().item.id.equals(kept) && g.leftMs() == 20000 && !g.canShowOptions());
        check("steal: a right answer gives the hexagon to the team that stole, and the next turn is the other team's as always", g.reveal() && g.judge(true) && g.board.owner[c] == HiveBoard.TEAM_TOP && g.turn() == HiveBoard.TEAM_TOP);
        c = cellOf(g, 'ز'); g.pick(c); now[0] += 20000; g.tick(); now[0] += 20000; g.tick();
        check("steal: when the stealing team's time is up too it waits for OK, it does not bounce back", g.phase() == HiveGame.Phase.STEAL && g.timeUp() && g.answering() == HiveBoard.TEAM_SIDES && g.reveal() && g.judge(false)
            && g.board.owner[c] == 0 && g.turn() == HiveBoard.TEAM_SIDES);

        // who answered (the host picks a team or nobody after the answer is shown)
        s = new HiveGame.Settings();
        g = game(small, s, 7, "جد"); c = cellOf(g, 'ج'); g.pick(c); now[0] += 15000; g.tick(); g.reveal();
        check("who answered: after the time ran out, the other team's answer gives it the hexagon and the point, not the team that picked", g.phase() == HiveGame.Phase.JUDGE && g.answering() == HiveBoard.TEAM_SIDES
            && !g.judge(3) && !g.judge(-1) && g.phase() == HiveGame.Phase.JUDGE && g.judge(HiveBoard.TEAM_TOP) && g.board.owner[c] == HiveBoard.TEAM_TOP);
        c = cellOf(g, 'د'); g.pick(c); g.reveal();
        check("who answered: «ما أحد» leaves the hexagon free for both teams", g.judge(0) && g.board.owner[c] == HiveBoard.FREE && g.phase() == HiveGame.Phase.PICK && g.turn() != 0);
        g = game(small, s, 7, "جد"); c = cellOf(g, 'ج'); g.pick(c); g.reveal();
        check("who answered: the team that picked can be chosen too, and the older yes or no still means the team that was answering", g.judge(HiveBoard.TEAM_SIDES) && g.board.owner[c] == HiveBoard.TEAM_SIDES
            && yesNoOwner(g, cellOf(g, 'د')));

        // who picks the next letter (the default: the team that answered right)
        s = new HiveGame.Settings();
        g = game(small, s, 8, "جد"); c = cellOf(g, 'ج'); g.pick(c); g.reveal(); g.judge(HiveBoard.TEAM_SIDES);
        check("next letter: the team that answered right picks the next one (it was in turn and answered, so it stays in turn)", g.turn() == HiveBoard.TEAM_SIDES && g.phase() == HiveGame.Phase.PICK && g.answering() == HiveBoard.TEAM_SIDES);
        c = cellOf(g, 'د'); g.pick(c); now[0] += 15000; g.tick(); g.reveal(); g.judge(HiveBoard.TEAM_TOP);
        check("next letter: when the other team answered right (the picker's time ran out) that other team picks next", g.turn() == HiveBoard.TEAM_TOP && g.board.owner[c] == HiveBoard.TEAM_TOP);
        c = cellOf(g, 'ر') >= 0 ? cellOf(g, 'ر') : cellOf(g, 'ب'); g.pick(c); g.reveal(); g.judge(0);
        check("next letter: when nobody answered the turn passes to the other team, and the hexagon stays free", g.turn() == HiveBoard.TEAM_SIDES && g.board.owner[c] == HiveBoard.FREE);
        s = new HiveGame.Settings(); s.alternate = true;
        g = game(small, s, 8, "جد"); c = cellOf(g, 'ج'); g.pick(c); g.reveal(); g.judge(HiveBoard.TEAM_SIDES);
        check("next letter: with «بالتناوب» the turn passes to the other team even after a right answer", g.turn() == HiveBoard.TEAM_TOP);
        check("next letter: the default is that the right answer picks", !new HiveGame.Settings().alternate);

        // levels, kids, challenge, English
        s = new HiveGame.Settings(); s.level = HiveGame.LEVEL_MEDIUM;
        g = game(small, s, 5, "س"); c = cellOf(g, 'س'); g.pick(c); boolean medium = g.question().item.level == 2; g.reveal(); g.judge(false); g.pick(c);
        check("level: «متوسط» asks the medium question first, then any level of that letter rather than nothing", medium && g.question().kind == HiveGame.Kind.BANK && g.question().item.level != 2);
        s = new HiveGame.Settings(); s.kids = true; s.challenge = true;
        g = game(small, s, 6, "ش"); c = cellOf(g, 'ش'); g.pick(c); boolean kid = g.question().item.kids; boolean noOptions = !g.canShowOptions() && !g.showOptions(); g.reveal(); g.judge(false); g.pick(c);
        check("kids: only kids questions, and when they run out the free question, never a grown-ups' one; «تحدّي» has no «خيارات»", kid && noOptions && g.question().kind == HiveGame.Kind.NAME);
        s = new HiveGame.Settings(); s.english = true;
        g = game(small, s, 7);
        String shownEn = new String(g.board.letter);
        boolean latin = shownEn.chars().distinct().count() == 25 && shownEn.chars().allMatch(x -> x >= 'A' && x <= 'Z') && shownEn.indexOf('A') >= 0 && shownEn.indexOf('B') >= 0;
        c = cellOf(g, 'A'); g.pick(c); boolean enQ = g.question().kind == HiveGame.Kind.BANK && g.question().item.en == 'A'; g.reveal(); g.judge(false);
        int cz = cellOf(g, shownEn.replace("A", "").replace("B", "").charAt(0)); g.pick(cz);
        check("English: the board shows 25 of A to Z, each once; a hexagon asks a question whose English answer starts with its letter; a letter without one falls back",
            latin && enQ && g.question().kind == HiveGame.Kind.NAME);
        g.reveal(); g.judge(false); int ca = cellOf(g, 'A'); g.pick(ca); g.reveal(); g.judge(false); g.pick(ca);
        check("English: the category question of A carries its English samples", g.board.letter[ca] == 'A' && (g.question().kind == HiveGame.Kind.BANK || g.question().hints[0].equals("Ant")));

        // the winning chain and the lit chain of BOTH teams, through the game itself (every answer right, the other team taking harmless cells): the screen lights exactly chain()
        for (int team = 1; team <= 2; team++) {
            HiveGame.Settings alt = new HiveGame.Settings(); alt.alternate = true;   // this plan is two teams taking turns
            HiveGame w = game(starter, alt, 7 + team);
            int[] plan = team == 1 ? new int[] {10, 11, 12, 13, 14} : new int[] {2, 7, 12, 17, 22};
            int[] filler = team == 1 ? new int[] {0, 1, 3, 4, 20, 21} : new int[] {0, 5, 10, 15, 20, 4};   // team 1 never gets column 4 with column 0 here, team 2 never top with bottom
            int p = 0, f = 0; boolean ok = true;
            while (w.phase() != HiveGame.Phase.WON && p + f < 40) {
                int at = w.turn() == team ? plan[p++] : filler[f++];
                ok &= w.pick(at) && w.reveal() && w.judge(true);
            }
            ok &= w.phase() == HiveGame.Phase.WON && w.winner() == team && Arrays.equals(w.chain(), team == 1 ? plan : plan) && w.chain() != null && w.chain().length == 5 && !w.pick(1);
            for (int i : plan) ok &= w.board.owner[i] == team;
            check("win through the game: team " + team + " joins its sides with five right answers; phase WON, winner " + team + ", chain() is the lit chain in order, and no cell can be picked after", ok);
        }
        // a whole game on the starter, played by the rules until somebody wins
        boolean allEnd = true, noneShown = true; int longest = 0, shownEmpty = 0;
        for (int seed = 0; seed < 300; seed++) {
            HiveGame.Settings st = new HiveGame.Settings(); st.english = seed % 2 == 1; st.kids = seed % 3 == 0; st.level = seed % 3; st.alternate = seed % 4 < 2;
            HiveGame w = game(starter, st, seed);
            if (!st.kids && !st.english) for (char l : w.board.letter) noneShown &= !starter.of(l).isEmpty(); Random r = new Random(seed); Set<String> asked = new HashSet<>(); int turns = 0; boolean ok = true;
            while (w.phase() != HiveGame.Phase.WON && turns < 2000) {
                List<Integer> free = new ArrayList<>();
                for (int i = 0; i < HiveBoard.CELLS; i++) if (w.board.owner[i] == 0) free.add(i);
                int at = free.get(r.nextInt(free.size())), team = w.turn();
                ok &= w.pick(at) && w.question() != null && w.question().letter == w.board.letter[at];
                HiveGame.Question q = w.question();
                if (q.kind == HiveGame.Kind.BANK) { ok &= asked.add(q.item.id) && (st.english ? q.item.en : q.item.ar) == q.letter && (!st.kids || q.item.kids); }
                else ok &= q.kind == HiveGame.Kind.NAME || q.hints.length > 0;
                shownEmpty += starter.of(q.letter).isEmpty() ? 1 : 0;
                boolean yes = r.nextInt(3) > 0;
                ok &= w.reveal() && w.judge(yes) && (w.phase() == HiveGame.Phase.WON || w.turn() == (yes && !st.alternate ? team : 3 - team));
                turns++;
            }
            ok &= w.phase() == HiveGame.Phase.WON && w.winner() == w.board.winner() && w.chain() != null && w.chain().length >= 5 && !w.pick(0);
            for (int i : w.chain()) ok &= w.board.owner[i] == w.winner();
            allEnd &= ok; longest = Math.max(longest, turns);
        }
        check("whole games: 300 random games on the starter (both languages, every level, kids) each end with a winner whose chain joins its sides; never an unplayable hexagon, never a question twice, right letter every time (longest " + longest + " turns)", allEnd);
        check("whole games: in Arabic with all ages, a letter that has no question in the bank at all (ظ ذ ض offline) was never on a board", noneShown);
        System.exit(failed == 0 ? 0 : 1);
    }
}
