package sa.urtv.screensaver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    /** the appointment after this one: a repeating one moves by whole periods until it is ahead of `now`; a single one stays (0 change) */
    static long advance(long due, int repeat, long now) {
        long p = period(repeat);
        if (p == 0 || due <= 0) return due;
        long d = due;
        if (d <= now) d += ((now - d) / p + 1) * p;
        return d;
    }

    /** every open repeating task whose appointment and grace are over is moved to its next; others are the same objects */
    static List<Task> roll(List<Task> v, long now) {
        List<Task> out = new ArrayList<>(v.size());
        for (Task t : v) {
            if (has(t) && t.repeat != NO && now >= t.due + GRACE) out.add(t.remind(advance(t.due, t.repeat, now), t.before, t.repeat, t.sound, t.where));
            else out.add(t);
        }
        return out;
    }

    /** false when anything in the list needs rolling (so the saved list is only written when it changed) */
    static boolean needsRoll(List<Task> v, long now) {
        for (Task t : v) if (has(t) && t.repeat != NO && now >= t.due + GRACE) return true;
        return false;
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
