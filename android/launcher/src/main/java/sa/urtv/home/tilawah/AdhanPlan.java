package sa.urtv.home.tilawah;

/**
 * What the adhan's alarm does when it goes off, in one place and with no Android in it (tests/adhan_options_test.py runs it).
 * Which of our screens is in front is read by AdhanFront; here it is only judged.
 */
final class AdhanPlan {
    /** what is in front: our home, the screensaver (the dream or its preview), another app or an input, or not known (the screensaver app did not answer) */
    static final int HOME = 0, DREAM = 1, OTHER = 2, UNKNOWN = 3;

    private AdhanPlan() { }

    /** AdhanFront's two answers as one: our home first; then what the screensaver app said (1 showing, 0 not, anything else not known). */
    static int front(boolean home, int dreaming) {
        if (home) return HOME;
        return dreaming == 1 ? DREAM : dreaming == 0 ? OTHER : UNKNOWN;
    }

    /**
     * «الأذان على شاشة التوقف فقط»: with it on the adhan plays only over our home or the screensaver. When it cannot be known what is in front,
     * the adhan plays: a missed adhan is worse than a heard one.
     */
    static boolean mayPlay(boolean saverOnly, int front) { return !saverOnly || front != OTHER; }

    /**
     * «شاشة التوقف وقت الأذان»: the screensaver starts only when our home is in front. Over the screensaver it is already there, and over
     * another app, an input or when it cannot be known it is never started (it would cover what someone is watching).
     */
    static boolean startSaver(boolean saverAt, int front) { return saverAt && front == HOME; }

    /** what `music.sh adhan` says about the last alarm, so a tester with the sound at 0 can judge by state: «front=other play=0 notice=1 saver=0» */
    static String note(int front, boolean play, boolean notice, boolean saver) {
        return "front=" + (front == HOME ? "home" : front == DREAM ? "dream" : front == OTHER ? "other" : "unknown")
                + " play=" + (play ? 1 : 0) + " notice=" + (notice ? 1 : 0) + " saver=" + (saver ? 1 : 0);
    }
}
