package sa.urtv.home.phone;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Which release «تحديث تلفزيوني» reads: "latest", "beta" («النسخ التجريبية» is on) or "next", the test build. The screensaver keeps the
 * word (`optimize/saver.sh updatechannel`, no screen shows it) and tells it through its ConfigProvider (same key); an older screensaver, or
 * any answer but "next", is the latest.
 */
public final class UpdateChannel {
    private UpdateChannel() { }

    /** «النسخ التجريبية» is on: reports and the beta notes of the home are for this channel only. Asks the screensaver: never on the UI thread (cached()) */
    public static boolean beta(Context c) { return "beta".equals(read(c)); }

    /**
     * 4.3.3 hotfix (a TCL report: «الريموت يعلق» since 4.3.3): the last channel read, with no question to the screensaver. The home's minute tick and
     * its return to the front asked read() on the UI thread, a cross-process query that starts the screensaver's process again when the TV killed it
     * (the reason the tick's own read moved off the UI thread, P3): on a TCL that held the remote for seconds. ConfigSnapshot, read off the UI thread,
     * keeps the word here; "latest" until the first read.
     */
    public static String cached() {
        String k = known;
        return k == null ? "latest" : k;
    }

    private static volatile String known;

    /** what the screensaver said (ConfigSnapshot's background read, or read()) */
    public static void remember(String v) { known = "next".equals(v) || "beta".equals(v) ? v : "latest"; }

    public static String read(Context c) {
        String v = ask(c);
        remember(v);
        return v;
    }

    private static String ask(Context c) {
        try (Cursor cur = c.getContentResolver().query(Uri.parse("content://sa.urtv.screensaver.config"), null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                int i = cur.getColumnIndex("update_channel");
                if (i >= 0) {
                    String v = cur.getString(i);
                    if ("next".equals(v) || "beta".equals(v)) return v;
                }
            }
        } catch (RuntimeException ignored) {
            // the screensaver is missing or signed with another key: the latest
        }
        return "latest";
    }
}
