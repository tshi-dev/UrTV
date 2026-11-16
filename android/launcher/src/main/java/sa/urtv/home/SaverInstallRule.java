package sa.urtv.home;

import java.util.Set;

/**
 * 4.3.2 «ثبّت الجزء الثاني»: UrTV is two apps, and some TVs (a Fire TV, or anyone who installed with Downloader) have only the home. The home's
 * «تلفزيونك», «المدينة», «فرقي» and the settings open screens of the second app (the screensaver, which holds the settings); with it missing
 * they did nothing at all. They now open InstallSaverActivity, which downloads our own release file and hands it to Android's installer.
 * What may be installed, and what to say, with no Android in it (tests/SaverInstallRuleRun.java runs it).
 */
final class SaverInstallRule {
    private SaverInstallRule() { }

    static final String PACKAGE = "sa.urtv.screensaver";
    /** the file of the latest release (the same one the setup and the updater read) */
    static final String URL = "https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-screensaver.apk";
    /** Downloader's code for the same file: the way in when the TV cannot download it itself (digits never turned to Arabic) */
    static final String DOWNLOADER_CODE = "2927902";

    /** a screen of the second app is asked for: it is installed (open it), or the install screen opens instead; never nothing */
    static boolean needsInstall(boolean installed) { return !installed; }

    /**
     * Null when the downloaded file may be handed to Android's installer: it is the second app, and it is signed by exactly the key this app
     * is signed with (so only our own release can be installed from here); else "other_app", "unsigned" or "other_key".
     */
    static String verify(String archivePackage, Set<String> selfSigners, Set<String> archiveSigners) { return verify(PACKAGE, archivePackage, selfSigners, archiveSigners); }

    /** the same check for another of our apps (4.3.3: «إضافات تلفزيونك», sa.urtv.extras) */
    static String verify(String want, String archivePackage, Set<String> selfSigners, Set<String> archiveSigners) {
        if (want == null || !want.equals(archivePackage)) return "other_app";
        if (selfSigners == null || archiveSigners == null || selfSigners.isEmpty() || archiveSigners.isEmpty()) return "unsigned";
        for (String d : selfSigners) if (!d.matches("[0-9a-f]{64}")) return "unsigned";
        for (String d : archiveSigners) if (!d.matches("[0-9a-f]{64}")) return "unsigned";
        return selfSigners.equals(archiveSigners) ? null : "other_key";
    }

    /** the words under the button for a verdict of {@link #verify} */
    static String refusal(String why) {
        return "other_key".equals(why) ? S.t("الملف ليس موقّعًا بمفتاح UrTV، فما ثبّتناه") : S.t("الملف ليس الجزء الثاني من UrTV، فما ثبّتناه");
    }
}
