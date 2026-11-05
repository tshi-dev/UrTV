package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/** For tests/reminders_test.py: Reminders.java, the reminder side of TaskList.java and ReminderKeys.java. Prints one line per check: name TAB ok. */
public final class ReminderRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    static final long M = Reminders.MINUTE, DAY = Reminders.DAY;
    static final TimeZone RIYADH = TimeZone.getTimeZone("Asia/Riyadh");

    public static void main(String[] args) {
        long due = Reminders.parse("2026-11-05 17:00", RIYADH);
        check("a date and time in the TV's city: 17:00 in Riyadh is 14:00 UTC", due == java.time.ZonedDateTime.of(2026, 11, 5, 14, 0, 0, 0, java.time.ZoneId.of("UTC")).toInstant().toEpochMilli());
        check("the same with a T", Reminders.parse("2026-11-05T17:00", RIYADH) == due);
        check("not a date: -1", Reminders.parse("tomorrow", RIYADH) == -1 && Reminders.parse("2026-13-40 25:61", RIYADH) == -1 && Reminders.parse(null, RIYADH) == -1 && Reminders.parse("", RIYADH) == -1);
        check("written back the same", Reminders.format(due, RIYADH).equals("2026-11-05 17:00"));
        check("«قبلها بـ» is 0, 10, 30 or 60 and anything else is 0", Reminders.beforeOf(30) == 30 && Reminders.beforeOf(60) == 60 && Reminders.beforeOf(7) == 0 && Reminders.beforeOf(-5) == 0);
        check("repeat names", Reminders.repeatOf("daily") == 1 && Reminders.repeatOf("weekly") == 2 && Reminders.repeatOf("x") == 0 && Reminders.repeatOf(null) == 0 && Reminders.repeatName(2).equals("weekly"));

        Task plain = new Task("اشتر خبز", false, 100, 1);
        check("a task from before reminders has none", plain.due == 0 && plain.before == 0 && plain.repeat == 0 && !plain.sound && plain.where == 0 && plain.snooze == 0 && !Reminders.has(plain));
        Task r = plain.remind(due, 30, 0, false, 0);
        check("a reminder keeps the task's text, id and time made", r.text.equals(plain.text) && r.id == 1 && r.at == 100 && Reminders.has(r));
        check("the card comes «قبلها بـ» ahead", Reminders.fireAt(r) == due - 30 * M);
        check("a snooze moves the card, not the appointment", Reminders.fireAt(r.snoozed(due - 5 * M)) == due - 5 * M && r.snoozed(due - 5 * M).due == due);
        check("a done task has no reminder", !Reminders.has(new Task("x", true, 1, 2, due, 0, 0, false, 0, 0)));

        List<Task> v = new ArrayList<>();
        v.add(plain);
        v.add(new Task("طبيب", false, 1, 2, due, 30, 0, false, 0, 0));
        v.add(new Task("ثاني", false, 1, 3, due + 60 * M, 0, 0, false, 0, 0));
        v.add(new Task("منجزة", true, 1, 4, due - 90 * M, 0, 0, false, 0, 0));
        check("the next card is the earliest one ahead (not a done one, not a plain one)", Reminders.next(v, due - 120 * M) == due - 30 * M);
        check("after it, the next", Reminders.next(v, due - 29 * M) == due + 60 * M);
        check("none ahead: 0", Reminders.next(v, due + 61 * M) == 0);
        check("nothing is showing before its time", Reminders.showing(v, due - 31 * M).isEmpty());
        check("showing from its time, soonest appointment first", Reminders.showing(v, due + 70 * M).size() == 1 && Reminders.showing(v, due - 20 * M).get(0).id == 2);
        check("it goes 30 minutes after the appointment when nobody answers", Reminders.showing(List.of(v.get(1)), due + 29 * M).size() == 1 && Reminders.showing(List.of(v.get(1)), due + 31 * M).isEmpty());
        check("…the task is still open then", !v.get(1).done);
        check("a snooze keeps the card showing until it has been 30 minutes", Reminders.showing(List.of(r.snoozed(due + 20 * M)), due + 49 * M).size() == 1);

        check("a daily one moves to the same hour tomorrow", Reminders.advance(due, Reminders.DAILY, due) == due + DAY);
        check("a weekly one to next week", Reminders.advance(due, Reminders.WEEKLY, due) == due + 7 * DAY);
        check("many missed days: the next one ahead, on the hour", Reminders.advance(due, Reminders.DAILY, due + 3 * DAY + 5 * M) == due + 4 * DAY);
        check("a single one stays", Reminders.advance(due, Reminders.NO, due + DAY) == due);
        Task daily = new Task("دواء", false, 1, 5, due, 0, Reminders.DAILY, false, 0, 0);
        List<Task> rolled = Reminders.roll(List.of(daily, r), due + 40 * M);
        check("an unanswered daily one is rolled to tomorrow once its grace is over; a single one is left", rolled.get(0).due == due + DAY && rolled.get(1) == r && Reminders.needsRoll(List.of(daily), due + 40 * M) && !Reminders.needsRoll(List.of(daily), due + 10 * M));

        List<Task> ids = TaskList.withIds(v, 0);
        check("ids keep the reminders", ids.get(1).due == due && ids.get(1).before == 30);
        List<Task> added = TaskList.addReminder(new ArrayList<>(), "موعد", 5, 1, 12, due, 10, 1, true, 2);
        check("a reminder is added with its rules", added != null && added.size() == 1 && added.get(0).due == due && added.get(0).before == 10 && added.get(0).repeat == 1 && added.get(0).sound && added.get(0).where == 2 && !added.get(0).done);
        check("the same limit as a task", TaskList.addReminder(fill(12), "x", 5, 99, 12, due, 0, 0, false, 0) == null);
        List<Task> set = TaskList.remind(plainList(), 1, due, 30, 2, true, 1);
        check("a reminder is set on one task", set != null && set.get(0).due == due && set.get(0).repeat == 2 && set.get(1).due == 0);
        check("…and taken off with 0", TaskList.remind(set, 1, 0, 0, 0, false, 0).get(0).due == 0 && !Reminders.has(TaskList.remind(set, 1, 0, 0, 0, false, 0).get(0)));
        check("no such task: null", TaskList.remind(plainList(), 9, due, 0, 0, false, 0) == null && TaskList.snooze(plainList(), 9, 5) == null);
        List<Task> sn = TaskList.snooze(set, 1, due - 3 * M);
        check("a snooze is kept on the task", sn.get(0).snooze == due - 3 * M && sn.get(0).due == due);
        List<Task> done = TaskList.markDone(sn, 1, due - 20 * M);
        check("done on a repeating reminder is done for this time only: it moves a week, stays open, no snooze", done.get(0).due == due + 7 * DAY && !done.get(0).done && done.get(0).snooze == 0);
        List<Task> one = TaskList.remind(plainList(), 1, due, 0, 0, false, 0);
        check("done on a single reminder finishes the task", TaskList.markDone(one, 1, due).get(0).done);
        check("done on a plain task as always", TaskList.markDone(plainList(), 2, due).get(1).done && TaskList.markDone(plainList(), 2).get(1).done);

        ReminderKeys k = new ReminderKeys();
        check("no card: every key goes on", k.decide(23, 0, 0, false) == ReminderKeys.PASS);
        check("OK presses a button", k.decide(23, 0, 0, true) == ReminderKeys.OK);
        check("its repeat and release are eaten, not passed on", k.decide(23, 0, 1, true) == ReminderKeys.EAT && k.decide(23, 1, 0, true) == ReminderKeys.EAT);
        check("then the next press counts again", k.decide(22, 0, 0, true) == ReminderKeys.MOVE);
        k.decide(22, 1, 0, true);
        check("an arrow moves", k.decide(21, 0, 0, true) == ReminderKeys.MOVE);
        k.decide(21, 1, 0, true);
        check("any other key puts the card away, and does not close the screensaver", k.decide(19, 0, 0, true) == ReminderKeys.AWAY && k.decide(19, 1, 0, true) == ReminderKeys.EAT);
        check("Enter and the pad's A are OK too", new ReminderKeys().decide(66, 0, 0, true) == ReminderKeys.OK && new ReminderKeys().decide(96, 0, 0, true) == ReminderKeys.OK);
        ReminderKeys q = new ReminderKeys();
        check("Power, Home, the volume and the wake keys stay Android's", q.decide(26, 0, 0, true) == ReminderKeys.PASS && q.decide(3, 0, 0, true) == ReminderKeys.PASS
                && q.decide(24, 0, 0, true) == ReminderKeys.PASS && q.decide(25, 0, 0, true) == ReminderKeys.PASS && q.decide(164, 0, 0, true) == ReminderKeys.PASS
                && q.decide(224, 0, 0, true) == ReminderKeys.PASS && q.decide(223, 0, 0, true) == ReminderKeys.PASS);
    }

    static List<Task> fill(int n) {
        List<Task> v = new ArrayList<>();
        for (int i = 0; i < n; i++) v.add(new Task("t" + i, false, 1, i + 1));
        return v;
    }

    static List<Task> plainList() {
        List<Task> v = new ArrayList<>();
        v.add(new Task("أولى", false, 1, 1));
        v.add(new Task("ثانية", false, 2, 2));
        return v;
    }
}
