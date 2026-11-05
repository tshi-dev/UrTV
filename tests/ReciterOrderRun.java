package sa.urtv.home.tilawah;

import java.util.*;

/** For tests/reciter_order_test.py: ReciterOrder (favourites first, the letter index, the dimmed letters, the saved list). Prints PASS/FAIL lines. */
public final class ReciterOrderRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    static String show(List<ReciterOrder.Item> l) {
        StringBuilder b = new StringBuilder();
        for (ReciterOrder.Item i : l) b.append(i.header ? "H" : String.valueOf(i.reciter) + (i.fav ? "*" : "")).append(' ');
        return b.toString().trim();
    }

    public static void main(String[] a) {
        List<String> ids = Arrays.asList("sudais", "shuraim", "muaiqly", "afasy", "ghamdi");
        List<String> ar = Arrays.asList("عبدالرحمن السديس", "سعود الشريم", "ماهر المعيقلي", "مشاري العفاسي", "سعد الغامدي");
        Set<String> none = new HashSet<>(), two = new HashSet<>(Arrays.asList("afasy", "sudais", "nobody"));
        check("no favourite: the catalogue as it was, no heading", show(ReciterOrder.order(ids, none)).equals("0 1 2 3 4"));
        check("favourites first under one heading, in the catalogue's order, then the rest without them (an unknown id ignored)", show(ReciterOrder.order(ids, two)).equals("H 0* 3* 1 2 4"));
        check("a null set is no favourite", show(ReciterOrder.order(ids, null)).equals("0 1 2 3 4"));
        check("the letter of a name: the first letter, alef forms and marks folded", ReciterOrder.letterOf("أحمد").equals("ا") && ReciterOrder.letterOf("إبراهيم").equals("ا") && ReciterOrder.letterOf("ٱلسديس").equals("ا")
            && ReciterOrder.letterOf("َعبد").equals("ع") && ReciterOrder.letterOf("ـمشاري").equals("م") && ReciterOrder.letterOf("ياسر").equals("ي") && ReciterOrder.letterOf("ة").equals("ه"));
        check("Latin names: the first letter in capitals; a digit or nothing is no letter", ReciterOrder.letterOf("abdul").equals("A") && ReciterOrder.letterOf("  Saud").equals("S") && ReciterOrder.letterOf("1st").equals("") && ReciterOrder.letterOf("").equals("") && ReciterOrder.letterOf(null).equals(""));
        boolean[] pres = ReciterOrder.present(ReciterOrder.ARABIC, ar);
        int ain = Arrays.asList(ReciterOrder.ARABIC).indexOf("ع"), mim = Arrays.asList(ReciterOrder.ARABIC).indexOf("م"), sin = Arrays.asList(ReciterOrder.ARABIC).indexOf("س"), ba = Arrays.asList(ReciterOrder.ARABIC).indexOf("ب");
        check("the letters that start a name are lit, the others dimmed", pres[ain] && pres[mim] && pres[sin] && !pres[ba] && ReciterOrder.ARABIC.length == 28 && ReciterOrder.LATIN.length == 26);
        List<ReciterOrder.Item> rows = ReciterOrder.order(ids, two);   // H 0* 3* 1 2 4
        check("the jump goes to the first row that starts with the letter (favourites are rows too), -1 for a dimmed letter", ReciterOrder.jump(rows, ar, "ع") == 1 && ReciterOrder.jump(rows, ar, "م") == 2
            && ReciterOrder.jump(rows, ar, "س") == 3 && ReciterOrder.jump(rows, ar, "ب") == -1);
        check("the next lit letter is found each way, dimmed ones are skipped, and the end stays put", ReciterOrder.nextPresent(pres, sin, 1) == ain && ReciterOrder.nextPresent(pres, ain, 1) == mim && ReciterOrder.nextPresent(pres, mim, -1) == ain
            && ReciterOrder.nextPresent(pres, mim, 1) == mim && ReciterOrder.nextPresent(pres, Arrays.asList(ReciterOrder.ARABIC).indexOf("ا"), -1) == 0);
        check("English: the English names have their Latin letters lit", ReciterOrder.present(ReciterOrder.LATIN, Arrays.asList("Abdulrahman Al-Sudais", "Saud Al-Shuraim", "Maher Al-Muaiqly"))[0]
            && ReciterOrder.present(ReciterOrder.LATIN, Arrays.asList("Abdulrahman", "Saud", "Maher"))[18] && !ReciterOrder.present(ReciterOrder.LATIN, Arrays.asList("Abdulrahman"))[1]);
        check("the saved list: add, remove, no repeat, order kept, junk dropped", ReciterOrder.toggle("a,b", "c", true).equals("a,b,c") && ReciterOrder.toggle("a,b,c", "b", false).equals("a,c")
            && ReciterOrder.toggle("a,b", "b", true).equals("a,b") && ReciterOrder.toggle(null, "x", true).equals("x") && ReciterOrder.toggle("a", "a", false).equals("")
            && ReciterOrder.parse("sudais, afasy ,bad id,../x,sudais").equals(Arrays.asList("sudais", "afasy")));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
