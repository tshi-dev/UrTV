package sa.urtv.home;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 4.3.3 the teams and leagues followed from «المباريات» while the screensaver app could not take them (it is missing, or older): kept by the
 * home (HomeFollows) and joined to the page's settings here, none twice; the screensaver's own list stays first. Plain Java: tests/follow_store_test.py.
 * A team is "id|name"; a league "id|ar|en|token", token being what #leagues= carries (a built-in league's English name, else id4719).
 */
final class FollowMerge {
    private FollowMerge() { }

    static final int MAX_TEAMS = 20, MAX_LEAGUES = 8;

    /** the first field: a team's or a league's id */
    static String key(String entry) { return entry == null ? "" : entry.split("\\|", 2)[0]; }

    /** a name as kept: none of the list's separators, one line, at most 60 */
    static String clean(String s) {
        String v = s == null ? "" : s.replaceAll("[;,|~&#\\r\\n\\t]+", " ").replaceAll("\\s+", " ").trim();
        return v.length() > 60 ? v.substring(0, 60).trim() : v;
    }

    static String team(String id, String name) { return id != null && id.matches("\\d{1,9}") ? id + "|" + clean(name) : null; }

    /** token: the page's word for the league (the built-in English name, else "id" + id) */
    static String league(String id, String ar, String en, String token) {
        if (id == null || !id.matches("\\d{1,6}")) return null;
        String t = token != null && token.matches("[A-Za-z0-9 \\-]{2,40}") ? token : "id" + id;
        return id + "|" + clean(ar) + "|" + clean(en) + "|" + t;
    }

    /** the list with the entry at its end; the same id is never twice; null when full or bad (unchanged) */
    static List<String> add(List<String> list, String entry, int max) {
        if (entry == null) return null;
        for (String s : list) if (key(s).equals(key(entry))) return list;
        if (list.size() >= max) return null;
        List<String> v = new ArrayList<>(list);
        v.add(entry);
        return v;
    }

    static List<String> remove(List<String> list, String id) {
        List<String> v = new ArrayList<>();
        for (String s : list) if (!key(s).equals(id)) v.add(s);
        return v;
    }

    /** the league's token in an entry */
    static String token(String entry) { String[] p = entry.split("\\|", -1); return p.length >= 4 ? p[3] : "id" + key(entry); }

    /** Does the screensaver's #leagues= list (its tokens) have this league already, by token or by id? */
    static boolean hasLeague(List<String> tokens, String entry) {
        for (String t : tokens) if (t.equalsIgnoreCase(token(entry)) || t.equals("id" + key(entry))) return true;
        return false;
    }

    /** The page's settings with the home's follows joined; null (the page's defaults) when there is no fragment and the home keeps none. */
    static String merge(String frag, List<String> teams, List<String> leagues) {
        if (frag == null && teams.isEmpty() && leagues.isEmpty()) return null;
        Map<String, String> p = new LinkedHashMap<>();
        if (frag != null) for (String kv : frag.split("&")) {
            if (kv.isEmpty()) continue;
            int i = kv.indexOf('=');
            p.put(i < 0 ? kv : kv.substring(0, i), i < 0 ? "" : kv.substring(i + 1));
        }
        List<String> ids = split(dec(p.get("teams")));
        for (String t : teams) if (!ids.contains(key(t)) && ids.size() < MAX_TEAMS) ids.add(key(t));
        if (!teams.isEmpty() || p.containsKey("teams")) p.put("teams", String.join(",", ids));
        List<String> toks = split(dec(p.get("leagues")));
        StringBuilder names = new StringBuilder(dec(p.get("lgn")));
        for (String l : leagues) {
            if (hasLeague(toks, l) || toks.size() >= MAX_LEAGUES) continue;
            toks.add(token(l));
            String[] e = l.split("\\|", -1);
            String n = e.length > 1 && !e[1].isEmpty() ? e[1] : e.length > 2 ? e[2] : "";
            if (token(l).startsWith("id") && !n.isEmpty() && !("|" + names).contains("|" + key(l) + "~")) names.append(names.length() > 0 ? "|" : "").append(key(l)).append('~').append(n);
        }
        if (!leagues.isEmpty() || p.containsKey("leagues")) p.put("leagues", enc(String.join(",", toks)));
        if (names.length() > 0) p.put("lgn", enc(names.toString()));
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : p.entrySet()) out.append(out.length() > 0 ? "&" : "").append(e.getKey()).append('=').append(e.getValue());
        return out.toString();
    }

    static List<String> split(String v) {
        List<String> out = new ArrayList<>();
        for (String s : v.split(",")) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }

    private static String dec(String v) {
        if (v == null) return "";
        try { return URLDecoder.decode(v.replace("+", "%2B"), "UTF-8"); } catch (UnsupportedEncodingException | IllegalArgumentException e) { return v; }
    }

    private static String enc(String v) {
        try { return URLEncoder.encode(v, "UTF-8").replace("+", "%20"); } catch (UnsupportedEncodingException e) { return v; }
    }
}
