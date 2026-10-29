package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;

/**
 * A short form of a built-in city's name ("مكة" for "مكة المكرمة"), for `saver.sh city`. The spelling is the club
 * search's (TeamSearch.normalize: no harakat, أ إ آ = ا, ة = ه, ى = ي, no «ال»). The first of these that finds
 * anything wins: the same name, a name that starts with it, a name that contains it.
 */
final class CityMatch {
    private CityMatch() {}

    private static final class Entry {
        final String[] city;
        final String ar, en, country;
        Entry(String[] city, String country) {
            this.city = city; this.country = country;
            ar = key(city[0]); en = city.length > 4 ? key(city[4]) : "";
        }
    }
    private static final List<Entry> ENTRIES = entries();
    private static List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        for (Cities.Region r : Cities.REGIONS) for (String[] c : r.cities) out.add(new Entry(c, r.country));
        return out;
    }

    private static String key(String name) {
        StringBuilder b = new StringBuilder();
        for (String w : TeamSearch.normalize(name)) b.append(b.length() > 0 ? " " : "").append(w);
        return b.toString();
    }

    /**
     * The time zone of the built-in city a typed name stands for (the name in full, or a short form that fits only one): Asia/Riyadh for a
     * Saudi one, the country's own for a Gulf one; null when it is not one. A place given with its own coordinates under a built-in name
     * has no zone of its own to give, and the TV's would be a guess, so it takes this one.
     */
    static String zoneOf(String typed) {
        for (Cities.Region r : Cities.REGIONS)
            for (String[] city : r.cities)
                if (city[0].equals(typed)) return zone(city);
        List<String[]> hits = find(typed);
        return hits.size() == 1 ? zone(hits.get(0)) : null;
    }

    static String zone(String[] city) { return city.length > 3 ? city[3] : "Asia/Riyadh"; }

    /** The built-in cities that the typed name stands for: none, one, or several (the caller lists them). */
    static List<String[]> find(String typed) {
        String country = "";
        if (typed != null && typed.length() > 3 && typed.charAt(2) == '|') {
            country = typed.substring(0, 2).toUpperCase(java.util.Locale.ROOT);
            typed = typed.substring(3);
        }
        String q = key(typed);
        List<String[]> out = new ArrayList<>();
        if (q.length() < 2) return out;
        // Existing exact Arabic names retain their original meaning after an update.
        if (country.isEmpty()) for (Entry e : ENTRIES)
            if (e.city.length < 5 && e.ar.equals(q)) { out.add(e.city); return out; }
        // Exact world names work directly; old short forms keep their meaning
        // before trying partial matches in the much larger worldwide list.
        for (int phase = 0; phase < 5 && out.isEmpty(); phase++) {
            int tier = phase == 0 ? 0 : phase == 1 || phase == 3 ? 1 : 2;
            for (Entry e : ENTRIES) {
                if (!country.isEmpty() && !country.equals(e.country)) continue;
                if (phase > 0 && (e.city.length < 5) != (phase < 3)) continue;
                boolean hit = tier == 0 ? e.ar.equals(q) || e.en.equals(q) : tier == 1 ? e.ar.startsWith(q) || e.en.startsWith(q) : e.ar.contains(q) || e.en.contains(q);
                if (hit) out.add(e.city);
            }
        }
        return out;
    }
}
