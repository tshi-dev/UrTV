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

    /** the room's question in the screen's language: the 4 options in a random order, `correct` the place of the answer */
    static QuizRoom.Q toQ(Item it, boolean en, Random rnd) {
        int l = en ? 1 : 0;
        List<String> opts = new ArrayList<>();
        opts.add(it.a[l]);
        for (String[] w : it.w) opts.add(w[l]);
        String right = it.a[l];
        Collections.shuffle(opts, rnd);
        return new QuizRoom.Q(it.q[l], opts.toArray(new String[0]), opts.indexOf(right));
    }
}
