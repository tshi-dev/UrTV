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
 *   JUDGE  OK showed the answer: the host says WHO answered, a team or nobody (judge(who)). The hexagon and the point go to that team (a team whose time ran out and
 *          whose rival answered gets nothing); nobody leaves it free. Then the turn: the team that answered right picks the next letter
 *          (the default), or when nobody answered it passes to the other team; with Settings.alternate the turn passes to the other team every time.
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
        /** false (the default): the team that answered right picks the next letter, and when nobody did the turn passes to the other team. true: the turns alternate whoever answered. */
        public boolean alternate;
    }

    public static final class Question {
        public final Kind kind; public final char letter;
        /** BANK only: the item, and where each of the four options sits (0 is the right answer, 1 to 3 the wrong ones) */
        public final HiveBank.Item item; public final int[] order;
        /** CATEGORY only: sample answers */
        public final String[] hints;
        /** BANK only: the wrong choices ({ar, en} each): answers of other questions of the SAME letter (and the same category first), never the answer, never twice */
        public final String[][] wrong;
        Question(Kind kind, char letter, HiveBank.Item item, int[] order, String[] hints, String[][] wrong) { this.kind = kind; this.letter = letter; this.item = item; this.order = order; this.hints = hints; this.wrong = wrong; }
        /** how many choices «خيارات» shows: 0 (an open answer: the letter has too few answers of its own), else MIN_CHOICES to MAX_CHOICES */
        public int choices() { return order == null || order.length < MIN_CHOICES ? 0 : order.length; }
        public String option(int place, boolean english) { int o = order[place]; return o == 0 ? item.a[english ? 1 : 0] : wrong[o - 1][english ? 1 : 0]; }
        public int rightPlace() { for (int i = 0; i < order.length; i++) if (order[i] == 0) return i; return -1; }
    }

    public final HiveBoard board = new HiveBoard();
    public final Settings settings;
    private final HiveBank bank; private final LongSupplier clock; private final Random rnd;
    private final Set<String> used = new HashSet<>();
    private final Set<String> recent = new HashSet<>();
    private final List<String> asked = new ArrayList<>();
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

    /** the ids asked in the last games on this TV: they come up again only when a letter has nothing else */
    public void avoid(java.util.Collection<String> ids) { recent.clear(); if (ids != null) recent.addAll(ids); }
    /** the ids of the bank questions this game has asked, in order */
    public List<String> asked() { return asked; }
    public Phase phase() { return phase; }
    /** the team whose turn it is (it picks) */
    public int turn() { return turn; }
    /** the team that answers now: the one in turn, or the other one during a steal */
    public int answering() { return answering; }
    public int cell() { return cell; }
    public Question question() { return question; }
    public boolean optionsShown() { return optionsShown; }
    public boolean canShowOptions() {
        return (phase == Phase.ASK || phase == Phase.STEAL) && !settings.challenge && !optionsUsed && question != null && question.kind == Kind.BANK && question.choices() >= MIN_CHOICES;
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
            if (used.contains(it.id) || (settings.kids && !it.kids) || !HiveBank.starts(letter, it.a[settings.english ? 1 : 0])) continue;   // never a question whose answer is not of the cell's letter (an index error would mix letters)
            any.add(it);
            if (settings.level == LEVEL_MIXED || it.level == settings.level) level.add(it);
        }
        List<HiveBank.Item> pool = level.isEmpty() ? any : level;
        List<HiveBank.Item> fresh = new ArrayList<>();
        for (HiveBank.Item it : pool) if (!recent.contains(it.id)) fresh.add(it);
        if (!fresh.isEmpty()) pool = fresh;   // one asked in the last games on this TV only when nothing else is left
        if (!pool.isEmpty()) {
            HiveBank.Item it = pool.get(rnd.nextInt(pool.size()));
            used.add(it.id); asked.add(it.id);
            String[][] wrong = wrongChoices(bank, it, letter, settings.english, settings.kids, rnd);
            if (wrong.length + 1 < MIN_CHOICES) return new Question(Kind.BANK, letter, it, new int[] {0}, null, wrong);   // too few answers of this letter: an open answer
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i <= wrong.length; i++) order.add(i);
            Collections.shuffle(order, rnd);
            int[] o = new int[order.size()]; for (int i = 0; i < o.length; i++) o[i] = order.get(i);
            return new Question(Kind.BANK, letter, it, o, null, wrong);
        }
        String[] hints = bank.hints(letter);
        return new Question(hints.length > 0 ? Kind.CATEGORY : Kind.NAME, letter, null, null, hints, null);
    }

    /** the fewest and the most choices «خيارات» shows (the right one included): every choice starts with the cell's letter, so with fewer than MIN_CHOICES such answers the cell is answered open */
    public static final int MIN_CHOICES = 3, MAX_CHOICES = 4;

    /**
     * The wrong choices of a question: answers of OTHER questions that start with the same letter (HiveBank.starts, the letter index's own folding; the question's language), the same
     * category first, then any category; never the right answer or the same text twice (compared folded); a kids game only from kids' questions. Up to MAX_CHOICES - 1; fewer when the letter has no more.
     */
    public static String[][] wrongChoices(HiveBank bank, HiveBank.Item it, char letter, boolean english, boolean kids, Random rnd) {
        int l = english ? 1 : 0;
        List<String[]> same = new ArrayList<>(), other = new ArrayList<>(); Set<String> seen = new HashSet<>();
        seen.add(fold(it.a[l]));
        for (HiveBank.Item o : bank.of(letter)) {
            if (o == it || o.id.equals(it.id) || (kids && !o.kids)) continue;
            String t = o.a[l];
            if (t == null || t.isEmpty() || !HiveBank.starts(letter, t) || !seen.add(fold(t))) continue;
            (o.cat.equals(it.cat) ? same : other).add(new String[] {o.a[0], o.a[1]});
        }
        Collections.shuffle(same, rnd); Collections.shuffle(other, rnd); same.addAll(other);
        return same.subList(0, Math.min(MAX_CHOICES - 1, same.size())).toArray(new String[0][]);
    }
    private static String fold(String s) { return s.replaceAll("[\\u064B-\\u0670\\u0640]", "").replace('\u0623', '\u0627').replace('\u0625', '\u0627').replace('\u0622', '\u0627').replace('\u0629', '\u0647').replace('\u0649', '\u064A').trim().toLowerCase(java.util.Locale.ROOT); }

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

    /** «من جاوب؟»: `who` is the team that answered right (TEAM_SIDES or TEAM_TOP), or 0 for nobody. The hexagon goes to that team. */
    public boolean judge(int who) {
        if (phase != Phase.JUDGE || (who != 0 && who != HiveBoard.TEAM_SIDES && who != HiveBoard.TEAM_TOP)) return false;
        if (who != 0) {
            board.owner[cell] = who;
            int w = board.winner();
            if (w != HiveBoard.FREE) { winner = w; chain = board.chain(w); phase = Phase.WON; return true; }
        }
        endTurn(who != 0 && !settings.alternate ? who : other(turn));
        return true;
    }

    /** the older yes or no: right means the team that was answering (the one in turn, or the one that stole), wrong means nobody */
    public boolean judge(boolean right) { return judge(right ? answering : 0); }

    /** the pause card's way out of a question: nobody answers, the answer was not shown, the question stays on its hexagon, the turn passes */
    public boolean skip() {
        if (phase != Phase.ASK && phase != Phase.STEAL) return false;
        endTurn(other(turn));
        return true;
    }

    private void endTurn(int next) {
        turn = next; answering = turn; cell = -1; question = null; optionsShown = false; timeUp = false;
        phase = Phase.PICK;
    }
}
