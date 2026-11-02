package sa.urtv.home;

/**
 * The rows of a task's reminder page («ذكّرني»، اليوم، الساعة، «قبلها بـ»، «يتكرر»، «صوت خفيف»، «أين يظهر»), with no Android in them
 * (tests/ReminderPlanRun.java runs it): what each step of a ◀ ▶ or OK does to the reminder. Every value is epoch milliseconds or a small number; the zone
 * only matters to the screen that writes them as a day and a time.
 */
final class ReminderEdit {
    static final long MINUTE = 60_000L, HOUR = 60 * MINUTE, DAY = 24 * HOUR, STEP = 15 * MINUTE;
    static final int[] BEFORE = {0, 10, 30, 60};
    static final int NO = 0, DAILY = 1, WEEKLY = 2;
    /** where it shows: 0 the screensaver and the home, 1 the screensaver only, 2 the home only */
    static final int WHERE = 3;

    boolean on;
    long due;
    int before, repeat, where;
    boolean sound;

    ReminderEdit(long due, int before, int repeat, boolean sound, int where) {
        this.on = due > 0;
        this.due = due;
        this.before = beforeOf(before);
        this.repeat = Math.max(0, Math.min(2, repeat));
        this.sound = sound;
        this.where = Math.max(0, Math.min(2, where));
    }

    static int beforeOf(int m) {
        for (int b : BEFORE) if (b == m) return b;
        return 0;
    }

    /** the next whole hour that is at least an hour away: what turning «ذكّرني» on starts from */
    static long suggest(long now) {
        long t = now + HOUR;
        return t % HOUR == 0 ? t : t - t % HOUR + HOUR;
    }

    /** «ذكّرني» on or off; on from nothing takes the suggested time */
    void toggle(long now) {
        on = !on;
        if (on && due <= now) due = suggest(now);
    }

    /** a day later (+1) or earlier (-1); never into the past: a step that would land before `now` is not taken. true when it moved */
    boolean day(int dir, long now) {
        long d = due + dir * DAY;
        if (d <= now) return false;
        due = d;
        return true;
    }

    /** a quarter of an hour later or earlier, wrapping within the day; the same guard as day() */
    boolean time(int dir, long now) {
        long d = due + dir * STEP;
        if (d <= now) return false;
        due = d;
        return true;
    }

    void cycleBefore(int dir) {
        int k = 0;
        for (int i = 0; i < BEFORE.length; i++) if (BEFORE[i] == before) k = i;
        before = BEFORE[Math.floorMod(k + dir, BEFORE.length)];
    }

    void cycleRepeat(int dir) { repeat = Math.floorMod(repeat + dir, 3); }

    void cycleWhere(int dir) { where = Math.floorMod(where + dir, WHERE); }

    void toggleSound() { sound = !sound; }

    /** the appointment to save: 0 takes the reminder off */
    long savedDue() { return on ? due : 0; }
}
