package sa.urtv.screensaver;

/** 4.4 review F3: a copy of an answer the launcher gives (kids mode): only a real answer replaces it; "did not answer" (null) leaves the last one. */
final class KeptAnswer {
    private KeptAnswer() { }

    /** the value to store, or null when nothing is to be written (no answer, or the same as the kept one) */
    static Boolean toStore(Boolean answer, boolean kept) { return answer == null || answer.booleanValue() == kept ? null : answer; }
}
