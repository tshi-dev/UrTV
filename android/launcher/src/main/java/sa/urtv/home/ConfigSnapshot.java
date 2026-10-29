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

    private ConfigSnapshot(String fragment, boolean greetingOn, boolean adhanBarOn, boolean listenPillOn, boolean updateBellOn, String appearance,
            boolean watchNextOn, String look) {
        this.look = GlassStyle.look(look);
        this.watchNextOn = watchNextOn;
        this.fragment = fragment;
        this.greetingOn = greetingOn;
        this.adhanBarOn = adhanBarOn;
        this.listenPillOn = listenPillOn;
        this.updateBellOn = updateBellOn;
        this.appearance = Look.mode(appearance);
    }

    String appearance() { return appearance; }
    String look() { return look; }

    /**
     * One home-screen return reads this four times (the clock, the row of pills, the cards, the digits), each a cross-process query that starts
     * the screensaver's process again when the TV's memory manager has killed it. Reads within this many ms share the one answer; a setting
     * saved a moment ago reaches the screen at the next tick or return, as before.
     */
    static final long SHARE_MS = 3000;
    private static ConfigSnapshot last;
    private static long lastAt;

    static synchronized ConfigSnapshot read(Context c) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (last != null && now - lastAt >= 0 && now - lastAt < SHARE_MS) return last;
        ConfigSnapshot s = query(c);
        if (s.fragment != null) { last = s; lastAt = now; return s; }
        // no answer (the screensaver was killed and the TV will not start it from the background, or it is missing): the last good values stay
        // on the screen, not the defaults (dark, glass, the TV's own zone); the next call asks again, so the real ones return as soon as it answers
        return last != null ? last : s;
    }

    private static ConfigSnapshot query(Context c) {
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
        return new ConfigSnapshot(fragment, greeting, bar, listenPill, updateBell, appearance, watchNext, look);
    }

    /** {city, zone id}; null when the screensaver gave nothing: the TV's own zone then */
    String[] cityZone() { return fragment == null ? null : ClockZone.parse(fragment); }

    /** «&clock=24» is in the fragment only while the setting is on; an older screensaver says nothing: the 12-hour clock */
    boolean clock24() { return fragment != null && (fragment.contains("&clock=24") || fragment.startsWith("clock=24")); }

    boolean latin() { return fragment != null && (fragment.contains("&digits=latin") || fragment.startsWith("digits=latin")); }
}
