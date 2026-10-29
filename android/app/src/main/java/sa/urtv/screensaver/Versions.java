package sa.urtv.screensaver;

/**
 * The apps' version as people read it and as the updater compares it. The NAME is the repository's VERSION file ("4.0") and the build (the run
 * number, the versionCode): «4.0 (build 151)». The comparison is by BUILD, never by name: version.txt stays "3.<build>" for every app that ever
 * shipped (an installed 3.142 reads it the same way), so the build is the part after the dot. Nothing of Android in it (tests/VersionsRun.java).
 */
final class Versions {
    private Versions() { }

    /** {major, minor} of "3.12" (a bare "4" is 4.0); null when it does not parse. */
    static int[] parse(String v) {
        if (v == null) return null;
        String[] part = v.trim().split("\\.", -1);
        if (part.length < 1 || part.length > 3) return null;
        try {
            int major = Integer.parseInt(part[0]), minor = part.length > 1 ? Integer.parseInt(part[1]) : 0;
            return major < 0 || minor < 0 ? null : new int[]{major, minor};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** >0 when a is newer than b, 0 when equal, <0 when older; null when either does not parse. */
    static Integer compare(String a, String b) {
        int[] x = parse(a), y = parse(b);
        if (x == null || y == null) return null;
        return x[0] != y[0] ? Integer.compare(x[0], y[0]) : Integer.compare(x[1], y[1]);
    }

    private static final java.util.regex.Pattern TAG = java.util.regex.Pattern.compile("(?:^|/)v(\\d+\\.\\d+)$");
    private static final java.util.regex.Pattern TAG_NAMED = java.util.regex.Pattern.compile("(?:^|/)v\\d+\\.\\d+\\.(\\d+)$");

    /** The build in a version.txt text or its stored form ("3.51" gives 51); -1 for anything else. */
    static int buildOf(String versionText) {
        String t = fromText(versionText);
        if (t == null) return -1;
        try { return Integer.parseInt(t.substring(t.indexOf('.') + 1)); } catch (NumberFormatException e) { return -1; }
    }

    /** What the person reads, in Arabic: «4.0 (بناء 151)». */
    static String display(String marketing, int build) { return marketing + " (بناء " + build + ")"; }

    /** What the agent reads (saver.sh show): "4.0 (build 151)". */
    static String displayEn(String marketing, int build) { return marketing + " (build " + build + ")"; }

    /** "3.51" from the text of the version.txt asset ("3.51\n"); null when it is anything else (a page, an error). */
    static String fromText(String body) {
        if (body == null) return null;
        String t = body.trim();
        return t.matches("\\d{1,6}\\.\\d{1,6}") && parse(t) != null ? t : null;
    }

    /**
     * "3.51" (the form version.txt has) from where /releases/latest points: the old numbered tag (.../releases/tag/v3.51) or a named one
     * (v4.0.151: the name and the build); null for another tag such as apk-latest.
     */
    static String fromTag(String location) {
        if (location == null) return null;
        java.util.regex.Matcher n = TAG_NAMED.matcher(location.trim());
        if (n.find()) return "3." + n.group(1);
        java.util.regex.Matcher m = TAG.matcher(location.trim());
        return m.find() ? m.group(1) : null;
    }
}
