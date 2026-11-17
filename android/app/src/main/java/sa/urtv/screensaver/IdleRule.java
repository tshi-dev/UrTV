package sa.urtv.screensaver;

/**
 * «بدء شاشة التوقف بعد»: what the TV's idle delay (system screen_off_timeout, in ms) means for the row. A user's TV said 2147483647 (Integer.MAX:
 * the TV turned its screensaver off), and the row showed «2147483 ثانية». No Android in here (tests/idle_rule_test.py runs it).
 */
final class IdleRule {
    private IdleRule() { }

    /** a day or more (Integer.MAX_VALUE is what TVs write for «never»): the screensaver never starts by itself */
    static final long NEVER_FROM_MS = 24L * 60 * 60 * 1000;

    static final int UNKNOWN = 0, NEVER = 1, MINUTES = 2, SECONDS = 3;

    static int kind(long ms) {
        if (ms <= 0) return UNKNOWN;
        if (ms >= NEVER_FROM_MS) return NEVER;
        return ms % 60000 == 0 ? MINUTES : SECONDS;
    }
}
