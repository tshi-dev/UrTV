package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The changes to the list of tasks, with no Android in them (tests/TaskListRun.java runs them). Every change names the task by its
 * id, not by its text or its place: two tasks may say the same thing, and the list may have moved since the screen drew it.
 * Each returns a new list (null when it cannot be done) and leaves the one it was given alone.
 */
final class TaskList {
    private TaskList() { }

    /** true when a task has no id yet, or two share one: the list is saved again with ids (text, done, time and order untouched) */
    static boolean needsIds(List<Task> v) {
        Set<Long> seen = new HashSet<>();
        for (Task t : v) if (t.id <= 0 || !seen.add(t.id)) return true;
        return false;
    }

    /** The same tasks in the same order, each with an id of its own; those that have one keep it. seq is the last id given out. */
    static List<Task> withIds(List<Task> v, long seq) {
        long next = seq;
        for (Task t : v) next = Math.max(next, t.id);
        Set<Long> seen = new HashSet<>();
        List<Task> out = new ArrayList<>();
        for (Task t : v) {
            long id = t.id;
            if (id <= 0 || !seen.add(id)) {
                id = ++next;
                seen.add(id);
            }
            out.add(t.with(t.text, t.done, id));
        }
        return out;
    }

    /** the biggest id in use or given out: the next task gets one more */
    static long lastId(List<Task> v, long seq) {
        long last = seq;
        for (Task t : v) last = Math.max(last, t.id);
        return last;
    }

    /** The list with a new open task, with its reminder, at the end; the same rules as add. */
    static List<Task> addReminder(List<Task> v, String text, long now, long id, int max, long due, int before, int repeat, boolean sound, int where) {
        List<Task> out = add(v, text, now, id, max);
        if (out == null) return null;
        Task t = out.get(out.size() - 1);
        out.set(out.size() - 1, t.remind(due, before, repeat, sound, where));
        return out;
    }

    /** The list with that task's reminder set (due 0 takes it off); null when there is no such task */
    static List<Task> remind(List<Task> v, long id, long due, int before, int repeat, boolean sound, int where) {
        int k = indexOf(v, id);
        if (k < 0) return null;
        List<Task> out = new ArrayList<>(v);
        out.set(k, v.get(k).remind(due, before, repeat, sound, where));
        return out;
    }

    /** The list with that task's card asked again at `until` (0 = no snooze); null when there is no such task */
    static List<Task> snooze(List<Task> v, long id, long until) {
        int k = indexOf(v, id);
        if (k < 0) return null;
        List<Task> out = new ArrayList<>(v);
        out.set(k, v.get(k).snoozed(until));
        return out;
    }

    /**
     * Done at `now`: a repeating reminder is done for this time only (it moves to the next one and stays open), any other task is marked
     * done. null when there is no such task.
     */
    static List<Task> markDone(List<Task> v, long id, long now) {
        int k = indexOf(v, id);
        if (k < 0) return null;
        Task t = v.get(k);
        if (!Reminders.has(t) || t.repeat == Reminders.NO) return markDone(v, id);
        List<Task> out = new ArrayList<>(v);
        out.set(k, t.remind(Reminders.advance(t.due, t.repeat, Math.max(now, t.due)), t.before, t.repeat, t.sound, t.where));
        return out;
    }

    /** The list with a new open task at the end; done tasks make room, oldest first. null when it is full of open ones. */
    static List<Task> add(List<Task> v, String text, long now, long id, int max) {
        List<Task> out = new ArrayList<>(v);
        while (out.size() >= max) {
            int old = -1;
            for (int k = 0; k < out.size() && old < 0; k++) if (out.get(k).done) old = k;
            if (old < 0) return null;
            out.remove(old);
        }
        out.add(new Task(text, false, now, id));
        return out;
    }

    static int indexOf(List<Task> v, long id) {
        if (id <= 0) return -1;
        for (int k = 0; k < v.size(); k++) if (v.get(k).id == id) return k;
        return -1;
    }

    /** the list with that task marked done; null when there is no such task */
    static List<Task> markDone(List<Task> v, long id) {
        int k = indexOf(v, id);
        if (k < 0) return null;
        List<Task> out = new ArrayList<>(v);
        Task t = v.get(k);
        out.set(k, t.with(t.text, true, t.id).snoozed(0));
        return out;
    }

    /** the list without that task; null when there is no such task */
    static List<Task> remove(List<Task> v, long id) {
        int k = indexOf(v, id);
        if (k < 0) return null;
        List<Task> out = new ArrayList<>(v);
        out.remove(k);
        return out;
    }
}
