package sa.urtv.screensaver.games.hive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * «خلية الحروف»: one game's rules (no Android, a clock and a Random are handed in; tests/HiveTest.java). games/letter-hive/SPEC.md, the remote-only flow:
 *
 *   PICK   the team in turn chooses a free hexagon.
 *   ASK    its question is up, the options hidden; «خيارات» shows them once a turn (never in «تحدّي»). The team answers aloud while the time runs. When the time
 *          is up: with the steal on, the other team may answer the same question (STEAL, a new time); otherwise the screen says so and waits for OK.
 *   JUDGE  OK showed the answer: «صح» gives the hexagon to the team that answered, «خطأ» leaves it free. Either way the turn passes to the other team.
 *   WON    a team's two sides are joined; chain() is what to light up.
 *
 * The board: 25 letters drawn for each game from the alphabet (three Arabic letters stay out, one English). A letter with no question for this game's filter
 * stays out first, then the letters with few questions more often than the rest.
 *
 * A question is never shown twice in a game. One that was asked but whose answer was NOT shown stays on its hexagon for whoever picks it next; once the answer
 * was shown a missed hexagon gets a new question. A letter with no question left is still playable: first any level, then the category
 * question (the host judges, sample answers as a hint), and where we have no samples either, the free «اذكر اسمًا» question.
 */
public final class HiveGame {
    public enum Phase { PICK, ASK, STEAL, JUDGE, WON }
    public enum Kind { BANK, CATEGORY, NAME }

    public static final int LEVEL_MIXED = 0, LEVEL_EASY = 1, LEVEL_MEDIUM = 2;
    public static final int[] SECONDS = {15, 20, 30};

    public static final class Settings {
        public int level = LEVEL_MIXED, seconds = 15;
        public boolean kids, challenge, steal, english;
    }

    public static final class Question {
        public final Kind kind; public final char letter;
        /** BANK only: the item, and where each of the four options sits (0 is the right answer, 1 to 3 the wrong ones) */
        public final HiveBank.Item item; public final int[] order;
        /** CATEGORY only: sample answers */
        public final String[] hints;
        Question(Kind kind, char letter, HiveBank.Item item, int[] order, String[] hints) { this.kind = kind; this.letter = letter; this.item = item; this.order = order; this.hints = hints; }
        public String option(int place, boolean english) { int o = order[place]; return o == 0 ? item.a[english ? 1 : 0] : item.w[o - 1][english ? 1 : 0]; }
        public int rightPlace() { for (int i = 0; i < 4; i++) if (order[i] == 0) return i; return -1; }
    }

    public final HiveBoard board = new HiveBoard();
    public final Settings settings;
    private final HiveBank bank; private final LongSupplier clock; private final Random rnd;
    private final Set<String> used = new HashSet<>();
    private final Question[] kept = new Question[HiveBoard.CELLS];

    private Phase phase = Phase.PICK;
    private int turn, answering, cell = -1, winner;
    private Question question;
    private boolean optionsShown, optionsUsed, timeUp;
    private long deadline;
    private int[] chain;

    public HiveGame(HiveBank bank, Settings settings, int firstTeam, LongSupplier clock, Random rnd) {
        this.bank = bank; this.settings = settings; this.clock = clock; this.rnd = rnd;
        this.turn = firstTeam == HiveBoard.TEAM_TOP ? HiveBoard.TEAM_TOP : HiveBoard.TEAM_SIDES;
        char[] letters = draw(bank, settings, rnd);
        for (int i = 0; i < HiveBoard.CELLS; i++) board.letter[i] = letters[i];
    }

    private static int count(HiveBank bank, char letter, boolean kids) {
        int n = 0;
        for (HiveBank.Item it : bank.of(letter)) if (!kids || it.kids) n++;
        return n;
    }

    /** the 25 letters of a game, shuffled: the letters with no question for this filter are left out first, then a letter is the likelier to stay out the fewer questions it has */
    public static char[] draw(HiveBank bank, Settings settings, Random rnd) {
        List<Character> in = new ArrayList<>(), none = new ArrayList<>();
        for (char c : (settings.english ? HiveBank.EN : HiveBank.AR).toCharArray()) (count(bank, c, settings.kids) == 0 ? none : in).add(c);
        Collections.shuffle(none, rnd);
        while (in.size() < HiveBoard.CELLS) in.add(none.remove(none.size() - 1));   // more empty letters than may stay out: some play with the category question
        while (in.size() > HiveBoard.CELLS) {
            double total = 0;
            for (char c : in) total += 1.0 / (1 + count(bank, c, settings.kids));
            double at = rnd.nextDouble() * total;
            int out = in.size() - 1;
            for (int i = 0; i < in.size(); i++) { at -= 1.0 / (1 + count(bank, in.get(i), settings.kids)); if (at < 0) { out = i; break; } }
            in.remove(out);
        }
        Collections.shuffle(in, rnd);
        char[] letters = new char[HiveBoard.CELLS];
        for (int i = 0; i < letters.length; i++) letters[i] = in.get(i);
        return letters;
    }

    public Phase phase() { return phase; }
    /** the team whose turn it is (it picks) */
    public int turn() { return turn; }
    /** the team that answers now: the one in turn, or the other one during a steal */
    public int answering() { return answering; }
    public int cell() { return cell; }
    public Question question() { return question; }
    public boolean optionsShown() { return optionsShown; }
    public boolean canShowOptions() {
        return (phase == Phase.ASK || phase == Phase.STEAL) && !settings.challenge && !optionsUsed && question != null && question.kind == Kind.BANK;
    }
    public boolean timeUp() { return timeUp; }
    public int winner() { return winner; }
    public int[] chain() { return chain; }
    public long leftMs() { return phase == Phase.ASK || phase == Phase.STEAL ? Math.max(0, deadline - clock.getAsLong()) : 0; }

    private static int other(int team) { return team == HiveBoard.TEAM_SIDES ? HiveBoard.TEAM_TOP : HiveBoard.TEAM_SIDES; }

    /** the team in turn takes a free hexagon: its question comes up and the time starts */
    public boolean pick(int at) {
        if (phase != Phase.PICK || at < 0 || at >= HiveBoard.CELLS || board.owner[at] != HiveBoard.FREE) return false;
        cell = at; answering = turn; optionsShown = false; optionsUsed = false; timeUp = false;
        question = kept[at] != null ? kept[at] : next(board.letter[at]);
        kept[at] = question;
        phase = Phase.ASK;
        deadline = clock.getAsLong() + settings.seconds * 1000L;
        return true;
    }

    private Question next(char letter) {
        List<HiveBank.Item> level = new ArrayList<>(), any = new ArrayList<>();
        for (HiveBank.Item it : bank.of(letter)) {
            if (used.contains(it.id) || (settings.kids && !it.kids)) continue;
            any.add(it);
            if (settings.level == LEVEL_MIXED || it.level == settings.level) level.add(it);
        }
        List<HiveBank.Item> pool = level.isEmpty() ? any : level;
        if (!pool.isEmpty()) {
            HiveBank.Item it = pool.get(rnd.nextInt(pool.size()));
            used.add(it.id);
            List<Integer> order = new ArrayList<>(java.util.Arrays.asList(0, 1, 2, 3));
            Collections.shuffle(order, rnd);
            return new Question(Kind.BANK, letter, it, new int[] {order.get(0), order.get(1), order.get(2), order.get(3)}, null);
        }
        String[] hints = bank.hints(letter);
        return new Question(hints.length > 0 ? Kind.CATEGORY : Kind.NAME, letter, null, null, hints);
    }

    /** «خيارات»: the four options, once a turn */
    public boolean showOptions() {
        if (!canShowOptions()) return false;
        optionsShown = true; optionsUsed = true;
        return true;
    }

    /** call it a few times a second: the time running out */
    public void tick() {
        if ((phase != Phase.ASK && phase != Phase.STEAL) || timeUp || clock.getAsLong() < deadline) return;
        if (phase == Phase.ASK && settings.steal) {
            phase = Phase.STEAL; answering = other(turn);
            deadline = clock.getAsLong() + settings.seconds * 1000L;
        } else timeUp = true;
    }

    /** OK: the answer is shown, the host judges what the answering team said */
    public boolean reveal() {
        if (phase != Phase.ASK && phase != Phase.STEAL) return false;
        phase = Phase.JUDGE;
        kept[cell] = null;   // its answer has been seen: this hexagon gets another question if it stays free
        return true;
    }

    /** «صح» or «خطأ» */
    public boolean judge(boolean right) {
        if (phase != Phase.JUDGE) return false;
        if (right) {
            board.owner[cell] = answering;
            int w = board.winner();
            if (w != HiveBoard.FREE) { winner = w; chain = board.chain(w); phase = Phase.WON; return true; }
        }
        endTurn();
        return true;
    }

    /** the pause card's way out of a question: nobody answers, the answer was not shown, the question stays on its hexagon, the turn passes */
    public boolean skip() {
        if (phase != Phase.ASK && phase != Phase.STEAL) return false;
        endTurn();
        return true;
    }

    private void endTurn() {
        turn = other(turn); answering = turn; cell = -1; question = null; optionsShown = false; timeUp = false;
        phase = Phase.PICK;
    }
}
