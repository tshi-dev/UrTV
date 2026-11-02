package sa.urtv.home;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/** The reminders, read from the screensaver app's tasks (its ConfigProvider, same signing key); an older screensaver app has none, and a screensaver app that is missing answers nothing. */
final class ReminderStore {
    private ReminderStore() { }

    private static final Uri TASKS = Uri.parse("content://sa.urtv.screensaver.config/tasks");

    /** every task that has a reminder; empty when the screensaver app is missing, too old or refuses us */
    static List<ReminderPlan.Row> read(Context c) {
        List<ReminderPlan.Row> out = new ArrayList<>();
        try (Cursor cur = c.getApplicationContext().getContentResolver().query(TASKS, null, null, null, null)) {
            if (cur == null || cur.getColumnIndex("due") < 0) return out;   // an older screensaver app has no reminders
            int id = cur.getColumnIndexOrThrow("id"), t = cur.getColumnIndexOrThrow("t"), done = cur.getColumnIndexOrThrow("done"), due = cur.getColumnIndexOrThrow("due");
            int bf = cur.getColumnIndexOrThrow("bf"), snd = cur.getColumnIndexOrThrow("snd"), wh = cur.getColumnIndexOrThrow("wh"), sn = cur.getColumnIndexOrThrow("sn");
            while (cur.moveToNext()) {
                if (cur.getLong(due) <= 0) continue;
                out.add(new ReminderPlan.Row(cur.getLong(id), cur.getString(t), cur.getInt(done) != 0, cur.getLong(due), cur.getInt(bf), cur.getInt(snd) == 1, cur.getInt(wh), cur.getLong(sn)));
            }
        } catch (RuntimeException e) {   // not installed, no permission
            out.clear();
        }
        return out;
    }

    static boolean done(Context c, long id) {
        ContentValues v = new ContentValues();
        v.put("id", id);
        try { return c.getApplicationContext().getContentResolver().update(TASKS, v, null, null) > 0; } catch (RuntimeException e) { return false; }
    }

    static boolean snooze(Context c, long id, int minutes) {
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("snooze", minutes);
        try { return c.getApplicationContext().getContentResolver().update(TASKS, v, null, null) > 0; } catch (RuntimeException e) { return false; }
    }
}
