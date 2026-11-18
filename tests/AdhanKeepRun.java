package sa.urtv.home.tilawah;

/** For tests/adhan_keep_test.py (4.4 beta 3): the rules of the kept adhan recordings (AdhanKeep). One line per case, «name<TAB>value». */
public final class AdhanKeepRun {
    static void p(String n, Object v) { System.out.println(n + "\t" + v); }

    static String arr(int[] a) { StringBuilder b = new StringBuilder(); for (int x : a) b.append(b.length() == 0 ? "" : ",").append(x); return b.toString(); }

    public static void main(String[] a) {
        long M = AdhanKeep.MINUTE, now = 1_000_000_000_000L;
        p("wanted_two", arr(AdhanKeep.wanted(3, 10, 21)));
        p("wanted_same", arr(AdhanKeep.wanted(3, 3, 21)));
        p("wanted_bad", arr(AdhanKeep.wanted(0, 99, 21)));
        p("due_nothing_missing", AdhanKeep.due(false, now, 0, 0));
        p("due_never_tried", AdhanKeep.due(true, now, 0, 0));
        p("due_tried_10min_far", AdhanKeep.due(true, now, now - 10 * M, now + 3 * 60 * M));
        p("due_tried_61min_far", AdhanKeep.due(true, now, now - 61 * M, now + 3 * 60 * M));
        p("due_tried_10min_soon", AdhanKeep.due(true, now, now - 10 * M, now + 30 * M));
        p("due_tried_5min_soon", AdhanKeep.due(true, now, now - 5 * M, now + 30 * M));
        p("due_clock_back", AdhanKeep.due(true, now, now + 5 * M, 0));
        boolean[] s = AdhanKeep.parseSaved("10, 4,x,99,0", 21);
        p("parse", AdhanKeep.savedText(s));
        p("stand_other_first", AdhanKeep.stand(3, 10, s));
        p("stand_lowest", AdhanKeep.stand(3, 7, s));
        p("stand_never_self", AdhanKeep.stand(4, 4, AdhanKeep.parseSaved("4", 21)));
        p("stand_none", AdhanKeep.stand(3, 10, AdhanKeep.parseSaved("", 21)));
        p("all_saved", AdhanKeep.allSaved(AdhanKeep.wanted(4, 10, 21), s) + " " + AdhanKeep.allSaved(AdhanKeep.wanted(3, 10, 21), s));
        p("missed", AdhanKeep.missedShown(now - 60 * M, now) + " " + AdhanKeep.missedShown(now - 25 * 60 * M, now) + " " + AdhanKeep.missedShown(now + M, now) + " " + AdhanKeep.missedShown(0, now));
    }
}
