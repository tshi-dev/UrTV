package sa.urtv.home.tilawah;

/** 4.4 touch «يبدأ بهدوء»: a dua's sound rises from silence to its level in FadeIn.MS, gently at first (the square of the time). No Android in it. */
final class FadeIn {
    private FadeIn() { }

    static final long MS = 3_000, STEP_MS = 100;

    /** the gain (0..1) this long after the start */
    static float gain(long elapsed) {
        if (elapsed <= 0) return 0f;
        if (elapsed >= MS) return 1f;
        float t = elapsed / (float) MS;
        return t * t;
    }
}
