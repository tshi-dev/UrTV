package sa.urtv.screensaver;

/**
 * 4.4 (a report: «the screensaver app stopped answering»): the dream asked the launcher on its main thread whether «إعداد تلفزيوني» holds it
 * (setupWaiting, setupRunning). With the launcher's process not running, or busy, that query waits for it to start (seconds; Android's limit is
 * 10 s), with the dream's window up and its main thread stuck: a key then is an ANR. Now the question goes on a thread and the dream starts on its
 * answer, or after WAIT_MS without one; an answer «held» that comes later still ends it. Plain Java (tests/DreamHoldRun.java).
 */
final class DreamHold {
    private DreamHold() { }

    /** how long the dream waits, on its dark ground, for the launcher's answer before it starts the page anyway */
    static final long WAIT_MS = 1500;

    static final int NOTHING = 0, START = 1, END = 2;

    /**
     * What to do when the answer (or the wait's end) comes. attached: the dream is still on; started: the page was already started; answered: a real
     * answer (false for the wait's end); held: the launcher said the setup holds the screensaver.
     */
    static int decide(boolean attached, boolean started, boolean answered, boolean held) {
        if (!attached) return NOTHING;
        if (answered && held) return END;       // never cover the debugging prompt, even when the answer is late
        return started ? NOTHING : START;      // a «not held», or no answer in time: the screensaver as always (once)
    }
}
