package sa.urtv.screensaver;

import java.util.Arrays;

/** For tests/league_groups_test.py: LeagueGroups' sections, search and count. One line per check: name TAB ok|FAIL. */
public final class LeagueGroupsRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] a) {
        check("the sections of the catalogue's countries", LeagueGroups.ofCountry("Saudi Arabia") == LeagueGroups.SAUDI && LeagueGroups.ofCountry("England") == LeagueGroups.EUROPE
            && LeagueGroups.ofCountry("Europe") == LeagueGroups.EUROPE && LeagueGroups.ofCountry("Egypt") == LeagueGroups.ASIA_AFRICA && LeagueGroups.ofCountry("Japan") == LeagueGroups.ASIA_AFRICA
            && LeagueGroups.ofCountry("Brazil") == LeagueGroups.WORLD && LeagueGroups.ofCountry("Worldwide") == LeagueGroups.WORLD && LeagueGroups.ofCountry(null) == LeagueGroups.WORLD
            && LeagueGroups.ofCountry("  SAUDI ARABIA ") == LeagueGroups.SAUDI);
        check("the built-in leagues' sections", LeagueGroups.ofBuiltIn("Saudi-Arabian Pro League") == LeagueGroups.SAUDI && LeagueGroups.ofBuiltIn("Spanish La Liga") == LeagueGroups.EUROPE
            && LeagueGroups.ofBuiltIn("Qatar Stars League") == LeagueGroups.ASIA_AFRICA && LeagueGroups.ofBuiltIn("NBA") == LeagueGroups.WORLD && LeagueGroups.ofBuiltIn("American Major League Soccer") == LeagueGroups.WORLD);
        check("search: every word, Arabic or English, case and hamza forms ignored; under 2 letters matches all", LeagueGroups.matches("الأمم", "دوري الأمم الأوروبية", "UEFA Nations League")
            && LeagueGroups.matches("الامم", "دوري الأمم الأوروبية", "") && LeagueGroups.matches("nations uefa", "x", "UEFA Nations League") && !LeagueGroups.matches("copa del", "كأس إيطاليا", "Coppa Italia")
            && LeagueGroups.matches("", "x") && LeagueGroups.matches("a", "x") && LeagueGroups.matches("كاس", "كأس العالم", ""));
        check("the count of the tournaments ticked", LeagueGroups.onCount(Arrays.asList("1", "2", "3"), Arrays.asList("2")) == 2 && LeagueGroups.onCount(Arrays.asList("1"), Arrays.asList()) == 1);
        java.util.List<String> ids = Arrays.asList("4480", "4481", "4429"), off = Arrays.asList("4481");
        java.util.List<String> p = LeagueGroups.pressTournament(ids, off, false, "4480");
        check("#64: a tournament pressed while the switch is off: only it ticked (the others unticked), so its tick shows and the count is 1",
            p.equals(Arrays.asList("4481", "4429")) && LeagueGroups.shownCount(ids, p, true) == 1 && !p.contains("4480"));
        java.util.List<String> q = LeagueGroups.pressTournament(ids, off, true, "4480"), r = LeagueGroups.pressTournament(ids, off, true, "4481");
        check("#64: the switch on: a press flips that tournament alone (unticked, or ticked back), nothing else changes", q.contains("4480") && q.contains("4481") && q.size() == 2
            && r.isEmpty() && off.equals(Arrays.asList("4481")));
        check("#64: the count beside the switch is the ticks shown: 0 while off (no tick shows), the ticked ones while on", LeagueGroups.shownCount(ids, off, false) == 0
            && LeagueGroups.shownCount(ids, off, true) == 2);
        boolean match = true;   // every state and every press: the count equals the ticks the rows show (row ticked = switch on and not unticked)
        for (int mask = 0; mask < 8; mask++) for (int sw = 0; sw < 2; sw++) for (String id : ids) {
            java.util.List<String> o = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) if ((mask >> i & 1) == 1) o.add(ids.get(i));
            java.util.List<String> after = LeagueGroups.pressTournament(ids, o, sw == 1, id);
            int ticks = 0;
            for (String x : ids) if (!after.contains(x)) ticks++;
            match &= LeagueGroups.shownCount(ids, after, true) == ticks;
            match &= sw == 1 || after.size() == ids.size() - 1 && !after.contains(id);
        }
        check("#64: after any press from any state, the switch is on and the count is the ticks the rows show", match);
        System.exit(0);
    }
}
