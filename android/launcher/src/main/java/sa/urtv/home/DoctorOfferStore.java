package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 4.3.7: the Android side of DoctorOfferRule. urtv-data's content/doctor.json is read in the background at most every 6 hours and its text kept as it came (only a
 * usable one: DoctorOfferRule.parse); a 404 (the file was taken away) forgets it, a failed fetch keeps the last good one. «لاحقاً» is kept as the build it was pressed
 * for. Nothing here asks for a permission, installs anything or shows anything: the home's top bar does, from the rule's answer.
 */
final class DoctorOfferStore {
    private DoctorOfferStore() { }

    static final String URL_FLAG = "https://raw.githubusercontent.com/tshi-dev/urtv-data/data/content/doctor.json";
    private static final String PREFS = "doctor_offer";
    private static final long EVERY_MS = 6 * 3600_000L;
    private static volatile boolean fetching;

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    /** the kept flag, null when there is none */
    static DoctorOfferRule.Flag flag(Context c) { return DoctorOfferRule.parse(prefs(c).getString("flag", null)); }

    /** the build «لاحقاً» was pressed for, 0 when never */
    static long dismissed(Context c) { return prefs(c).getLong("dismissed_build", 0); }

    /** «لاحقاً»: never again for this build */
    static void dismiss(Context c, long build) { prefs(c).edit().putLong("dismissed_build", build).apply(); }

    /** was this app updated (not freshly installed): its last update is later than its first install */
    static boolean updated(Context c) {
        try {
            PackageInfo p = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return DoctorOfferRule.updated(p.firstInstallTime, p.lastUpdateTime);
        } catch (Exception e) { return false; }
    }

    /** fetch the flag when the kept one is older than 6 hours; {@code changed} runs (on the fetching thread) when what is kept is different now */
    static void refreshIfDue(final Context c, final Runnable changed) {
        final Context app = c.getApplicationContext();
        SharedPreferences sp = prefs(app);
        long now = System.currentTimeMillis(), last = sp.getLong("fetched", 0);
        if (fetching || (last > 0 && now - last < EVERY_MS && last <= now)) return;
        fetching = true;
        Thread t = new Thread(() -> {
            try {
                SharedPreferences p = prefs(app);
                String before = p.getString("flag", null);
                HttpURLConnection h = (HttpURLConnection) new URL(URL_FLAG).openConnection();
                h.setConnectTimeout(8000);
                h.setReadTimeout(8000);
                try {
                    int code = h.getResponseCode();
                    if (code == 200) {
                        try (InputStream in = h.getInputStream()) {
                            String text = new String(Bounded.readAll(in, 4096), "UTF-8");
                            if (DoctorOfferRule.parse(text) != null) p.edit().putString("flag", text).apply();
                        }
                    } else if (code == 404) p.edit().remove("flag").apply();
                } finally { h.disconnect(); }
                p.edit().putLong("fetched", System.currentTimeMillis()).apply();
                String after = p.getString("flag", null);
                if (changed != null && (before == null ? after != null : !before.equals(after))) changed.run();
            } catch (Exception ignored) {
                // offline or refused: the last good copy stays, and the next start asks again after the wait
            } finally { fetching = false; }
        }, "doctor-flag");
        t.setDaemon(true);
        t.start();
    }
}
