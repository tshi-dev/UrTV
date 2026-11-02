package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/**
 * When a task's reminder shows on the home and over another app, with no Android in it (tests/ReminderPlanRun.java runs it). The screensaver
 * app owns the tasks and the same rules (its Reminders.java): the card comes «قبلها بـ» minutes ahead of the appointment, or at the snooze time,
 * and goes 30 minutes after the appointment when nobody answered. Where a reminder shows: 0 the screensaver and the home, 1 the screensaver
 * only, 2 the home only; the home and another app's notice take 0 and 2.
 */
final class ReminderPlan {
    private ReminderPlan() { }

    static final long MINUTE = 60_000L;
    static final long GRACE = 30 * MINUTE;

    static final class Row {
        final long id, due, snooze;
        final int before, where;
        final boolean sound, done;
        final String text;

        Row(long id, String text, boolean done, long due, int before, boolean sound, int where, long snooze) {
            this.id = id; this.text = text; this.done = done; this.due = due; this.before = before; this.sound = sound; this.where = where; this.snooze = snooze;
        }

        long fireAt() { return snooze > 0 ? snooze : due - before * MINUTE; }

        boolean open() { return !done && due > 0 && where != 1; }
    }

    /** the first moment after `now` a card is due over every open reminder the home shows; 0 when there is none */
    static long next(List<Row> v, long now) {
        long best = 0;
        for (Row r : v) {
            if (!r.open()) continue;
            long f = r.fireAt();
            if (f > now && (best == 0 || f < best)) best = f;
        }
        return best;
    }

    /** the reminders whose card is showing at `now`, soonest appointment first */
    static List<Row> showing(List<Row> v, long now) {
        List<Row> out = new ArrayList<>();
        for (Row r : v) if (r.open() && r.fireAt() <= now && now < Math.max(r.due, r.snooze) + GRACE) out.add(r);
        out.sort((a, b) -> Long.compare(a.due, b.due));
        return out;
    }

    /** the key a card was shown for, so the same card is not shown twice (the alarm, the home coming back, a boot) */
    static String key(Row r) { return r.id + ":" + r.fireAt(); }

    /** "الساعة …، بعد …" at `now`: the minutes left, or «الآن» from the appointment on */
    static int minutesLeft(Row r, long now) {
        return (int) Math.max(1, (r.due - now + MINUTE - 1) / MINUTE);
    }
}
