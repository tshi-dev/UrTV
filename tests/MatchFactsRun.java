package sa.urtv.home;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** For tests/match_app_test.py: MatchFacts on made-up games. One line per check: name TAB ok|FAIL. */
public final class MatchFactsRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    static Map<String, Object> game(long now) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("home_ar", "الهلال"); m.put("home_en", "Al-Hilal"); m.put("away_ar", "النصر"); m.put("away_en", "Al-Nassr");
        m.put("competition_ar", "دوري روشن"); m.put("competition_en", "Saudi Pro League"); m.put("kickoff", String.valueOf(now + 3600_000L));
        m.put("channels", Arrays.asList("SSC 1"));
        return m;
    }

    public static void main(String[] a) {
        long now = 1_800_000_000_000L;
        MatchFacts f = MatchFacts.of(game(now), now);
        check("a game: every field kept as given", f != null && "الهلال".equals(f.text("home_ar")) && "Al-Nassr".equals(f.text("away_en")) && "Saudi Pro League".equals(f.text("competition_en"))
            && f.kickoff == now + 3600_000L && f.channels.equals(Arrays.asList("SSC 1")));
        Map<String, Object> m = game(now); m.remove("channels"); m.remove("competition_ar");
        MatchFacts g = MatchFacts.of(m, now);
        check("no channels and an unknown competition: empty, still sent", g != null && g.channels.isEmpty() && "".equals(g.text("competition_ar")));
        m = game(now); m.put("url", "http://x");
        check("any other key (a link, a stream): refused whole", MatchFacts.of(m, now) == null);
        m = game(now); m.put("home_en", "a\nb");
        check("a control character: refused", MatchFacts.of(m, now) == null);
        m = game(now); m.put("home_en", "x".repeat(81));
        check("a name over 80 characters: refused", MatchFacts.of(m, now) == null);
        m = game(now); m.put("kickoff", 5L);
        check("the kick-off not a string of digits, or far from now: refused", MatchFacts.of(m, now) == null
            && MatchFacts.of(put(game(now), "kickoff", String.valueOf(now + 20L * 24 * 3600_000L)), now) == null && MatchFacts.of(put(game(now), "kickoff", "12ab"), now) == null);
        check("a side with no name in either language: refused", MatchFacts.of(put(put(game(now), "home_ar", ""), "home_en", ""), now) == null);
        check("at most four channels, each a short text", MatchFacts.of(put(game(now), "channels", Arrays.asList("1", "2", "3", "4", "5")), now) == null
            && MatchFacts.of(put(game(now), "channels", Arrays.asList(5L)), now) == null);
        check("spaces folded", "Al Hilal".equals(MatchFacts.of(put(game(now), "home_en", "  Al   Hilal "), now).text("home_en")));
    }

    static Map<String, Object> put(Map<String, Object> m, String k, Object v) { m.put(k, v); return m; }
}
