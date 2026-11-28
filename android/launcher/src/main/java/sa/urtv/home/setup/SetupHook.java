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

    /** 4.4 QA-13: the setup ledger's small files are read on a thread of their own, never in the home's onCreate; what they decide (the safety switch, the notice) comes a moment later */
    public static void atHomeStart(final Context c) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() { try { atHomeStartNow(app); } catch (RuntimeException ignored) { } }
        }, "setup-home-check").start();
    }

    private static void atHomeStartNow(final Context app) {
        try {
            clearOldFails(app);
            sa.urtv.home.LiteAutoStore.tellOnce(app);   // 4.4 beta 3: «الوضع الخفيف» came on by itself: said once
            if (sa.urtv.home.RoleBootRule.PENDING.equals(trim(new FileStore(app).get(ROLE_NOTICE)))) sa.urtv.home.RoleNotice.waiting(app);   // R-401: the TV took Home back at a boot
            endNotSeen(app);   // 4.4 (R2)
            if (!SetupRecovery.pending(app)) { migrate(app); return; }
            SetupRecovery.rearmFromDisk(app);
            if (SetupRecovery.gaveUp(app)) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() { Toast.makeText(app, SetupRecovery.GAVE_UP_NOTICE, Toast.LENGTH_LONG).show(); }
                });
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
            if (j == null || !StockMigration.due(field(j, "state"), field(j, "stock_before"), field(j, "wizard_before"), store.get(SetupEngine.FACTORY), store.get(StockMigration.MARK))) {
                noJournal(app, store);   // 4.4 (R7): a TV switched the old way from the PC
                return;
            }
            new Thread(new Runnable() {
                @Override public void run() {
                    try (SetupSession s = SetupSession.openAny(app, 0, null, new SetupEngine.Out() { @Override public void line(String text) { } })) {
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

    /**
     * 4.4 (R7): no stock home answers Home (asked of this app's own package manager, no adb), the person did not choose «عطّل واجهة المصنع», and nothing was done yet:
     * where this app's adb key exists (it was let in before) the stock home is turned back on (SetupEngine.migrateStockNoJournal); else the person is told the way
     * by hand, once (no «السماح بتصحيح USB؟» popping up at a home start).
     */
    private static void noJournal(final Context app, FileStore store) {
        String f = store.get(SetupEngine.FACTORY), m = store.get(StockMigration.MARK);
        if ((f != null && f.startsWith("on")) || (m != null && !m.trim().isEmpty())) return;
        java.util.List<String> homes = new java.util.ArrayList<>();
        for (android.content.pm.ResolveInfo r : app.getPackageManager().queryIntentActivities(new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME), 0))
            if (r.activityInfo != null) homes.add(r.activityInfo.packageName);
        if (RescueHome.stockHomeOn(homes)) return;
        if (!AdbKeys.exists(app)) {
            String told = store.get(StockMigration.TOLD_MARK);
            if (told != null && !told.trim().isEmpty()) return;
            try { store.put(StockMigration.TOLD_MARK, "told\n"); } catch (java.io.IOException e) { return; }
            new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() { @Override public void run() { Toast.makeText(app, StockMigration.TOLD, Toast.LENGTH_LONG).show(); } });
            return;
        }
        new Thread(new Runnable() {
            @Override public void run() {
                try (SetupSession s = SetupSession.openAny(app, 0, null, new SetupEngine.Out() { @Override public void line(String text) { } })) {
                    final SetupEngine.Result r = s.engine.migrateStockNoJournal();
                    if (r != null && r.status == SetupEngine.Status.DONE) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() { @Override public void run() { Toast.makeText(app, StockMigration.NOTICE, Toast.LENGTH_LONG).show(); } });
                    }
                } catch (Exception ignored) {
                    // the TV did not answer: the next start tries again
                }
            }
        }, "stock-migration-pc").start();
    }

    /**
     * 4.4 (R2: a Skyworth IRIS «stopped at 26 %, then the app closed by itself»): a full run that ended badly while its screen was not in front (it ended at a
     * boot, or the app was closed) is said once at the next start of UrTV, in one line, with the way on. The setup screen shows the whole end (restoreEnd).
     */
    private static void endNotSeen(final Context app) {
        try {
            FileStore store = new FileStore(app);
            String t = store.get(SetupRunner.END_FILE);
            if (t == null || !t.startsWith("bad\n")) return;   // ok, told already, or none
            final String text = t.substring(4).trim();
            store.put(SetupRunner.END_FILE, "bad told\n" + text + "\n");
            if (text.isEmpty()) return;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() { @Override public void run() {
                Toast.makeText(app, "«إعداد تلفزيوني» ما كمل: " + text + " افتح «إعداد تلفزيوني» لتكمل.", Toast.LENGTH_LONG).show();
            } });
        } catch (java.io.IOException | RuntimeException ignored) { }
    }

    /** 4.4 (step 100): this app holds an adb key the TV has let in before (no question pops up) */
    public static boolean hasKey(Context c) { return AdbKeys.exists(c); }

    /**
     * 4.4 (step 100): the safe page's «افتح الشاشة الأصلية» on a TV whose stock home an earlier setup turned off: that one package is turned back on over this
     * app's adb key (no question pops up: only where the key exists), then read back. Only a known stock home (StockHome.KNOWN), never the setup wizard.
     * Blocks: call it off the UI thread. True when the TV says it is on now.
     */
    public static boolean enableStock(Context c, String pkg) {
        if (!AdbKeys.exists(c)) return false;
        try (SetupSession s = SetupSession.openAny(c, 0, null, new SetupEngine.Out() { @Override public void line(String text) { } })) {
            return StockEnable.run(s::run, pkg, sa.urtv.home.StockHome.known());
        } catch (Exception e) {
            return false;
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

    // ---- 4.4 (the owner's TV, step 86 B): the switch survives, and Google TV taking the role back is answered once

    private static final String TAG = "UrTVSetup";

    /** our home is what Android opens for HOME now */
    static boolean oursIsDefault(Context c) {
        try {
            android.content.pm.ResolveInfo ri = c.getPackageManager().resolveActivity(new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME),
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            return ri != null && ri.activityInfo != null && c.getPackageName().equals(ri.activityInfo.packageName);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Our home was drawn while it is the TV's default home and a switch is still «switching» (the person left the setup page, the run died): the switch
     * works, so the safety switch is disarmed here (SetupEngine.confirmDrawn); while a run is going the run decides itself.
     */
    static void confirmIfDrawn(Context c) {
        try {
            Context app = c.getApplicationContext();
            if (!SetupRecovery.pending(app) || SetupRunner.get().running() || !oursIsDefault(app)) return;
            if (SetupEngine.confirmDrawn(new FileStore(app), new SetupDeadman(app))) android.util.Log.i(TAG, "home switch: confirmed (our home was drawn as the TV's home); safety switch off");
        } catch (Exception ignored) {
            // the home screen comes first
        }
    }

    private static volatile long reapplyTried;

    /**
     * The person's chosen home is ours (HOME_WANTED) and the switch is done, but Android's default home is not ours any more: the TV took the role back
     * by itself. Given back ONCE over this app's own adb (the key is kept), on a background thread; a second loss is logged and left (it may be the
     * person's own choice in the TV's settings; «تراجع» or «إعداد تلفزيوني» from there). At most one try every ten minutes while adb does not answer.
     */
    public static void reapplyIfLost(Context c) {
        try {
            final Context app = c.getApplicationContext();
            final FileStore store = new FileStore(app);
            if (!"1".equals(trim(store.get(SetupEngine.HOME_WANTED))) || stockBackOffered(app) || SetupRecovery.pending(app) || SetupRunner.get().running() || oursIsDefault(app)) return;
            if (!trim(store.get(SetupEngine.HOME_REAPPLIED)).isEmpty()) {
                if (reapplyTried == 0) { reapplyTried = 1; android.util.Log.i(TAG, "home role: lost again after one re-apply; left as it is"); }
                return;
            }
            long now = System.currentTimeMillis();
            if (now - reapplyTried < 10 * 60_000L) return;
            reapplyTried = now;
            new Thread(new Runnable() {
                @Override public void run() {
                    try (SetupSession s = SetupSession.openAny(app, 0, null, new SetupEngine.Out() { @Override public void line(String text) { } })) {
                        boolean ok = s.engine.reapplyHomeRole();
                        android.util.Log.i(TAG, "home role: the TV took it back; re-applied once: " + (ok ? "ours again" : "not ours"));
                    } catch (Exception e) {
                        android.util.Log.i(TAG, "home role: the TV took it back; re-apply waits (adb did not answer)");
                    }
                }
            }, "home-reapply").start();
        } catch (RuntimeException ignored) {
            // the home screen comes first
        }
    }

    private static volatile Boolean toldWatch;

    /** 4.4 (step 88): the add-on is told HOME_WANTED now, whatever it was told before (the end of a setup or an undo); quiet without it */
    static void tellWatchNow(Context c) {
        toldWatch = null;
        tellWatch(c);
    }

    /** The add-on learns whether the person's chosen home is ours (its watch then brings ours forward even without the role); once per change */
    static void tellWatch(Context c) {
        try {
            final Context app = c.getApplicationContext();
            final boolean wanted = "1".equals(trim(new FileStore(app).get(SetupEngine.HOME_WANTED)));
            if (toldWatch != null && toldWatch == wanted) return;
            toldWatch = wanted;
            new Thread(new Runnable() { @Override public void run() { if (!sa.urtv.home.Extras.homeWanted(app, wanted)) toldWatch = null; } }, "watch-wanted").start();
        } catch (RuntimeException ignored) { }
    }

    /**
     * 4.4 «بدء شاشة التوقف بعد» from the screensaver's settings (NowPlayingProvider call("set_idle"), same key only): the TV's idle delay over this
     * app's own adb, because the screensaver app may not write system settings on most TVs (a user's TV said 2147483647: its screensaver off).
     * Runs on the caller's thread (the screensaver calls from its own background thread). Answers "ok", "no_link" (no kept key, adb did not
     * answer, or the TV refused), "busy" (a setup is running) or "bad" (not one of IdleSetRule.MINUTES).
     */
    public static String setIdle(Context c, String minutes) {
        long ms = IdleSetRule.ms(minutes);
        if (ms <= 0) return "bad";
        try {
            final Context app = c.getApplicationContext();
            if (SetupRunner.get().running()) return "busy";
            if (!AdbKeys.exists(app)) { android.util.Log.i(TAG, "idle: no adb key (the setup never ran here)"); return "no_link"; }
            try (SetupSession s = SetupSession.openAny(app, 0, null, new SetupEngine.Out() { @Override public void line(String text) { } })) {
                String probe = s.run(IdleSetRule.PROBE).output;
                for (String cmd : IdleSetRule.commands(ms, probe)) {
                    if (s.run(cmd).code != 0) { android.util.Log.i(TAG, "idle: the TV refused a setting"); return "no_link"; }
                }
            }
            android.util.Log.i(TAG, "idle: set over adb");
            return "ok";
        } catch (Exception e) {
            android.util.Log.i(TAG, "idle: adb did not answer");
            return "no_link";
        }
    }

    /** R-401: the boot's notice that the TV took Home back (RoleBootRule.PENDING, then TOLD; empty once the role is ours again) */
    static final String ROLE_NOTICE = "home-role-notice.txt";

    /**
     * 4.4 (R-401): at BOOT_COMPLETED, who holds HOME is read again and logged (RoleBootRule.holderWord: never an app the person installed by name), and
     * RoleBootRule decides: the role given back once over our kept adb link (no new key is ever offered: no approval wait), or the one notice for the
     * home's next start. A role found ours again clears the notice. On its own thread; never breaks the boot.
     */
    public static void roleAtBoot(Context c) {
        try {
            final Context app = c.getApplicationContext();
            final FileStore store = new FileStore(app);
            boolean wanted = "1".equals(trim(store.get(SetupEngine.HOME_WANTED)));
            String role = sa.urtv.home.Extras.homeRole(app);
            String pkg = null, cls = null;
            try {
                android.content.pm.ResolveInfo ri = app.getPackageManager().resolveActivity(new android.content.Intent(android.content.Intent.ACTION_MAIN)
                    .addCategory(android.content.Intent.CATEGORY_HOME), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
                if (ri != null && ri.activityInfo != null) { pkg = ri.activityInfo.packageName; cls = ri.activityInfo.name; }
            } catch (RuntimeException ignored) { }
            boolean ours = sa.urtv.home.ExtrasWords.ROLE_OURS.equals(role) || (sa.urtv.home.ExtrasWords.ROLE_UNKNOWN.equals(role) && app.getPackageName().equals(pkg));
            android.util.Log.i(TAG, "home role at boot: holder " + sa.urtv.home.RoleBootRule.holderWord(pkg, cls, app.getPackageName()) + ", role " + role
                + (wanted ? ", the person's home is ours" : ", the person's home is not ours"));
            String notice = trim(store.get(ROLE_NOTICE));
            if (ours) { if (!notice.isEmpty()) store.put(ROLE_NOTICE, ""); return; }
            boolean busy = stockBackOffered(app) || SetupRecovery.pending(app) || SetupRunner.get().running();
            int d = sa.urtv.home.RoleBootRule.decide(wanted, false, pkg, !trim(store.get(SetupEngine.HOME_REAPPLIED)).isEmpty(), notice, busy);
            if (d == sa.urtv.home.RoleBootRule.NOTICE) {
                store.put(ROLE_NOTICE, sa.urtv.home.RoleBootRule.PENDING);
                android.util.Log.i(TAG, "home role at boot: not ours; the home says so once");
                return;
            }
            if (d != sa.urtv.home.RoleBootRule.REASSERT) return;
            reapplyTried = System.currentTimeMillis();   // the home's start does not try it again at once
            new Thread(new Runnable() {
                @Override public void run() {
                    try (SetupSession s = SetupSession.openAny(app, 0, null, new SetupEngine.Out() { @Override public void line(String text) { } })) {
                        boolean ok = s.engine.reapplyHomeRole();
                        android.util.Log.i(TAG, "home role at boot: given back once: " + (ok ? "ours again" : "not ours"));
                        if (!ok) store.put(ROLE_NOTICE, sa.urtv.home.RoleBootRule.PENDING);
                    } catch (Exception e) {
                        reapplyTried = 0;   // adb not up yet: the home's start tries again (reapplyIfLost)
                        android.util.Log.i(TAG, "home role at boot: not ours; adb did not answer yet");
                    }
                }
            }, "home-role-boot").start();
        } catch (Exception ignored) {
            // the boot must not be broken by this
        }
    }

    /** R-401: the home showed the notice (RoleNotice.shown): said once */
    public static void roleNoticeTold(Context c) {
        try { new FileStore(c.getApplicationContext()).put(ROLE_NOTICE, sa.urtv.home.RoleBootRule.TOLD); } catch (Exception ignored) { }
    }

    /** 4.4 the Home doctor (DockReceiver HOME_WANTED, adb only): «on» / «off» set HOME_WANTED and tell the add-on; «status» reads it. Answers yes or no. */
    public static String homeWanted(Context c, String mode) {
        try {
            final Context app = c.getApplicationContext();
            FileStore store = new FileStore(app);
            if (!"status".equals(mode)) {
                store.put(SetupEngine.HOME_WANTED, "on".equals(mode) ? "1" : "");
                toldWatch = null;
                tellWatch(app);
            }
            return "1".equals(trim(store.get(SetupEngine.HOME_WANTED))) ? "yes" : "no";
        } catch (Exception e) {
            return "no";
        }
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    static final String FAILS_BUILD = "home-fails-build.txt";

    /**
     * 4.4 (step 88): the count of failed home switches belongs to the build that failed. After an update (another versionCode) it is cleared once, so the
     * new build's way (on Google TV the add-on and «wanted») is tried. Quiet and cheap: two small files.
     */
    static void clearOldFails(Context app) {
        try {
            FileStore store = new FileStore(app);
            String now = String.valueOf(sa.urtv.home.BuildConfig.VERSION_CODE);
            if (now.equals(trim(store.get(FAILS_BUILD)))) return;
            String had = store.get(HomeFails.FILE);
            if (had != null && !had.trim().isEmpty()) store.put(HomeFails.FILE, "");
            store.put(FAILS_BUILD, now);
        } catch (Exception ignored) {
            // the home screen comes first
        }
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

    /** 4.4 review A2-F1: the person pressed the bar to take the TV's own home: our wish for the home goes too (reapplyIfLost and the add-on's watch would undo the rescue), and the add-on is told at once */
    public static void stockTaken(Context c) {
        clearStockBackOffer(c);
        try { new FileStore(c).put(SetupEngine.HOME_WANTED, ""); } catch (Exception ignored) { }
        tellWatchNow(c);
    }

    /** Our home reached the screen (MainActivity.onResume): this boot is fine and BootGuard's count starts again (the factory step's, and 4.4 the home's own). One small read; a write only when it changes. */
    public static void homeUp(final Context c) {
        // 4.4 QA-13: on a thread of its own (the ledger's files, the journal, the guard), one at a time; the home's return never reads them
        if (!homeUpRunning.compareAndSet(false, true)) return;
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                try { homeUpNow(app); } catch (RuntimeException ignored) { } finally { homeUpRunning.set(false); }
            }
        }, "setup-home-up").start();
    }

    private static final java.util.concurrent.atomic.AtomicBoolean homeUpRunning = new java.util.concurrent.atomic.AtomicBoolean();

    private static void homeUpNow(Context c) {
        confirmIfDrawn(c);
        reapplyIfLost(c);
        tellWatch(c);
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
