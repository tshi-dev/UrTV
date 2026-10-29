package sa.urtv.home.setup;

import java.util.Set;

/**
 * When the setup may hand an APK to the TV's package manager. An update of an installed app goes by «تحديث تلفزيونك»'s rule (phone.UpdateRules.verify:
 * the same app, newer, the very same signer). A FRESH install (the package is not on the TV) has no installed signer to compare with, so it is
 * allowed only when the APK is signed with exactly this launcher's own certificate(s): the screensaver from our release is signed with the same key
 * as this app; anything else (another key, or no signature at all) is refused. Signers are SHA-256 digests of the certificates, in lowercase hex.
 * (Kept in the setup package for now: the phone package is being changed elsewhere; it can move into UpdateRules once that has landed.)
 */
final class InstallRules {
    private InstallRules() { }

    /** Null when the APK may be installed where its package is not installed yet; else "other_app", "unsigned" or "other_key". */
    static String fresh(String expectedPackage, String archivePackage, Set<String> selfSigners, Set<String> archiveSigners) {
        if (expectedPackage == null || !expectedPackage.equals(archivePackage)) return "other_app";
        if (selfSigners == null || archiveSigners == null || selfSigners.isEmpty() || archiveSigners.isEmpty()) return "unsigned";
        for (String d : selfSigners) if (!d.matches("[0-9a-f]{64}")) return "unsigned";
        for (String d : archiveSigners) if (!d.matches("[0-9a-f]{64}")) return "unsigned";
        if (!selfSigners.equals(archiveSigners)) return "other_key";
        return null;
    }
}
