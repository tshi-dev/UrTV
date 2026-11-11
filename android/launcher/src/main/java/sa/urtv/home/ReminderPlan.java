package sa.urtv.home;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

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

    static final long DAY = 24 * 60 * MINUTE;
    static final int NO = 0, DAILY = 1, WEEKLY = 2;   // the screensaver's Reminders

    static final class Row {
        final long id, due, snooze;
        final int before, where, repeat;
        final boolean sound, done;
        final String text;
        final TimeZone zone;   // where its repeats count whole days (the screensaver's city), the TV's own when it does not say

        Row(long id, String text, boolean done, long due, int before, boolean sound, int where, long snooze) {
            this(id, text, done, due, before, sound, where, snooze, NO, null);
        }

        Row(long id, String text, boolean done, long due, int before, boolean sound, int where, long snooze, int repeat, TimeZone zone) {
            this.id = id; this.text = text; this.done = done; this.due = due; this.before = before; this.sound = sound; this.where = where; this.snooze = snooze;
            this.repeat = repeat == DAILY || repeat == WEEKLY ? repeat : NO;
            this.zone = zone == null ? ZoneFix.tvZone() : zone;
        }

        long fireAt() { return snooze > 0 ? snooze : due - before * MINUTE; }

        boolean open() { return !done && due > 0 && where != 1; }
    }

    /** «ذكّرني قبلها» on a game in «المباريات»: its «قبلها بـ», one of the 0 10 30 60 a task may have */
    static final int MATCH_BEFORE = 30;

    /** the reminder a game gets (MatchesActivity.MatchReminder): {due, before, repeat, sound, where} = kick-off, 30, none, silent, the screensaver
     *  and the home; null for a kick-off that is not a time */
    static long[] forMatch(long kickoff) {
        if (kickoff <= 0) return null;
        return new long[] {kickoff, MATCH_BEFORE, 0, 0, 0};
    }

    /**
     * The first moment after `now` a card is due over every open reminder the home shows; 0 when there is none. A repeating reminder whose
     * card is due or showing now (nobody answered it yet: the screensaver rolls it only when its grace is over) also has its next
     * appointment's card ahead, planned here from its repeat and zone, so the alarm never depends on a later read of the list: the card
     * now stays, and the next one is still on the alarm (another app in front, a reboot, a delivery missed).
     */
    static long next(List<Row> v, long now) {
        long best = 0;
        for (Row r : v) {
            if (!r.open()) continue;
            long f = r.fireAt();
            if (f <= now && r.repeat != NO) f = nextFire(r, now);
            if (f > now && (best == 0 || f < best)) best = f;
        }
        return best;
    }

    /** the card of the first appointment after this one whose card is after `now` (calendar periods in the reminder's zone) */
    static long nextFire(Row r, long now) {
        int n = r.repeat == DAILY ? 1 : r.repeat == WEEKLY ? 7 : 0;
        if (n == 0) return 0;
        long d = step(r.due, n, r.zone);
        long skip = (now - d) / (n * DAY) - 1;   // most of a long gap at once, then one by one
        if (skip > 0) d = step(d, (int) Math.min(skip * n, 100_000), r.zone);
        while (d - r.before * MINUTE <= now) d = step(d, n, r.zone);
        return d - r.before * MINUTE;
    }

    /**
     * `days` calendar days after `due` at the same wall-clock time in `tz` (summer time keeps the hour); a wall time the clocks jump over
     * comes that much later (the offset from before the jump), one that comes twice is the first. The screensaver's Reminders.step, the same rule.
     */
    static long step(long due, int days, TimeZone tz) {
        if (days == 0) return due;
        Calendar wall = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        wall.setTimeInMillis(due + tz.getOffset(due));
        wall.add(Calendar.DAY_OF_MONTH, days);
        long w = wall.getTimeInMillis();
        int before = tz.getOffset(w - DAY), after = tz.getOffset(w + DAY);
        boolean a = tz.getOffset(w - before) == before, b = tz.getOffset(w - after) == after;
        if (a && b) return Math.min(w - before, w - after);
        if (a) return w - before;
        if (b) return w - after;
        return w - before;
    }

    /**
     * The cards showing now that were not delivered yet (by their key: a reminder, at its time; a snooze or the next repeat is a new key),
     * soonest appointment first: two reminders inside one grace each get theirs, and an alarm delivered twice delivers nothing twice.
     */
    static List<Row> fresh(List<Row> showing, Set<String> delivered) {
        List<Row> out = new ArrayList<>();
        for (Row r : showing) if (!delivered.contains(key(r))) out.add(r);
        return out;
    }

    /** What stays remembered as delivered: the keys still showing, with the ones just delivered (an old key goes once its card is over). */
    static Set<String> remember(Set<String> delivered, List<Row> showing, List<Row> now) {
        Set<String> keep = new LinkedHashSet<>();
        for (Row r : showing) if (delivered.contains(key(r))) keep.add(key(r));
        for (Row r : now) keep.add(key(r));
        return keep;
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
