package sa.urtv.home;

/**
 * What the PIN keypad does with each digit, without Android or timers (PinPrompt holds those; tests/pin_recovery_test.py runs this).
 * Longest 4 (a build with no recovery code, PinRecovery): it gives the digits up on the fourth, as it always did. Longest 8: on the fourth
 * digit it asks for a quiet look at them (PROBE: are they the PIN?); the PIN goes at once, else it keeps listening and gives up what was
 * typed when the keys stop for a moment (idle) or at the eighth digit. The screen only ever shows four dots.
 */
final class PinKeys {
    static final int NONE = 0, SUBMIT = 1, PROBE = 2, WAIT = 3;   // WAIT and PROBE: (re)start the quiet timer for gen()

    private final int longest;
    private final StringBuilder digits = new StringBuilder();
    private boolean done, idleDue;
    private String looked;   // the four digits the last look answered for (it may come back while more are typed, and still count)
    private int gen;

    PinKeys(int longest) { this.longest = Math.max(4, longest); }

    int digit(char d) {
        if (done || digits.length() >= longest || d < '0' || d > '9') return NONE;
        digits.append(d);
        gen++;
        int n = digits.length();
        if (n < 4) return NONE;
        if (n == longest) {
            done = true;
            return SUBMIT;
        }
        if (n == 4) {
            idleDue = false;
            return PROBE;
        }
        return WAIT;
    }

    int erase() {
        if (done) return NONE;
        if (digits.length() > 0) digits.deleteCharAt(digits.length() - 1);
        gen++;
        if (digits.length() < 4) idleDue = false;
        return digits.length() >= 4 ? WAIT : NONE;
    }

    void clear() {
        if (done) return;
        digits.setLength(0);
        gen++;
        idleDue = false;
    }

    /** The look at {@code four} came back. */
    int answer(String four, boolean isPin) {
        if (done) return NONE;
        looked = four;
        if (!four.equals(code())) return NONE;
        if (isPin || idleDue) {
            done = true;
            return SUBMIT;
        }
        return NONE;
    }

    /** The quiet timer started at {@code at} (a gen()) ran out. */
    int idle(int at) {
        if (done || at != gen || digits.length() < 4) return NONE;
        if (digits.length() == 4 && !code().equals(looked)) {
            idleDue = true;   // the look is still out: its answer gives them up
            return NONE;
        }
        done = true;
        return SUBMIT;
    }

    int gen() { return gen; }
    int shown() { return Math.min(4, digits.length()); }
    boolean done() { return done; }
    String code() { return digits.toString(); }

    /** Forget the digits (the keypad closed). */
    void wipe() {
        digits.setLength(0);
        done = true;
    }
}
