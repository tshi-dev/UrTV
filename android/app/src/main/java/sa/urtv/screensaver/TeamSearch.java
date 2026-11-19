package sa.urtv.screensaver;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Search of the club list (assets/shared/teams.json) by an Arabic or English name, offline. The same rules as
 * optimize/team_search.py (read its header); tests/team_search_cases.tsv runs against both.
 */
final class TeamSearch {
    private TeamSearch() {}

    /** One entry of teams.json. */
    static final class Team {
        final String id, en, ar;
        final String[] leagues, alt;
        final String sport;   // TheSportsDB's strSport: "Soccer", "Basketball", "American Football"
        private List<List<String>> words;   // 4.4 beta 2: its names normalised once (the first search), not at every search

        synchronized List<List<String>> words() {
            if (words == null) {
                List<List<String>> w = new ArrayList<>();
                List<String> names = new ArrayList<>();
                names.add(en);
                names.add(ar);
                if (alt != null) Collections.addAll(names, alt);
                for (String x : names) if (x != null && !x.isEmpty()) w.add(normalize(x));
                words = w;
            }
            return words;
        }
        Team(String id, String en, String ar, String[] leagues, String[] alt) { this(id, en, ar, leagues, alt, "Soccer"); }
        Team(String id, String en, String ar, String[] leagues, String[] alt, String sport) {
            this.id = id; this.en = en; this.ar = ar; this.leagues = leagues; this.alt = alt; this.sport = sport;
        }
        boolean saudi() {
            for (String l : leagues) if (l.contains("Saudi")) return true;
            return false;
        }
    }

    private static final Pattern HARAKAT = Pattern.compile("[ً-ٰٟـۖ-ۭ]");
    private static final Pattern DOTTED = Pattern.compile("\\b([fs])\\.c\\.");
    private static final Pattern WORD = Pattern.compile("[a-z0-9]+|[ء-يٮ-ۓ٠-٩]+");
    private static final Pattern ARABIC_WORD = Pattern.compile("[ء-ي]+");
    private static final Pattern ARABIC_START = Pattern.compile("^[ء-ي]");
    private static final Set<String> FILLER_AR = set("نادي", "فريق");
    private static final Set<String> FILLER_EN = set("fc", "sc", "cf", "afc", "sfc", "club", "football", "clube");
    private static final Set<String> KIND_AR = set("منتخب");

    private static Set<String> set(String... v) {
        Set<String> s = new HashSet<>();
        Collections.addAll(s, v);
        return s;
    }

    private static String letters(String s) {   // أ إ آ ٱ -> ا, ة -> ه, ى ئ -> ي, ؤ -> و
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case 'أ': case 'إ': case 'آ': case 'ٱ': b.append('ا'); break;
                case 'ة': b.append('ه'); break;
                case 'ى': case 'ئ': b.append('ي'); break;
                case 'ؤ': b.append('و'); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    /** The words of a name, normalised. */
    static List<String> normalize(String text) {
        String s = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKD)
            .replace("ı", "i").replace("ß", "ss");
        s = HARAKAT.matcher(s).replaceAll("");
        StringBuilder plain = new StringBuilder(s.length());
        for (char c : s.toCharArray()) if (Character.getType(c) != Character.NON_SPACING_MARK) plain.append(c);
        s = DOTTED.matcher(plain.toString().toLowerCase(Locale.ROOT)).replaceAll("$1c");
        s = letters(s);
        List<String> out = new ArrayList<>();
        Matcher m = WORD.matcher(s);
        while (m.find()) {
            String w = m.group();
            if (ARABIC_START.matcher(w).find()) {
                if (w.length() > 3 && w.startsWith("ال")) w = w.substring(2);
                if (FILLER_AR.contains(w)) continue;
            } else if (FILLER_EN.contains(w)) {
                continue;
            }
            out.add(w);
        }
        return out;
    }

    private static List<String> dropAl(List<String> w) {
        return w.size() > 1 && w.get(0).equals("al") ? w.subList(1, w.size()) : w;
    }

    private static String join(List<String> w) {
        StringBuilder b = new StringBuilder();
        for (String s : w) b.append(s);
        return b.toString();
    }

    private static final Pattern LATIN = Pattern.compile("[a-z0-9]+");

    /** "nassr" -> "nsr", "wehda" -> "whd": a name without doubled letters or vowels after the first. In Arabic the
     *  vowel letters ا و ي go too, and a leading تش is ش: تشيلسي, تشلسي and شيلسي are one name. */
    static String skeleton(String word) {
        String s;
        if (LATIN.matcher(word).matches()) {
            s = word.replaceAll("(.)\\1+", "$1");
            s = s.charAt(0) + s.substring(1).replaceAll("[aeiouy]", "");
        } else if (ARABIC_WORD.matcher(word).matches()) {
            s = word.replaceAll("(.)\\1+", "$1").replaceFirst("^تش", "ش");
            s = s.charAt(0) + s.substring(1).replaceAll("[اويء]", "");
        } else {
            return "";
        }
        return s.length() >= 3 ? s : "";
    }

    /** 0 exactly, 1 in doubled letters and vowels only, 2 the start of a word or of the name, 3 the start of every
     *  word, 9 not at all; 0 and 1 rank together (GROUP). */
    static int tier(List<String> q, List<String> name) {
        if (q.isEmpty() || name.isEmpty()) return 9;
        String cq = join(q), cn = join(name), bare = join(dropAl(q)), bareName = join(dropAl(name));
        if (cq.equals(cn) || bare.equals(bareName)) return 0;
        String sk = skeleton(bare);
        if (!sk.isEmpty() && sk.equals(skeleton(bareName))) return 1;
        if (bare.length() >= 2) {
            boolean start = cn.startsWith(bare) || bareName.startsWith(bare);
            for (String w : name) start |= w.startsWith(bare);
            if (start) return 2;
        }
        for (String w : dropAl(q)) {
            boolean found = false;
            for (String n : name) found |= w.length() >= 2 && n.startsWith(w);
            if (!found) return 9;
        }
        return 3;
    }

    private static final int[] GROUP = {0, 0, 1, 2};   // a misspelt name still ranks as the same name: Saudi first

    private static final Set<String> ARTICLES = set("ال", "al", "el");   // a query of nothing else finds nothing

    static List<String> queryWords(String query) {
        List<String> q = normalize(query), stripped = new ArrayList<>();
        for (String w : q) if (!KIND_AR.contains(w)) stripped.add(w);
        if (!stripped.isEmpty()) q = stripped;
        for (String w : q) if (!ARTICLES.contains(w)) return q;
        return new ArrayList<>();
    }

    /** The teams matching query, best first; empty for an empty or silly query. Never throws. */
    static List<Team> search(List<Team> teams, String query) {
        List<Team> out = new ArrayList<>();
        try {
            final List<String> q = queryWords(query);
            if (q.isEmpty() || join(q).length() < 2 || teams == null) return out;
            final List<int[]> ranked = new ArrayList<>();   // {group, not saudi, tier, index}
            for (int n = 0; n < teams.size(); n++) {
                Team t = teams.get(n);
                int best = 9;
                for (List<String> w : t.words()) best = Math.min(best, tier(q, w));
                if (best < 9) ranked.add(new int[] {GROUP[best], t.saudi() ? 0 : 1, best, n});
            }
            Collections.sort(ranked, new Comparator<int[]>() {
                public int compare(int[] a, int[] b) {
                    for (int i = 0; i < 4; i++) if (a[i] != b[i]) return a[i] < b[i] ? -1 : 1;
                    return 0;
                }
            });
            for (int[] r : ranked) out.add(teams.get(r[3]));
        } catch (RuntimeException e) {
            out.clear();   // a bad entry in the list must not take the settings screen down
        }
        return out;
    }
}
