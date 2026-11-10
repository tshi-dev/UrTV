package sa.urtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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

    /** the kept bank if it passes the check, else the starter set (which passes the same check); never throws */
    static List<QuizPick.Item> load(Context c) {
        List<QuizPick.Item> v = null;
        File f = new File(c.getFilesDir(), FILE);
        if (f.isFile()) v = QuizBankCheck.parse(read(f));
        if (v == null || v.size() < QuizPick.MIN_ITEMS) {   // nothing fetched yet, or a damaged file: the starter set
            try (InputStream in = c.getAssets().open("quiz/starter.json")) { v = QuizBankCheck.parse(Bounded.readAll(in, QuizBankCheck.BYTES_MAX)); }
            catch (Exception e) { v = null; }
        }
        return v == null ? new ArrayList<>() : v;
    }

    private static byte[] read(File f) {
        if (f.length() > QuizBankCheck.BYTES_MAX) return null;
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) { return Bounded.readAll(in, QuizBankCheck.BYTES_MAX); }
        catch (Exception e) { return null; }
    }

    /** a background fetch of the verified bank when the kept one is older than 3 days (or missing); used from the next game on. The download is read with a
     *  size cap and checked whole (QuizBankCheck) before it replaces the kept file; anything wrong and the last good copy stays. */
    static void refresh(final Context c) {
        final Context app = c.getApplicationContext();
        final File f = new File(app.getFilesDir(), FILE);
        if (fetching || (f.isFile() && System.currentTimeMillis() - f.lastModified() < REFRESH_MS)) return;
        fetching = true;
        new Thread(() -> {
            try {
                Net.Reply r = Net.get(URL, null, 15000, 20000, QuizBankCheck.BYTES_MAX);   // no redirect, a size cap, IPv4 first (Net)
                if (r.status != 200) return;
                byte[] b = r.body;
                List<QuizPick.Item> v = QuizBankCheck.parse(b);
                if (v == null || v.size() < QuizPick.MIN_ITEMS) return;   // never replace a good file with a bad one
                File tmp = new File(app.getFilesDir(), FILE + ".tmp");
                try (FileOutputStream o = new FileOutputStream(tmp)) { o.write(b); }
                if (!tmp.renameTo(f)) tmp.delete();
            } catch (Exception ignored) {
                // no network, a file too big, a bad answer: the kept bank or the starter set is used
            } finally {
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
