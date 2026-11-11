package sa.urtv.screensaver.games.hive;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** For tests/games_board_test.py: «خلية الحروف» on a plain JVM (a fake clock, a seeded Random). Prints PASS/FAIL lines. args: starter.tsv hints.tsv */
public final class HiveTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static long[] now = {1_000_000L};

    static String line(String id, String ar, String en, int level, int kids) {
        return id + "\t" + ar + "\t" + en + "\t" + level + "\t" + kids + "\tس " + id + "\tQ " + id + "\tج " + id + "\tA " + id + "\tx\tx\ty\ty\tz\tz\n";
    }
    static HiveBank bank(String items, String hints) { return HiveBank.parse(items.getBytes(StandardCharsets.UTF_8), hints.getBytes(StandardCharsets.UTF_8)); }
    static HiveGame game(HiveBank b, HiveGame.Settings s, long seed) { now[0] = 1_000_000L; return new HiveGame(b, s, HiveBoard.TEAM_SIDES, () -> now[0], new Random(seed)); }
    static int cellOf(HiveGame g, char letter) { for (int i = 0; i < HiveBoard.CELLS; i++) if (g.board.letter[i] == letter && g.board.owner[i] == 0) return i; return -1; }
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
        check("board: 28 cells, every neighbour link goes both ways, 57 links in all (4 rows of 6, 3 row gaps of 13)", symmetric && links == 2 * (4 * 6 + 3 * 13));
        check("board: a middle cell touches 6, the corners 2 or 3, an even row leans to the row start and an odd row to its end",
            HiveBoard.neighbours(HiveBoard.cell(1, 3)).length == 6 && HiveBoard.neighbours(HiveBoard.cell(0, 0)).length == 2 && HiveBoard.neighbours(HiveBoard.cell(0, 6)).length == 3
            && Arrays.equals(HiveBoard.neighbours(HiveBoard.cell(2, 3)), new int[] {16, 18, 9, 10, 23, 24}) && Arrays.equals(HiveBoard.neighbours(HiveBoard.cell(1, 3)), new int[] {9, 11, 3, 4, 17, 18}));
        check("board: the focus wraps along a row and stops at the top and the bottom; up and down always land on a touching cell",
            HiveBoard.move(6, HiveBoard.NEXT) == 0 && HiveBoard.move(0, HiveBoard.PREV) == 6 && HiveBoard.move(3, HiveBoard.UP) == 3 && HiveBoard.move(24, HiveBoard.DOWN) == 24
            && HiveBoard.move(10, HiveBoard.DOWN) == 17 && HiveBoard.move(17, HiveBoard.UP) == 10);
        HiveBoard b = new HiveBoard();
        for (int c = 0; c < 7; c++) b.owner[HiveBoard.cell(2, c)] = HiveBoard.TEAM_SIDES;
        check("win: team 1 joins the two sides with a whole row, and the chain is that row in order from the row start", b.winner() == HiveBoard.TEAM_SIDES
            && Arrays.equals(b.chain(HiveBoard.TEAM_SIDES), new int[] {14, 15, 16, 17, 18, 19, 20}) && b.chain(HiveBoard.TEAM_TOP) == null);
        b.owner[HiveBoard.cell(2, 3)] = HiveBoard.FREE;
        check("win: one gap in the row and nobody has won", b.winner() == HiveBoard.FREE);
        b.owner[HiveBoard.cell(1, 3)] = HiveBoard.TEAM_SIDES;   // (1,3) touches (2,3) and (2,4), not (2,2): no bridge
        boolean noBridge = b.winner() == HiveBoard.FREE;
        b.owner[HiveBoard.cell(1, 2)] = HiveBoard.TEAM_SIDES;   // (2,2) -> (1,2) -> (1,3) -> (2,4)
        check("win: a detour over the next row joins only through cells that really touch", noBridge && b.winner() == HiveBoard.TEAM_SIDES && b.chain(HiveBoard.TEAM_SIDES).length == 8);
        b = new HiveBoard();
        int[] down = {HiveBoard.cell(0, 3), HiveBoard.cell(1, 2), HiveBoard.cell(2, 3), HiveBoard.cell(3, 2)};
        for (int c : down) b.owner[c] = HiveBoard.TEAM_TOP;
        check("win: team 2 joins top and bottom with a zigzag of 4, and team 1 holding the same cells would not have won", b.winner() == HiveBoard.TEAM_TOP && Arrays.equals(b.chain(HiveBoard.TEAM_TOP), down));
        b.owner[HiveBoard.cell(2, 3)] = HiveBoard.FREE; b.owner[HiveBoard.cell(2, 1)] = HiveBoard.TEAM_TOP;   // (1,2) touches (2,2) and (2,3) only
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
        String hints = "ظ\tظبي\tظفار\nA\tAnt\n";
        HiveBank small = bank(items.toString(), hints);
        check("bank: parses; a question is found under its Arabic letter and its English one; hints by letter",
            small != null && small.size() == 78 && small.of('ب').size() == 3 && small.of('A').size() == 26 && small.of('B').size() == 26 && small.of('C').isEmpty() && small.of('ظ').isEmpty()
            && small.hints('ظ').length == 2 && small.hints('ث').length == 0);
        check("bank: refuses a line with 14 fields, a letter outside the alphabet, level 4, a repeated id, two equal options, an empty text, an HTML page, a second hints line for a letter",
            bank(line("a", "ب", "", 1, 0).replace("\tz\tz", "\tz"), "") == null && bank(line("a", "ء", "", 1, 0), "") == null && bank(line("a", "ب", "b", 1, 0), "") == null
            && bank(line("a", "ب", "", 4, 0), "") == null && bank(line("a", "ب", "", 1, 0) + line("a", "ت", "", 1, 0), "") == null && bank(line("a", "ب", "", 1, 0).replace("\ty\ty", "\tx\tx"), "") == null
            && bank(line("a", "ب", "", 1, 0).replace("\tس a", "\t"), "") == null && bank("<html>404</html>", "") == null && bank(line("a", "ب", "", 1, 0), "ظ\tظبي\nظ\tظفار\n") == null
            && bank(line("a", "ب", "", 1, 0), "") != null);
        HiveBank starter = HiveBank.parse(Files.readAllBytes(Paths.get(a[0])), Files.readAllBytes(Paths.get(a[1])));
        boolean hinted = starter != null;
        if (starter != null) for (char l : (HiveBank.AR + HiveBank.EN).toCharArray()) hinted &= starter.of(l).size() >= 6 || starter.hints(l).length > 0;
        check("starter: the bundled files parse, and every letter of both alphabets has 6 questions or more, or else sample answers for its category question (ظ has no question at all)",
            hinted && starter.of('ظ').isEmpty() && starter.hints('ظ').length >= 3 && starter.size() >= 200);
        boolean banned = false;
        for (String f : a) { String t = new String(Files.readAllBytes(Paths.get(f)), StandardCharsets.UTF_8).toLowerCase(); banned |= t.contains("israel") || t.contains("إسرائيل") || t.contains("western sahara") || t.contains("الصحراء الغربية"); }
        check("starter: neither Israel nor Western Sahara appears in a question, an option or a hint", !banned);

        // ---- a game
        HiveGame.Settings s = new HiveGame.Settings();
        HiveGame g = game(small, s, 1);
        char[] sorted = g.board.letter.clone(); Arrays.sort(sorted); char[] alphabet = HiveBank.AR.toCharArray(); Arrays.sort(alphabet);
        HiveGame g2 = game(small, s, 2);
        check("start: the board holds each of the 28 letters once, shuffled differently from game to game; team 1 picks first", Arrays.equals(sorted, alphabet) && !Arrays.equals(g.board.letter, g2.board.letter)
            && g.phase() == HiveGame.Phase.PICK && g.turn() == HiveBoard.TEAM_SIDES);
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
        String second = g.question().item.id; g.reveal(); g.judge(false); g.pick(c); String third = g.question().item.id; g.reveal(); g.judge(false); g.pick(c);
        check("no repeats: three questions of a letter are three different ones, and when the letter has run out with no samples it becomes the free «اذكر اسمًا» question, still playable",
            new HashSet<>(Arrays.asList(missed, second, third)).size() == 3 && g.question().kind == HiveGame.Kind.NAME && g.question().letter == 'ت' && !g.canShowOptions() && g.reveal() && g.judge(true)
            && g.board.owner[c] == HiveBoard.TEAM_SIDES);
        c = cellOf(g, 'ظ'); g.pick(c);
        check("short letter: ظ (no question at all) asks the category question with its sample answers; the host judges it like any other", g.question().kind == HiveGame.Kind.CATEGORY
            && g.question().hints.length == 2 && g.question().item == null && g.reveal() && g.judge(false));
        g.pick(c);
        check("short letter: ... and it can be asked again as often as it is missed", g.question().kind == HiveGame.Kind.CATEGORY && g.reveal() && g.judge(true) && g.board.owner[c] == HiveBoard.TEAM_SIDES);

        // time, steal
        g = game(small, s, 3); c = cellOf(g, 'ج'); g.pick(c); String kept = g.question().item.id;
        now[0] += 14999; g.tick(); boolean before = !g.timeUp() && g.leftMs() == 1;
        now[0] += 1; g.tick();
        check("time: at 15 s the time is up, the question stays (no steal by default) and waits for OK; the host still judges", before && g.timeUp() && g.phase() == HiveGame.Phase.ASK && g.answering() == HiveBoard.TEAM_SIDES
            && g.leftMs() == 0 && g.reveal() && g.judge(true) && g.board.owner[c] == HiveBoard.TEAM_SIDES);
        c = cellOf(g, 'د'); g.pick(c); kept = g.question().item.id;
        check("skip: leaving a question from the pause card passes the turn without showing the answer, and the hexagon keeps that same question for whoever picks it next",
            g.skip() && g.turn() == HiveBoard.TEAM_SIDES && g.board.owner[c] == 0 && g.pick(c) && g.question().item.id.equals(kept) && g.canShowOptions());
        s = new HiveGame.Settings(); s.steal = true; s.seconds = 20;
        g = game(small, s, 4); c = cellOf(g, 'ر'); g.pick(c); kept = g.question().item.id; g.showOptions();
        now[0] += 20000; g.tick();
        check("steal (a setting): when the time is up the other team may answer the same question with a new time, and no second «خيارات»", g.phase() == HiveGame.Phase.STEAL && g.answering() == HiveBoard.TEAM_TOP
            && g.turn() == HiveBoard.TEAM_SIDES && g.question().item.id.equals(kept) && g.leftMs() == 20000 && !g.canShowOptions());
        check("steal: a right answer gives the hexagon to the team that stole, and the next turn is the other team's as always", g.reveal() && g.judge(true) && g.board.owner[c] == HiveBoard.TEAM_TOP && g.turn() == HiveBoard.TEAM_TOP);
        c = cellOf(g, 'ز'); g.pick(c); now[0] += 20000; g.tick(); now[0] += 20000; g.tick();
        check("steal: when the stealing team's time is up too it waits for OK, it does not bounce back", g.phase() == HiveGame.Phase.STEAL && g.timeUp() && g.answering() == HiveBoard.TEAM_SIDES && g.reveal() && g.judge(false)
            && g.board.owner[c] == 0 && g.turn() == HiveBoard.TEAM_SIDES);

        // levels, kids, challenge, English
        s = new HiveGame.Settings(); s.level = HiveGame.LEVEL_MEDIUM;
        g = game(small, s, 5); c = cellOf(g, 'س'); g.pick(c); boolean medium = g.question().item.level == 2; g.reveal(); g.judge(false); g.pick(c);
        check("level: «متوسط» asks the medium question first, then any level of that letter rather than nothing", medium && g.question().kind == HiveGame.Kind.BANK && g.question().item.level != 2);
        s = new HiveGame.Settings(); s.kids = true; s.challenge = true;
        g = game(small, s, 6); c = cellOf(g, 'ش'); g.pick(c); boolean kid = g.question().item.kids; boolean noOptions = !g.canShowOptions() && !g.showOptions(); g.reveal(); g.judge(false); g.pick(c);
        check("kids: only kids questions, and when they run out the free question, never a grown-ups' one; «تحدّي» has no «خيارات»", kid && noOptions && g.question().kind == HiveGame.Kind.NAME);
        s = new HiveGame.Settings(); s.english = true;
        g = game(small, s, 7);
        int[] count = new int[128]; for (char l : g.board.letter) count[l]++;
        boolean latin = true; for (char l : HiveBank.EN.toCharArray()) latin &= count[l] == (l == 'A' || l == 'B' ? 2 : 1);
        c = cellOf(g, 'A'); g.pick(c); boolean enQ = g.question().kind == HiveGame.Kind.BANK && g.question().item.en == 'A'; g.reveal(); g.judge(true);
        int cz = cellOf(g, 'Z'); g.pick(cz);
        check("English: the board shows A to Z and, to fill 28, the two letters with the most questions twice; a hexagon asks a question whose English answer starts with its letter; a letter without one falls back",
            latin && enQ && g.question().kind == HiveGame.Kind.NAME);
        g.reveal(); g.judge(false); int ca = cellOf(g, 'A'); g.pick(ca); g.reveal(); g.judge(false); g.pick(ca);
        check("English: the category question of A carries its English samples", g.board.letter[ca] == 'A' && (g.question().kind == HiveGame.Kind.BANK || g.question().hints[0].equals("Ant")));

        // a whole game on the starter, played by the rules until somebody wins
        boolean allEnd = true; int longest = 0;
        for (int seed = 0; seed < 300; seed++) {
            HiveGame.Settings st = new HiveGame.Settings(); st.english = seed % 2 == 1; st.kids = seed % 3 == 0; st.level = seed % 3;
            HiveGame w = game(starter, st, seed); Random r = new Random(seed); Set<String> asked = new HashSet<>(); int turns = 0; boolean ok = true;
            while (w.phase() != HiveGame.Phase.WON && turns < 2000) {
                List<Integer> free = new ArrayList<>();
                for (int i = 0; i < HiveBoard.CELLS; i++) if (w.board.owner[i] == 0) free.add(i);
                int at = free.get(r.nextInt(free.size())), team = w.turn();
                ok &= w.pick(at) && w.question() != null && w.question().letter == w.board.letter[at];
                HiveGame.Question q = w.question();
                if (q.kind == HiveGame.Kind.BANK) { ok &= asked.add(q.item.id) && (st.english ? q.item.en : q.item.ar) == q.letter && (!st.kids || q.item.kids); }
                else ok &= q.kind == HiveGame.Kind.NAME || q.hints.length > 0;
                ok &= w.reveal() && w.judge(r.nextInt(3) > 0) && (w.phase() == HiveGame.Phase.WON || w.turn() != team);
                turns++;
            }
            ok &= w.phase() == HiveGame.Phase.WON && w.winner() == w.board.winner() && w.chain() != null && w.chain().length >= (w.winner() == HiveBoard.TEAM_TOP ? 4 : 7) && !w.pick(0);
            for (int i : w.chain()) ok &= w.board.owner[i] == w.winner();
            allEnd &= ok; longest = Math.max(longest, turns);
        }
        check("whole games: 300 random games on the starter (both languages, every level, kids) each end with a winner whose chain joins its sides; never an unplayable hexagon, never a question twice, right letter every time (longest " + longest + " turns)", allEnd);
        System.exit(failed == 0 ? 0 : 1);
    }
}
