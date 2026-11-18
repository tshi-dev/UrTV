package sa.urtv.home;

/**
 * When the home offers «تحديث تلفزيوني» (UpdateNotice): one of our apps (the home, the screensaver app, «إضافات تلفزيونك» where it is installed) older than another one or
 * than the newest build of the person's channel (apk-latest, or apk-next for a tester), how often the release is asked, and when the one-time card «في تحديث لـ …» shows.
 * No Android in here (tests/UpdateNoticeRun.java).
 */
final class UpdateNoticeRule {
    private UpdateNoticeRule() { }

    /** 4.4: the release is asked at most every 6 hours (it was 12) */
    static final long EVERY_MS = 6 * 3600_000L;
    /** the card comes back only for a newer build, or after 3 days */
    static final long AGAIN_MS = 3 * 24 * 3600_000L;
    /** behind(): which apps are behind, as bits */
    static final int HOME = 1, SAVER = 2, EXTRAS = 4;

    /** the screensaver is installed and its build is lower than this home's (one release carries both: a TV that updated only one is half old) */
    static boolean older(long saverCode, long homeCode) { return saverCode >= 0 && homeCode > 0 && saverCode < homeCode; }

    /** the release's newest build is higher than this home's */
    static boolean newer(int newestBuild, long homeCode) { return newestBuild > 0 && homeCode > 0 && newestBuild > homeCode; }

    /** a read of the release is due: never made, or older than EVERY_MS (a clock gone back counts as due) */
    static boolean due(long lastAt, long now) { return lastAt <= 0 || now < lastAt || now - lastAt >= EVERY_MS; }

    /** the build every app should be at: the highest of the installed ones and the release's newest (0 when nothing is known) */
    static long target(long home, long saver, long extras, int newest) { return Math.max(Math.max(home, saver), Math.max(extras, (long) newest)); }

    /**
     * Which of our apps are behind the target, as bits (HOME, SAVER, EXTRAS). A build of 0 or less is «not installed» or «not known»: never behind (a missing screensaver
     * app has its own way, «ثبّت الجزء الثاني»; a missing add-on is optional).
     */
    static int behind(long home, long saver, long extras, int newest) {
        long t = target(home, saver, extras, newest);
        int b = 0;
        if (home > 0 && home < t) b |= HOME;
        if (saver > 0 && saver < t) b |= SAVER;
        if (extras > 0 && extras < t) b |= EXTRAS;
        return b;
    }

    /**
     * Does the one-time card show now? Something is behind and: it is on screen in this visit already ({@code onScreen} = its target), or it was never shown for a target this
     * high, or the last showing is AGAIN_MS old (a clock gone back counts as old).
     */
    static boolean cardDue(int behind, long target, long shownTarget, long shownAt, long onScreen, long now) {
        if (behind == 0 || target <= 0) return false;
        if (onScreen == target) return true;
        if (target > shownTarget) return true;
        return shownAt <= 0 || now < shownAt || now - shownAt >= AGAIN_MS;
    }
}
