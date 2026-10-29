package sa.urtv.home;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * One read of the screensaver's ConfigProvider (same signing key) for everything the home screen's minute tick needs: the
 * city's zone, the greeting, the bar before the adhan, the prayer times' place. Each of those used to ask the provider (a
 * cross-process query) for itself, eight times a minute.
 */
final class ConfigSnapshot {
    static final Uri URI = Uri.parse("content://sa.urtv.screensaver.config");

    /** "city=..&lat=..&lon=..&tz=..&..." as the page reads it; null when the screensaver is missing or does not answer */
    final String fragment;
    /** an older screensaver does not say: on, the default */
    final boolean greetingOn, adhanBarOn, listenPillOn, updateBellOn;
    /** «أكمل المشاهدة» under the favourites; an older screensaver does not say: on (the row still needs the TV listings permission) */
    final boolean watchNextOn;
    /** «المظهر»: auto, light or dark (Look.mode); an older screensaver does not say: dark, as the home always was */
    private final String appearance;
    private final String look;   // «المظهر ← الشكل» (GlassStyle.look): an older screensaver says nothing: glass
    /** an old copy, handed out because a read failed: fine to show, never to be taken for an answer (see placeFragment) */
    final boolean stale;
    /** taken before the read that made this began (PlaceTicket): a cached or old copy keeps it, so it never beats a newer read as the place */
    private final long placeTicket;

    private ConfigSnapshot(String fragment, boolean greetingOn, boolean adhanBarOn, boolean listenPillOn, boolean updateBellOn, String appearance,
            boolean watchNextOn, String look, boolean stale, long placeTicket) {
        this.stale = stale;
        this.placeTicket = placeTicket;
        this.look = GlassStyle.look(look);
        this.watchNextOn = watchNextOn;
        this.fragment = fragment;
        this.greetingOn = greetingOn;
        this.adhanBarOn = adhanBarOn;
        this.listenPillOn = listenPillOn;
        this.updateBellOn = updateBellOn;
        this.appearance = Look.mode(appearance);
    }

    /** The same values, marked as an old copy. */
    ConfigSnapshot asStale() { return new ConfigSnapshot(fragment, greetingOn, adhanBarOn, listenPillOn, updateBellOn, appearance, watchNextOn, look, true, placeTicket); }

    /**
     * The settings fragment for the PRAYER PLACE (Place, the adhan alarm, the day parts): null when this is an old copy, so that only a real
     * answer of the screensaver can become the remembered place or move the alarm. Display values (zone, digits, clock) may use the old copy.
     */
    String placeFragment() { return stale ? null : fragment; }

    /** The ticket to give with placeFragment() (Place, DayParts.now, AdhanSync.onConfig). */
    long placeTicket() { return placeTicket; }

    String appearance() { return appearance; }
    String look() { return look; }

    /**
     * One home-screen return reads this four times (the clock, the row of pills, the cards, the digits), each a cross-process query that starts
     * the screensaver's process again when the TV's memory manager has killed it. Reads within this many ms share the one answer; a setting
     * saved a moment ago reaches the screen at the next tick or return, as before.
     */
    static final long SHARE_MS = 3000;

    private static final LastGood<ConfigSnapshot> LAST = new LastGood<>();

    static ConfigSnapshot read(final Context c) {
        final ConfigSnapshot[] none = {null};
        LastGood.Read<ConfigSnapshot> r = LAST.read(android.os.SystemClock.elapsedRealtime(), SHARE_MS, new LastGood.Query<ConfigSnapshot>() {
            @Override public ConfigSnapshot ask() {
                ConfigSnapshot s = query(c);
                if (s.fragment != null) return s;
                none[0] = s;   // the defaults, for when there is nothing older either
                return null;
            }
        });
        // no answer: the last good values stay on the screen (marked old), not the defaults; the next call asks again, so the real ones
        // return as soon as the screensaver answers; with no older copy at all, the defaults as before
        if (r.value == null) return none[0] != null ? none[0] : query(c);
        return r.stale ? r.value.asStale() : r.value;
    }

    private static ConfigSnapshot query(Context c) {
        long ticket = sa.urtv.home.tilawah.PlaceTicket.next();   // before the provider is asked
        String fragment = null;
        boolean greeting = true, bar = true, listenPill = true, updateBell = true, watchNext = true;
        String appearance = null, look = null;
        try (Cursor cur = c.getContentResolver().query(URI, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                fragment = cur.getString(0);
                int g = cur.getColumnIndex("greeting");
                if (g >= 0 && "0".equals(cur.getString(g))) greeting = false;
                int b = cur.getColumnIndex("adhan_bar");
                if (b >= 0 && "0".equals(cur.getString(b))) bar = false;
                int l = cur.getColumnIndex("listen_pill");   // an older screensaver does not say: the button is shown, as it always was
                if (l >= 0 && "0".equals(cur.getString(l))) listenPill = false;
                int u = cur.getColumnIndex("update_bell");   // an older screensaver does not say: on, the default
                if (u >= 0 && "0".equals(cur.getString(u))) updateBell = false;
                int w = cur.getColumnIndex("watch_next");
                if (w >= 0 && "0".equals(cur.getString(w))) watchNext = false;
                int a = cur.getColumnIndex("appearance");
                if (a >= 0) appearance = cur.getString(a);
                int k = cur.getColumnIndex("look");
                if (k >= 0) look = cur.getString(k);
            }
        } catch (RuntimeException ignored) {
            // the screensaver is missing or signed with another key: the defaults (Riyadh, on)
        }
        return new ConfigSnapshot(fragment, greeting, bar, listenPill, updateBell, appearance, watchNext, look, false, ticket);
    }

    /** {city, zone id}; null when the screensaver gave nothing: the TV's own zone then */
    String[] cityZone() { return fragment == null ? null : ClockZone.parse(fragment); }

    /** «&clock=24» is in the fragment only while the setting is on; an older screensaver says nothing: the 12-hour clock */
    boolean clock24() { return fragment != null && (fragment.contains("&clock=24") || fragment.startsWith("clock=24")); }

    boolean latin() { return fragment != null && (fragment.contains("&digits=latin") || fragment.startsWith("digits=latin")); }
}
