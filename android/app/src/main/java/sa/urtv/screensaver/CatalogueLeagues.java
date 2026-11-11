package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 4.3 «كل الدوريات»: a league of the catalogue (CI's data-catalogue branch, .github/tools/league_catalogue.py) beside the built-in ones
 * of Prefs.LEAGUES. Kept in the "leagues" list as "id4719|العربية|English" (either name may be empty), sent to the page as id4719 with
 * its name in #lgn=4719~name|… The catalogue itself is read only while the leagues picker is open (SettingsActivity). Plain Java
 * (tests/catalogue_leagues_test.py runs it).
 */
final class CatalogueLeagues {
    private CatalogueLeagues() { }

    /** raw.githubusercontent: the branch CI pushes, read only when the picker opens */
    static final String URL = "https://raw.githubusercontent.com/tshi-dev/UrTV/data-catalogue/catalogue/catalogue.json";
    static final int NAME_MAX = 60;

    /** A name as stored and sent: no separator of the list (; , |), none of the page's (~), one line, at most NAME_MAX. */
    static String clean(String s) {
        if (s == null) return "";
        String v = s.replaceAll("[;,|~\\r\\n\\t]+", " ").replaceAll("\\s+", " ").trim();
        return v.length() > NAME_MAX ? v.substring(0, NAME_MAX).trim() : v;
    }

    /** "id4719|ar|en", null for an id that is not one (digits, at most 6). */
    static String entry(String id, String ar, String en) {
        if (id == null || !id.matches("\\d{1,6}")) return null;
        return "id" + id + "|" + clean(ar) + "|" + clean(en);
    }

    /** {id, ar, en} of a stored entry, a bare "id4719" (saver.sh) too; null for anything else (a built-in league's name). */
    static String[] parse(String stored) {
        if (stored == null) return null;
        String[] p = stored.split("\\|", -1);
        if (!p[0].matches("id\\d{1,6}")) return null;
        return new String[] {p[0].substring(2), p.length > 1 ? clean(p[1]) : "", p.length > 2 ? clean(p[2]) : ""};
    }

    /** The name to show: the setting's language first, the other next, then «#id». */
    static String name(String[] e, boolean english) {
        String first = english ? e[2] : e[1], second = english ? e[1] : e[2];
        return !first.isEmpty() ? first : !second.isEmpty() ? second : "#" + e[0];
    }

    /** What #leagues= carries for a stored league: id4719 for a catalogue one, the English name of a built-in one. */
    static String pageToken(String stored) {
        String[] e = parse(stored);
        return e == null ? stored : "id" + e[0];
    }

    /** #lgn=: "4719~name|…" for the catalogue leagues of the list, "" for none (a name the page falls back on strLeague for: left out). */
    static String names(List<String> leagues, boolean english) {
        StringBuilder b = new StringBuilder();
        for (String s : leagues) {
            String[] e = parse(s);
            if (e == null) continue;
            String n = english ? (e[2].isEmpty() ? e[1] : e[2]) : (e[1].isEmpty() ? e[2] : e[1]);
            if (n.isEmpty()) continue;
            if (b.length() > 0) b.append('|');
            b.append(e[0]).append('~').append(n);
        }
        return b.toString();
    }

    /** The same league, whether stored with its names or bare. */
    static boolean same(String a, String b) {
        String[] x = parse(a), y = parse(b);
        return x != null && y != null ? x[0].equals(y[0]) : a.equalsIgnoreCase(b);
    }

    /** a kept copy is shown at once (and refreshed for next time) only while it is younger than this; older, a fresh one is fetched first */
    static final long FRESH_MS = 6 * 3600_000L;

    /**
     * Show the kept copy at once? Only when it was fetched less than FRESH_MS ago (a clock set back: no) and every country in it has its Arabic
     * name (an older catalogue had English ones: build 228 showed «United Kingdom», Albania… from a copy kept before the fix). Otherwise the
     * published one is fetched first, the kept copy only when that fails. rows: {id, en, ar, country, country_ar, sport, top}.
     */
    static boolean useKept(List<String[]> rows, long keptAt, long now) {
        if (rows == null || rows.isEmpty() || keptAt <= 0 || now < keptAt || now - keptAt >= FRESH_MS) return false;
        for (String[] r : rows) if (!r[3].isEmpty() && r[4].isEmpty()) return false;
        return true;
    }

    /** The countries of the catalogue, in its order of first appearance, for the first list of the picker: {country, its label}. */
    static List<String[]> countries(List<String[]> rows, boolean english) {
        List<String[]> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (String[] r : rows) {   // r: {id, en, ar, country, country_ar, sport, top}
            String key = r[3].toLowerCase(Locale.ROOT);
            if (seen.contains(key)) continue;
            seen.add(key);
            out.add(new String[] {r[3], english || r[4].isEmpty() ? (r[3].isEmpty() ? "—" : r[3]) : r[4]});
        }
        return out;
    }
}
