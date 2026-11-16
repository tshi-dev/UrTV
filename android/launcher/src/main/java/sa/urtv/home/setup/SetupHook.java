package sa.urtv.home.setup;

import android.content.Context;
import android.widget.Toast;

/**
 * The one door the home screen uses (a single call in MainActivity.onCreate): after a Force-stop or a restart, pressing Home starts OUR home, and a home
 * switch that was cut off must not be forgotten. If the journal says «switching», the safety switch is set again from its saved deadline and one attempt
 * to put the stock home back is made on a background thread. After too many failed attempts a notice is shown at every start until it is dealt with.
 * Never throws: it must not be able to break the home screen.
 */
public final class SetupHook {
    private SetupHook() { }

    public static void atHomeStart(final Context c) {
        try {
            final Context app = c.getApplicationContext();
            if (!SetupRecovery.pending(app)) { migrate(app); return; }
            SetupRecovery.rearmFromDisk(app);
            if (SetupRecovery.gaveUp(app)) {
                Toast.makeText(app, SetupRecovery.GAVE_UP_NOTICE, Toast.LENGTH_LONG).show();
                return;
            }
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        SetupRecovery.attempt(app, new SetupEngine.Out() { @Override public void line(String text) { } });
                    } catch (RuntimeException ignored) { }
                }
            }, "setup-home-start").start();
        } catch (RuntimeException ignored) {
            // the home screen comes first
        }
    }

    /**
     * 4.3.2: a TV switched the old way (the stock home, the wizard and its helpers turned off) gets them back ONCE (StockMigration, SetupEngine.migrateStockHome), by the journal.
     * The files say first whether there is anything to look at; only then the TV is asked (a short ADB session on a background thread), and the person is told in one line.
     */
    static void migrate(final Context app) {
        try {
            FileStore store = new FileStore(app);
            String j = store.get("home-switch.json");
            if (j == null || !StockMigration.due(field(j, "state"), field(j, "stock_before"), field(j, "wizard_before"), store.get(SetupEngine.FACTORY), store.get(StockMigration.MARK))) return;
            new Thread(new Runnable() {
                @Override public void run() {
                    try (SetupSession s = SetupSession.open(app, 5000, null, new SetupEngine.Out() { @Override public void line(String text) { } })) {
                        final SetupEngine.Result r = s.engine.migrateStockHome();
                        if (r != null && r.status == SetupEngine.Status.DONE) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() { @Override public void run() { Toast.makeText(app, StockMigration.NOTICE, Toast.LENGTH_LONG).show(); } });
                        }
                    } catch (Exception ignored) {
                        // the TV did not answer: the next start tries again
                    }
                }
            }, "stock-migration").start();
        } catch (RuntimeException ignored) {
            // the home screen comes first
        }
    }

    private static String field(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^  \"" + java.util.regex.Pattern.quote(key) + "\": \"(.*)\",?$", java.util.regex.Pattern.MULTILINE).matcher(json);
        return m.find() ? m.group(1) : "";
    }

    /** the stock interface is off by us («تخلّص من واجهة المصنع» is on): the settings' row says so through the provider (read only) */
    /** 4.4 «إنقاذ تلفزيونك»: what our setup turned off (the ledger and the community list's record), one package per line, for the rescue app (ExtrasBridge, our key only) */
    public static String ledger(Context c) {
        try {
            FileStore f = new FileStore(c);
            String a = f.get("disabled-by-us.txt"), b = f.get("community-applied.txt");
            return (a == null ? "" : a) + "\n" + (b == null ? "" : b);
        } catch (RuntimeException e) { return ""; }
    }

    public static boolean factoryOff(Context c) {
        try { String f = new FileStore(c).get(SetupEngine.FACTORY); return f != null && f.startsWith("on"); }
        catch (RuntimeException e) { return false; }
    }

    /** 4.4: the home's own check after a bad boot (SetupBootReceiver.homeGuard): our home is the TV's Home and did not come up twice in a row with a crash; the home offers «رجّع الواجهة الأصلية» first */
    static final String HOME_GUARD = "home-guard.txt", STOCK_OFFER = "home-offer.txt";

    public static boolean stockBackOffered(Context c) {
        try { String f = new FileStore(c).get(STOCK_OFFER); return f != null && f.startsWith("1"); } catch (RuntimeException e) { return false; }
    }

    /** the person answered the offer (or «لاحقاً»): it goes, and the count starts again */
    public static void clearStockBackOffer(Context c) {
        try { FileStore f = new FileStore(c); f.put(STOCK_OFFER, ""); f.put(HOME_GUARD, BootGuard.fresh(System.currentTimeMillis()).text()); } catch (Exception ignored) { }
    }

    /** Our home reached the screen (MainActivity.onResume): this boot is fine and BootGuard's count starts again (the factory step's, and 4.4 the home's own). One small read; a write only when it changes. */
    public static void homeUp(Context c) {
        try {
            FileStore store = new FileStore(c.getApplicationContext());
            BootGuard.State g = BootGuard.State.parse(store.get(HOME_GUARD));
            if (!g.up || g.failed != 0) store.put(HOME_GUARD, BootGuard.homeUp(g).text());
            String f = store.get(SetupEngine.FACTORY);
            if (f == null || !f.startsWith("on")) return;
            BootGuard.State s = BootGuard.State.parse(store.get(BootGuard.FILE));
            if (s.up && s.failed == 0) return;
            store.put(BootGuard.FILE, BootGuard.homeUp(s).text());
        } catch (Exception ignored) {
            // the home screen comes first
        }
    }
}
