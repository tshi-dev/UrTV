package sa.urtv.home;

/** When the home offers «تحديث تلفزيوني» (UpdateNotice): the screensaver older than this home, a newer build out, and how often the release is asked. No Android in here. */
final class UpdateNoticeRule {
    private UpdateNoticeRule() { }

    static final long EVERY_MS = 12 * 3600_000L;

    /** the screensaver is installed and its build is lower than this home's (one release carries both: a TV that updated only one is half old) */
    static boolean older(long saverCode, long homeCode) { return saverCode >= 0 && homeCode > 0 && saverCode < homeCode; }

    /** the release's newest build is higher than this home's */
    static boolean newer(int newestBuild, long homeCode) { return newestBuild > 0 && homeCode > 0 && newestBuild > homeCode; }

    /** a read of the release is due: never made, or older than EVERY_MS (a clock gone back counts as due) */
    static boolean due(long lastAt, long now) { return lastAt <= 0 || now < lastAt || now - lastAt >= EVERY_MS; }
}
