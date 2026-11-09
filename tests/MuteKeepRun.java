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
        // the boot's window is counted from the boot: a process that started at uptime 40 s still guards until 90 s; any other wake keeps 30 s
        long bw = MuteKeepRule.windowFor(true, 40_000), ow = MuteKeepRule.windowFor(false, 40_000);
        check("a boot wake that began at uptime 40 s is guarded until uptime 90 s (50 s), never shorter than 30 s; another wake keeps 30 s",
            bw == 50_000 && MuteKeepRule.windowFor(true, 80_000) == 30_000 && ow == 30_000);
        check("inside the boot window an unmute at uptime 85 s is muted again, at 91 s it is the owner's",
            MuteKeepRule.decide(true, true, 7, false, 40_000, 85_000, false, 7, false, bw).equals("mute") && MuteKeepRule.decide(true, true, 7, false, 40_000, 91_000, false, 7, false, bw).equals("window_over"));
        check("the boot's checks are increasing and the last is inside the longest boot window", MuteKeepRule.BOOT_CHECKS_MS[MuteKeepRule.BOOT_CHECKS_MS.length - 1] <= MuteKeepRule.BOOT_UNTIL_MS);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
