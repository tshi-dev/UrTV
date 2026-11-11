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

    static int state(Context c) {
        Cursor q = null;
        try {
            q = c.getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/clean_on"), null, null, null, null);
            if (q != null && q.moveToFirst() && q.getColumnIndex("on") >= 0) {
                int s = q.getInt(q.getColumnIndex("on")) != 1 ? OFF : q.getColumnIndex("prayer") >= 0 && q.getInt(q.getColumnIndex("prayer")) == 1 ? ON_PRAYER : ON;
                c.getSharedPreferences("hub_seen", Context.MODE_PRIVATE).edit().putInt("clean_mask", s).apply();
                return s;
            }
            return OFF;   // the home answers "off" or nothing is installed: the full pages
        } catch (RuntimeException e) {
            return c.getSharedPreferences("hub_seen", Context.MODE_PRIVATE).getInt("clean_mask", OFF);   // the home did not answer: as it last said
        } finally { if (q != null) q.close(); }
    }

    /** the page's flag: #clean=1 (photo and clock), #clean=2 (and the next-prayer line); nothing at all while it is off, so the link is what it was */
    static String fragment(int state) { return state == OFF ? "" : "&clean=" + state; }
}
