package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import sa.urtv.home.phone.PhoneUpdater;
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
                || UpdateNoticeRule.newer(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("newest", 0), home);
    }

    /** reads version.txt when one is due (off the main thread; {@code done} runs on that thread after a good read) */
    static void checkIfDue(final Context app, final Runnable done) {
        final SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!UpdateNoticeRule.due(sp.getLong("at", 0), System.currentTimeMillis())) return;
        sp.edit().putLong("at", System.currentTimeMillis()).apply();   // asked: the next try is in 12 hours, a failure included
        new Thread(new Runnable() {
            @Override public void run() {
                HttpURLConnection con = null;
                try {
                    con = (HttpURLConnection) new URL(UpdateRules.BASE + "version.txt").openConnection();
                    con.setConnectTimeout(10000);
                    con.setReadTimeout(10000);
                    if (con.getResponseCode() != HttpURLConnection.HTTP_OK) return;
                    byte[] buf = new byte[64];
                    int n = 0, r;
                    try (InputStream in = con.getInputStream()) { while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r; }
                    int build = UpdateRules.buildOf(new String(buf, 0, n, "UTF-8"));
                    if (build > 0) {
                        sp.edit().putInt("newest", build).apply();
                        if (done != null) done.run();
                    }
                } catch (java.io.IOException | RuntimeException ignored) {
                } finally {
                    if (con != null) con.disconnect();
                }
            }
        }, "update-notice").start();
    }
}
