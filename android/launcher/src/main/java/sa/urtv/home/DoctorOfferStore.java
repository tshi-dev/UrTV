package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;

/**
 * 4.3.7 / 4.4: the Android side of DoctorOfferRule. urtv-data's release/doctor.json (its main keeps it as content/doctor.json) (branch data; "v": 1 like every hand-edited file) is kept by ContentFile (read
 * from the kept copy at once, fetched again in the background at most every 6 hours; a bad answer leaves the last good one). To turn it off publish "on": false.
 * «لاحقاً» is kept as the build it was pressed for (the same preference file as 4.3.7's: a 4.3.7 answer is for 4.3.7's build, and 4.4 asks again). Nothing here
 * asks for a permission, installs anything or shows anything: the home's top bar does, from the rule's answer.
 */
final class DoctorOfferStore {
    private DoctorOfferStore() { }

    static final String PATH = "release/doctor.json", NAME = "doctor.json";
    private static final String PREFS = "doctor_offer";

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    /** the background fetch, at most every 6 hours */
    static void refresh(Context c) { ContentFile.refresh(c.getApplicationContext(), PATH, NAME, 6 * 3600_000L); }

    /** the home's tick (off the UI thread) keeps what the flag says for the suggested apps list: the Downloader code while it is on, else nothing */
    static void keep(Context c, DoctorOfferRule.Flag f) {
        String code = f != null && f.on ? f.code : "";
        SharedPreferences p = prefs(c);
        if (!code.equals(p.getString("listed_code", ""))) p.edit().putString("listed_code", code).apply();
    }

    /** the Downloader code the suggested apps list shows (empty: the tile is hidden, the flag is off or missing) */
    static String listedCode(Context c) { return prefs(c).getString("listed_code", ""); }

    /** the suggested apps list shows «فحص تلفزيونك» only while the flag is on with a code (hidden while off or missing) */
    static boolean listed(Context c) { return !listedCode(c).isEmpty(); }

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
}
