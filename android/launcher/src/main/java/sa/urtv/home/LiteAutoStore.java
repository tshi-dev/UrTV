package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 4.4 beta 3 (LiteAuto): the signals of a weak TV, kept in the «ram» preferences next to «الوضع الخفيف». Written on a thread of its own (never the UI thread);
 * Ram.tier reads them at the home's next start. When it comes on, it is said once (tellOnce, from the home's start thread): never a silent change.
 */
public final class LiteAutoStore {
    private LiteAutoStore() { }

    static final String KEY = LiteAuto.KEY, TOLD = "auto_told";
    /** the UI session's wording (4.4 beta 3): the notice once, and the «الوضع الخفيف» row's line; one set of reasons for both */
    static final String SAID = S.n("شغّلنا «الوضع الخفيف» تلقائيًا لأن %s مرتين هذا الأسبوع. تقدر تطفيه من الإعدادات في «الوضع الخفيف»."),
            ROW = S.n("تلقائي: شغّال الآن لأن %s مرتين هذا الأسبوع"),
            WHY_OOM = S.n("النظام أغلق UrTV لنقص الذاكرة"), WHY_TRIM = S.n("ذاكرة التلفزيون امتلأت"), WHY_SLOW = S.n("التشغيل كان بطيئًا");
    private static final java.util.concurrent.ExecutorService BG = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lite-auto");
        t.setDaemon(true);
        return t;
    });

    private static SharedPreferences sp(Context c) { return c.getApplicationContext().getSharedPreferences("ram", Context.MODE_PRIVATE); }

    /** one signal (LiteAuto.TRIM, SLOW or OOM), now; off the caller's thread */
    static void note(Context c, final String kind) {
        final Context app = c.getApplicationContext();
        final long now = System.currentTimeMillis();
        try {
            BG.execute(() -> {
                try {
                    SharedPreferences p = sp(app);
                    p.edit().putString(KEY, LiteAuto.add(p.getString(KEY, ""), kind, now)).commit();
                    android.util.Log.i("UrTVRam", "lite signal " + kind);
                } catch (RuntimeException ignored) { }
            });
        } catch (RuntimeException ignored) { }
    }

    /** «الوضع الخفيف» is «تلقائي» and this TV's signals say it is weak */
    static boolean on(Context c) {
        try { return "auto".equals(sp(c).getString("lite", "auto")) && LiteAuto.on(sp(c).getString(KEY, ""), System.currentTimeMillis()); }
        catch (RuntimeException e) { return false; }
    }

    /** the report's line */
    static String report(Context c) {
        try { return LiteAuto.report(sp(c).getString(KEY, ""), System.currentTimeMillis()); } catch (RuntimeException e) { return "?"; }
    }

    private static String why(String kind) { return LiteAuto.OOM.equals(kind) ? WHY_OOM : LiteAuto.TRIM.equals(kind) ? WHY_TRIM : WHY_SLOW; }

    /**
     * the «الوضع الخفيف» row's line in the screensaver's settings (asked through NowPlayingProvider «lite_line»): «تلقائي: شغّال الآن لأن …» while this TV's
     * signs keep it on, "" otherwise (the row then says «تلقائي» as before). Prefs only; the screensaver asks off its UI thread.
     */
    public static String rowLine(Context c) {
        try {
            SharedPreferences p = sp(c);
            if (!"auto".equals(p.getString("lite", "auto"))) return "";
            long now = System.currentTimeMillis();
            String s = p.getString(KEY, "");
            if (!LiteAuto.on(s, now)) return "";
            return String.format(S.tv(ROW), S.tv(why(LiteAuto.reasonKind(s, now))));
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** called on the home's start thread (SetupHook.atHomeStart): the first time it is on, one line says so and why */
    public static void tellOnce(final Context c) {
        try {
            SharedPreferences p = sp(c);
            if (!"auto".equals(p.getString("lite", "auto"))) return;
            long now = System.currentTimeMillis();
            String s = p.getString(KEY, "");
            long since = LiteAuto.since(s, now);
            if (since == 0 || p.getLong(TOLD, 0) == since) return;
            pendingText = String.format(S.tv(SAID), S.tv(why(LiteAuto.reasonKind(s, now))));
            pendingSince = since;   // said by the home at its next resume (HomeNotice), marked told only then
        } catch (RuntimeException ignored) { }
    }

    /** the notice waiting for the home's next resume (UI thread reads it: memory only), or null */
    private static volatile String pendingText;
    private static volatile long pendingSince;

    static String pending() { return pendingText; }

    /** the home showed it: marked told (off the UI thread), so it is said once per time it comes on */
    static void shown(Context c) {
        final long since = pendingSince;
        pendingText = null;
        final Context app = c.getApplicationContext();
        try { BG.execute(() -> { try { sp(app).edit().putLong(TOLD, since).commit(); } catch (RuntimeException ignored) { } }); } catch (RuntimeException ignored) { }
    }
}
