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
 * (assets/quiz/bank.json, the whole verified bank of the day the app was built, so a game works offline; assets/quiz/starter.json is the last resort); the newer bank is
 * fetched from the data branch (quiz/quiz-v2.json, built by games/tools/build_meta.py) in the background when a quiz screen opens, at most once a day and only when
 * quiz/rev.txt says it is newer, kept in the app's files and used from the next time a quiz screen opens. A game reads only from local storage, whole, before it starts. What is remembered: the chips of the last game, the sound switch and the ids of the last 100 questions asked (so they are not repeated).
 * No player, name or score is ever stored.
 */
final class QuizBank {
    private QuizBank() {}

    static final String URL = "https://raw.githubusercontent.com/tshi-dev/UrTV/data-games/quiz/quiz.json", URL_V2 = "https://raw.githubusercontent.com/tshi-dev/UrTV/data-games/quiz/quiz-v2.json";
    /** the tiny file next to the bank that says which build of the bank is published (the UTC date and hour, 2026111600): the app downloads the bank only when this is newer than what it holds */
    static final String URL_REV = "https://raw.githubusercontent.com/tshi-dev/UrTV/data-games/quiz/rev.txt";
    static final long CHECK_MS = 24L * 3600 * 1000;
    /** the build of the bank bundled in the app (assets/quiz/bank.json): a downloaded copy older than this is not used */
    static final long BUNDLED_REV = 2026111609L;
    private static final String FILE = "quiz-bank.json", PREFS = "quiz";
    private static volatile boolean fetching;
    /** a round is being played: the background download waits (it is only ever started when a quiz screen opens, and it never writes anything a round reads) */
    static volatile boolean playing;

    /** the bank of a game, always from this TV's own storage, whole and in memory before the first question: the downloaded copy when it is at least as new as the
     *  bundled one and passes the check, else the bundled bank (assets/quiz/bank.json), else the small starter set. No network, no error screen; never throws.
     *  The downloaded packs of questions are NOT in it (each is parsed with a key derivation, a decryption and an inflate: seconds on a small TV): a screen opens with this
     *  bank and calls {@link #withExtras} to have them added from a thread. */
    static List<QuizPick.Item> load(Context c) {
        List<QuizPick.Item> v = null;
        File f = new File(c.getFilesDir(), FILE);
        if (f.isFile() && sp(c).getLong("rev", 0) >= BUNDLED_REV) v = QuizBankCheck.parse(read(f));
        if (v == null || v.size() < QuizPick.MIN_ITEMS) v = asset(c, "quiz/bank.json");
        if (v == null || v.size() < QuizPick.MIN_ITEMS) v = asset(c, "quiz/starter.json");
        return v == null ? new ArrayList<>() : v;
    }

    /** a thread adds the installed packs of questions to `base` and `ready` runs on the main thread with the whole bank (not at all when there is no pack: the screen has what it needs). */
    static void withExtras(Context c, final List<QuizPick.Item> base, final java.util.function.Consumer<List<QuizPick.Item>> ready) {
        final Context app = c.getApplicationContext();
        Thread t = new Thread(() -> {
            try {
                final List<QuizPick.Item> extra = extras(app);
                if (extra.isEmpty()) return;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> ready.accept(merge(base, extra)));
            } catch (Throwable ignored) { /* the screen keeps the bank it has */ }
        }, "quiz-packs");
        t.setPriority(Thread.MIN_PRIORITY); t.setDaemon(true); t.start();
    }

    /** the merged questions of the installed packs of questions (kind «quiz»), in the index's order: an id seen first is kept; a missing or bad pack adds nothing. No question is bundled in the app.
     *  Blocking (call it from a thread). Reading a pack here is not using it (.used is not stamped: see {@link #remember}); the answer is kept until a pack comes, goes or changes. */
    static synchronized List<QuizPick.Item> extras(Context c) {
        List<GamePacks.Pack> packs = GamePackStore.quizPacks(c);
        StringBuilder key = new StringBuilder();
        for (GamePacks.Pack p : packs) key.append(p.id).append(':').append(GamePackStore.size(c, p.id)).append(':').append(GamePackStore.version(c, p.id)).append(';');
        if (key.toString().equals(extrasKey) && extrasList != null) return extrasList;
        List<QuizPick.Item> out = new ArrayList<>();
        java.util.Map<String, String> owner = new java.util.HashMap<>();
        for (GamePacks.Pack p : packs) {
            try {
                byte[] raw = GameBundles.peekBytes(c, p.id);
                List<QuizPick.Item> extra = raw == null ? null : QuizBankCheck.parse(raw);
                if (extra == null) continue;
                int before = out.size();
                out = merge(out, extra);
                for (QuizPick.Item it : out.subList(before, out.size())) owner.put(it.id, p.id);
            } catch (RuntimeException e) { /* a bad pack adds nothing */ }
        }
        extrasKey = key.toString(); extrasList = out; owners = owner;
        return out;
    }
    private static String extrasKey;
    private static List<QuizPick.Item> extrasList;
    /** the id of a question that came from a pack -> the pack */
    private static volatile java.util.Map<String, String> owners = new java.util.HashMap<>();
    static List<QuizPick.Item> merge(List<QuizPick.Item> base, List<QuizPick.Item> extra) {
        java.util.Set<String> ids = new java.util.HashSet<>(); for (QuizPick.Item it : base) ids.add(it.id);
        List<QuizPick.Item> out = new ArrayList<>(base);
        for (QuizPick.Item it : extra) if (ids.add(it.id)) out.add(it);
        return out;
    }

    private static List<QuizPick.Item> asset(Context c, String name) {
        try (InputStream in = c.getAssets().open(name)) { return QuizBankCheck.parse(Bounded.readAll(in, QuizBankCheck.BYTES_MAX)); }
        catch (Exception e) { return null; }
    }

    private static byte[] read(File f) {
        if (f.length() > QuizBankCheck.BYTES_MAX) return null;
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) { return Bounded.readAll(in, QuizBankCheck.BYTES_MAX); }
        catch (Exception e) { return null; }
    }

    /** In the background, when a quiz screen opens and at most once a day: ask for the published build (rev.txt, a few bytes) and download the bank only when it is
     *  newer than what this TV holds. The download is read with a size cap and checked whole (QuizBankCheck) before it replaces the kept file; anything wrong, no network
     *  or a round in play, and the kept copy (or the bundled one) stays. It is used from the next time a quiz screen opens. */
    static void refresh(final Context c) {
        GamePackStore.ensureQuiz(c, null);   // the new packs of questions, when the person opens a quiz screen (a small file first, a pack only when there is room)
        final Context app = c.getApplicationContext();
        final File f = new File(app.getFilesDir(), FILE);
        final SharedPreferences p = sp(app);
        if (fetching || System.currentTimeMillis() - p.getLong("checked", 0) < CHECK_MS) return;
        fetching = true;
        Thread t = new Thread(() -> {
            try {
                long remote = fetchRev();
                if (remote < 0) return;                      // no network, or no rev file: nothing is touched, asked again next time
                long have = f.isFile() ? Math.max(BUNDLED_REV, p.getLong("rev", 0)) : BUNDLED_REV;
                if (remote > have) {
                    if (playing) return;                     // never while a round is on
                    byte[] b = fetch(URL_V2);                // the versioned file first; an app that finds none (or a bad one) takes the old array
                    if (b == null) b = fetch(URL);
                    if (b == null) return;                   // never replace a good file with a bad one
                    File tmp = new File(app.getFilesDir(), FILE + ".tmp");
                    try (FileOutputStream o = new FileOutputStream(tmp)) { o.write(b); }
                    if (!tmp.renameTo(f)) { tmp.delete(); return; }
                    p.edit().putLong("rev", remote).apply();
                }
                p.edit().putLong("checked", System.currentTimeMillis()).apply();
            } catch (Exception ignored) {
                // no network, a file too big, a bad answer: the kept bank or the bundled one is used
            } finally {
                fetching = false;
            }
        }, "quiz-bank");
        t.setPriority(Thread.MIN_PRIORITY); t.setDaemon(true); t.start();
    }

    /** the published build number, or -1 */
    static long fetchRev() {
        try {
            Net.Reply r = DataHost.get(URL_REV, 8000, 10000, 64);   // urtv-data, then the old branch (DataHost)
            return r.status == 200 ? QuizBankCheck.parseRev(r.body) : -1;
        } catch (Exception e) { return -1; }
    }
    /** one download, read with a size cap and checked whole; the bytes, or null when anything is wrong */
    private static byte[] fetch(String url) {
        try {
            Net.Reply r = DataHost.get(url, 15000, 20000, QuizBankCheck.BYTES_MAX);   // urtv-data, then the old branch (DataHost); no redirect, a size cap, IPv4 first (Net)
            if (r.status != 200) return null;
            List<QuizPick.Item> v = QuizBankCheck.parse(r.body);
            return v == null || v.size() < QuizPick.MIN_ITEMS ? null : r.body;
        } catch (Exception e) { return null; }
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

    /** the ids of this game's questions go to the front of the last 100; a pack that gave one of them is used now (its 14 days start again: only a pack a round really used) */
    static void remember(Context c, List<QuizPick.Item> asked) {
        java.util.Set<String> used = new java.util.HashSet<>();
        for (QuizPick.Item it : asked) { String pid = owners.get(it.id); if (pid != null) used.add(pid); }
        for (String pid : used) GamePackStore.touch(c, pid);
        List<String> all = new ArrayList<>();
        for (QuizPick.Item it : asked) all.add(it.id);
        for (String id : recent(c)) if (!all.contains(id)) all.add(id);
        if (all.size() > QuizPick.RECENT_MAX) all = all.subList(0, QuizPick.RECENT_MAX);
        sp(c).edit().putString("recent", String.join(",", all)).apply();
    }
}
