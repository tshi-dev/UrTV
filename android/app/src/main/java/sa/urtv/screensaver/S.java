package sa.urtv.screensaver;

import android.content.Context;
import android.content.res.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The screens' words in «اللغة» (docs/ENGLISH.md). Every Arabic text a screen shows goes through t(): S.t("حفظ") is "حفظ" with Arabic, the
 * default, and the entry of i18n/en/*.tsv with English (the Arabic when it has none; tests/i18n_test.py fails the build on a missing one).
 * Arabic never reads the table. The setting itself is Prefs.lang() (this app holds it); wrap() reads it for each screen.
 */
final class S {
    private S() { }

    private static volatile String lang = Tr.AR;
    private static volatile Map<String, String> table;
    private static volatile Context app;

    /** the Arabic text in the setting's language, with %s (or %1$s…) filled from args */
    static String t(String ar, Object... args) { return Tr.t(lang, Tr.EN.equals(lang) ? table() : null, ar, args); }

    /**
     * A word kept for later, e.g. a label in a constant array: S.n("هندية ١٢٣") is the text itself, and marks it for the table (the build
     * test reads S.n literals as it reads S.t ones). The screen shows it with S.tv(label).
     */
    static String n(String ar) { return ar; }

    /** t() for a value that is not a literal here: a text marked with S.n, or one the code got from the table's own words */
    static String tv(String ar, Object... args) { return Tr.t(lang, Tr.EN.equals(lang) ? table() : null, ar, args); }

    static String lang() { return lang; }
    static boolean en() { return Tr.EN.equals(lang); }

    /** the setting as it is saved now (a service or receiver that shows words calls it before t) */
    static void init(Context c) {
        app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        lang = Tr.norm(new Prefs(c).lang());
    }

    /** Prefs.setLang: the next t() follows at once (an open screen is redrawn by its own code) */
    static void set(String v) { lang = Tr.norm(v); }

    /**
     * For each Activity's attachBaseContext: reads the setting, and with English gives the screen an English configuration (its
     * values-en resources, a left-to-right default). With Arabic it returns base itself: the screen is exactly what it was.
     */
    static Context wrap(Context base) {
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
            table = m;
            return m;
        }
    }
}
