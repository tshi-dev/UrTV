package sa.urtv.screensaver;

import java.util.List;
import java.util.TimeZone;

/** For tests/reminder_fixes_test.py (the 4.2 review, R08 on the screensaver's side): repeats, rollover, done and snooze in calendar days. */
public final class ReminderDstRun {
    static final long M = Reminders.MINUTE, H = 60 * M, DAY = Reminders.DAY;
    static final TimeZone RIYADH = TimeZone.getTimeZone("Asia/Riyadh"), BERLIN = TimeZone.getTimeZone("Europe/Berlin");
    static StringBuilder out = new StringBuilder();
    static void check(String name, boolean ok, Object detail) { out.append(name).append('\t').append(ok ? "ok" : String.valueOf(detail)).append('\n'); }

    static long local(TimeZone z, int y, int mo, int d, int h, int mi) {
        java.util.Calendar c = java.util.Calendar.getInstance(z);
        c.clear();
        c.set(y, mo - 1, d, h, mi);
        return c.getTimeInMillis();
    }

    static String wall(long t) { return Reminders.format(t, BERLIN); }

    public static void main(String[] a) {
        long spring = local(BERLIN, 2026, 3, 28, 17, 0), fall = local(BERLIN, 2026, 10, 24, 17, 0);
        check("advance daily over spring forward: 17:00 again", wall(Reminders.advance(spring, Reminders.DAILY, spring, BERLIN)).equals("2026-03-29 17:00"), wall(Reminders.advance(spring, Reminders.DAILY, spring, BERLIN)));
        check("advance daily over fall back: 17:00 again", wall(Reminders.advance(fall, Reminders.DAILY, fall, BERLIN)).equals("2026-10-25 17:00"), wall(Reminders.advance(fall, Reminders.DAILY, fall, BERLIN)));
        check("advance weekly over the change", wall(Reminders.advance(spring - 3 * DAY, Reminders.WEEKLY, spring, BERLIN)).equals("2026-04-01 17:00"), wall(Reminders.advance(spring - 3 * DAY, Reminders.WEEKLY, spring, BERLIN)));
        check("many missed days over the change: the next one ahead, on the hour", wall(Reminders.advance(spring - 10 * DAY, Reminders.DAILY, spring + 2 * DAY + H, BERLIN)).equals("2026-03-31 17:00"), wall(Reminders.advance(spring - 10 * DAY, Reminders.DAILY, spring + 2 * DAY + H, BERLIN)));
        check("months missed: still the hour", wall(Reminders.advance(local(BERLIN, 2025, 12, 1, 17, 0), Reminders.DAILY, local(BERLIN, 2026, 7, 1, 12, 0), BERLIN)).equals("2026-07-01 17:00"), "");
        long gap = local(BERLIN, 2026, 3, 28, 2, 30), twice = local(BERLIN, 2026, 10, 24, 2, 30);
        check("a time the clocks jump over: after the jump", Reminders.step(gap, 1, BERLIN) == local(BERLIN, 2026, 3, 29, 3, 30), wall(Reminders.step(gap, 1, BERLIN)));
        check("a time that comes twice: the first", Reminders.step(twice, 1, BERLIN) == twice + DAY, Reminders.step(twice, 1, BERLIN) - twice);
        Task t = new Task("x", false, 1, 7, spring, 10, Reminders.DAILY, false, 0, 0);
        List<Task> rolled = Reminders.roll(List.of(t), spring + 40 * M, BERLIN);
        check("rollover over spring forward: tomorrow 17:00", wall(rolled.get(0).due).equals("2026-03-29 17:00"), wall(rolled.get(0).due));
        Task f = new Task("x", false, 1, 8, fall, 0, Reminders.WEEKLY, false, 0, 0);
        rolled = Reminders.roll(List.of(f), fall + 40 * M, BERLIN);
        check("weekly rollover over fall back: next week 17:00", wall(rolled.get(0).due).equals("2026-10-31 17:00"), wall(rolled.get(0).due));
        List<Task> done = TaskList.markDone(List.of(t), 7, spring + 5 * M, BERLIN);
        check("done over the change: tomorrow 17:00, still open", wall(done.get(0).due).equals("2026-03-29 17:00") && !done.get(0).done, wall(done.get(0).due));
        // a snooze is real minutes (10 minutes are 10 minutes, even over the change); the rollover after it is calendar days, and drops it
        long night = local(BERLIN, 2026, 3, 29, 1, 55);
        Task s = new Task("x", false, 1, 9, night, 0, Reminders.DAILY, false, 0, 0).snoozed(night + 10 * M);
        check("a snooze of 10 minutes over the jump: 03:05", wall(Reminders.fireAt(s)).equals("2026-03-29 03:05"), wall(Reminders.fireAt(s)));
        rolled = Reminders.roll(List.of(s), night + 45 * M, BERLIN);
        check("then rolled: tomorrow 01:55, the snooze gone", wall(rolled.get(0).due).equals("2026-03-30 01:55") && rolled.get(0).snooze == 0, wall(rolled.get(0).due));
        long riy = local(RIYADH, 2026, 11, 5, 17, 0);
        check("Riyadh: as before, exactly a day and a week", Reminders.advance(riy, Reminders.DAILY, riy, RIYADH) == riy + DAY && Reminders.advance(riy, Reminders.WEEKLY, riy, RIYADH) == riy + 7 * DAY, "");
        check("an unknown zone is the TV's own", Reminders.zone("Nowhere/City").getID().equals(TimeZone.getDefault().getID()) && Reminders.zone("").getID().equals(TimeZone.getDefault().getID()) && Reminders.zone("Europe/Berlin").getID().equals("Europe/Berlin"), "");
        long due = local(RIYADH, 2026, 11, 5, 17, 0), bdue = spring, wk = local(BERLIN, 2026, 3, 25, 9, 0);
        long[] ins = {bdue, fall, wk, gap, twice, due};
        TimeZone[] zs = {BERLIN, BERLIN, BERLIN, BERLIN, BERLIN, RIYADH};
        StringBuilder same = new StringBuilder();
        for (int i = 0; i < ins.length; i++) same.append(Reminders.step(ins[i], 1, zs[i])).append(',').append(Reminders.step(ins[i], 7, zs[i])).append(';');
        out.append("steps\t").append(same).append('\n');
        System.out.print(out);
    }
}
