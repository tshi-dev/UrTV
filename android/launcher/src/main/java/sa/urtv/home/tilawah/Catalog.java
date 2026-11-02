package sa.urtv.home.tilawah;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The reciters, the live streams and the 114 surah names, read once from the bundled JSON (again when «اللغة» changes). With English
 * the names are the JSON's "en" (the transliterated surah names, the reciters' names as written in Latin letters); a station is still
 * found by its Arabic name (key), the one the computer and the saved choice use.
 */
final class Catalog {

    static final class Reciter {
        final String id, name, server;
        final int surahs;
        Reciter(String id, String name, String server, int surahs) {
            this.id = id; this.name = name; this.server = server; this.surahs = surahs;
        }
    }

    static final class Station {
        /** the Arabic name, whatever «اللغة» says: what stationIndex finds it by */
        String key;
        final String name, url;
        final boolean live;
        Station(String name, String url, boolean live) { this.name = name; this.url = url; this.live = live; }
    }

    final List<Reciter> reciters = new ArrayList<>();
    final List<Station> stations = new ArrayList<>();
    final String[] surahNames = new String[115];

    private static Catalog cached;
    private String lang;

    static synchronized Catalog get(Context context) {
        sa.urtv.home.S.init(context);
        if (cached == null || !sa.urtv.home.S.lang().equals(cached.lang)) cached = load(context.getApplicationContext());
        return cached;
    }

    private static Catalog load(Context c) {
        Catalog catalog = new Catalog();
        catalog.lang = sa.urtv.home.S.lang();
        boolean en = sa.urtv.home.S.en();
        try {
            JSONArray reciters = new JSONObject(read(c, "quran/reciters.json")).getJSONArray("reciters");
            for (int i = 0; i < reciters.length(); i++) {
                JSONObject r = reciters.getJSONObject(i);
                catalog.reciters.add(new Reciter(r.getString("id"), en ? r.optString("en", r.getString("name")) : r.getString("name"),
                        r.getString("server"), r.getInt("surahs")));
            }
            JSONArray stations = new JSONObject(read(c, "quran/stations.json")).getJSONArray("stations");
            for (int i = 0; i < stations.length(); i++) {
                JSONObject s = stations.getJSONObject(i);
                Station st = new Station(en ? s.optString("en", s.getString("name")) : s.getString("name"), s.getString("url"), s.optBoolean("live", true));
                st.key = s.getString("name");
                catalog.stations.add(st);
            }
            JSONArray surahs = new JSONObject(read(c, "quran/surahs.json")).getJSONArray("surahs");
            for (int i = 0; i < surahs.length(); i++) {
                JSONObject s = surahs.getJSONObject(i);
                int n = s.getInt("n");
                if (n >= 1 && n <= 114) catalog.surahNames[n] = en ? s.optString("en", s.getString("name")) : s.getString("name");
            }
        } catch (Exception e) {
            throw new IllegalStateException("quran assets are missing or broken", e);
        }
        return catalog;
    }

    private static String read(Context c, String path) throws Exception {
        try (InputStream in = c.getAssets().open(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    String surahName(int n) {
        return n >= 1 && n <= 114 && surahNames[n] != null ? surahNames[n] : String.valueOf(n);
    }

    int reciterIndex(String id) {
        for (int i = 0; i < reciters.size(); i++) if (reciters.get(i).id.equals(id)) return i;
        return -1;
    }

    int stationIndex(String name) {
        for (int i = 0; i < stations.size(); i++) if (stations.get(i).key.equals(name) || stations.get(i).name.equals(name)) return i;
        return -1;
    }
}
