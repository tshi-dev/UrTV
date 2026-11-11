package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** «الأسرع»: which questions a game uses (no Android, tested on a plain JVM). games/live-quiz/SPEC.md «Data»: a category is offered for a level and filter only with
 *  at least 40 items; «مزيج» always; never a question twice in a game, and not one of the last 100 asked on this TV unless nothing else is left. */
final class QuizPick {
    private QuizPick() {}

    static final int MIN_ITEMS = 40, RECENT_MAX = 100;
    static final String MIX = "mix";
    /** the category chips, in order: key (the bank's category), Arabic name */
    static final String[][] CATEGORIES = {
        {MIX, "مزيج"}, {"geography", "جغرافيا"}, {"science", "علوم"}, {"islamic", "إسلامية"}, {"maths-logic", "رياضيات ومنطق"}, {"animals", "حيوانات"}, {"saudi-gulf", "السعودية والخليج"}};
    /** the level chips: 0 any («متنوع»), 1 easy, 2 medium, 3 hard */
    static final String[] LEVELS = {"سهل", "متوسط", "صعب", "متنوع"};
    static final int[] LEVEL_VALUE = {1, 2, 3, 0};
    static final int[] COUNTS = {5, 10, 15};

    static final class Item {
        final String id, cat; final int diff; final boolean kids; final String[] q, a; final String[][] w;
        Item(String id, String cat, int diff, boolean kids, String[] q, String[] a, String[][] w) { this.id = id; this.cat = cat; this.diff = diff; this.kids = kids; this.q = q; this.a = a; this.w = w; }
    }

    static boolean matches(Item it, String cat, int level, boolean kids) {
        return (MIX.equals(cat) || it.cat.equals(cat)) && (level == 0 || it.diff == level) && (!kids || it.kids);
    }

    static int available(List<Item> bank, String cat, int level, boolean kids) {
        int n = 0;
        for (Item it : bank) if (matches(it, cat, level, kids)) n++;
        return n;
    }

    /** a category chip is offered when it holds at least 40 items for this level and filter; «مزيج» is offered whenever there is a bank */
    static boolean offered(List<Item> bank, String cat, int level, boolean kids, int wanted) {
        int n = available(bank, cat, level, kids);
        return MIX.equals(cat) ? n >= Math.min(wanted, MIN_ITEMS) : n >= MIN_ITEMS;
    }

    /** `count` items: matching ones first, never the same twice, the recent ones only when too few others remain */
    static List<Item> pick(List<Item> bank, String cat, int level, boolean kids, int count, Set<String> recent, Random rnd) {
        List<Item> fresh = new ArrayList<>(), old = new ArrayList<>();
        for (Item it : bank) if (matches(it, cat, level, kids)) (recent != null && recent.contains(it.id) ? old : fresh).add(it);
        Collections.shuffle(fresh, rnd); Collections.shuffle(old, rnd);
        List<Item> out = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (Item it : fresh) if (out.size() < count && seen.add(it.id)) out.add(it);
        for (Item it : old) if (out.size() < count && seen.add(it.id)) out.add(it);
        return out;
    }

    /** the question in BOTH languages, the four options in one random order (the same place for the right one in each): the TV shows its own language, each phone its own */
    static QuizRoom.Q toQ(Item it, Random rnd) {
        List<Integer> order = new ArrayList<>(java.util.Arrays.asList(0, 1, 2, 3));   // 0 is the right answer, 1 to 3 the wrong ones
        Collections.shuffle(order, rnd);
        String[][] by = new String[2][4];
        for (int l = 0; l < 2; l++) for (int p = 0; p < 4; p++) by[l][p] = order.get(p) == 0 ? it.a[l] : it.w[order.get(p) - 1][l];
        return new QuizRoom.Q(new String[] {it.q[0], it.q[1]}, by, order.indexOf(0));
    }

    /** an item may be played in an English room only when its English text is really English: present, and with no Arabic letters in the question or any answer (never shown in Arabic instead) */
    static boolean playable(Item it, boolean en) {
        if (!en) return true;
        if (!clean(it.q[1]) || !clean(it.a[1])) return false;
        for (String[] w : it.w) if (!clean(w[1])) return false;
        return true;
    }
    private static boolean clean(String s) {
        if (s == null || s.trim().isEmpty() || s.length() > 200) return false;   // QuizBankCheck.TEXT_MAX
        for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); if (c >= 0x0600 && c <= 0x06FF) return false; }
        return true;
    }
    /** the bank as this room can play it */
    static List<Item> forLang(List<Item> bank, boolean en) {
        if (!en) return bank;
        List<Item> out = new ArrayList<>();
        for (Item it : bank) if (playable(it, true)) out.add(it);
        return out;
    }
}
