package sa.urtv.screensaver;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Our club list, assets/shared/teams.json (every club of Prefs.LEAGUES and the national teams, built in CI):
 *  the same list the screen's names and the setup's find-team read. Empty if it can't be read, never a crash. */
final class Teams {
    private Teams() {}

    static final String NATIONAL = "National teams";   // the "league" of the national teams in teams.json

    private static List<TeamSearch.Team> all;
    private static final Map<String, TeamSearch.Team> BY_ID = new HashMap<>();
    private static final Set<String> TRANSLIT = new HashSet<>();   // ids whose Arabic name is only a transliteration

    static synchronized List<TeamSearch.Team> all(Context c) {
        if (all != null) return all;
        List<TeamSearch.Team> v = new ArrayList<>();
        try (InputStream in = c.getAssets().open("shared/teams.json")) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            JSONArray a = new JSONArray(b.toString("UTF-8"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null || o.optString("id").isEmpty()) continue;
                TeamSearch.Team t = new TeamSearch.Team(o.optString("id"), o.optString("en"), o.optString("ar"),
                    strings(o.optJSONArray("leagues")), strings(o.optJSONArray("alt")), o.optString("sport", "Soccer"));
                v.add(t);
                BY_ID.put(t.id, t);
                if ("translit".equals(o.optString("arFrom"))) TRANSLIT.add(t.id);
            }
        } catch (Exception e) {
            v.clear();
            BY_ID.clear();
        }
        all = v;
        return all;
    }

    private static String[] strings(JSONArray a) {
        if (a == null) return new String[0];
        String[] out = new String[a.length()];
        for (int i = 0; i < out.length; i++) out[i] = a.optString(i);
        return out;
    }

    static TeamSearch.Team byId(Context c, String id) {
        all(c);
        synchronized (Teams.class) { return BY_ID.get(id); }
    }

    /** The name to show: Arabic, with the English one beside a mere transliteration; with «اللغة» English, the English name. */
    static String label(Context c, TeamSearch.Team t) {
        all(c);
        if (t.ar.isEmpty() || (S.en() && !t.en.isEmpty())) return t.en;
        synchronized (Teams.class) { return TRANSLIT.contains(t.id) ? t.ar + "  ·  " + t.en : t.ar; }
    }

    /** The clubs of one league (or NATIONAL), in Arabic alphabetical order. */
    static List<TeamSearch.Team> inLeague(Context c, String league) {
        List<TeamSearch.Team> out = new ArrayList<>();
        for (TeamSearch.Team t : all(c)) for (String l : t.leagues) if (l.equalsIgnoreCase(league)) { out.add(t); break; }
        final Collator ar = Collator.getInstance(new Locale("ar"));
        Collections.sort(out, (a, b) -> ar.compare(a.ar.isEmpty() ? a.en : a.ar, b.ar.isEmpty() ? b.en : b.ar));
        return out;
    }

    static List<TeamSearch.Team> search(Context c, String q) { return TeamSearch.search(all(c), q); }
}
