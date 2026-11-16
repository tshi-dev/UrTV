package sa.urtv.home.tilawah;

/** tests/surah_set_test.py runs this: one line per case, «name<TAB>ok|bad». */
public class SurahSetRun {
    static void out(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "bad")); }

    public static void main(String[] a) {
        int[] none = SurahSet.clean(null);
        out("no list: all 114, as every reciter before", none.length == 0 && SurahSet.count(none) == 114 && SurahSet.has(1, none) && SurahSet.has(114, none));
        out("no list: next and previous as before (wrap at 114)", SurahSet.next(5, none) == 6 && SurahSet.next(114, none) == 1 && SurahSet.prev(1, none) == 114 && SurahSet.prev(6, none) == 5);

        int[] m = SurahSet.clean(new int[]{7, 3, 3, 0, 115, -1, 114, 4});
        out("clean: sorted, in range, no repeats", java.util.Arrays.equals(m, new int[]{3, 4, 7, 114}));
        out("count: 114 minus the missing", SurahSet.count(m) == 110);
        out("has: a missing one is not there", !SurahSet.has(3, m) && !SurahSet.has(114, m) && SurahSet.has(5, m) && !SurahSet.has(0, m) && !SurahSet.has(115, m));
        out("next steps over a run of missing ones", SurahSet.next(2, m) == 5);
        out("next after the last one there is wraps to 1 (114 missing)", SurahSet.next(113, m) == 1);
        out("previous steps over them too", SurahSet.prev(5, m) == 2 && SurahSet.prev(8, m) == 6);
        out("previous before 1 goes to the last one there is", SurahSet.prev(1, m) == 113);
        out("a pick of a missing one plays the next one there is", SurahSet.atOrAfter(3, m) == 5 && SurahSet.atOrAfter(7, m) == 8 && SurahSet.atOrAfter(114, m) == 1);
        out("a pick of one there is plays it", SurahSet.atOrAfter(18, m) == 18);
        out("out of range is brought in first", SurahSet.atOrAfter(0, m) == 1 && SurahSet.atOrAfter(500, m) == 1);

        int[] all = new int[114];
        for (int i = 0; i < 114; i++) all[i] = i + 1;
        out("a list that leaves nothing is dropped (the reciter plays as before)", SurahSet.clean(all).length == 0);

        int[] seven = SurahSet.clean(new int[]{10, 20, 30, 40, 50, 60, 70});
        out("seven missing: 107 surahs", SurahSet.count(seven) == 107);
        int steps = 0, n = 1;
        boolean onlyThere = true;
        do { n = SurahSet.next(n, seven); steps++; onlyThere &= SurahSet.has(n, seven); } while (n != 1 && steps < 200);
        out("a whole round of next plays each one there is once and never a missing one", steps == 107 && onlyThere);
    }
}
