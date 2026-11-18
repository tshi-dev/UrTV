package sa.urtv.home.tilawah;

/**
 * 4.4 (a user report on 4.3: «عند تشغيل القرآن من تبويب القراء يحدث تقطيع باستمرار»): what a surah does after its player failed in the middle.
 * Before, it was prepared again after 3 s, and every successful prepare forgave the failures: a connection that drops every minute made a loop of
 * prepare, a minute of sound, a gap, prepare again, for ever. Now it goes on from its place after a growing wait (WAITS_MS), saying
 * «يعيد الاتصال…», and only MAX_FAILS failures within WINDOW_MS are said as a failure (the person's play key, a new pick or the network
 * coming back start again with nothing held against it). Pure logic (no Android classes): tests/surah_recover_test.py runs it.
 */
final class SurahRecover {
    static final long[] WAITS_MS = {1_500, 3_000, 6_000, 12_000, 20_000, 30_000};
    static final int MAX_FAILS = WAITS_MS.length;
    static final long WINDOW_MS = 10 * 60_000L;

    private final long[] at = new long[MAX_FAILS];
    private int n;

    /** a new pick, the play key, the network back: nothing held against it */
    void reset() { n = 0; }

    /** failures counted now (within WINDOW_MS of `nowMs`) */
    int count(long nowMs) { drop(nowMs); return n; }

    /** after a failure at `nowMs`: the wait before it goes on from its place, or -1 when it is said as a failure */
    long after(long nowMs) {
        drop(nowMs);
        if (n >= MAX_FAILS) return -1;
        at[n++] = nowMs;
        return WAITS_MS[n - 1];
    }

    private void drop(long nowMs) {
        int k = 0;
        for (int i = 0; i < n; i++) if (nowMs - at[i] < WINDOW_MS) at[k++] = at[i];
        n = k;
    }
}
