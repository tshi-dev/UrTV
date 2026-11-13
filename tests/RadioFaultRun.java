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
        System.exit(failed == 0 ? 0 : 1);
    }
}
