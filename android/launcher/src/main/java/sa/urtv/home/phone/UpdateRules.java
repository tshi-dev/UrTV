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
    /** The beta channel («النسخ التجريبية»): the apk-beta release, where the owner's promote-beta workflow copies an approved apk-next build. */
    public static final String BETA_BASE = RELEASES + "apk-beta/";
    /** package, file: the screensaver first, «إضافات تلفزيونك» (4.3.3, only where it is installed), the home screen last (installing it restarts this app). */
    public static final String[][] APKS = {{"sa.urtv.screensaver", "urtv-screensaver.apk"}, {"sa.urtv.extras", "urtv-extras.apk"}, {"sa.urtv.home", "urtv-home.apk"}};
    public static final long MAX_APK_BYTES = 80_000_000L;

    /** PhoneUpdater.sizeOf's answer for a file the release does not hold (HTTP 404); -1 stays «not known» (a network error, no size given). */
    public static final long MISSING = -2;

    private UpdateRules() { }

    /**
     * R-288: the home and the screensaver are required (the update fails without their files); every other package (the add-on, «إضافات تلفزيونك») is optional:
     * a release may not hold its file (apk-beta did not), and then it is skipped, never a failure.
     */
    public static boolean isRequired(String pkg) { return "sa.urtv.home".equals(pkg) || "sa.urtv.screensaver".equals(pkg); }

    /** a 404 from a server that answered: the file is not in the release. Any other answer (a 500, a block, no answer at all) is not «missing». */
    public static boolean assetMissing(int httpCode) { return httpCode == 404; }

    /** the packages to update: those installed, except an optional one whose file is missing at the release */
    public static java.util.List<String> wanted(java.util.Collection<String> installed, Set<String> missing) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String p : installed) if (isRequired(p) || missing == null || !missing.contains(p)) out.add(p);
        return out;
    }

    /**
     * The lowest build among the installed packages that count: a package whose file is missing at the release and that is optional is left out (it can never
     * be updated from there, so it must not keep «a newer build» on for ever). 0 when none is installed. Builds below 0 are «not installed».
     */
    public static long lowestInstalled(java.util.Map<String, Long> installedByPackage, Set<String> missing) {
        long low = Long.MAX_VALUE;
        for (java.util.Map.Entry<String, Long> e : installedByPackage.entrySet()) {
            long k = e.getValue() == null ? -1 : e.getValue();
            if (k < 0 || !wanted(java.util.Collections.singleton(e.getKey()), missing).contains(e.getKey())) continue;
            if (k < low) low = k;
        }
        return low == Long.MAX_VALUE ? 0 : low;
    }

    /** Where the files are for a channel: "next" is the test release, "beta" the beta one, anything else (nothing, an unknown word) the latest. */
    public static String baseFor(String channel) { return "next".equals(channel) ? NEXT_BASE : "beta".equals(channel) ? BETA_BASE : BASE; }

    /** Exactly version.txt or one of our APKs, in one of our two releases, nothing else (the check every download goes through). */
    public static boolean isReleaseUrl(String url) {
        if (url == null) return false;
        for (String base : new String[] {BASE, NEXT_BASE, BETA_BASE}) {
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

    /**
     * 4.3.4: «النسخ التجريبية» reads BOTH releases and takes the newer: the beta build only when it is above the latest one. A 404 on apk-beta
     * (nobody promoted a beta) or an older beta: the latest, so a beta user is never left behind an official release. Builds: -1 when not read.
     */
    public static boolean betaWins(int betaBuild, int latestBuild) { return betaBuild > 0 && betaBuild > latestBuild; }

    /** 4.3.4: the beta build is offered when it wins and is newer than the one the person answered «لاحقاً» to (an official build is never held back) */
    public static boolean betaOffered(int betaBuild, int latestBuild, int declined) { return betaWins(betaBuild, latestBuild) && betaBuild > declined; }

    /** 4.3.4: a beta build waits for the TV's question («نسخة تجريبية»): download only once «حمّل» was answered for this very build */
    public static boolean betaAsks(boolean fromBeta, int newest, int agreed) { return fromBeta && newest != agreed; }

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
