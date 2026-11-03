package sa.urtv.home;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/** For tests/reminder_fixes_test.py (the 4.2 review, R04 R07 R08 on the home's side): the alarm's plan with the repeat and zone, the
 *  delivered keys, the calendar step. Prints "name<TAB>ok" or "name<TAB>detail". */
public final class ReminderFixHomeRun {
    static final long M = ReminderPlan.MINUTE, H = 60 * M, DAY = 24 * H;
    static final TimeZone RIYADH = TimeZone.getTimeZone("Asia/Riyadh"), BERLIN = TimeZone.getTimeZone("Europe/Berlin"), NY = TimeZone.getTimeZone("America/New_York");
    static StringBuilder out = new StringBuilder();
    static void check(String name, boolean ok, Object detail) { out.append(name).append('\t').append(ok ? "ok" : String.valueOf(detail)).append('\n'); }

    static ReminderPlan.Row row(long id, long due, int before, int repeat, TimeZone z) { return new ReminderPlan.Row(id, "t" + id, false, due, before, false, 0, 0, repeat, z); }

    /** what the screensaver does when the list is read (Prefs.tasks -> Reminders.roll): a repeating one past its grace moves on */
    static ReminderPlan.Row roll(ReminderPlan.Row r, long now) {
        if (r.repeat == ReminderPlan.NO || now < r.due + ReminderPlan.GRACE) return r;
        int n = r.repeat == ReminderPlan.DAILY ? 1 : 7;
        long d = r.due;
        while (d <= now) d = ReminderPlan.step(d, n, r.zone);
        return row(r.id, d, r.before, r.repeat, r.zone);
    }

    /** the alarm's chain with nobody answering and another app in front: fire, deliver, plan the next; `late` ms added to each delivery */
    static List<Long> chain(ReminderPlan.Row r, long start, int times, long late) {
        List<Long> at = new ArrayList<>();
        long t = start;
        for (int i = 0; i < times; i++) {
            r = roll(r, t);
            long next = ReminderPlan.next(List.of(r), t);
            if (next <= 0) { at.add(0L); return at; }
            at.add(next);
            t = next + late;
        }
        return at;
    }

    static long local(TimeZone z, int y, int mo, int d, int h, int mi) {
        java.util.Calendar c = java.util.Calendar.getInstance(z);
        c.clear();
        c.set(y, mo - 1, d, h, mi);
        return c.getTimeInMillis();
    }

    static String wall(long t, TimeZone z) {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm Z", java.util.Locale.US);
        f.setTimeZone(z);
        return f.format(new java.util.Date(t));
    }

    public static void main(String[] a) {
        // ---- R04: an unanswered repeating reminder keeps its future alarms
        long due = local(RIYADH, 2026, 11, 5, 17, 0);
        ReminderPlan.Row daily = row(1, due, 10, ReminderPlan.DAILY, RIYADH);
        long fire = due - 10 * M;
        check("R04 before the card: the card's own time", ReminderPlan.next(List.of(daily), fire - M) == fire, ReminderPlan.next(List.of(daily), fire - M));
        check("R04 at its first firing (the card stays): tomorrow's card is on the alarm", ReminderPlan.next(List.of(daily), fire) == fire + DAY, wall(ReminderPlan.next(List.of(daily), fire), RIYADH));
        check("R04 inside the grace, unanswered: still tomorrow's", ReminderPlan.next(List.of(daily), due + 29 * M) == fire + DAY, ReminderPlan.next(List.of(daily), due + 29 * M));
        check("R04 the card now still shows (it is not rolled early)", ReminderPlan.showing(List.of(daily), due + 29 * M).size() == 1, "");
        ReminderPlan.Row weekly = row(2, due, 30, ReminderPlan.WEEKLY, RIYADH);
        check("R04 weekly, at its firing: next week's", ReminderPlan.next(List.of(weekly), due - 30 * M) == due - 30 * M + 7 * DAY, ReminderPlan.next(List.of(weekly), due - 30 * M));
        ReminderPlan.Row single = row(3, due, 10, ReminderPlan.NO, RIYADH);
        check("R04 a single one has nothing after its card", ReminderPlan.next(List.of(single), fire) == 0, "");
        List<Long> c = chain(daily, fire - H, 6, 0);
        boolean ok = c.size() == 6;
        for (int i = 0; i < c.size(); i++) ok &= c.get(i) == fire + i * DAY;
        check("R04 daily, unanswered, another app in front: six cards in a row, one a day", ok, c);
        c = chain(weekly, due - H, 4, 0);
        ok = c.size() == 4;
        for (int i = 0; i < c.size(); i++) ok &= c.get(i) == due - 30 * M + i * 7 * DAY;
        check("R04 weekly, unanswered: four weeks in a row", ok, c);
        // a reboot or an update puts the alarm back from scratch (the boot receiver: ReminderScheduler.schedule), inside the grace and after it
        check("R04 a reboot inside the grace: the next card", ReminderPlan.next(List.of(roll(daily, due + 20 * M)), due + 20 * M) == fire + DAY, "");
        check("R04 a reboot after three days off: the next card ahead", ReminderPlan.next(List.of(roll(daily, due + 3 * DAY + H)), due + 3 * DAY + H) == fire + 4 * DAY, ReminderPlan.next(List.of(roll(daily, due + 3 * DAY + H)), due + 3 * DAY + H));
        // a delivery missed or late (the TV asleep, Android late): 2 hours late every time, the chain still goes on day by day
        c = chain(daily, fire - H, 5, 2 * H);
        ok = c.size() == 5;
        for (int i = 0; i < c.size(); i++) ok &= c.get(i) == fire + i * DAY;
        check("R04 deliveries 2 hours late: the next card each day still", ok, c);
        // with an hour before, the card comes 30 minutes before the last one's grace is over: still planned
        ReminderPlan.Row early = row(4, due, 60, ReminderPlan.DAILY, RIYADH);
        check("R04 «قبلها بـ» 60: the next card", ReminderPlan.next(List.of(early), due + 10 * M) == due - H + DAY, "");
        // across summer time: the next card at the same wall time
        long bdue = local(BERLIN, 2026, 3, 28, 17, 0);
        ReminderPlan.Row b = row(5, bdue, 0, ReminderPlan.DAILY, BERLIN);
        check("R04 the planned next card keeps the hour across summer time", wall(ReminderPlan.next(List.of(b), bdue), BERLIN).equals("2026-03-29 17:00 +0200"), wall(ReminderPlan.next(List.of(b), bdue), BERLIN));

        // ---- R07: two reminders inside one grace, and an alarm delivered twice
        ReminderPlan.Row first = row(10, due, 0, ReminderPlan.NO, RIYADH), second = row(11, due + 10 * M, 0, ReminderPlan.NO, RIYADH);
        List<ReminderPlan.Row> both = List.of(first, second);
        Set<String> sent = new LinkedHashSet<>();
        List<ReminderPlan.Row> show = ReminderPlan.showing(both, due), fr = ReminderPlan.fresh(show, sent);
        check("R07 the first one at its time", fr.size() == 1 && fr.get(0).id == 10, fr.size());
        sent = ReminderPlan.remember(sent, show, fr);
        check("R07 the alarm then goes for the second", ReminderPlan.next(both, due) == due + 10 * M, "");
        fr = ReminderPlan.fresh(ReminderPlan.showing(both, due), sent);
        check("R07 the alarm delivered twice: nothing twice", fr.isEmpty(), fr.size());
        show = ReminderPlan.showing(both, due + 10 * M);
        fr = ReminderPlan.fresh(show, sent);
        check("R07 inside the first's grace, the second is the one delivered (not the older one again)", show.size() == 2 && fr.size() == 1 && fr.get(0).id == 11, show.size() + " " + fr.size());
        sent = ReminderPlan.remember(sent, show, fr);
        check("R07 the second delivered twice: nothing", ReminderPlan.fresh(ReminderPlan.showing(both, due + 10 * M), sent).isEmpty(), "");
        show = ReminderPlan.showing(both, due + 35 * M);
        sent = ReminderPlan.remember(sent, show, new ArrayList<>());
        check("R07 a key goes once its card is over", sent.size() == 1 && sent.contains(ReminderPlan.key(second)), sent);
        ReminderPlan.Row snoozed = new ReminderPlan.Row(11, "t11", false, due + 10 * M, 0, false, 0, due + 40 * M, ReminderPlan.NO, RIYADH);
        check("R07 a snooze is a new key: its card comes again", ReminderPlan.fresh(ReminderPlan.showing(List.of(snoozed), due + 40 * M), sent).size() == 1, "");
        check("R07 both due together: both delivered at once", ReminderPlan.fresh(ReminderPlan.showing(List.of(first, row(12, due, 0, 0, RIYADH)), due), new HashSet<>()).size() == 2, "");

        // ---- R08: calendar days in the zone
        check("R08 spring forward, daily: the same hour", wall(ReminderPlan.step(bdue, 1, BERLIN), BERLIN).equals("2026-03-29 17:00 +0200") && ReminderPlan.step(bdue, 1, BERLIN) - bdue == 23 * H, wall(ReminderPlan.step(bdue, 1, BERLIN), BERLIN));
        long fall = local(BERLIN, 2026, 10, 24, 17, 0);
        check("R08 fall back, daily: the same hour", wall(ReminderPlan.step(fall, 1, BERLIN), BERLIN).equals("2026-10-25 17:00 +0100") && ReminderPlan.step(fall, 1, BERLIN) - fall == 25 * H, wall(ReminderPlan.step(fall, 1, BERLIN), BERLIN));
        long wk = local(BERLIN, 2026, 3, 25, 9, 0);
        check("R08 weekly across the change", wall(ReminderPlan.step(wk, 7, BERLIN), BERLIN).equals("2026-04-01 09:00 +0200"), wall(ReminderPlan.step(wk, 7, BERLIN), BERLIN));
        long gap = local(BERLIN, 2026, 3, 28, 2, 30);
        check("R08 a time the clocks jump over comes after the jump", wall(ReminderPlan.step(gap, 1, BERLIN), BERLIN).equals("2026-03-29 03:30 +0200"), wall(ReminderPlan.step(gap, 1, BERLIN), BERLIN));
        long twice = local(BERLIN, 2026, 10, 24, 2, 30);
        check("R08 a time that comes twice: the first", wall(ReminderPlan.step(twice, 1, BERLIN), BERLIN).equals("2026-10-25 02:30 +0200"), wall(ReminderPlan.step(twice, 1, BERLIN), BERLIN));
        long ny = local(NY, 2026, 11, 1, 1, 30);   // New York's own fall back is at 02:00 -> 01:00 on 1 November 2026
        check("R08 New York: 01:30 the day after the change", wall(ReminderPlan.step(ny, 1, NY), NY).equals("2026-11-02 01:30 -0500"), wall(ReminderPlan.step(ny, 1, NY), NY));
        check("R08 Riyadh (no summer time): exactly a day", ReminderPlan.step(due, 1, RIYADH) == due + DAY && ReminderPlan.step(due, 7, RIYADH) == due + 7 * DAY, "");
        // the step both apps use: printed for the screensaver's run to compare
        long[] ins = {bdue, fall, wk, gap, twice, due};
        TimeZone[] zs = {BERLIN, BERLIN, BERLIN, BERLIN, BERLIN, RIYADH};
        StringBuilder same = new StringBuilder();
        for (int i = 0; i < ins.length; i++) same.append(ReminderPlan.step(ins[i], 1, zs[i])).append(',').append(ReminderPlan.step(ins[i], 7, zs[i])).append(';');
        out.append("steps\t").append(same).append('\n');
        System.out.print(out);
    }
}
