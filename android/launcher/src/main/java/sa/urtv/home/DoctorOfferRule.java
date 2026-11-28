package sa.urtv.home;

/**
 * 4.3.7: after an UPDATE, once, the top bar offers the diagnostic app «فحص تلفزيونك» (sa.urtv.doctor). Remote-gated: urtv-data's release/doctor.json (its main keeps it as content/doctor.json)
 * ({"on":true,"code":"<Downloader code>", ...}); no file, a bad one or "on":false = no card, so the release does not wait for the doctor's. As plain code so
 * tests/doctor_offer_test.py runs it without Android. Never: when the doctor is installed, on a fresh install (the first run of an app nobody updated), in
 * kids mode, after «لاحقاً» for this version (a later version asks again), or when the flag has no code to show (the install is Downloader's, no permission
 * of ours: the code is the way).
 */
final class DoctorOfferRule {
    private DoctorOfferRule() { }

    static final String PKG = "sa.urtv.doctor";
    /** the app was updated, not freshly installed: it has been replaced after its first install (a minute of slack: a first install can write both times a moment apart) */
    static final long UPDATED_AFTER_MS = 60_000L;

    /** the flag as read: {@code on} and the Downloader {@code code} (digits or letters, 1 to 16 characters), null when the text is not a usable flag */
    static final class Flag {
        final boolean on; final String code;
        Flag(boolean on, String code) { this.on = on; this.code = code; }
    }

    /** the first {"on": true|false} and {"code": "…"} of the text; null for no text, no «on», or an «on» that is neither true nor false. A bad code is an empty one (nothing to show). */
    static Flag parse(String json) {
        if (json == null || json.length() > 4096) return null;
        java.util.regex.Matcher on = java.util.regex.Pattern.compile("\"on\"\\s*:\\s*(true|false)").matcher(json);
        if (!on.find()) return null;
        java.util.regex.Matcher code = java.util.regex.Pattern.compile("\"code\"\\s*:\\s*\"([0-9A-Za-z]{1,16})\"").matcher(json);
        return new Flag("true".equals(on.group(1)), code.find() ? code.group(1) : "");
    }

    /** 4.4: the flag from urtv-data's parsed file (ContentFile, MiniJson: a Map with "on": Boolean and "code": String); null when it is not a usable flag. A bad code is an empty one. */
    static Flag fromDoc(Object doc) {
        if (!(doc instanceof java.util.Map)) return null;
        Object on = ((java.util.Map<?, ?>) doc).get("on"), code = ((java.util.Map<?, ?>) doc).get("code");
        if (!(on instanceof Boolean)) return null;
        return new Flag((Boolean) on, code instanceof String && ((String) code).matches("[0-9A-Za-z]{1,16}") ? (String) code : "");
    }

    /** was the app updated (not freshly installed): its last update is later than its first install by more than the slack */
    static boolean updated(long firstInstallMs, long lastUpdateMs) { return firstInstallMs > 0 && lastUpdateMs - firstInstallMs > UPDATED_AFTER_MS; }

    /** is the card shown: the flag is on with a code, the doctor is not installed, the app was updated, not kids mode, and «لاحقاً» was not pressed for this build */
    static boolean show(Flag flag, boolean installed, boolean updated, boolean kids, long dismissedBuild, long thisBuild) {
        if (flag == null || !flag.on || flag.code.isEmpty()) return false;
        if (installed || !updated || kids) return false;
        return dismissedBuild != thisBuild;
    }
}
