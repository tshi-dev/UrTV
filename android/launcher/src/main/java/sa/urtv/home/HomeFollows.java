package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.3.3 «المباريات» follows a team or a league even when the screensaver app (it keeps the lists) is missing or older: the home keeps it here
 * until the screensaver takes it (sync, each time the page opens), and the page gets both lists joined, none twice (FollowMerge).
 */
final class HomeFollows {
    private HomeFollows() { }

    private static final Uri CONFIG = Uri.parse("content://sa.urtv.screensaver.config");

    private static SharedPreferences sp(Context c) { return c.getSharedPreferences("home_follows", Context.MODE_PRIVATE); }

    static List<String> list(Context c, String k) {
        List<String> out = new ArrayList<>();
        try { for (String s : sp(c).getString(k, "").split(";")) if (!s.isEmpty()) out.add(s); } catch (RuntimeException ignored) { }
        return out;
    }

    private static boolean put(Context c, String k, List<String> v) {
        if (v == null) return false;
        try { return sp(c).edit().putString(k, String.join(";", v)).commit(); } catch (RuntimeException e) { return false; }
    }

    static boolean addTeam(Context c, String id, String name) { return put(c, "teams", FollowMerge.add(list(c, "teams"), FollowMerge.team(id, name), FollowMerge.MAX_TEAMS)); }
    static boolean removeTeam(Context c, String id) { List<String> v = list(c, "teams"); List<String> w = FollowMerge.remove(v, id); return w.size() < v.size() && put(c, "teams", w); }
    static boolean addLeague(Context c, String id, String ar, String en, String token) {
        return put(c, "leagues", FollowMerge.add(list(c, "leagues"), FollowMerge.league(id, ar, en, token), FollowMerge.MAX_LEAGUES));
    }
    static boolean removeLeague(Context c, String id) { List<String> v = list(c, "leagues"); List<String> w = FollowMerge.remove(v, id); return w.size() < v.size() && put(c, "leagues", w); }

    /** the page's settings with the home's own follows joined */
    static String fragment(Context c, String frag) { return FollowMerge.merge(frag, list(c, "teams"), list(c, "leagues")); }

    /** The screensaver answers: each kept follow goes to it (or is there already) and leaves the home's list; one it refuses or cannot take stays. */
    static void sync(Context c) {
        List<String> teams = list(c, "teams"), leagues = list(c, "leagues");
        if (teams.isEmpty() && leagues.isEmpty()) return;
        String frag = null;
        try (Cursor cur = c.getContentResolver().query(CONFIG, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) frag = cur.getString(0);
        } catch (RuntimeException e) { return; }
        if (frag == null) return;   // missing, or signed with another key: kept here
        String merged = FollowMerge.merge(frag, new ArrayList<String>(), new ArrayList<String>());
        List<String> ids = new ArrayList<>(), toks = new ArrayList<>();
        for (String kv : merged.split("&")) {
            if (kv.startsWith("teams=")) ids = FollowMerge.split(kv.substring(6));
            if (kv.startsWith("leagues=")) toks = FollowMerge.split(Uri.decode(kv.substring(8)));
        }
        List<String> keepT = new ArrayList<>(), keepL = new ArrayList<>();
        for (String t : teams) if (!ids.contains(FollowMerge.key(t)) && !ok(c, "phone_follow", FollowMerge.key(t) + ":" + t.split("\\|", 2)[1])) keepT.add(t);
        for (String l : leagues) {
            String[] e = l.split("\\|", -1);
            if (!FollowMerge.hasLeague(toks, l) && !ok(c, "phone_follow_league", e[0] + "|" + e[1] + "|" + e[2])) keepL.add(l);
        }
        put(c, "teams", keepT);
        put(c, "leagues", keepL);
    }

    /** the screensaver's answer: null when it is missing or older (the method is not there), else its "ok" */
    static Boolean call(Context c, String method, String arg) {
        try {
            Bundle out = c.getContentResolver().call(CONFIG, method, arg, null);
            return out == null || !out.containsKey("ok") ? null : out.getBoolean("ok");
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 4.4 what one more league or club means, as the screensaver (it keeps the lists) says: "add", "warn" (past the soft limit, its one
     * warning not yet answered) or "full"; "" when it is missing or older (the page keeps its old rule then)
     */
    static String state(Context c, String kind) {
        try {
            Bundle out = c.getContentResolver().call(CONFIG, "phone_follow_check", "leagues".equals(kind) ? "leagues" : "teams", null);
            String s = out == null ? null : out.getString("state");
            return s != null && s.matches("add|warn|full") ? s : "";
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static boolean ok(Context c, String method, String arg) { return Boolean.TRUE.equals(call(c, method, arg)); }
}
