package sa.urtv.screensaver;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * «UrTV النظيف» as this app sees it: a mask the home owns (CleanMode) and this app only READS through the home's provider; nothing here writes the person's choices, so
 * off brings the page and the hub back exactly as they were. {@link #state}: 0 off, 1 on, 2 on with the next-prayer line kept. The last answer is kept for when the home does not answer.
 */
final class CleanMask {
    private CleanMask() { }

    static final int OFF = 0, ON = 1, ON_PRAYER = 2;

    private static volatile boolean busy;

    /**
     * 4.4 QA-12: the mask as the home last said it (a copy kept in hub_seen), never a query: it is read on the UI thread (the hub's open, the
     * screensaver's start). A thread asks the home again; when the answer differs it is kept and the screens that are up are told (the
     * settings-changed broadcast: the dream is built again, the hub goes straight on to the settings), so a mask turned on a moment ago
     * reaches them a moment later, not a start later.
     */
    static int state(Context c) {
        final Context app = c.getApplicationContext();
        final int kept = app.getSharedPreferences("hub_seen", Context.MODE_PRIVATE).getInt("clean_mask", OFF);
        if (!busy) {
            busy = true;
            Thread t = new Thread(() -> {
                try {
                    int now = ask(app);
                    if (now >= 0 && now != kept) {
                        app.getSharedPreferences("hub_seen", Context.MODE_PRIVATE).edit().putInt("clean_mask", now).apply();   // before the broadcast: the next state() is this one
                        app.sendBroadcast(new android.content.Intent(Prefs.CHANGED).setPackage(app.getPackageName()));
                    }
                } catch (RuntimeException ignored) { /* the last answer stays */ }
                finally { busy = false; }
            }, "clean-mask");
            t.setPriority(Thread.MIN_PRIORITY); t.setDaemon(true); t.start();
        }
        return kept;
    }

    /** the home's answer (a query to its process: only on a thread); -1 when it did not answer */
    private static int ask(Context c) {
        Cursor q = null;
        try {
            q = c.getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/clean_on"), null, null, null, null);
            if (q != null && q.moveToFirst() && q.getColumnIndex("on") >= 0)
                return q.getInt(q.getColumnIndex("on")) != 1 ? OFF : q.getColumnIndex("prayer") >= 0 && q.getInt(q.getColumnIndex("prayer")) == 1 ? ON_PRAYER : ON;
            return OFF;   // the home answers "off" or nothing is installed: the full pages
        } catch (RuntimeException e) {
            return -1;   // the home did not answer: as it last said
        } finally { if (q != null) q.close(); }
    }

    /** the page's flag: #clean=1 (photo and clock), #clean=2 (and the next-prayer line); nothing at all while it is off, so the link is what it was */
    static String fragment(int state) { return state == OFF ? "" : "&clean=" + state; }
}
