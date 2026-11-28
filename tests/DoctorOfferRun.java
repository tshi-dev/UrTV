package sa.urtv.home;

public final class DoctorOfferRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        DoctorOfferRule.Flag on = DoctorOfferRule.parse("{\"on\": true, \"url\": \"https://x/y.apk\", \"sha256\": \"ab\", \"code\": \"123456\"}");
        DoctorOfferRule.Flag off = DoctorOfferRule.parse("{\"on\":false,\"code\":\"123456\"}");
        long build = 700, other = 650;
        check("the flag: on with its code", on != null && on.on && "123456".equals(on.code));
        check("the flag: off, and no flag at all (null, empty, not JSON, no «on», «on» not a boolean), say no card",
            off != null && !off.on && DoctorOfferRule.parse(null) == null && DoctorOfferRule.parse("") == null && DoctorOfferRule.parse("<html>") == null
            && DoctorOfferRule.parse("{\"code\":\"123\"}") == null && DoctorOfferRule.parse("{\"on\":\"yes\"}") == null && DoctorOfferRule.parse("{\"on\":1}") == null);
        check("the code is 1 to 16 letters or digits: anything else is no code", "".equals(DoctorOfferRule.parse("{\"on\":true,\"code\":\"12 34\"}").code)
            && "".equals(DoctorOfferRule.parse("{\"on\":true,\"code\":\"" + "1".repeat(17) + "\"}").code) && "".equals(DoctorOfferRule.parse("{\"on\":true}").code));
        check("a flag text over 4 KB is not read", DoctorOfferRule.parse("{\"on\":true,\"code\":\"1\"}" + " ".repeat(5000)) == null);
        check("the card shows: the flag on with a code, an updated app, the doctor not installed, not kids, not dismissed", DoctorOfferRule.show(on, false, true, false, 0, build));
        check("rule: no flag (missing or unreadable) means no card", !DoctorOfferRule.show(null, false, true, false, 0, build));
        check("rule: the flag off means no card", !DoctorOfferRule.show(off, false, true, false, 0, build));
        check("rule: the flag on without a code means no card (there is nothing to show)", !DoctorOfferRule.show(DoctorOfferRule.parse("{\"on\":true}"), false, true, false, 0, build));
        check("rule: the doctor already installed means no card", !DoctorOfferRule.show(on, true, true, false, 0, build));
        check("rule: a fresh install (never updated) means no card", !DoctorOfferRule.show(on, false, false, false, 0, build));
        check("rule: kids mode means no card", !DoctorOfferRule.show(on, false, true, true, 0, build));
        check("rule: «لاحقاً» means never again for this build, and a later build asks again", !DoctorOfferRule.show(on, false, true, false, build, build) && DoctorOfferRule.show(on, false, true, false, other, build));
        check("updated: a replaced app yes, a first install (the same time, or a few seconds apart) no, an unknown first install no",
            DoctorOfferRule.updated(1_000_000L, 1_000_000L + 3600_000L) && !DoctorOfferRule.updated(1_000_000L, 1_000_000L) && !DoctorOfferRule.updated(1_000_000L, 1_000_000L + 5000L) && !DoctorOfferRule.updated(0, 5_000_000L));
        java.util.Map<String, Object> doc = new java.util.HashMap<>();
        doc.put("v", 1.0); doc.put("on", Boolean.TRUE); doc.put("code", "654321");
        java.util.Map<String, Object> bad = new java.util.HashMap<>();
        bad.put("on", "yes"); bad.put("code", 5.0);
        check("4.4: the flag from the parsed file (a Map): on with its code, a bad code is none, a non-boolean «on» or no map is no flag",
            DoctorOfferRule.fromDoc(doc).on && "654321".equals(DoctorOfferRule.fromDoc(doc).code) && DoctorOfferRule.fromDoc(bad) == null && DoctorOfferRule.fromDoc(null) == null
            && DoctorOfferRule.fromDoc("text") == null && "".equals(DoctorOfferRule.fromDoc(java.util.Collections.singletonMap("on", Boolean.TRUE)).code)
            && "".equals(DoctorOfferRule.fromDoc(new java.util.HashMap<String, Object>() {{ put("on", Boolean.FALSE); put("code", "1 2"); }}).code));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
