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
        System.exit(0);
    }
}
