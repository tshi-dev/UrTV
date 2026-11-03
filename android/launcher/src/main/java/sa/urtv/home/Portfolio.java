package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

/**
 * Where «محفظتي» is kept, and the only door to it (docs/PROPOSALS-NOV2.md §7). The holdings stay on this TV:
 * <ul>
 *   <li>this app's private prefs ({@code MODE_PRIVATE}; Android backup is off in the manifest, {@code allowBackup="false"});</li>
 *   <li>never in a URL, never in the screensaver's ConfigProvider (so {@code saver.sh show}, the agent's tools and the other app cannot read
 *       them), never in a report (Report.java, optimize/share.py) or a settings export (optimize/settings_move.py);</li>
 *   <li>no command reads them: there is no {@code saver.sh portfolio} or dock.sh one, by design, as with «المهام»;</li>
 *   <li>the phone page reads and writes them only from a paired phone, and only while the TV is editing them ({@link #editing}): the tab
 *       open on the TV, or «أسهل من الجوال» pressed, for EDIT_MS after.</li>
 * </ul>
 * tests/portfolio_privacy_test.py checks each of these. {@code dock.sh pin reset} wipes them (Parental.adbReset), as uninstalling does.
 */
final class Portfolio {
    private Portfolio() { }

    private static final String PREFS = "portfolio";
    private static final String HOLDINGS = "holdings", HIDE = "hide_amounts", LOCK = "lock_with_pin";
    /** how long the phone may read and write them after the TV last opened them */
    static final long EDIT_MS = 10 * 60_000L;

    /** in memory only: a restart closes it; on SystemClock.elapsedRealtime, renewed only from the TV (PortfolioWindow) */
    private static final PortfolioWindow WINDOW = new PortfolioWindow();

    /** the markets page while it is on the screen: told (on the phone server's thread) when the phone saved */
    static volatile Runnable listener;

    static void changed() {
        Runnable r = listener;
        if (r != null) r.run();
    }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static synchronized List<Holdings.Holding> load(Context c) { return Holdings.parse(prefs(c).getString(HOLDINGS, "")); }

    /** false when the disk refused it */
    static synchronized boolean save(Context c, List<Holdings.Holding> all) {
        return prefs(c).edit().putString(HOLDINGS, Holdings.write(all)).commit();
    }

    /** «أخفِ المبالغ»: stays until shown again */
    static boolean hidden(Context c) { return prefs(c).getBoolean(HIDE, false); }
    static void setHidden(Context c, boolean on) { prefs(c).edit().putBoolean(HIDE, on).apply(); }

    /** «اقفل محفظتي»: the tab asks for the parental PIN first (only while a PIN exists) */
    static boolean locked(Context c) { return prefs(c).getBoolean(LOCK, false); }
    static void setLocked(Context c, boolean on) {
        prefs(c).edit().putBoolean(LOCK, on).apply();
        if (on) closeEditing();   // locked: a phone in the window now is out until the PIN is given on the TV again
    }

    /** «احذف المحفظة» and {@code dock.sh pin reset}: everything, the switches too */
    static synchronized void wipe(Context c) {
        prefs(c).edit().clear().commit();
        closeEditing();
    }

    // ---------------------------------------------------------------- the phone's window

    /** The TV clock the window runs on: monotonic, so a clock change neither extends nor ends it. */
    static long now() { return android.os.SystemClock.elapsedRealtime(); }

    /**
     * The owner on the TV shows the holdings (the tab, «أسهل من الجوال», or the pairing screen after the PIN): a paired phone may read and
     * write them for EDIT_MS from now (now()). Only the TV calls this; a phone's save never does, so a phone cannot renew its own window.
     */
    static void openEditing(long now) { WINDOW.open(now, EDIT_MS); }

    /** «اقفل محفظتي», «احذف المحفظة», pin reset, a phone session ended: the phone is out until the TV opens it again. */
    static void closeEditing() { WINDOW.close(); }

    static boolean editing(long now) { return WINDOW.open(now); }
}
