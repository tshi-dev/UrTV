package sa.urtv.home.phone;

import java.util.Map;

/**
 * «تحديث تلفزيوني», the decision (PhoneUpdater gathers the facts and acts on the answer; no Android in here). Two steps: first, what the
 * version file and the permission say; then, once the files are downloaded, what the checks on them say. Any doubt is «CANNOT»: the TV shows
 * the message that sends the owner to their AI assistant, and nothing is installed.
 */
public final class UpdateFlow {
    public enum Result { UP_TO_DATE, OFFLINE, NEED_PERMISSION, CONTINUE, FOUND, AVAILABLE, CANNOT }

    /** The answer, and for CANNOT the reason in a word (kept for the log and the phone page, never shown to the owner). */
    public static final class Verdict {
        public final Result result;
        public final String reason;

        Verdict(Result result, String reason) {
            this.result = result;
            this.reason = reason;
        }
    }

    private UpdateFlow() { }

    private static Verdict of(Result r) { return new Verdict(r, ""); }

    private static Verdict cannot(String why) { return new Verdict(Result.CANNOT, why); }

    /** A build number is newer only when it is greater: the same or an older one is never installed (no downgrade). */
    public static boolean isNewer(long candidate, long installed) { return candidate > installed; }

    /**
     * Step one. newest: the build in version.txt, or -1 when it could not be read (no network, a page instead of the file). installed: the
     * lowest build among our apps on this TV. canRequestInstalls: «install unknown apps» is allowed for the launcher (always true before Android 8).
     * CONTINUE means: go on to download and check the files.
     */
    public static Verdict first(int newest, long installed, boolean canRequestInstalls) {
        if (newest < 0) return of(Result.OFFLINE);
        if (!isNewer(newest, installed)) return of(Result.UP_TO_DATE);
        if (!canRequestInstalls) return of(Result.NEED_PERMISSION);
        return of(Result.CONTINUE);
    }

    /**
     * What reading version.txt itself says, before the build number is looked at. reached: the server answered (false only when there is no
     * network); httpCode: its answer; newest: the build read from the body (-1 for a page instead of the file). «OFFLINE» is only for no
     * network: a server that answered with an error or a page is «CANNOT» (download), the owner is not told to check the Wi-Fi.
     */
    public static Verdict versionRead(boolean reached, int httpCode, int newest) {
        if (!reached) return of(Result.OFFLINE);
        if (httpCode != 200 || newest < 0) return cannot("download");
        return of(Result.CONTINUE);
    }

    /**
     * The same for a release that may not exist yet (the apk-beta release, «النسخ التجريبية»): the server answering 404 means nobody promoted a beta build, so
     * there is nothing newer, the same as «لا يوجد أحدث» on the latest channel, never «cannot update here». Any other answer is judged as for the latest.
     */
    public static Verdict versionRead(boolean reached, int httpCode, int newest, boolean mayBeMissing) {
        if (reached && mayBeMissing && httpCode == 404) return of(Result.UP_TO_DATE);
        return versionRead(reached, httpCode, newest);
    }

    /** From the phone page's «افحص التحديث» only version.txt is read: CONTINUE then stops at FOUND (nothing downloaded until «ثبّت»). */
    public static Verdict plan(Verdict first, boolean downloadNow) {
        if (first.result == Result.CONTINUE && !downloadNow) return of(Result.FOUND);
        return first;
    }

    /** Once version.txt was read, a file that could not be fetched (404, a renamed file, too big, empty, a dropped connection) is never «offline». */
    public static Verdict fetchFailed() { return cannot("download"); }

    /** Any other exception while checking, after or before the files: never «offline» either. */
    public static Verdict crashed() { return cannot("exception"); }

    /**
     * What the screen shows for the state the engine reports. «waiting_owner» that is no longer busy (ten minutes without an answer) and has no
     * confirm screen to open again would be a dead end with no button: it is shown as the failure «failed» (its button is «تحقق من التحديث»).
     * Returns {status, reason}.
     */
    public static String[] shown(String status, String reason, boolean busy, boolean confirmKnown) {
        if ("waiting_owner".equals(status) && !busy && !confirmKnown) return new String[] {"error", "failed"};
        return new String[] {status, reason == null ? "" : reason};
    }

    /** Before downloading: Wi-Fi or Ethernet is up, and there is room for every file twice (the download and the installer's copy). */
    public static Verdict room(boolean wifiOrEthernet, long freeBytes, long[] apkBytes) {
        if (!wifiOrEthernet) return cannot("network");
        long need = 0;
        for (long b : apkBytes) need += b;
        if (freeBytes < need * 2) return cannot("space");
        return of(Result.CONTINUE);
    }

    /**
     * After downloading: the answer of UpdateRules.verify for each package, in install order (null = may be installed). A package that is not
     * newer is skipped (it is already up to date), except the launcher: a launcher file that is not newer than the installed one, though
     * version.txt said so, is refused. Any other word (other_key, other_app, unsigned, unreadable) refuses the whole update.
     */
    public static Verdict files(Map<String, String> verifyByPackage) {
        if (verifyByPackage == null || verifyByPackage.isEmpty()) return cannot("nothing");
        boolean launcher = false;
        for (Map.Entry<String, String> e : verifyByPackage.entrySet()) {
            String why = e.getValue();
            boolean isLauncher = UpdateOrder.LAUNCHER.equals(e.getKey());
            if (isLauncher) launcher = true;
            if (why == null) continue;
            if ("not_newer".equals(why) && !isLauncher) continue;
            return cannot(why);
        }
        if (!launcher) return cannot("nothing");
        return of(Result.AVAILABLE);
    }

    /** What an answer of Android's installer (PackageInstaller.EXTRA_STATUS) means: the word for a failure, null for success or «waiting for the owner». */
    public static String failure(int status) {
        switch (status) {
            case 0: return null;           // STATUS_SUCCESS
            case -1: return null;          // STATUS_PENDING_USER_ACTION: the confirm screen is next
            case 2: return "blocked";      // STATUS_FAILURE_BLOCKED: the firmware or a policy refuses
            case 3: return "declined";     // STATUS_FAILURE_ABORTED: the owner pressed Back or Cancel
            case 4: return "invalid";      // STATUS_FAILURE_INVALID
            case 5: return "conflict";     // STATUS_FAILURE_CONFLICT
            case 6: return "storage";      // STATUS_FAILURE_STORAGE
            case 7: return "incompatible"; // STATUS_FAILURE_INCOMPATIBLE
            default: return "failed";
        }
    }

    /** A failure that is the owner's own «no» is not a fault of the TV: no message about the AI assistant for it. */
    public static boolean isDeclined(String reason) { return "declined".equals(reason); }
}
