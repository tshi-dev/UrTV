package sa.urtv.home.setup;

import android.content.Context;

/**
 * Puts the stock home back when a home switch was cut off or its safety switch fired. One short attempt per call, run from the alarm, from the TV's
 * start and from the setup screen. Before each attempt the next alarm is already set, with a growing wait (30 s, 1, 2, 4, 8, 10 minutes), so an attempt
 * that is cut short (the TV is not up yet, ADB does not answer, the process is killed) is tried again until the stock home is CHECKED; only then
 * the journal says «restored» and the alarm is cancelled (SetupEngine.recoverIfCutOff).
 */
final class SetupRecovery {
    private SetupRecovery() { }

    static final int GIVE_UP_AFTER = 40;

    static int wait(int n) { return SetupEngine.backoffSeconds(n); }

    static final String GAVE_UP_NOTICE = "الشاشة الأصلية لم تُرجَّع، افتح إعداد تلفزيوني";

    /** True after the automatic attempts ran out: the person is told, at every start of the home and in the setup screen, until it is dealt with. */
    static boolean gaveUp(Context c) {
        try { return Integer.parseInt(String.valueOf(new FileStore(c).get("home-retries.txt")).trim()) >= GIVE_UP_AFTER; } catch (NumberFormatException e) { return false; }
    }

    /** True when there is a switch to recover (the journal says «switching»). */
    static boolean pending(Context c) {
        String j = new FileStore(c).get("home-switch.json");
        return j != null && j.contains("\"state\": \"switching\"");
    }

    /** One attempt. Returns the engine's result, or null when there was nothing to do or the TV did not answer. */
    static SetupEngine.Result attempt(Context c, SetupEngine.Out out) {
        FileStore store = new FileStore(c);
        if (!pending(c)) { new SetupDeadman(c).disarm(); return null; }
        int n = 0;
        try { n = Integer.parseInt(String.valueOf(store.get("home-retries.txt")).trim()); } catch (NumberFormatException ignored) { }
        if (n < GIVE_UP_AFTER) {
            try { store.put("home-retries.txt", (n + 1) + "\n"); } catch (java.io.IOException ignored) { }
            new SetupDeadman(c).arm(wait(n));    // the next try is set before this one starts
        }
        try (SetupSession s = SetupSession.open(c, 5000, null, out)) {
            SetupEngine.Result r = s.engine.recoverIfCutOff();
            if (r != null && !pending(c)) {
                try { store.put("home-retries.txt", ""); } catch (java.io.IOException ignored) { }
                SetupSession.deleteKeyIfFinished(c);
            }
            return r;
        } catch (Exception e) {
            return null;    // the TV did not answer: the alarm set above tries again
        }
    }

    /** After a restart or a start of the setup screen: the alarm comes back at the saved deadline (at once when it is past). */
    static void rearmFromDisk(Context c) {
        if (!pending(c)) return;
        FileStore store = new FileStore(c);
        long deadline = 0;
        try { deadline = Long.parseLong(String.valueOf(store.get("home-deadline.txt")).trim()); } catch (NumberFormatException ignored) { }
        long left = (deadline - System.currentTimeMillis()) / 1000;
        new SetupDeadman(c).arm((int) Math.max(5, Math.min(left, 300)));
    }
}
