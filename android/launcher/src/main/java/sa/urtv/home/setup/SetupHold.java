package sa.urtv.home.setup;

/**
 * Whether a setup run is going in this process (from the TV's prompt to the end of the run). Our screensaver asks it when it starts
 * (content://sa.urtv.home.nowplaying/setup, the same-key permission) and steps aside while it is true: a prompt sent while the screensaver showed
 * stayed pending until the TV restarted (tester's diagnosis, 30 Oct), and a TCL ignores the setup screen's keep-the-screen-on flag. It lives in
 * memory only: a process that is gone holds nothing, so the screensaver can never be kept off by a run that ended.
 */
public final class SetupHold {
    private SetupHold() { }

    public static boolean on() { return SetupRunner.get().running(); }
}
