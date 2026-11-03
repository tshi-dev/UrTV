package sa.urtv.screensaver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The time side of a task's reminder, with no Android in it (tests/ReminderRun.java runs it): when the card comes, what a repeat does, the date a
 * person or `saver.sh tasks add --at` writes. A reminder is open while its task is not done and it has a due time.
 */
final class Reminders {
    private Reminders() { }

    static final long MINUTE = 60_000L;
    static final long DAY = 24 * 60 * MINUTE;
    /** the choices of «قبلها بـ», in minutes */
    static final int[] BEFORE = {0, 10, 30, 60};
    static final int NO = 0, DAILY = 1, WEEKLY = 2;
    /** a card that nobody answered goes away this long after the appointment (the task stays, with its bell) */
    static final long GRACE = 30 * MINUTE;

    static boolean has(Task t) { return !t.done && t.due > 0; }

    /** when the card comes: the snooze when there is one, else the appointment less «قبلها بـ» */
    static long fireAt(Task t) {
        return t.snooze > 0 ? t.snooze : t.due - t.before * MINUTE;
    }

    /** the first moment after `now` a card is due, over every open reminder; 0 when there is none */
    static long next(List<Task> v, long now) {
        long best = 0;
        for (Task t : v) {
            if (!has(t)) continue;
            long f = fireAt(t);
            if (f > now && (best == 0 || f < best)) best = f;
        }
        return best;
    }

    /** the reminders whose card is showing at `now`: its time has come and the appointment is not over by more than GRACE, soonest first */
    static List<Task> showing(List<Task> v, long now) {
        List<Task> out = new ArrayList<>();
        for (Task t : v) if (has(t) && fireAt(t) <= now && now < Math.max(t.due, t.snooze) + GRACE) out.add(t);
        out.sort((a, b) -> Long.compare(a.due, b.due));
        return out;
    }

    static long period(int repeat) {
        return repeat == DAILY ? DAY : repeat == WEEKLY ? 7 * DAY : 0;
    }

    /** whole days a repeat moves by: 1, 7, or 0 for a single one */
    static int days(int repeat) { return repeat == DAILY ? 1 : repeat == WEEKLY ? 7 : 0; }

    /**
     * The appointment `days` calendar days after `due` at the same wall-clock time in `tz` (not a fixed 24 h: across a summer-time change
     * the hour stays). A wall time that does not exist that day (the clocks jump over it) is read with the offset from before the jump, so
     * it comes that much later (02:30 in a 02:00 to 03:00 gap is 03:30), and from then on it stays at that time; a wall time that comes
     * twice (the clocks go back) is the first of the two. The same rule as the home's ReminderPlan.step.
     */
    static long step(long due, int days, TimeZone tz) {
        if (days == 0) return due;
        Calendar wall = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);   // the wall clock, with no change of its own
        wall.setTimeInMillis(due + tz.getOffset(due));
        wall.add(Calendar.DAY_OF_MONTH, days);
        long w = wall.getTimeInMillis();
        int before = tz.getOffset(w - DAY), after = tz.getOffset(w + DAY);
        boolean a = tz.getOffset(w - before) == before, b = tz.getOffset(w - after) == after;
        if (a && b) return Math.min(w - before, w - after);   // twice (or once, the same): the first
        if (a) return w - before;
        if (b) return w - after;
        return w - before;   // not at all: read with the offset from before the jump
    }

    /** the appointment after this one: a repeating one moves by whole calendar periods in `tz` until it is ahead of `now`; a single one stays */
    static long advance(long due, int repeat, long now, TimeZone tz) {
        int n = days(repeat);
        if (n == 0 || due <= 0) return due;
        long d = due;
        if (d > now) return d;
        long skip = (now - d) / (n * DAY) - 1;   // most of a long gap at once (whole periods of wall time), then one by one
        if (skip > 0) d = step(d, (int) Math.min(skip * n, 100_000), tz);
        while (d <= now) d = step(d, n, tz);
        return d;
    }

    /** advance in the TV's default zone (the tests' old form) */
    static long advance(long due, int repeat, long now) { return advance(due, repeat, now, TimeZone.getDefault()); }

    /** every open repeating task whose appointment and grace are over is moved to its next (calendar periods in `tz`); others are the same objects */
    static List<Task> roll(List<Task> v, long now, TimeZone tz) {
        List<Task> out = new ArrayList<>(v.size());
        for (Task t : v) {
            if (has(t) && t.repeat != NO && now >= t.due + GRACE) out.add(t.remind(advance(t.due, t.repeat, now, tz), t.before, t.repeat, t.sound, t.where));
            else out.add(t);
        }
        return out;
    }

    static List<Task> roll(List<Task> v, long now) { return roll(v, now, TimeZone.getDefault()); }

    /** false when anything in the list needs rolling (so the saved list is only written when it changed) */
    static boolean needsRoll(List<Task> v, long now) {
        for (Task t : v) if (has(t) && t.repeat != NO && now >= t.due + GRACE) return true;
        return false;
    }

    /** the zone of an id such as "Asia/Riyadh"; the TV's own when it is empty or unknown */
    static TimeZone zone(String id) {
        if (id == null || id.isEmpty()) return TimeZone.getDefault();
        TimeZone z = TimeZone.getTimeZone(id);
        return "GMT".equals(z.getID()) && !"GMT".equals(id) ? TimeZone.getDefault() : z;
    }

    /** "2026-11-05 17:00" (or with a T) in the zone, as epoch milliseconds; -1 when it is not a date and time */
    static long parse(String s, TimeZone tz) {
        if (s == null) return -1;
        String x = s.trim().replace('T', ' ');
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        f.setTimeZone(tz);
        f.setLenient(false);
        try {
            Date d = f.parse(x);
            return d == null ? -1 : d.getTime();
        } catch (ParseException e) {
            return -1;
        }
    }

    static String format(long ms, TimeZone tz) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        f.setTimeZone(tz);
        return f.format(new Date(ms));
    }

    static int beforeOf(int minutes) {
        for (int b : BEFORE) if (b == minutes) return b;
        return 0;
    }

    static int repeatOf(String s) {
        if ("daily".equals(s)) return DAILY;
        if ("weekly".equals(s)) return WEEKLY;
        return NO;
    }

    static String repeatName(int r) {
        return r == DAILY ? "daily" : r == WEEKLY ? "weekly" : "no";
    }
}
