package sa.urtv.home.setup;

/**
 * 4.4 beta 4 (review F87): what {@link SetupSession#openAny} does with a 5555 failure. A Cancel on «Allow USB debugging?» is an ANSWER: the paired route is not tried after
 * it, and the failure is thrown exactly as it was (SetupRunner and friendly() read it by its words: «إلغاء» is its own message with its own retry, R-262). Any other
 * failure gets the paired route's reason added, so the screen says why neither answered. No Android in it (tests/PairFallbackRun.java).
 */
final class PairFallback {
    private PairFallback() { }

    /** a Cancel, or the end of the run itself: nothing else is tried */
    static boolean answered(String firstMessage) {
        return AdbClient.cancelled(firstMessage) || "interrupted".equals(firstMessage);
    }

    /** the message of the failure when the paired route was not tried or did not answer either */
    static String message(String firstMessage, String pairedWhy) {
        return answered(firstMessage) ? firstMessage : firstMessage + "; paired route: " + pairedWhy;
    }
}
