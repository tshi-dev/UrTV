package sa.urtv.home.tilawah;

/** tests/focus_rule_test.py runs this: one line per case, «name<TAB>ok|bad». */
public class FocusRuleRun {
    static void out(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "bad")); }

    public static void main(String[] a) {
        out("a loss for good while playing: paused and watched", FocusRule.on(FocusRule.LOSS, true) == FocusRule.Act.PAUSE_AND_WATCH);
        out("a short loss (a call, the voice search): paused until the focus comes back", FocusRule.on(FocusRule.LOSS_TRANSIENT, true) == FocusRule.Act.PAUSE_UNTIL_GAIN);
        out("may duck: it plays on, quieter", FocusRule.on(FocusRule.LOSS_TRANSIENT_CAN_DUCK, true) == FocusRule.Act.DUCK && FocusRule.DUCK > 0f && FocusRule.DUCK < 1f);
        out("a loss with nothing of ours on: nothing to do", FocusRule.on(FocusRule.LOSS, false) == FocusRule.Act.NOTHING);
        out("the focus back: full sound and on again", FocusRule.on(FocusRule.GAIN, false) == FocusRule.Act.GAIN);

        int quiet = FocusRule.quietAfter(false, FocusRule.MODE_NORMAL, 0);
        out("one quiet look is not enough", FocusRule.look(2_000, false, FocusRule.MODE_NORMAL, 0, true) == FocusRule.Look.AGAIN && quiet == 1);
        out("two quiet looks in a row: the sound is taken back", FocusRule.look(4_000, false, FocusRule.MODE_NORMAL, quiet, true) == FocusRule.Look.TAKE_BACK);
        out("another player sounds: the count starts again", FocusRule.quietAfter(true, FocusRule.MODE_NORMAL, 1) == 0
                && FocusRule.look(4_000, true, FocusRule.MODE_NORMAL, 1, true) == FocusRule.Look.AGAIN);
        out("a call is never talked over", FocusRule.look(10_000, false, 2 /* MODE_IN_CALL */, 5, true) != FocusRule.Look.TAKE_BACK
                && FocusRule.look(10_000, false, 3 /* MODE_IN_COMMUNICATION */, 5, true) != FocusRule.Look.TAKE_BACK);
        out("another player still on after the while: given up, paused until the person plays", FocusRule.look(FocusRule.TAKE_BACK_MS, true, FocusRule.MODE_NORMAL, 0, true) == FocusRule.Look.GIVE_UP);
        out("the person paused or stopped meanwhile: never taken back", FocusRule.look(4_000, false, FocusRule.MODE_NORMAL, 5, false) == FocusRule.Look.GIVE_UP);
        out("the while is short (a minute) and looked at every two seconds", FocusRule.TAKE_BACK_MS == 60_000 && FocusRule.CHECK_MS == 2_000 && FocusRule.QUIET_CHECKS == 2);

        out("the adhan brings back a surah that was playing", FocusRule.onBeforeAdhan(true, false, false));
        out("the adhan brings back a surah still loading with play asked", FocusRule.onBeforeAdhan(false, true, false));
        out("the adhan brings back a surah paused by another sound (was forgotten before)", FocusRule.onBeforeAdhan(false, false, true));
        out("the adhan does not start what the person paused", !FocusRule.onBeforeAdhan(false, false, false));

        out("ours plays without the foreground notice: asked again", FocusRule.promote(true, false));
        out("already foreground, or nothing plays: nothing sent", !FocusRule.promote(true, true) && !FocusRule.promote(false, false));
    }
}
