package sa.urtv.home.tilawah;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

/**
 * What the screensaver's «يُتلى الآن» card asks: one row (playing, title, who) while a surah or a station is playing, no row for
 * anything else (nothing on, paused, the adhan). Read-only; only the screensaver, signed with the same key, may ask (the manifest's
 * READ_NOW_PLAYING). Also ".../adhan": one row (prayer, any_key) while an adhan for a prayer plays, which the screensaver's bar shows; and
 * ".../setup_hold": one row (held 1 or 0), 1 while «إعداد تلفزيوني» waits for the TV's debugging prompt (SaverHold), which the screensaver
 * asks when it starts and then ends at once; and call("stop_adhan"), which ends that adhan (the same permission is checked; a key press on the screensaver is the only caller). The screensaver asks when it starts and then once a minute, so nothing is pushed from here.
 * And ".../setup": one row (running) while «إعداد تلفزيوني» runs a setup (SetupHold): the screensaver steps aside then.
 * And ".../adhan_on": one row (on 1 or 0), the adhan's switch, which the screensaver's settings show on their «الأذان» button. Nothing here
 * changes it: that button opens the adhan's page through the home, behind its family lock.
 */
public class NowPlayingProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        if (getContext() == null) return new MatrixCursor(new String[]{"playing", "title", "who"});
        if (uri != null && "setup_hold".equals(uri.getLastPathSegment())) {   // «إعداد تلفزيوني» waits for «السماح بتصحيح USB؟»: no screensaver over it
            MatrixCursor h = new MatrixCursor(new String[]{"held"});
            h.addRow(new Object[]{sa.urtv.home.setup.SaverHold.held() ? 1 : 0});
            return h;
        }
        if (uri != null && "setup".equals(uri.getLastPathSegment())) {
            MatrixCursor h = new MatrixCursor(new String[]{"running"});
            if (sa.urtv.home.setup.SetupHold.on()) h.addRow(new Object[]{1});
            return h;
        }
        if (uri != null && "adhan_on".equals(uri.getLastPathSegment())) {   // the settings' «الأذان» button shows the switch's state (read only)
            MatrixCursor a = new MatrixCursor(new String[]{"on"});
            a.addRow(new Object[]{new Prefs(getContext()).adhanOn() ? 1 : 0});
            return a;
        }
        if (uri != null && "adhan".equals(uri.getLastPathSegment())) {
            MatrixCursor a = new MatrixCursor(new String[]{"prayer", "any_key"});
            if (MusicService.adhanLive()) a.addRow(new Object[]{MusicService.adhanPrayer(), new Prefs(getContext()).adhanStopKey() ? 1 : 0});
            return a;
        }
        MatrixCursor out = new MatrixCursor(new String[]{"playing", "title", "who"});
        NowPlaying.State s = NowPlaying.read(getContext());
        if (s.show && s.playing) out.addRow(new Object[]{1, s.title, s.who});
        return out;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if ("stop_adhan".equals(method) && getContext() != null
                && getContext().checkCallingPermission("sa.urtv.home.READ_NOW_PLAYING") == PackageManager.PERMISSION_GRANTED) {
            if (MusicService.adhanLive()) MusicService.stopAdhan(getContext());
        }
        return null;
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
