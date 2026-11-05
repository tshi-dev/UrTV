package sa.urtv.screensaver;

/**
 * One task kept on the TV. The id is its own and stays with it (0: a task saved before ids existed, given one when the list is read).
 * A reminder is a task with a due time: due is the appointment itself (epoch milliseconds, 0 = none), before the minutes ahead of it the
 * card comes, repeat 0 no / 1 daily / 2 weekly, sound a chime with the card, where 0 the screensaver and the home / 1 the screensaver
 * only / 2 the home only, snooze the time «ذكّرني بعد ١٠ دقائق» asked for (0 = none). A task saved before reminders has none of them.
 */
final class Task {
    final String text;
    final boolean done;
    final long at;
    final long id;
    final long due;
    final int before;
    final int repeat;
    final boolean sound;
    final int where;
    final long snooze;

    Task(String text, boolean done, long at, long id) {
        this(text, done, at, id, 0, 0, 0, false, 0, 0);
    }

    Task(String text, boolean done, long at, long id, long due, int before, int repeat, boolean sound, int where, long snooze) {
        this.text = text;
        this.done = done;
        this.at = at;
        this.id = id;
        this.due = due;
        this.before = before;
        this.repeat = repeat;
        this.sound = sound;
        this.where = where;
        this.snooze = snooze;
    }

    /** the same task with another text, done flag and id, its reminder kept as it was */
    Task with(String text, boolean done, long id) {
        return new Task(text, done, at, id, due, before, repeat, sound, where, snooze);
    }

    /** the same task with this reminder (due 0 = none) and no snooze */
    Task remind(long due, int before, int repeat, boolean sound, int where) {
        return new Task(text, done, at, id, due, before, repeat, sound, where, 0);
    }

    Task snoozed(long until) {
        return new Task(text, done, at, id, due, before, repeat, sound, where, until);
    }
}
