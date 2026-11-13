package sa.urtv.home;

/**
 * 4.3.1: the home and the screensaver are built together (the same versionCode), but a Downloader update brings the home alone, so a TV can have a 4.3 home and a
 * 4.2 screensaver (no «تلفزيونك», no games). When the two builds differ the home says so in one line and OK opens «تحديث تلفزيوني», which updates both. No Android in here.
 */
final class VersionMismatchRule {
    private VersionMismatchRule() { }

    static final int SAME = 0, SAVER_OLDER = 1, HOME_OLDER = 2;

    /** the screensaver's build is 0 when it is not installed (nothing to match); a build of 0 or less for the home is unknown too */
    static int which(long homeBuild, long saverBuild) {
        if (homeBuild <= 0 || saverBuild <= 0 || homeBuild == saverBuild) return SAME;
        return saverBuild < homeBuild ? SAVER_OLDER : HOME_OLDER;
    }
}
