package sa.urtv.home;

import android.content.Context;

import sa.urtv.home.tilawah.DayParts;
import sa.urtv.home.tilawah.NowPlaying;

/**
 * The slim bar at the top of the home screen before the adhan: «الأذان بعد ٨ دقائق» in the last 10 minutes before each
 * prayer, «الإقامة بعد ١٢ دقيقة» from the adhan to the iqama. The times are the adhan's own (DayParts), the words
 * AdhanBarText's; «تنبيه قبل الأذان» in the screensaver's settings turns it off (its ConfigProvider, same signing key).
 */
final class AdhanBar {
    private AdhanBar() { }

    /** An adhan for a prayer is playing and the bar is on: the bar names it and says how to stop it (a key stops it: AdhanKeys). */
    static boolean playing(ConfigSnapshot snap) {
        return snap.adhanBarOn && NowPlaying.adhanLive();
    }

    /** What the bar says now, or null: it is off, nothing is near, or the times are out of reach. */
    static String text(Context c, ConfigSnapshot snap) {
        if (!snap.adhanBarOn) return null;
        try {
            if (NowPlaying.adhanLive()) {
                boolean friday = false;
                try { friday = DayParts.now(c, snap.placeFragment()).friday; } catch (RuntimeException ignored) { }
                return AdhanBarText.playing(NowPlaying.adhanPrayer(), friday, NowPlaying.adhanStopKey(c));
            }
            DayParts d = DayParts.now(c, snap.placeFragment());
            return AdhanBarText.text(System.currentTimeMillis(), new long[]{d.fajr, d.dhuhr, d.asr, d.maghrib, d.isha}, d.friday, snap.latin());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
