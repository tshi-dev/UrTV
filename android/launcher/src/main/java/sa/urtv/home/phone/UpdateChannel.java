package sa.urtv.home.phone;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Which release «تحديث تلفزيوني» reads: "latest", "beta" («النسخ التجريبية» is on) or "next", the test build. The screensaver keeps the
 * word (`optimize/saver.sh updatechannel`, no screen shows it) and tells it through its ConfigProvider (same key); an older screensaver, or
 * any answer but "next", is the latest.
 */
final class UpdateChannel {
    private UpdateChannel() { }

    static String read(Context c) {
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
