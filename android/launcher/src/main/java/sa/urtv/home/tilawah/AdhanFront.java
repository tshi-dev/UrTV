package sa.urtv.home.tilawah;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * What is in front when the adhan's alarm goes off. Our home says so itself (home(), from its onResume and onPause: the process is the home's
 * own, so a dead process is a home that is not there). The screensaver is another app: its provider answers «هل تعمل الآن» for the dream and
 * its preview. Nothing else needs to be known: anything but these two is «another app», an input or a standby.
 */
public final class AdhanFront {
    private static volatile boolean home;

    private AdhanFront() { }

    /** the home screen came to the front (true) or left it (false) */
    public static void home(boolean inFront) { home = inFront; }

    /** AdhanPlan.HOME, DREAM, OTHER or UNKNOWN */
    static int now(Context c) { return AdhanPlan.front(home, dreaming(c)); }

    /** 1 the screensaver (dream or preview) is showing, 0 it is not, -1 not known: an older screensaver app answers any path with its settings, so the column is checked */
    private static int dreaming(Context c) {
        Cursor cur = null;
        try {
            cur = c.getApplicationContext().getContentResolver().query(Uri.parse("content://sa.urtv.screensaver.config/dreaming"), null, null, null, null);
            String[] cols = cur == null ? null : cur.getColumnNames();
            if (cols == null || cols.length != 1 || !"dreaming".equals(cols[0]) || !cur.moveToFirst()) return -1;
            return cur.getInt(0) == 1 ? 1 : 0;
        } catch (RuntimeException e) {   // not installed, no permission, the process would not start
            return -1;
        } finally {
            if (cur != null) cur.close();
        }
    }
}
