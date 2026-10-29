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

    /** the values of res/values/config.xml (nothing was ever read): the alarm set from it is marked in the identity, so any real place differs */
    final boolean bundled;

    private Place(double lat, double lng, String tz, int[] adj, String pm, boolean bundled) {
        this.lat = lat; this.lng = lng; this.tz = tz; this.adj = adj; this.method = PrayerMath.methodFor(tz, pm); this.bundled = bundled;
    }

    /** What an alarm set from this place is recorded as (AdhanScheduler.schedule): "" for the bundled place, else the place, adjustments and method. */
    String identity() {
        return SchedIdentity.of(bundled, PrayerMath.identity(lat, lng, tz, PrayerAdjust.text(adj), method));
    }

    private static final PlaceMemory MEMORY = new PlaceMemory();
    private static final java.util.concurrent.atomic.AtomicLong TICKETS = new java.util.concurrent.atomic.AtomicLong();

    private static PlaceMemory.Store store(final Context c) {
        return new PlaceMemory.Store() {
            @Override public String load() { return c.getSharedPreferences("place", Context.MODE_PRIVATE).getString("fragment", null); }
            @Override public void save(String fragment) { c.getSharedPreferences("place", Context.MODE_PRIVATE).edit().putString("fragment", fragment).apply(); }
        };
    }

    static Place get(Context c) {
        long ticket = TICKETS.incrementAndGet();   // taken before the read starts: a read started later wins over this one
        String fresh = null;
        try (Cursor cur = c.getContentResolver().query(CONFIG, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) fresh = cur.getString(0);
        } catch (RuntimeException ignored) {
            // screensaver app missing, signed with another key, or the TV will not start it from the background: the last good place
        }
        return get(c, fresh, ticket);
    }

    /**
     * From a settings fragment the caller has just read (one read for a whole tick); null = the read failed or only an old copy of it was
     * left (ConfigSnapshot.placeFragment): the last good place, the bundled one only if none was ever read.
     */
    static Place get(Context c, String fragment) { return get(c, fragment, TICKETS.incrementAndGet()); }

    private static Place get(Context c, String fragment, long ticket) {
        PlaceChoice.Pick pick = MEMORY.resolve(store(c), fragment, ticket);
        if (MEMORY.streakStarted(PlaceFragment.parse(fragment) == null, pick.remembered))
            android.util.Log.i("UrTVAdhan", "place read failed: the last good place is used");
        if (pick.fragment == null) return bundled(c);
        PlaceFragment f = PlaceFragment.parse(pick.fragment);
        return new Place(f.lat, f.lng, f.tz, f.adj, f.pm, false);
    }

    private static Place bundled(Context c) {
        return new Place(number(c.getString(R.string.latitude), 24.6877),
                number(c.getString(R.string.longitude), 46.7219),
                orDefault(c.getString(R.string.timezone), "Asia/Riyadh"), new int[PrayerAdjust.COUNT], null, true);
    }

    private static double number(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return fallback; }
    }

    private static String orDefault(String s, String fallback) {
        return s == null || s.trim().isEmpty() ? fallback : s.trim();
    }
}
