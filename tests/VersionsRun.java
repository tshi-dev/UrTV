package sa.urtv.screensaver;

/** For tests/versions_test.py: Versions.java on a plain JVM. Prints PASS/FAIL lines. */
public final class VersionsRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    static boolean newer(String a, String b) { Integer c = Versions.compare(a, b); return c != null && c > 0; }

    public static void main(String[] args) {
        check("3.12 is newer than 3.9 (a number, not text)", newer("3.12", "3.9"), "");
        check("4.0 is newer than 3.12 (major first)", newer("4.0", "3.12"), "");
        check("4.1 is newer than 3.99", newer("4.1", "3.99"), "");
        check("3.9 is not newer than 4.0", !newer("3.9", "4.0"), "");
        check("equal versions are not newer", Versions.compare("3.12", "3.12") == 0, "");
        check("3.5 is older than 3.12", Versions.compare("3.5", "3.12") < 0, "");
        check("a bare 4 is 4.0", Versions.compare("4", "4.0") == 0 && newer("4", "3.30"), "");
        check("what does not parse is null", Versions.parse("abc") == null && Versions.parse(null) == null && Versions.parse("") == null
                && Versions.parse("3.x") == null && Versions.parse("-1.2") == null && Versions.compare("x", "3.1") == null, "");
        check("version.txt: 3.51 with its newline", "3.51".equals(Versions.fromText("3.51\n")), "");
        check("version.txt: a page or an error is not a version", Versions.fromText("<html>404</html>") == null && Versions.fromText("") == null
                && Versions.fromText(null) == null && Versions.fromText("3.51 extra") == null && Versions.fromText("Not Found") == null, "");
        check("redirect to the numbered tag", "3.51".equals(Versions.fromTag("https://github.com/tshi-dev/UrTV/releases/tag/v3.51")), "");
        check("redirect to apk-latest (what GitHub gives today) is not a version", Versions.fromTag("https://github.com/tshi-dev/UrTV/releases/tag/apk-latest") == null
                && Versions.fromTag("https://github.com/tshi-dev/UrTV/releases/tag/apk-next") == null && Versions.fromTag(null) == null, "");
        check("3.100 from the file is newer than 3.99", newer(Versions.fromText("3.100"), "3.99"), "");
        // the name is the VERSION file's and the comparison is by BUILD (version.txt stays "3.<build>")
        check("the build in version.txt: 3.151 gives 151, anything else -1", Versions.buildOf("3.151\n") == 151 && Versions.buildOf("3.9") == 9 && Versions.buildOf("Not Found") == -1
                && Versions.buildOf(null) == -1 && Versions.buildOf("3.151 extra") == -1 && Versions.buildOf("151") == -1, "");
        check("a newer build is newer whatever the marketing name is (4.0 builds 151 and 152)", Versions.buildOf("3.152") > 151 && !(Versions.buildOf("3.151") > 151) && !(Versions.buildOf("3.150") > 151), "");
        check("the name shown to people: «4.0 (بناء 151)», and for the agent «4.0 (build 151)»", "4.0 (بناء 151)".equals(Versions.display("4.0", 151)) && "4.0 (build 151)".equals(Versions.displayEn("4.0", 151)), "");
        check("a named release tag (v4.0.151) gives the version.txt form 3.151; the old v3.51 and apk-latest are as before",
                "3.151".equals(Versions.fromTag("https://github.com/tshi-dev/UrTV/releases/tag/v4.0.151")) && "3.51".equals(Versions.fromTag("https://github.com/tshi-dev/UrTV/releases/tag/v3.51"))
                && Versions.fromTag("https://github.com/tshi-dev/UrTV/releases/tag/v4.0") != null && Versions.fromTag("https://github.com/tshi-dev/UrTV/releases/tag/apk-latest") == null, "");
        // an INSTALLED 3.142 (frozen copy of its Updater: fromText, then major-first compare of "3.N" against its own versionName) fed the new version.txt
        String versionTxt = "3." + 151 + "\n";   // exactly what the workflow writes
        String old = Versions.fromText(versionTxt);
        check("an installed 3.142 reads the new version.txt (\"3.151\") and sees an update", old != null && Versions.compare(old, "3.142") != null && Versions.compare(old, "3.142") > 0, "");
        check("and no update for the same or a newer build of its own", Versions.compare("3.151", "3.151") == 0 && Versions.compare("3.150", "3.151") < 0, "");
        System.exit(failed == 0 ? 0 : 1);
    }
}
