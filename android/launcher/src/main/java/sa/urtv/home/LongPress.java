package sa.urtv.home;

/**
 * 4.4 (R-503, docs/research/LAUNCHERS-GITHUB.md idea 6): a long press is known by how long the key was held, not only by its repeats. Many remotes send
 * no repeats, and TCL's Android 12 drops them while any accessibility service filters keys, so a long press that waited for a repeat never came. The
 * first repeat still acts at once (as before); a remote that sends none acts on the release, when it was held LONG_MS or more. No Android in here
 * (tests/LongPressRun.java).
 */
public final class LongPress {
    private LongPress() { }

    /** Android's own long-press time is 400 to 500 ms (RemoteKeys.LONG_MS, BackExit's hold are the same length) */
    public static final long LONG_MS = 500;

    /** the release: a long press when no repeat fired it and the key was held long enough (the remotes that send no repeats) */
    public static boolean onRelease(long eventTime, long downTime, boolean fired) { return !fired && eventTime - downTime >= LONG_MS; }
}
