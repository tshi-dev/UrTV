package sa.urtv.screensaver;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Arabic names for teams and shows: Wikidata's Arabic label, else a transliteration. Same rules as the page. */
final class Arabic {
    private Arabic() {}

    private static Map<String, String> known;

    /** The hand-checked names in assets/shared/teams-ar.json, shared with the screen. */
    static synchronized String known(android.content.Context c, String en) {
        if (known == null) {
            known = new HashMap<>();
            try (InputStream in = c.getAssets().open("shared/teams-ar.json")) {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
                JSONObject o = new JSONObject(b.toString("UTF-8"));
                for (java.util.Iterator<String> it = o.keys(); it.hasNext(); ) { String k = it.next(); known.put(key(k), o.getString(k)); }
            } catch (Exception ignored) { }
        }
        return known.get(key(en));
    }

    /** Same normalisation as teamKey() in the page: "Al-Adalah FC" == "al adalah" == "AL ADALAH", "Atlético" ==
     *  "Atletico". */
    static String key(String s) {
        s = java.text.Normalizer.normalize(s.replace("\u0131", "i").replace("ß", "ss"), java.text.Normalizer.Form.NFKD)
            .replaceAll("\\p{M}+", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("\\b(fc|sc|cf|afc|club|saudi club)\\b", "")
            .replaceAll("[^a-z0-9]+", " ").trim().replaceFirst("^al ", "al");
    }

    /** For lists: a known Arabic name, else the transliteration (instant, no network). */
    static String team(android.content.Context c, String en) {
        String k = known(c, en);
        return k != null ? k : translit(en);
    }

    static final Pattern TEAM = Pattern.compile("football|soccer|club|team", Pattern.CASE_INSENSITIVE);
    static final Pattern SHOW = Pattern.compile("television|series|sitcom|anime|miniseries|show", Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> DIGRAPHS = new HashMap<>();
    private static final Map<Character, String> LETTERS = new HashMap<>();
    private static final Map<Character, String> SPELLED = new HashMap<>();
    private static final Map<String, String> WORDS = new HashMap<>();
    static {
        String[] d = {"sh", "ش", "kh", "خ", "th", "ث", "dh", "ذ", "gh", "غ", "ch", "تش", "ph", "ف", "ck", "ك",
            "qu", "كو", "ou", "و", "oo", "و", "ee", "ي"};
        for (int i = 0; i < d.length; i += 2) DIGRAPHS.put(d[i], d[i + 1]);
        String[] l = {"a", "ا", "b", "ب", "c", "ك", "d", "د", "e", "ي", "f", "ف", "g", "ج", "h", "ه", "i", "ي",
            "j", "ج", "k", "ك", "l", "ل", "m", "م", "n", "ن", "o", "و", "p", "ب", "q", "ق", "r", "ر", "s", "س",
            "t", "ت", "u", "و", "v", "ف", "w", "و", "x", "كس", "y", "ي", "z", "ز"};
        for (int i = 0; i < l.length; i += 2) LETTERS.put(l[i].charAt(0), l[i + 1]);
        String[] s = {"a", "إيه", "b", "بي", "c", "سي", "d", "دي", "e", "إي", "f", "إف", "g", "جي", "h", "إتش",
            "i", "آي", "j", "جيه", "k", "كيه", "l", "إل", "m", "إم", "n", "إن", "o", "أو", "p", "بي", "q", "كيو",
            "r", "آر", "s", "إس", "t", "تي", "u", "يو", "v", "في", "w", "دبليو", "x", "إكس", "y", "واي", "z", "زد"};
        for (int i = 0; i < s.length; i += 2) SPELLED.put(s[i].charAt(0), s[i + 1]);
        String[] w = {"the", "ذا", "of", "أوف", "and", "آند", "in", "إن", "on", "أون", "to", "تو", "us", "أس",
            "fc", "", "cf", "", "sc", "", "afc", "", "united", "يونايتد", "city", "سيتي", "real", "ريال",
            "club", "كلوب", "sporting", "سبورتنغ", "saint", "سانت", "st", "سانت"};
        for (int i = 0; i < w.length; i += 2) WORDS.put(w[i], w[i + 1]);
    }

    static boolean hasArabic(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) >= 0x0600 && s.charAt(i) <= 0x06FF) return true;
        return false;
    }

    static String translit(String en) {
        StringBuilder out = new StringBuilder();
        for (String w : en.replace("&", " و ").replaceAll("\\bAl (?=[A-Z])", "Al-").split("\\s+")) {
            String a = word(w);
            if (a.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(a);
        }
        return out.toString();
    }

    private static String word(String w) {
        if (hasArabic(w) || !w.matches(".*[A-Za-z0-9].*")) return w;
        String lw = w.toLowerCase(Locale.ROOT);
        if (WORDS.containsKey(lw)) return WORDS.get(lw);
        if (w.matches("[A-Z]{2,4}")) {
            StringBuilder b = new StringBuilder();
            for (char c : w.toLowerCase(Locale.ROOT).toCharArray()) { if (b.length() > 0) b.append(' '); b.append(SPELLED.get(c)); }
            return b.toString();
        }
        if (lw.startsWith("al-") && lw.length() > 3) return "ال" + word(w.substring(3));
        String x = lw.replaceAll("(.)\\1", "$1");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < x.length(); ) {
            String two = i + 1 < x.length() ? DIGRAPHS.get(x.substring(i, i + 2)) : null;
            char ch = x.charAt(i);
            if (i == 0 && two == null && "aeiou".indexOf(ch) >= 0) {
                out.append(ch == 'a' || ch == 'u' ? "أ" : ch == 'o' ? "أو" : "إ"); i++; continue;
            }
            if (two != null) { out.append(two); i += 2; continue; }
            if (ch == 'e' && i > 0) { i++; continue; }
            if (ch == 'c' && i + 1 < x.length() && "eiy".indexOf(x.charAt(i + 1)) >= 0) { out.append('س'); i++; continue; }
            if (ch >= '0' && ch <= '9') out.append(DigitsText.convert(String.valueOf(ch), false));
            else if (LETTERS.containsKey(ch)) out.append(LETTERS.get(ch));
            i++;
        }
        return out.toString();
    }

    /** Blocking. Wikidata's Arabic label for the first hit whose description matches kind, or the transliteration. */
    static String lookup(String en, Pattern kind) {
        if (hasArabic(en)) return en;
        try {
            String api = "https://www.wikidata.org/w/api.php?format=json";
            JSONArray hits = new JSONObject(get(api + "&action=wbsearchentities&type=item&limit=7&language=en&search="
                + Uri.encode(en))).optJSONArray("search");
            StringBuilder ids = new StringBuilder();
            for (int i = 0; hits != null && i < hits.length() && ids.length() < 40; i++) {
                JSONObject h = hits.getJSONObject(i);
                if (!kind.matcher(h.optString("description")).find()) continue;
                if (ids.length() > 0) ids.append('|');
                ids.append(h.getString("id"));
            }
            if (ids.length() > 0) {
                JSONObject ents = new JSONObject(get(api + "&action=wbgetentities&props=labels&languages=ar&ids="
                    + Uri.encode(ids.toString(), "|"))).optJSONObject("entities");
                for (String id : ids.toString().split("\\|")) {
                    JSONObject l = ents == null ? null : ents.optJSONObject(id);
                    l = l == null ? null : l.optJSONObject("labels");
                    l = l == null ? null : l.optJSONObject("ar");
                    if (l != null && !l.optString("value").isEmpty())
                        return l.optString("value").replaceAll("\\s*\\([^)]*\\)\\s*", " ").replaceAll("^(نادي|فريق)\\s+", "").trim();
                }
            }
        } catch (Exception ignored) {
        }
        return translit(en);
    }

    private static String get(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        NetLog.count(String.valueOf(con.getURL()));   // G3: the request log (NetLog)
        try {
            con.setConnectTimeout(10000);
            con.setReadTimeout(12000);
            con.setRequestProperty("User-Agent", "urtv/2 (Android TV screensaver)");
            try (InputStream in = con.getInputStream()) {
                return new String(Bounded.readAll(in, Bounded.JSON_MAX), "UTF-8");
            }
        } finally {
            con.disconnect();
        }
    }
}
