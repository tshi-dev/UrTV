package sa.urtv.screensaver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 4.3.1 «اختر دورياتك» (LeaguesActivity): which of the four sections a league belongs to, and the search. Plain Java (tests/league_groups_test.py runs it).
 * Order of the screen: «البطولات الكبرى» (the switch and its members), then SAUDI, EUROPE, ASIA_AFRICA, WORLD.
 */
final class LeagueGroups {
    private LeagueGroups() { }

    static final int SAUDI = 0, EUROPE = 1, ASIA_AFRICA = 2, WORLD = 3;

    private static final Set<String> EUROPE_C = new HashSet<>(Arrays.asList("europe", "england", "scotland", "wales", "northern ireland", "united kingdom", "spain", "italy", "germany", "france",
        "portugal", "the netherlands", "netherlands", "belgium", "turkey", "greece", "russia", "switzerland", "austria", "denmark", "norway", "sweden", "ukraine", "croatia", "serbia", "poland",
        "czechia", "czech republic", "romania", "hungary", "bulgaria", "slovakia", "slovenia", "finland", "ireland", "iceland", "israel", "cyprus", "albania", "bosnia and herzegovina",
        "north macedonia", "montenegro", "kosovo", "belarus", "georgia", "armenia", "azerbaijan", "kazakhstan"));
    private static final Set<String> ASIA_AFRICA_C = new HashSet<>(Arrays.asList("united arab emirates", "qatar", "kuwait", "oman", "bahrain", "iraq", "jordan", "lebanon", "syria", "palestine",
        "yemen", "egypt", "morocco", "algeria", "tunisia", "libya", "sudan", "south africa", "nigeria", "ghana", "senegal", "cameroon", "ivory coast", "kenya", "ethiopia", "tanzania", "iran",
        "japan", "south korea", "china", "australia", "india", "uzbekistan", "thailand", "indonesia", "malaysia", "vietnam", "singapore", "hong kong", "pakistan", "asia", "africa"));

    /** a catalogue league's section from its country (English, as the catalogue spells it); an unknown country is «العالم» */
    static int ofCountry(String country) {
        String c = country == null ? "" : country.trim().toLowerCase(Locale.ROOT);
        if (c.equals("saudi arabia")) return SAUDI;
        if (EUROPE_C.contains(c)) return EUROPE;
        if (ASIA_AFRICA_C.contains(c)) return ASIA_AFRICA;
        return WORLD;
    }

    /** a built-in league's section (Prefs.LEAGUES' English key) */
    static int ofBuiltIn(String key) {
        switch (key) {
            case "Saudi-Arabian Pro League": case "Saudi First Division League": return SAUDI;
            case "English Premier League": case "Spanish La Liga": case "Italian Serie A": case "German Bundesliga": case "French Ligue 1":
            case "Portuguese Primeira Liga": case "Dutch Eredivisie": case "Turkish Super Lig": case "UEFA Champions League": return EUROPE;
            case "Egyptian Premier League": case "UAE Pro League": case "Qatar Stars League": case "Iraqi Premier League": case "AFC Champions League Elite": return ASIA_AFRICA;
            default: return WORLD;
        }
    }

    /** the search: every word of the query is in the Arabic or the English name (case and the Arabic tatweel/hamza forms ignored); a query shorter than 2 letters matches all */
    static boolean matches(String query, String... names) {
        String q = fold(query);
        if (q.length() < 2) return true;
        StringBuilder all = new StringBuilder();
        for (String n : names) all.append(' ').append(fold(n));
        for (String w : q.split(" ")) if (!w.isEmpty() && all.indexOf(w) < 0) return false;
        return true;
    }

    static String fold(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ى', 'ي').replace('ة', 'ه').replace("ـ", "").replaceAll("[\\u064B-\\u0652]", "").trim();
    }

    /** the tournaments whose box is ticked: all of ids but the ones the person unticked */
    static int onCount(List<String> ids, List<String> off) {
        int n = 0;
        for (String id : ids) if (!off.contains(id)) n++;
        return n;
    }
}
