package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 4.3.3: the offline city search (the settings' «بحث عن مدينة أو قرية» first, `saver.sh city` when the built-in list has no such name), over the
 * built-in list and assets/cities-more.txt (CitiesMore). A name is matched as the club search matches one (TeamSearch.normalize and tier: no
 * harakat, أ إ آ = ا, ة = ه, ى = ي, with or without «ال», a word's start, a misspelt vowel): in Arabic, in English and in the other spellings.
 * The best match comes first; between equal matches the bigger place, a city of the main list and a whole name weighing more. No Android in here (the tests).
 */
final class CitySearch {
    private CitySearch() {}

    /** TeamSearch.tier (0 the name, 1 the same letters but vowels, 2 a word's start, 3 every word's start) to a place's rank: a misspelt
     *  vowel comes last, after every start of a name («خميس» is خميس مشيط before الخمس, «ابها» أبها before إيبوه). */
    private static final int[] RANK = {0, 3, 1, 2};

    private static final class Keys {
        final List<List<String>> names = new ArrayList<>();
    }
    private static volatile List<Keys> keys;
    private static volatile List<CitiesMore.Place> keyed;

    private static List<Keys> keys(List<CitiesMore.Place> all) {
        List<Keys> k = keys;
        if (k != null && keyed == all) return k;
        synchronized (CitySearch.class) {
            if (keys != null && keyed == all) return keys;
            List<Keys> out = new ArrayList<>(all.size());
            for (CitiesMore.Place p : all) {
                Keys e = new Keys();
                e.names.add(TeamSearch.normalize(p.row[0]));
                if (p.row[4] != null && !p.row[4].isEmpty()) e.names.add(TeamSearch.normalize(p.row[4]));
                for (String a : p.aliases) e.names.add(TeamSearch.normalize(a));
                out.add(e);
            }
            keyed = all;
            keys = out;
            return out;
        }
    }

    /** How well the typed words fit a place: 0 its name, 1 a word's start, 2 the start of every typed word, 3 a misspelt vowel, 9 not at all. */
    static int score(List<String> q, List<List<String>> names) {
        int best = 9;
        for (List<String> n : names) {
            int t = TeamSearch.tier(q, n);
            if (t < 9) best = Math.min(best, RANK[t]);
        }
        return best;
    }

    /** the Arabic letters of a name as the search compares them: no harakat or tatweel, أ إ آ ٱ = ا, ة = ه, ى = ي, one space */
    static String fold(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : (s == null ? "" : s.trim()).toCharArray()) {
            if ((c >= '\u064b' && c <= '\u0652') || c == '\u0670' || c == '\u0640') continue;
            switch (c) {
                case 'أ': case 'إ': case 'آ': case 'ٱ': b.append('ا'); break;
                case 'ة': b.append('ه'); break;
                case 'ى': b.append('ي'); break;
                default: b.append(Character.isWhitespace(c) ? ' ' : c);
            }
        }
        return b.toString().replaceAll(" +", " ");
    }

    /** The places that fit, best first, at most max; country: a two-letter code to keep to, or "" / null for all. */
    static List<CitiesMore.Place> find(String typed, String country, int max) {
        List<CitiesMore.Place> out = new ArrayList<>();
        List<CitiesMore.Place> all = CitiesMore.all();
        for (long[] h : hits(typed, country, all)) {
            if (out.size() >= max) break;
            out.add(all.get((int) h[4]));
        }
        return out;
    }

    /** The places of the best fit only (all of equal score, bigger first), for `saver.sh city`: one is the city, several are listed back. */
    static List<String[]> best(String typed, String country, int max) {
        List<String[]> out = new ArrayList<>();
        List<CitiesMore.Place> all = CitiesMore.all();
        List<long[]> hits = hits(typed, country, all);
        // one place far heavier than the next of its kind (Egypt's Alexandria, not Virginia's) is the one meant
        if (hits.size() > 1 && hits.get(0)[0] == 0 && hits.get(0)[5] == hits.get(1)[5] && hits.get(0)[1] == hits.get(1)[1] && -hits.get(0)[2] >= -10 * hits.get(1)[2]) {
            out.add(all.get((int) hits.get(0)[4]).row);
            return out;
        }
        for (long[] h : hits) {
            if (h[5] != hits.get(0)[5] || h[0] != hits.get(0)[0] || out.size() >= max) break;   // the same kind of fit as the best: «Riyadh» is the one city of that whole name
            out.add(all.get((int) h[4]).row);
        }
        return out;
    }

    /** the fit (0 a whole name or a name's start, 1 every word's start, 2 a misspelt vowel), 0 for a name that starts as typed with «ال», -weight
     *  (or 0 for a built-in city and 1 for the search list's), -population, index; best first */
    private static List<long[]> hits(String typed, String country, List<CitiesMore.Place> all) {
        List<long[]> hits = new ArrayList<>();
        List<String> q = TeamSearch.normalize(typed);
        StringBuilder joined = new StringBuilder();
        for (String w : q) joined.append(w);
        if (joined.length() < 2) return hits;
        List<Keys> k = keys(all);
        String article = fold(typed);   // typed with «ال»: the names that start with it, so, first («المدينه» is المدينة المنورة, not مدينة هو تشي منه)
        if (!article.startsWith("ال") || article.length() < 4) article = null;
        for (int i = 0; i < all.size(); i++) {
            CitiesMore.Place p = all.get(i);
            if (country != null && !country.isEmpty() && !country.equalsIgnoreCase(p.row[5])) continue;
            int s = score(q, k.get(i).names);
            if (s >= 9) continue;
            // the whole name and the start of a name rank together, by weight: the population (a floor for the towns without one), a hundred
            // times for the whole name, twice for a city of the main list, three times in Saudi Arabia (the app's home). So «خميس» is خميس مشيط
            // before a village called الخميس, «ديرة» is Dubai's Deira before Dera Ismail Khan, «Al Ula» is العلا before Ulan Bator.
            long weight = Math.max(p.pop, p.builtin ? 5000 : 500) * (s == 0 ? 100L : 1L) * (p.builtin ? 2L : 1L) * ("SA".equals(p.row[5]) ? 3L : 1L);
            hits.add(new long[] {Math.max(0, s - 1), article == null || fold(p.row[0]).startsWith(article) ? 0 : 1, s <= 1 ? -weight : (p.builtin ? 0 : 1), -(long) p.pop, i, s});
        }
        Collections.sort(hits, (a, b) -> {
            for (int j = 0; j < 5; j++) if (a[j] != b[j]) return Long.compare(a[j], b[j]);
            return 0;
        });
        return hits;
    }
}
