package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/** For tests/reminders_test.py: ReminderPlan.java (when the home's card and the alarm are due) and ReminderEdit.java (what each row's step does). Prints one line per check: name TAB ok. */
public final class ReminderHomeRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    static final long M = 60_000L, H = 60 * M, D = 24 * H;

    static ReminderPlan.Row row(long id, long due, int before, int where, long snooze, boolean done) {
        return new ReminderPlan.Row(id, "t" + id, done, due, before, false, where, snooze);
    }

    public static void main(String[] args) {
        long due = 1_800_000_000_000L;
        List<ReminderPlan.Row> v = new ArrayList<>();
        v.add(row(1, due, 30, 0, 0, false));
        v.add(row(2, due + H, 0, 2, 0, false));
        v.add(row(3, due + 2 * H, 0, 1, 0, false));   // the screensaver only: never the home's
        v.add(row(4, due - H, 0, 0, 0, true));        // done
        check("the alarm is for the first card ahead", ReminderPlan.next(v, due - 2 * H) == due - 30 * M);
        check("then the next one the home shows (a screensaver-only one is skipped)", ReminderPlan.next(v, due - 29 * M) == due + H);
        check("nothing ahead: 0", ReminderPlan.next(v, due + 3 * H) == 0);
        check("a snooze is the card time", row(5, due, 30, 0, due - 5 * M, false).fireAt() == due - 5 * M);
        check("showing: from the card time until 30 minutes after the appointment", ReminderPlan.showing(v, due - 31 * M).isEmpty() && ReminderPlan.showing(v, due - 30 * M).size() == 1
                && ReminderPlan.showing(v, due + 29 * M).size() == 1 && ReminderPlan.showing(v, due + 31 * M).isEmpty());
        check("home-only shows on the home", ReminderPlan.showing(v, due + H).size() == 1 && ReminderPlan.showing(v, due + H).get(0).id == 2);
        check("the same card has the same key, a snoozed one another", ReminderPlan.key(v.get(0)).equals(ReminderPlan.key(row(1, due, 30, 0, 0, false))) && !ReminderPlan.key(v.get(0)).equals(ReminderPlan.key(row(1, due, 30, 0, due - 5 * M, false))));
        check("minutes left, rounded up, never under one", ReminderPlan.minutesLeft(v.get(0), due - 30 * M) == 30 && ReminderPlan.minutesLeft(v.get(0), due - 29 * M - 1) == 30 && ReminderPlan.minutesLeft(v.get(0), due - 10_000) == 1);

        ReminderEdit e = new ReminderEdit(0, 0, 0, false, 0);
        long now = due;
        check("a task with no reminder starts off", !e.on && e.savedDue() == 0);
        e.toggle(now);
        check("turning it on suggests a whole hour at least an hour away", e.on && e.due >= now + H && e.due < now + 2 * H + 1 && e.due % H == 0);
        long d0 = e.due;
        check("a day later and a day earlier", e.day(1, now) && e.due == d0 + D && e.day(-1, now) && e.due == d0);
        check("never into the past: a step back that lands before now is not taken", !e.day(-1, d0 + D - 1) || e.due > d0 + D - 1);
        check("a quarter of an hour at a time", e.time(1, now) && e.due == d0 + 15 * M && e.time(-1, now) && e.due == d0);
        e.before = 0;
        e.cycleBefore(1); check("«قبلها بـ» goes 0, 10, 30, 60", e.before == 10);
        e.cycleBefore(1); e.cycleBefore(1); check("…60", e.before == 60);
        e.cycleBefore(1); check("…and round to 0", e.before == 0);
        e.cycleBefore(-1); check("…and back", e.before == 60);
        e.cycleRepeat(1); check("repeat: daily", e.repeat == ReminderEdit.DAILY);
        e.cycleRepeat(1); e.cycleRepeat(1); check("…weekly, then no", e.repeat == ReminderEdit.NO);
        check("the soft chime is off until asked", !new ReminderEdit(due, 0, 0, false, 0).sound);
        e.toggleSound(); check("…and a step turns it on", e.sound);
        e.cycleWhere(1); e.cycleWhere(1); e.cycleWhere(1); check("where goes round the three", e.where == 0);
        e.toggle(now); check("off: nothing is saved as the appointment", !e.on && e.savedDue() == 0);
        ReminderEdit set = new ReminderEdit(due, 33, 9, true, 7);
        check("odd values from an app are made safe", set.before == 0 && set.repeat == 2 && set.where == 2 && set.on);
    }
}
