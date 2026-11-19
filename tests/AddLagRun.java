package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;

/** tests/add_lag_test.py: LeagueGroups' folded matching gives exactly the old answers, on the catalogue's shapes */
public class AddLagRun {
    public static void main(String[] a) {
        String[][] rows = {{"4328", "English Premier League", "الدوري الإنجليزي الممتاز", "England", "إنجلترا"},
            {"4668", "Saudi-Arabian Pro League", "دوري روشن السعودي", "Saudi Arabia", "السعودية"},
            {"4719", "AFC Champions League Elite", "دوري أبطال آسيا للنخبة", "Asia", "آسيا"},
            {"5000", "Liga MX", "", "Mexico", ""}, {"5001", "Qatar Stars League", "دوري نجوم قطر", "Qatar", "قطر"}};
        String[] queries = {"", "a", "دوري", "الدوري", "الإنجليزي", "الانجليزي", "premier", "PREMIER league", "saudi", "سعودي", "أبطال", "ابطال اسيا", "mx", "qatar stars", "نجوم", "zz", "قطر  نجوم"};
        int bad = 0, n = 0;
        for (String q : queries) for (String[] r : rows) {
            n++;
            boolean old = LeagueGroups.matches(q, r[2], r[1], r[4], r[3]);
            boolean now = LeagueGroups.matchesFolded(LeagueGroups.fold(q), LeagueGroups.folded(r[2], r[1], r[4], r[3]));
            if (old != now) { bad++; System.out.println("FAIL " + q + " / " + r[1] + " old=" + old + " now=" + now); }
        }
        // the time: 842 rows folded once, then 20 queries; against folding every row at every query
        List<String> folded = new ArrayList<>();
        List<String[]> big = new ArrayList<>();
        for (int i = 0; i < 842; i++) big.add(new String[] {"" + i, "League " + i + " Premier", "الدوري " + i + " الممتاز", "Land " + (i % 60), "بلد " + (i % 60)});
        for (String[] r : big) folded.add(LeagueGroups.folded(r[2], r[1], r[4], r[3]));
        long t0 = System.nanoTime(); int hits = 0;
        for (int k = 0; k < 20; k++) for (String f : folded) if (LeagueGroups.matchesFolded(LeagueGroups.fold("الممتاز " + k), f)) hits++;
        long folded20 = System.nanoTime() - t0;
        t0 = System.nanoTime(); int hits2 = 0;
        for (int k = 0; k < 20; k++) for (String[] r : big) if (LeagueGroups.matches("الممتاز " + k, r[2], r[1], r[4], r[3])) hits2++;
        long every20 = System.nanoTime() - t0;
        System.out.println((bad == 0 ? "PASS" : "FAIL") + " folded matching gives the old answers (" + n + " cases)");
        System.out.println((hits == hits2 ? "PASS" : "FAIL") + " the same hits on 842 rows (" + hits + ")");
        System.out.println("INFO 20 searches over 842 rows: folded once " + folded20 / 1000000 + " ms, folded every time " + every20 / 1000000 + " ms");
        System.out.println((folded20 < every20 ? "PASS" : "FAIL") + " folding once is cheaper");
    }
}
