package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

/**
 * 4.4 beta 3 (LiteAuto): the signals of a weak TV, kept in the «ram» preferences next to «الوضع الخفيف». Written on a thread of its own (never the UI thread);
 * Ram.tier reads them at the home's next start. When it comes on, it is said once (tellOnce, from the home's start thread): never a silent change.
 */
public final class LiteAutoStore {
    private LiteAutoStore() { }

    static final String KEY = LiteAuto.KEY, TOLD = "auto_told";
    static final String SAID = S.n("شغّلنا «الوضع الخفيف» تلقائياً: %s. تقدر تطفيه من الإعدادات ← الوضع الخفيف."),
            WHY_OOM = S.n("النظام أغلق UrTV لنقص الذاكرة"), WHY_TRIM = S.n("ذاكرة التلفزيون امتلأت"), WHY_SLOW = S.n("التشغيل كان بطيئاً");
    private static final java.util.concurrent.ExecutorService BG = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lite-auto");
        t.setDaemon(true);
        return t;
    });

    private static SharedPreferences sp(Context c) { return c.getApplicationContext().getSharedPreferences("ram", Context.MODE_PRIVATE); }

    /** one signal (LiteAuto.TRIM, SLOW or OOM), now; off the caller's thread */
    static void note(Context c, final String kind) { noteAt(c, kind, System.currentTimeMillis()); }

    /** the same at the time it happened (a low-memory kill read at the next start); one older than LiteAuto.HOLD is let go by LiteAuto.add */
    static void noteAt(Context c, final String kind, final long at) {
        final Context app = c.getApplicationContext();
        final long now = System.currentTimeMillis();
        try {
            BG.execute(() -> {
                try {
                    SharedPreferences p = sp(app);
                    p.edit().putString(KEY, LiteAuto.add(p.getString(KEY, ""), kind, at, now)).commit();
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

    /** called on the home's start thread (SetupHook.atHomeStart): the first time it is on, one line says so and why */
    public static void tellOnce(final Context c) {
        try {
            SharedPreferences p = sp(c);
            if (!"auto".equals(p.getString("lite", "auto"))) return;
            long now = System.currentTimeMillis();
            String s = p.getString(KEY, "");
            long since = LiteAuto.since(s, now);
            if (since == 0 || p.getLong(TOLD, 0) == since) return;
            p.edit().putLong(TOLD, since).commit();
            String k = LiteAuto.reasonKind(s, now);
            final String why = LiteAuto.OOM.equals(k) ? WHY_OOM : LiteAuto.TRIM.equals(k) ? WHY_TRIM : WHY_SLOW;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> Toast.makeText(c.getApplicationContext(),
                String.format(S.tv(SAID), S.tv(why)), Toast.LENGTH_LONG).show());
        } catch (RuntimeException ignored) { }
    }
}
