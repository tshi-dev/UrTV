package sa.urtv.home.phone;

import java.util.Set;

/**
 * «تحديث تلفزيونك», the rules (PhoneUpdater does the downloading and the installing): where the newest APKs are, and when a downloaded one
 * may be handed to Android's installer. It must be the same app, newer than the one installed, and signed with exactly the certificate(s)
 * of the installed one. A different signer is refused: such an APK could never replace the app anyway, and one that claimed to would be an attack.
 * The idea of downloading, verifying and opening Android's own install screen is from itsAbdulelah's pull request #10. No Android in it.
 */
public final class UpdateRules {
    /** The same release optimize/install.sh downloads from (APK_TAG defaults to apk-latest there too). */
    public static final String RELEASES = "https://github.com/tshi-dev/UrTV/releases/download/";
    public static final String BASE = RELEASES + "apk-latest/";
    /** The test channel (agents only, `saver.sh updatechannel next`): the same files from the apk-next release, with the same checks. */
    public static final String NEXT_BASE = RELEASES + "apk-next/";
    /** package, file: the screensaver first, the home screen last (installing it restarts this app). */
    public static final String[][] APKS = {{"sa.urtv.screensaver", "urtv-screensaver.apk"}, {"sa.urtv.home", "urtv-home.apk"}};
    public static final long MAX_APK_BYTES = 80_000_000L;

    private UpdateRules() { }

    /** Where the files are for a channel: "next" is the test release, anything else (nothing, an unknown word) the latest. */
    public static String baseFor(String channel) { return "next".equals(channel) ? NEXT_BASE : BASE; }

    /** Exactly version.txt or one of our two APKs, in one of our two releases, nothing else (the check every download goes through). */
    public static boolean isReleaseUrl(String url) {
        if (url == null) return false;
        for (String base : new String[] {BASE, NEXT_BASE}) {
            if (!url.startsWith(base)) continue;
            String name = url.substring(base.length());
            if (name.equals("version.txt")) return true;
            for (String[] apk : APKS) if (name.equals(apk[1])) return true;
        }
        return false;
    }

    /** The build number in version.txt ("3.51" gives 51), or -1 for anything else. */
    /**
     * 4.3 A5: is an installer's answer for the update that runs now? Only when a package is being installed, the answer names it, and the
     * installer's own package name (when it gives one) is the same. A late answer of an earlier try or one for another package changes nothing.
     */
    public static boolean answerIsOurs(String installing, String pkg, String installerSaid) {
        return installing != null && !installing.isEmpty() && installing.equals(pkg) && (installerSaid == null || installerSaid.equals(pkg));
    }

    public static int buildOf(String versionText) {
        if (versionText == null) return -1;
        String t = versionText.trim();
        if (!t.matches("[0-9]{1,3}\\.[0-9]{1,6}")) return -1;
        return Integer.parseInt(t.substring(t.indexOf('.') + 1));
    }

    /**
     * Null when the downloaded APK may be installed, else why not: "other_app" (not the package expected), "not_newer", "unsigned" (either
     * side has no certificate), "other_key" (the certificates differ). Signers are SHA-256 digests of the certificates, in hex.
     */
    public static String verify(String expectedPackage, String archivePackage, long installedCode, long archiveCode, Set<String> installedSigners, Set<String> archiveSigners) {
        if (expectedPackage == null || !expectedPackage.equals(archivePackage)) return "other_app";
        if (archiveCode <= installedCode) return "not_newer";
        if (installedSigners == null || archiveSigners == null || installedSigners.isEmpty() || archiveSigners.isEmpty()) return "unsigned";
        for (String d : installedSigners) if (!d.matches("[0-9a-f]{64}")) return "unsigned";
        for (String d : archiveSigners) if (!d.matches("[0-9a-f]{64}")) return "unsigned";
        if (!installedSigners.equals(archiveSigners)) return "other_key";
        return null;
    }

    public static String hex(byte[] bytes) {
        StringBuilder h = new StringBuilder();
        for (byte b : bytes) h.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
        return h.toString();
    }
}
