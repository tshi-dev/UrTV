package sa.urtv.home.tilawah;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import sa.urtv.home.R;

/**
 * Where the TV is, for the prayer times: the city picked in the screensaver (its ConfigProvider,
 * same signing key); when a read fails, the last good one this app kept (the TV may refuse to start the screensaver from the background);
 * values in res/values/config.xml only when that app is missing or older and nothing was ever read.
 */
final class Place {
    static final Uri CONFIG = Uri.parse("content://sa.urtv.screensaver.config");

    final double lat, lng;
    final String tz;
    /** «تعديل وقت الصلاة»: minutes added to the five prayers (see PrayerAdjust); zeros unless the settings say so */
    final int[] adj;
    /** the calculation (PrayerMath.Method id): the settings' {@code pm=jafari}, else the city's country's, told by its zone */
    final int method;

    private Place(double lat, double lng, String tz, int[] adj, String pm) {
        this.lat = lat; this.lng = lng; this.tz = tz; this.adj = adj; this.method = PrayerMath.methodFor(tz, pm);
    }

    /** The last good fragment (what the screensaver said last), kept in memory and in this app's preferences. */
    private static String lastGood;
    private static boolean failing;   // a read failed and is not yet well again: the log says it once per streak

    static Place get(Context c) {
        String fresh = null;
        try (Cursor cur = c.getContentResolver().query(CONFIG, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) fresh = cur.getString(0);
        } catch (RuntimeException ignored) {
            // screensaver app missing, signed with another key, or the TV will not start it from the background: the last good place
        }
        return get(c, fresh);
    }

    /** From a settings fragment the caller has already read (one read for a whole tick); null = the read failed: the last good place, the bundled one only if none was ever read. */
    static Place get(Context c, String fragment) {
        PlaceChoice.Pick pick = PlaceChoice.pick(fragment, remembered(c));
        if (pick.fragment == null) return bundled(c);
        if (!pick.remembered) remember(c, pick.fragment);
        note(pick.remembered);
        PlaceFragment f = PlaceFragment.parse(pick.fragment);
        return new Place(f.lat, f.lng, f.tz, f.adj, f.pm);
    }

    private static synchronized String remembered(Context c) {
        if (lastGood == null) lastGood = c.getSharedPreferences("place", Context.MODE_PRIVATE).getString("fragment", null);
        return lastGood;
    }

    private static synchronized void remember(Context c, String fragment) {
        if (fragment.equals(lastGood)) return;
        lastGood = fragment;
        c.getSharedPreferences("place", Context.MODE_PRIVATE).edit().putString("fragment", fragment).apply();
    }

    /** One line (tag UrTVAdhan, no place) when a read failed and the last good place is used, once per streak of failures. */
    private static synchronized void note(boolean usedRemembered) {
        if (usedRemembered && !failing) android.util.Log.i("UrTVAdhan", "place read failed: the last good place is used");
        failing = usedRemembered;
    }

    private static Place bundled(Context c) {
        return new Place(number(c.getString(R.string.latitude), 24.6877),
                number(c.getString(R.string.longitude), 46.7219),
                orDefault(c.getString(R.string.timezone), "Asia/Riyadh"), new int[PrayerAdjust.COUNT], null);
    }

    private static double number(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return fallback; }
    }

    private static String orDefault(String s, String fallback) {
        return s == null || s.trim().isEmpty() ? fallback : s.trim();
    }
}
