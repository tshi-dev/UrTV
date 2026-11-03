package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The screens' words in «اللغة» (docs/ENGLISH.md). Every Arabic text a screen shows goes through t(): S.t("حفظ") is "حفظ" with Arabic, the
 * default, and the entry of i18n/en/*.tsv with English (the Arabic when it has none; tests/i18n_test.py fails the build on a missing one).
 * Arabic never reads the table.
 *
 * The setting is the screensaver's (Prefs.lang), read the way Digits reads «الأرقام»: through ConfigSnapshot (&lang=en in the fragment,
 * Tr.fromFragment) and kept here for the next start, so a screen opened alone has it at once. Digits.init and Digits.remember pass each
 * snapshot on (remember below); wrap() gives an Activity the kept value without asking the screensaver.
 */
public final class S {
    private S() { }

    private static final String PREFS = "urtv_lang";
    private static volatile String lang = Tr.AR;
    private static volatile boolean loaded;
    private static volatile Map<String, String> table;
    private static volatile Context app;

    /** the Arabic text in the setting's language, with %s (or %1$s…) filled from args */
    public static String t(String ar, Object... args) { return Tr.t(lang, Tr.EN.equals(lang) ? table() : null, ar, args); }

    /**
     * A word kept for later, e.g. a label in a constant array: S.n("هندية ١٢٣") is the text itself, and marks it for the table (the build
     * test reads S.n literals as it reads S.t ones). The screen shows it with S.tv(label).
     */
    public static String n(String ar) { return ar; }

    /** t() for a value that is not a literal here: a text marked with S.n, or one the code got from the table's own words */
    public static String tv(String ar, Object... args) { return Tr.t(lang, Tr.EN.equals(lang) ? table() : null, ar, args); }

    public static String lang() { return lang; }
    public static boolean en() { return Tr.EN.equals(lang); }

    /** the kept setting (no cross-process read): for a service or receiver that shows words, before t */
    public static void init(Context c) {
        Context a = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        app = a;
        if (loaded) return;
        lang = Tr.norm(a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("lang", Tr.AR));
        loaded = true;
    }

    /** The person chose a language on the installer's start screen: kept here at once (the next screen is built in it); the screensaver, which holds the setting, is told by SetupLang. */
    public static void set(Context c, String v) {
        init(c);
        lang = Tr.norm(v);
        c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("lang", lang).apply();
    }

    /** what the screensaver says now (Digits passes every snapshot on); a missing screensaver leaves the kept value */
    static void remember(Context c, ConfigSnapshot snap) {
        init(c);
        if (snap == null || snap.fragment == null) return;
        String now = snap.lang();
        if (now.equals(lang)) return;
        lang = now;
        SharedPreferences sp = c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString("lang", now).apply();
    }

    /**
     * For each Activity's attachBaseContext: with English, an English configuration (values-en resources, a left-to-right default). With
     * Arabic it returns base itself: the screen is exactly what it was.
     */
    public static Context wrap(Context base) {
        init(base);
        if (!en()) return base;
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.setLocale(Locale.ENGLISH);
        return base.createConfigurationContext(cfg);
    }

    /** every i18n/en/*.tsv in the assets, read once, the first time English needs it; empty when there is none */
    private static Map<String, String> table() {
        Map<String, String> m = table;
        if (m != null) return m;
        final Context c = app;
        if (c == null) return java.util.Collections.emptyMap();   // before init: the Arabic words, and the table is read later
        synchronized (S.class) {
            if (table != null) return table;
            m = new HashMap<>();
            try {
                String[] files = c.getAssets().list("i18n/en");
                if (files != null) for (String f : files) {
                    if (!f.endsWith(".tsv")) continue;
                    try (InputStream in = c.getAssets().open("i18n/en/" + f)) {
                        ByteArrayOutputStream b = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        for (int n; (n = in.read(buf)) > 0; ) b.write(buf, 0, n);
                        Tr.parse(new String(b.toByteArray(), "UTF-8"), m);
                    }
                }
            } catch (java.io.IOException | RuntimeException ignored) {
                // no table: the Arabic words, never a crash
            }
            spaced(m);
            table = m;
            return m;
        }
    }

    /**
     * The build (aapt) keeps an XML string's runs of spaces as ONE space, so getString gives «متوقف • الأذان كاملًا» for the «متوقف  •  الأذان
     * كاملًا» of res/values/strings.xml and of the table (tester step 20: the Tilawah and adhan footers stayed Arabic). Each entry with a run of
     * spaces is found by its one-space form too (its English as written).
     */
    static void spaced(Map<String, String> m) {
        for (Map.Entry<String, String> e : new java.util.ArrayList<>(m.entrySet())) {
            String one = e.getKey().replaceAll(" {2,}", " ");
            if (!one.equals(e.getKey()) && !m.containsKey(one)) m.put(one, e.getValue());
        }
    }
}
