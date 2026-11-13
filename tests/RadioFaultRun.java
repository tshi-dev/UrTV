package sa.urtv.home.tilawah;

/** For tests/radio_fault_test.py: RadioFault's mapping and its report record. One line per check: PASS/FAIL name. */
public final class RadioFaultRun {
    static int failed;
    static void check(String name, boolean ok, Object got) { System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + got)); if (!ok) failed++; }

    public static void main(String[] x) {
        check("no network is «no internet», whatever the player said", RadioFault.reason(false, "io", 0, true, false, 3) == RadioFault.Reason.NO_INTERNET
            && RadioFault.reason(false, "malformed", 404, false, true, 1) == RadioFault.Reason.NO_INTERNET, "");
        check("a server status of 400 or more is «the station did not answer»", RadioFault.reason(true, "io", 404, false, false, 3) == RadioFault.Reason.NO_ANSWER
            && RadioFault.reason(true, "unknown", 503, true, false, 1) == RadioFault.Reason.NO_ANSWER, "");
        check("a format nobody played is «not supported on this TV»", RadioFault.reason(true, "unsupported", 0, false, true, 3) == RadioFault.Reason.UNSUPPORTED
            && RadioFault.reason(true, "malformed", 0, false, true, 3) == RadioFault.Reason.UNSUPPORTED, "");
        check("a format error after it played is not «unsupported»", RadioFault.reason(true, "unsupported", 0, true, true, 1) != RadioFault.Reason.UNSUPPORTED, "");
        check("it played and stopped again and again: «stopped after N tries»", RadioFault.reason(true, "io", 0, true, false, 5) == RadioFault.Reason.RETRIES, "");
        check("it never played and timed out or broke: «did not answer»", RadioFault.reason(true, "timeout", 0, false, false, 3) == RadioFault.Reason.NO_ANSWER
            && RadioFault.reason(true, "io", 0, false, false, 3) == RadioFault.Reason.NO_ANSWER, "");
        check("anything else is the plain «could not play»", RadioFault.reason(true, "unknown", 0, false, false, 3) == RadioFault.Reason.UNKNOWN
            && RadioFault.reason(true, "server_died", 0, false, false, 1) == RadioFault.Reason.UNKNOWN, "");
        check("the lines are Arabic words, the number is Latin, no exception text", RadioFault.message(RadioFault.Reason.RETRIES, 5).equals("توقف البث بعد 5 محاولات")
            && RadioFault.message(RadioFault.Reason.NO_INTERNET, 0).contains("الإنترنت") && !RadioFault.message(RadioFault.Reason.UNKNOWN, 0).contains("Exception"), RadioFault.message(RadioFault.Reason.RETRIES, 5));
        String rec = RadioFault.record("saudi-quran", "io", 404, 34, "OMX.google.aac.decoder");
        check("the record: station, category, HTTP, SDK, decoder", rec.equals("station=saudi-quran category=io http=404 sdk=34 decoder=OMX.google.aac.decoder"), rec);
        String bad = RadioFault.record("https://x.example/a?token=abc", "io", -5, 28, null);
        check("the record never carries an address or a token (only letters, digits and . _ -), a missing value is -", !bad.contains("/") && !bad.contains("?") && !bad.contains(":") && bad.contains("http=0") && bad.contains("decoder=-"), bad);
        check("the waits: 2, 5, 10, 20 s, then every 30 s, never shorter", RadioPolicy.retryMs(1) == 2000 && RadioPolicy.retryMs(2) == 5000 && RadioPolicy.retryMs(3) == 10000
            && RadioPolicy.retryMs(4) == 20000 && RadioPolicy.retryMs(5) == 30000 && RadioPolicy.retryMs(99) == 30000 && RadioPolicy.retryMs(0) == 2000, "");
        RadioPolicy.State never = new RadioPolicy.State();
        check("a station that never played and whose every answer said «format» is final; any other failure is not", RadioPolicy.finalFault(never, 0)
            && RadioPolicy.finalFault(new RadioPolicy.State(), PlayerFault.SERVER_DIED), "");
        never.formatEveryTime = false;
        check("a connection failure is never final, however many times", !RadioPolicy.finalFault(never, 0), "");
        RadioPolicy.State pt = new RadioPolicy.State();
        java.util.List<RadioPolicy.Step> steps = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) steps.add(RadioPolicy.patient(pt, true));
        check("with a second source the sources swap every " + StreamUrl.TRIES + " failures, back and forth, and it keeps going",
            steps.get(2) == RadioPolicy.Step.ALT && steps.get(5) == RadioPolicy.Step.ALT && steps.get(8) == RadioPolicy.Step.ALT && steps.get(0) == RadioPolicy.Step.RETRY && steps.get(4) == RadioPolicy.Step.RETRY, steps);
        RadioPolicy.State single = new RadioPolicy.State();
        boolean onlyRetry = true;
        for (int i = 0; i < 20; i++) onlyRetry &= RadioPolicy.patient(single, false) == RadioPolicy.Step.RETRY;
        check("with no second source it only retries", onlyRetry, "");
        check("the retry line says «نحاول نرجّع البث…» and the reason when there is one",
            RadioFault.retryMessage(RadioFault.Reason.NO_INTERNET).startsWith("نحاول نرجّع البث…") && RadioFault.retryMessage(RadioFault.Reason.NO_ANSWER).contains("لا تردّ")
            && RadioFault.retryMessage(RadioFault.Reason.UNKNOWN).equals("نحاول نرجّع البث…"), "");
        System.exit(failed == 0 ? 0 : 1);
    }
}
