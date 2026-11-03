package sa.urtv.home;

/** For tests/mute_keep_test.py: MuteKeepRule, the rules that keep a muted TV muted after its own sleep or a restart. Prints PASS/FAIL lines. */
public final class MuteKeepRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    static String d(boolean has, boolean was, int vol0, boolean done, long wakeAt, long now, boolean mutedNow, int vol, boolean adhan) {
        return MuteKeepRule.decide(has, was, vol0, done, wakeAt, now, mutedNow, vol, adhan);
    }

    public static void main(String[] args) {
        long w = 1_000_000;
        check("muted before, the TV unmuted at the wake: mute again", d(true, true, 7, false, w, w + 2_000, false, 7, false).equals("mute"));
        check("muted before and still muted: nothing to do yet, look again", d(true, true, 7, false, w, w + 2_000, true, 7, false).equals("still_muted"));
        check("not muted before: the TV is left alone (the owner's sound is theirs)", d(true, false, 7, false, w, w + 2_000, false, 7, false).equals("not_muted_before"));
        check("nothing remembered: nothing done", d(false, true, 7, false, w, w + 2_000, false, 7, false).equals("no_record"));
        check("at most once per wake: after one mute no more", d(true, true, 7, true, w, w + 9_000, false, 7, false).equals("already_done"));
        check("never during the adhan", d(true, true, 7, false, w, w + 2_000, false, 7, true).equals("adhan"));
        check("never after the owner moved the volume since", d(true, true, 7, false, w, w + 2_000, false, 9, false).equals("volume_changed"));
        check("inside 30 s only: at 28 s yes, after 30 s no (an unmute later is the owner's)", d(true, true, 7, false, w, w + 28_000, false, 7, false).equals("mute") && d(true, true, 7, false, w, w + 30_001, false, 7, false).equals("window_over"));
        check("a clock gone back is no window", d(true, true, 7, false, w, w - 1, false, 7, false).equals("window_over"));
        check("an unknown volume (-1) does not block the mute", d(true, true, -1, false, w, w + 2_000, false, 7, false).equals("mute") && d(true, true, 7, false, w, w + 2_000, false, -1, false).equals("mute"));
        check("the checks are five, increasing, the last inside the window", MuteKeepRule.CHECKS_MS.length == 5 && MuteKeepRule.CHECKS_MS[4] <= MuteKeepRule.WINDOW_MS && MuteKeepRule.CHECKS_MS[0] < MuteKeepRule.CHECKS_MS[1]);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
