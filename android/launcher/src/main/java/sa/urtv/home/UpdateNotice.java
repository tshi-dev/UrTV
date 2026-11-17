package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import sa.urtv.home.phone.PhoneUpdater;
import sa.urtv.home.phone.UpdateChannel;
import sa.urtv.home.phone.UpdateRules;

/**
 * 4.3: the home says on the TV that an update is worth opening «تحديث تلفزيوني» for: the screensaver app is older than this home (the two come in one release; a
 * TV that updated only one of them is half old), or a newer build is out (version.txt of the release, read at most every 12 hours, off the main thread, only
 * its first 64 bytes). A pill in the top row opens the update screen, which does everything (the checks, the install: nothing is installed from here).
 * The rules (UpdateNoticeRule) are plain Java: tests/UpdateNoticeRun.java runs them.
 */
final class UpdateNotice {
    private UpdateNotice() { }

    static final String PREFS = "update_notice";

    static boolean wanted(Context c) {
        long home = PhoneUpdater.installedCode(c, c.getPackageName());
        return UpdateNoticeRule.older(PhoneUpdater.installedCode(c, "sa.urtv.screensaver"), home)
                || UpdateNoticeRule.newer(newestFor(c, UpdateChannel.cached()), home);   // 4.3.3 hotfix: no question to the screensaver on the UI thread
    }

    /** 4.3.3: the newest build read from the release of THIS channel (apk-beta while «النسخ التجريبية» is on); one read from another channel counts for nothing */
    static int newestFor(Context c, String channel) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return channel.equals(sp.getString("channel", "latest")) ? sp.getInt("newest", 0) : 0;
    }

    /** one release's version.txt: its build, or -1 (no answer, not 200) */
    private static int newestAt(String base) {
        try {
            Net.Reply r = Net.get(base + "version.txt", null, 10000, 12000, 64);   // IPv4 first, 4 s per address, 12 s in all (Net)
            return r.status == 200 ? UpdateRules.buildOf(r.text()) : -1;
        } catch (java.io.IOException | RuntimeException e) {
            return -1;
        }
    }

    /** reads version.txt when one is due (off the main thread; {@code done} runs on that thread after a good read) */
    static void checkIfDue(final Context app, final Runnable done) {
        final SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final String channel = UpdateChannel.cached();   // 4.3.3 hotfix: on the UI thread (the minute tick): the kept word, never a query
        boolean sameChannel = channel.equals(sp.getString("channel", "latest"));   // another channel than the last read: asked again now, not in 12 hours
        if (sameChannel && !UpdateNoticeRule.due(sp.getLong("at", 0), System.currentTimeMillis())) return;
        sp.edit().putLong("at", System.currentTimeMillis()).apply();   // asked: the next try is in 12 hours, a failure included
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    int build = newestAt(UpdateRules.baseFor(channel));
                    // 4.3.4: on «النسخ التجريبية» the newer of apk-beta and apk-latest (a beta user is never left behind an official release)
                    if ("beta".equals(channel)) { int latest = newestAt(UpdateRules.BASE); if (!UpdateRules.betaWins(build, latest)) build = latest; }
                    if (build > 0) {
                        sp.edit().putInt("newest", build).putString("channel", channel).apply();
                        if (done != null) done.run();
                    }
                } catch (java.io.IOException | RuntimeException ignored) {
                }
            }
        }, "update-notice").start();
    }
}
