package sa.urtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * «الأسرع»: the question bank and what the quiz remembers (docs/QUIZ.md). The questions are loaded on demand: a small starter set ships with the app
 * (assets/quiz/starter.json, so the first game works with no network); the verified bank is fetched from the data branch (quiz/quiz.json, built by
 * .github/tools/make_quiz_bank.py), kept in the app's files and used from the next game on; a fetch happens at most every 3 days and only when the quiz
 * screen opens. What is remembered: the chips of the last game, the sound switch and the ids of the last 100 questions asked (so they are not repeated).
 * No player, name or score is ever stored.
 */
final class QuizBank {
    private QuizBank() {}

    static final String URL = "https://raw.githubusercontent.com/tshi-dev/UrTV/data-games/quiz/quiz.json";
    static final long REFRESH_MS = 3L * 24 * 3600 * 1000;
    private static final String FILE = "quiz-bank.json", PREFS = "quiz";
    private static volatile boolean fetching;

    static List<QuizPick.Item> load(Context c) {
        List<QuizPick.Item> v = null;
        File f = new File(c.getFilesDir(), FILE);
        if (f.isFile()) v = parse(read(f));
        if (v == null || v.size() < QuizPick.MIN_ITEMS) {   // nothing fetched yet, or a damaged file: the starter set
            try (InputStream in = c.getAssets().open("quiz/starter.json")) { v = parse(new String(Bounded.readAll(in, Bounded.JSON_MAX), "UTF-8")); }
            catch (Exception e) { v = new ArrayList<>(); }
        }
        return v;
    }

    private static String read(File f) {
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) { return new String(Bounded.readAll(in, Bounded.JSON_MAX), "UTF-8"); }
        catch (Exception e) { return null; }
    }

    static List<QuizPick.Item> parse(String json) {
        if (json == null) return null;
        List<QuizPick.Item> v = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                JSONArray q = o.optJSONArray("q"), an = o.optJSONArray("a"), w = o.optJSONArray("w");
                if (q == null || an == null || w == null || w.length() != 3 || q.length() < 2 || an.length() < 2) continue;
                String[][] wrong = new String[3][];
                boolean ok = true;
                for (int k = 0; k < 3; k++) {
                    JSONArray x = w.optJSONArray(k);
                    if (x == null || x.length() < 2) { ok = false; break; }
                    wrong[k] = new String[]{x.getString(0), x.getString(1)};
                }
                int d = o.optInt("d", 0);
                if (!ok || d < 1 || d > 3 || o.optString("i").isEmpty()) continue;
                v.add(new QuizPick.Item(o.getString("i"), o.optString("c"), d, o.optInt("k") == 1, new String[]{q.getString(0), q.getString(1)}, new String[]{an.getString(0), an.getString(1)}, wrong));
            }
        } catch (Exception e) {
            return null;
        }
        return v;
    }

    /** a background fetch of the verified bank when the kept one is older than 3 days (or missing); used from the next game on */
    static void refresh(final Context c) {
        final Context app = c.getApplicationContext();
        final File f = new File(app.getFilesDir(), FILE);
        if (fetching || (f.isFile() && System.currentTimeMillis() - f.lastModified() < REFRESH_MS)) return;
        fetching = true;
        new Thread(() -> {
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) new URL(URL).openConnection();
                con.setConnectTimeout(10000);
                con.setReadTimeout(20000);
                con.setRequestProperty("User-Agent", "urtv/2 (Android TV)");
                if (con.getResponseCode() != 200) return;
                byte[] b;
                try (InputStream in = con.getInputStream()) { b = Bounded.readAll(in, Bounded.JSON_MAX); }
                List<QuizPick.Item> v = parse(new String(b, "UTF-8"));
                if (v == null || v.size() < QuizPick.MIN_ITEMS) return;   // never replace a good file with a bad one
                File tmp = new File(app.getFilesDir(), FILE + ".tmp");
                try (FileOutputStream o = new FileOutputStream(tmp)) { o.write(b); }
                if (!tmp.renameTo(f)) tmp.delete();
            } catch (Exception ignored) {
                // no network: the kept bank or the starter set is used
            } finally {
                if (con != null) con.disconnect();
                fetching = false;
            }
        }, "quiz-bank").start();
    }

    // ---------------------------------------------------------------- what is remembered

    private static SharedPreferences sp(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static String category(Context c) { return sp(c).getString("cat", QuizPick.MIX); }
    static int level(Context c) { return sp(c).getInt("level", 3); }        // an index into QuizPick.LEVELS (3 = «متنوع»)
    static int count(Context c) { return sp(c).getInt("count", 1); }        // an index into QuizPick.COUNTS (1 = 10)
    static boolean kids(Context c) { return sp(c).getBoolean("kids", false); }
    static boolean sound(Context c) { return sp(c).getBoolean("sound", true); }
    static void save(Context c, String cat, int level, int count, boolean kids, boolean sound) {
        sp(c).edit().putString("cat", cat).putInt("level", level).putInt("count", count).putBoolean("kids", kids).putBoolean("sound", sound).apply();
    }

    static Set<String> recent(Context c) {
        String s = sp(c).getString("recent", "");
        Set<String> v = new LinkedHashSet<>();
        if (!s.isEmpty()) for (String x : s.split(",")) v.add(x);
        return v;
    }

    /** the ids of this game's questions go to the front of the last 100 */
    static void remember(Context c, List<QuizPick.Item> asked) {
        List<String> all = new ArrayList<>();
        for (QuizPick.Item it : asked) all.add(it.id);
        for (String id : recent(c)) if (!all.contains(id)) all.add(id);
        if (all.size() > QuizPick.RECENT_MAX) all = all.subList(0, QuizPick.RECENT_MAX);
        sp(c).edit().putString("recent", String.join(",", all)).apply();
    }
}
